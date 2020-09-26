package com.hyd.ssdb;

import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.Server;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实现 SsdbClient 的一些底层方法
 *
 * @author Yiding
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractClient {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractClient.class);

    /**
     * 管理所有的 SSDB 连接
     */
    private ConnectionPoolManager connectionPoolManager;

    /**
     * 字符编码
     */
    private Charset charset = DEFAULT_CHARSET;

    //////////////////////////////////////////////////////////////

    public AbstractClient(Sharding sharding) {
        this.connectionPoolManager = new ConnectionPoolManager(sharding);
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public Charset getCharset() {
        return charset;
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

    public Response sendRequest(Server server, String command) {
        Request request = new Request(command);
        request.setForceServer(server);
        return sendRequest(request);
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

        SsdbException.clearThreadLocal();
        boolean needResend;
        Response response = null;

        // 这是一个在失败时重新发送请求的循环。
        // 发送请求会遇到下面几种失败情况，分别有对应的 catch 块：
        // 1、无法获得 Connection，这时候会遇到 SsdbNoServerAvailableException 或 SsdbNoClusterAvailableException，直接抛出；
        // 2、能够获得 Connection，但执行收发时出错，这时候会遇到
        //    SsdbSocketFailedException 异常，需要标记服务器为不可用，并重新尝试循环；
        // 3、执行收发完成，但服务器返回的是错误信息，这时候会遇到 SsdbServerException 异常，直接抛出。
        // 4、其他 SsdbException 或 Exception 异常，表示代码逻辑可能存在问题，直接抛出或包装后抛出。
        do {
            PoolAndConnection poolAndConnection = connectionPoolManager.getConnection(request);
            ConnectionPool connectionPool = null;
            Connection connection = null;
            try {
                connectionPool = poolAndConnection.getConnectionPool();
                connection = poolAndConnection.getConnection();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("choose server " + connection.getHost() + ":" + connection.getPort());
                }

                response = sendRequest(request, connection);
                needResend = false;
            } catch (SsdbServerException | SsdbNoServerAvailableException | SsdbNoClusterAvailableException e) {
                SsdbException.clearThreadLocal();
                throw e;
            } catch (SsdbClientException e) {
                LOG.error("Connection error", e);

                // 标记不可用的服务器，这样下次循环就会切换到其他服务器了
                if (connection != null) {
                    connectionPoolManager.reportInvalidConnection(connection);
                }
                needResend = true;
            } catch (SsdbException e) {
                SsdbException.clearThreadLocal();
                throw e;
            } catch (Exception e) {
                SsdbException.clearThreadLocal();
                throw new SsdbException(e);
            } finally {
                if (connection != null) {
                    connectionPool.returnObject(connection);  // 把连接返回给连接池
                }
            }
        } while (needResend);

        return response;
    }

    protected List<Response> sendRequestToAll(Object... tokens) {
        Request request = new Request(tokens);
        request.setCharset(this.charset);
        return sendRequestToAll(request);
    }

    protected List<Response> sendRequestToAll(Request request) {
        List<PoolAndConnection> pacList = connectionPoolManager.getAllConnections(request);
        List<Response> responseList = new ArrayList<>();

        for (PoolAndConnection pac : pacList) {
            Connection connection = pac.getConnection();

            try {
                responseList.add(sendRequest(request, connection));
            } finally {
                pac.getConnectionPool().returnObject(connection);
            }
        }

        return responseList;
    }

    // 发送一个命令，但不会把连接返回给连接池（内部使用）
    private Response sendRequest(Request request, Connection connection) {
        try {
            byte[] bytes = request.toBytes();
            connection.send(bytes);
            Response response = connection.receivePacket(this.charset);
            checkResponse(request.getHeader().toString(), response);
            return response;

        } catch (SsdbException e) {
            throw e;
        } catch (Exception e) {
            throw new SsdbException(e);
        }
    }

    // 检查服务器回应，如果是错误回应则抛出一个异常
    private static void checkResponse(String requestHeader, Response response) {
        String header = response.getHead().toString();
        LOG.debug("RESPONSE(" + requestHeader + "): [" + header + "] - (" + response.getBody().size() + " blocks)");

        if (!(header.equals("ok") || header.equals("not_found"))) {
            SsdbException e = new SsdbException("Server return error: '" + header + "'");
            e.setServerErrorCode(header);
            throw e;
        }
    }

    /**
     * 将 token 插入到 parameters 的第一位，生成一个新的数组
     *
     * @param token         要插入的内容
     * @param parameterList 参数
     *
     * @return 新生成的数组
     */
    protected String[] prependCommand(String token, List<String> parameterList) {
        String[] parameters = parameterList.toArray(new String[0]);
        String[] command = new String[parameters.length + 1];
        command[0] = token;
        System.arraycopy(parameters, 0, command, 1, parameters.length);
        return command;
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
        return prependCommand(token1, token2, parameters.toArray(new String[0]));
    }

    // 将 token1，token2 和 keyValues 组合成一个字符串数组
    protected Object[] prependCommandKeyValue(String token1, String token2, List<KeyValue> keyValues) {
        Object[] command = new Object[keyValues.size() * 2 + 2];
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
     * 根据 Sharding 对 key 列表进行分组
     *
     * @param keys 要进行分组的 key 列表
     *
     * @return 分组结果
     */
    protected List<String[]> splitKeys(String[] keys) {

        // {clusterId -> List<key>}
        Map<String, List<String>> groups = new HashMap<>();

        for (String key : keys) {
            Cluster cluster = getSharding().getClusterByKey(key);
            String clusterId = cluster.getId();

            if (!groups.containsKey(clusterId)) {
                groups.put(clusterId, new ArrayList<>());
            }

            groups.get(clusterId).add(key);
        }

        ArrayList<String[]> result = new ArrayList<>();
        for (List<String> keyList : groups.values()) {
            result.add(keyList.toArray(new String[0]));
        }

        return result;
    }

    protected List<String[]> splitKeys(List<String> keys) {
        return splitKeys(keys.toArray(new String[0]));
    }

    protected List<String[]> splitKeyValues(String[] keyValues) {

        // {clusterId -> List<key,value,...>}
        Map<String, List<String>> groups = new HashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            Cluster cluster = getSharding().getClusterByKey(key);
            String clusterId = cluster.getId();

            if (!groups.containsKey(clusterId)) {
                groups.put(clusterId, new ArrayList<>());
            }

            groups.get(clusterId).add(key);
            groups.get(clusterId).add(value);
        }

        ArrayList<String[]> result = new ArrayList<>();
        for (List<String> keyList : groups.values()) {
            result.add(keyList.toArray(new String[0]));
        }

        return result;
    }

    protected List<List<KeyValue>> splitKeyValues(List<KeyValue> keyValues) {

        // {clusterId -> List<key,value,...>}
        Map<String, List<KeyValue>> groups = new HashMap<>();

        for (KeyValue keyValue : keyValues) {
            String key = keyValue.getKeyString();
            Cluster cluster = getSharding().getClusterByKey(key);
            String clusterId = cluster.getId();

            if (!groups.containsKey(clusterId)) {
                groups.put(clusterId, new ArrayList<>());
            }

            groups.get(clusterId).add(keyValue);
        }

        return new ArrayList<>(groups.values());
    }

    protected List<String> combineBlocks(List<Response> responseList) {
        List<String> result = new ArrayList<>();
        for (Response response : responseList) {
            result.addAll(response.getBlocks());
        }
        return result;
    }

    protected List<KeyValue> combineKeyValues(List<Response> responseList) {
        List<KeyValue> result = new ArrayList<>();
        for (Response response : responseList) {
            result.addAll(response.getKeyValues());
        }
        return result;
    }
}
