package com.hyd.ssdb;

import com.hyd.ssdb.conf.Sharding;
import com.hyd.ssdb.conn.Connection;
import com.hyd.ssdb.conn.ConnectionPool;
import com.hyd.ssdb.conn.ConnectionPoolManager;
import com.hyd.ssdb.conn.PoolAndConnection;
import com.hyd.ssdb.protocol.Request;
import com.hyd.ssdb.protocol.Response;
import com.hyd.ssdb.protocol.WriteRequest;
import com.hyd.ssdb.util.IdScore;
import com.hyd.ssdb.util.KeyValue;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 实现 SsdbClient 的一些底层方法
 *
 * @author Yiding
 */
public abstract class AbstractClient {

    static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AbstractClient.class);

    /**
     * 管理所有的 SSDB 连接
     */
    private ConnectionPoolManager connectionPoolManager;

    //////////////////////////////////////////////////////////////

    public AbstractClient(Sharding sharding) {
        this.connectionPoolManager = new ConnectionPoolManager(sharding);
    }

    /**
     * 获取负载均衡策略（可用于在运行时变更配置）
     *
     * @return 负载均衡策略对象
     */
    public Sharding getSharding() {
        return this.connectionPoolManager.getSharding();
    }

    /**
     * 发送一整条命令，例如 "set key1 value1"
     *
     * @param command 一整条命令
     *
     * @return 执行结果
     */
    public Response sendRequest(String command) {
        return sendRequest(new Request(command));
    }

    /**
     * 发送包含写入操作的命令，每个参数为命令的一部分，例如 "set", "key1", "value1"
     *
     * @param tokens 命令中的各个部分
     *
     * @return 执行结果
     */
    public Response sendWriteRequest(Object... tokens) {
        return sendRequest(new WriteRequest(tokens));
    }

    /**
     * 发送命令，每个参数为命令的一部分，例如 "set", "key1", "value1"
     *
     * @param tokens 命令中的各个部分
     *
     * @return 执行结果
     */
    public Response sendRequest(Object... tokens) {
        return sendRequest(new Request(tokens));
    }

    /**
     * 发送一个命令
     *
     * @param request 命令对象
     *
     * @return 执行结果
     */
    public Response sendRequest(Request request) {
        String key = request.getKey();
        boolean write = request instanceof WriteRequest;
        boolean needResend = false;
        Response response = null;

        // 这是一个在失败时重新发送请求的循环。
        // 发送请求会遇到下面几种失败情况，分别有对应的 catch 块：
        // 1、无法获得 Connection，这时候会遇到 SsdbNoServerAvailableException 异常，直接抛出；
        // 2、能够获得 Connection，但执行收发时出错，这时候会遇到
        //    SsdbSocketFailedException 异常，需要标记服务器为不可用，并重新尝试循环；
        // 3、执行收发完成，但服务器返回的是错误信息，这时候会遇到 SsdbServerException 异常，直接抛出。
        // 4、其他 SsdbException 或 Exception 异常，表示代码逻辑可能存在问题，直接抛出或包装后抛出。
        do {
            PoolAndConnection poolAndConnection = connectionPoolManager.getConnection(key, write);
            ConnectionPool connectionPool = null;
            Connection connection = null;
            try {
                connectionPool = poolAndConnection.getConnectionPool();
                connection = poolAndConnection.getConnection();
                response = sendRequest(request, connection);
                needResend = false;
            } catch (SsdbNoServerAvailableException e) {
                throw e;
            } catch (SsdbClientException e) {
                LOG.error("Connection error", e);

                // 标记不可用的服务器，这样下次调用 getConnectionPool() 就会切换到其他服务器了
                connectionPoolManager.reportInvalidConnection(connection);
                needResend = true;
            } catch (SsdbServerException e) {
                throw e;
            } catch (SsdbException e) {
                throw e;
            } catch (Exception e) {
                throw new SsdbException(e);
            } finally {
                if (connection != null) {
                    connectionPool.returnObject(connection);  // 把连接返回给连接池
                }
            }
        } while (needResend);

        return response;
    }

    // 发送一个命令，但不会把连接返回给连接池（内部使用）
    private Response sendRequest(Request request, Connection connection) {
        try {
            connection.send(request.toBytes());
            byte[] respBytes = connection.receivePacket();

            Response response = new Response(respBytes);
            checkResponse(request.getHeader().toString(), response);
            return response;

        } catch (SsdbException e) {
            throw e;
        } catch (Exception e) {
            throw new SsdbException(e);
        }
    }

    /**
     * 从连接池获取连接。如果需要登录，则自动发送一个登录请求
     *
     * @param connectionPool 连接池
     *
     * @return 连接
     *
     * @throws Exception 如果获取连接失败
     */
    private Connection getConnection(ConnectionPool connectionPool) throws Exception {

        Connection connection = connectionPool.borrowObject();
        if (connection == null) {
            throw new SsdbException("No available connection");
        }

        String pass = connectionPool.getConnectionFactory().getServer().getPass();
        automaticLogin(pass, connection);
        return connection;
    }

    // 检查并自动发送登录请求
    private void automaticLogin(String pass, Connection connection) {
        boolean loggedIn = connection.hasProperty("authenticated");

        if (!loggedIn) {
            boolean havePass = pass != null;
            if (havePass) {
                sendRequest(new Request("auth", pass));
            }

            // 不管是否真的需要验证，都设置 authenticated 属性为 1，这样下次就跳过这个判断了
            connection.setProperty("authenticated", 1);
        }
    }

    // 检查服务器回应，如果是错误回应则抛出一个异常
    private void checkResponse(String requestHeader, Response response) {
        String header = response.getHeader();
        LOG.debug("RESPONSE(" + requestHeader + "): [" + header + "] - (" + response.getBlocks().size() + " blocks)");

        if (!(header.equals("ok") || header.equals("not_found"))) {
            SsdbException e = new SsdbException("Server return error: '" + header + "'");
            e.setServerErrorCode(header);
            throw e;
        }
    }

    /**
     * 将 token 插入到 parameters 的第一位，生成一个新的数组
     *
     * @param token      要插入的内容
     * @param parameters 参数
     *
     * @return 新生成的数组
     */
    protected String[] prependCommand(String token, String[] parameters) {
        String[] command = new String[parameters.length + 1];
        command[0] = token;
        System.arraycopy(parameters, 0, command, 1, parameters.length);
        return command;
    }

    /**
     * 将两个 token 插入到 parameters 的第一位，生成一个新的数组
     *
     * @param token1     要插入的内容
     * @param token2     要插入的内容
     * @param parameters 参数
     *
     * @return 新生成的数组
     */
    protected String[] prependCommand(String token1, String token2, String[] parameters) {
        String[] command = new String[parameters.length + 2];
        command[0] = token1;
        command[1] = token2;
        System.arraycopy(parameters, 0, command, 2, parameters.length);
        return command;
    }

    // 将 token1，token2 和 parameters 组合成一个字符串数组
    protected String[] prependCommand(String token1, String token2, List<String> parameters) {
        return prependCommand(token1, token2, parameters.toArray(new String[parameters.size()]));
    }

    // 将 token1，token2 和 keyValues 组合成一个字符串数组
    protected String[] prependCommandKeyValue(String token1, String token2, List<KeyValue> keyValues) {
        String[] command = new String[keyValues.size() * 2 + 2];
        command[0] = token1;
        command[1] = token2;

        for (int i = 0; i < keyValues.size(); i++) {
            KeyValue keyValue = keyValues.get(i);
            command[i * 2 + 2] = keyValue.getKey();
            command[i * 2 + 3] = keyValue.getValue();
        }
        return command;
    }

    // 将 token1，token2 和 idScores 组合成一个字符串数组
    protected String[] prependCommandIdScore(String token1, String token2, List<IdScore> idScores) {
        String[] command = new String[idScores.size() * 2 + 2];
        command[0] = token1;
        command[1] = token2;

        for (int i = 0; i < idScores.size(); i++) {
            IdScore idScore = idScores.get(i);
            command[i * 2 + 2] = idScore.getId();
            command[i * 2 + 3] = String.valueOf(idScore.getScore());
        }
        return command;
    }

    /**
     * 关闭 SSDB 客户端
     */
    public void close() {
        this.connectionPoolManager.close();
    }

    /**
     * 如果字符串为 null 或只包含空白字符，则替换为 def；否则返回字符串本身
     *
     * @param s   要替换的字符串
     * @param def 缺省值
     *
     * @return 替换后的字符串
     */
    public String def(String s, String def) {
        if (s == null) {
            return def;
        } else if (s.trim().length() == 0) {
            return def;
        } else {
            return s;
        }
    }
}
