package com.hyd.ssdb;

import com.hyd.ssdb.conf.Sharding;
import com.hyd.ssdb.conn.Connection;
import com.hyd.ssdb.conn.ConnectionPool;
import com.hyd.ssdb.conn.ConnectionPoolManager;
import com.hyd.ssdb.protocol.Request;
import com.hyd.ssdb.protocol.Response;
import com.hyd.ssdb.protocol.WriteRequest;
import org.slf4j.LoggerFactory;

/**
 * 首先 SsdbClient 的一些底层方法
 *
 * @author Yiding
 */
public abstract class AbstractClient {

    static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AbstractClient.class);

    private ConnectionPoolManager connectionPoolManager;

    public AbstractClient(Sharding sharding) {
        this.connectionPoolManager = new ConnectionPoolManager(sharding);
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
        ConnectionPool connectionPool = connectionPoolManager.getConnectionPool(key, write);

        Connection connection = null;
        try {
            connection = getConnection(connectionPool);
            return sendRequest(request, connection);
        } catch (SsdbException e) {
            throw e;
        } catch (Exception e) {
            throw new SsdbException(e);
        } finally {
            if (connection != null) {
                connectionPool.returnObject(connection);  // 把连接返回给连接池
            }
        }

    }

    // 发送一个命令，但不会把连接返回给连接池（内部使用）
    private Response sendRequest(Request request, Connection connection) {
        try {
            connection.send(request.toBytes());
            byte[] respBytes = connection.receivePacket();

            Response response = new Response(respBytes);
            checkResponse(response);
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
    private void checkResponse(Response response) {
        String header = response.getHeader();
        LOG.debug("RESPONSE: [" + header + "] - " + response.getBlocks());

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

    ////////////////////////////////////////////////////////////////

}
