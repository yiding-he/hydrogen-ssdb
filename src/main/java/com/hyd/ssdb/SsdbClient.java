package com.hyd.ssdb;

import com.hyd.ssdb.conf.*;
import com.hyd.ssdb.protocol.Response;
import com.hyd.ssdb.sharding.ConsistentHashSharding;
import com.hyd.ssdb.util.*;
import java.util.*;

/**
 * 包含连接池的客户端类，对于一个 SSDB 服务器只需要创建一个 SsdbClient 客户端。
 * <p>
 * 应用关闭时，需要调用 {@link #close()} 方法释放资源。
 *
 * @author Yiding
 */
@SuppressWarnings({"WeakerAccess", "unused", "RedundantCast"})
public class SsdbClient extends AbstractClient {

    /**
     * 构造方法
     *
     * @param host 服务器地址
     * @param port 服务器端口
     *
     * @throws SsdbException 如果连接服务器失败
     */
    public SsdbClient(String host, int port) throws SsdbException {
        super(new ConsistentHashSharding(Cluster.fromSingleServer(host, port)));
    }

    // 创建只连接到一台服务器的 SsdbClient 对象
    public SsdbClient(String host, int port, int timeoutSeconds) throws SsdbException {
        super(new ConsistentHashSharding(Cluster.fromSingleServer(host, port, timeoutSeconds)));
    }

    // 创建只连接到一台服务器的 SsdbClient 对象
    public SsdbClient(String host, int port, int timeoutSeconds, int bufferSize) throws SsdbException {
        super(new ConsistentHashSharding(Cluster.fromSingleServer(host, port, timeoutSeconds, bufferSize)));
    }

    // 创建只连接到一台服务器的，带密码的 SsdbClient 对象
    public SsdbClient(String host, int port, int timeoutSeconds, String pass) throws SsdbException {
        super(new ConsistentHashSharding(Cluster.fromSingleServer(
            host, port, pass, timeoutSeconds, SocketConfig.DEFAULT_SO_BUFFER_SIZE)));
    }

    // 创建只连接到一台服务器的，带密码的 SsdbClient 对象
    public SsdbClient(String host, int port, String pass) throws SsdbException {
        super(new ConsistentHashSharding(Cluster.fromSingleServer(host, port, pass)));
    }

    // 基于一台服务器的配置创建 SsdbClient 对象
    public SsdbClient(Server server) {
        super(new ConsistentHashSharding(Cluster.fromSingleServer(server)));
    }

    // 创建连接到被 Sharding 托管的服务器集群的 SsdbClient 对象
    public SsdbClient(Sharding sharding) {
        super(sharding);
    }

    // 根据服务器列表，生成权重均等的基于一致性哈希环的分片集群，并创建连接到该集群的 SsdbClient 对象
    public SsdbClient(List<Server> servers) {
        super(new ConsistentHashSharding(Cluster.toClusters(servers)));
    }

    //////////////////////////////////////////////////////////////

    public SsdbClient(String host, int port, String pass, int timeoutSeconds, int poolMaxTotal) throws SsdbException {
        super(new ConsistentHashSharding(new Cluster(
                new Server(host, port, pass, true, timeoutSeconds, poolMaxTotal)
        )));
    }

    public static SsdbClient fromSingleCluster(List<Server> servers) {
        Cluster cluster = Cluster.fromServers(servers);
        return new SsdbClient(new ConsistentHashSharding(cluster));
    }

    public static SsdbClient fromClusters(List<Cluster> clusters) {
        return new SsdbClient(new ConsistentHashSharding(clusters));
    }

    //////////////////////////////////////////////////////////////

    public long dbsize(Server server) {
        Response response = sendRequest(server, "dbsize");
        return Long.parseLong(response.firstBlock());
    }

    public String info(Server server) {
        Response response = sendRequest(server, "info");
        return response.joinBlocks('\n');
    }

    //////////////////////////////////////////////////////////////// key value commands

    public String get(String key) {
        return sendRequest("get", key).firstBlock();
    }

    /**
     * 获取字节内容。注意，只有当 set 进去的内容为 byte[] 时，才应当使用 getBytes() 来取，否则得到的内容很可能是不完整的。
     *
     * @param key 键
     *
     * @return 值
     */
    public byte[] getBytes(String key) {
        return sendRequest("get", key).getBytes();
    }

    public void set(String key, Object value) {
        if (value == null) {
            throw new SsdbException("Cannot save null to SSDB");
        }

        sendWriteRequest("set", key, value);
    }

    public String getset(String key, Object value) {
        if (value == null) {
            throw new SsdbException("Cannot save null to SSDB");
        }

        return sendWriteRequest("getset", key, value).firstBlock();
    }

    public void setx(String key, Object value, int ttlSeconds) {
        if (value == null) {
            throw new SsdbException("Cannot save null to SSDB");
        }

        sendWriteRequest("setx", key, value, ttlSeconds);
    }

    public int setnx(String key, Object value) {
        if (value == null) {
            throw new SsdbException("Cannot save null to SSDB");
        }

        return sendWriteRequest("setnx", key, value).getIntResult();
    }

    public int expire(String key, int ttlSeconds) {
        return sendWriteRequest("expire", key, ttlSeconds).getIntResult();
    }

    public int ttl(String key) {
        return sendRequest("ttl", key).getIntResult();
    }

    /**
     * 删除一个或多个 key，注意这个方法对 zlist 无效，zlist 需要调用
     * {@link #zclear(String)} 方法
     *
     * @param keys 要删除的 key
     */
    public void del(String... keys) {
        if (keys.length == 1) {
            sendWriteRequest("del", keys[0]);

        } else if (keys.length > 1) {
            // 需要根据 Cluster 分组，将适合放到同一个 Cluster 的 key 分到同一组
            List<String[]> keysList = splitKeys(keys);
            for (String[] keyArr : keysList) {
                sendWriteRequest((Object[]) prependCommand("multi_del", keyArr));
            }

        }
    }

    public void del(List<String> keys) {
        if (keys.size() == 1) {
            sendWriteRequest("del", keys.get(0));
        } else {
            // 需要根据 Cluster 分组，将适合放到同一个 Cluster 的 key 分到同一组
            List<String[]> keysList = splitKeys(keys);
            for (String[] keyArr : keysList) {
                sendWriteRequest((Object[]) prependCommand("multi_del", keyArr));
            }
        }
    }

    public long incr(String key) {
        return incr(key, 1);
    }

    public long incr(String key, long incr) {
        return sendWriteRequest("incr", key, incr).getLongResult();
    }

    public boolean exists(String key) {
        return sendRequest("exists", key).getIntResult() > 0;
    }

    public int getbit(String key, long offset) {
        // TO-not-DO ssdb 的这个命令有 BUG，返回值的顺序是反的
        // 这个问题已经无法修复了
        return sendRequest("getbit", key, offset).getIntResult();
    }

    public int setbit(String key, long offset) {
        if (offset > Restrictions.MAX_BIT_OFFSET) {
            throw new SsdbException("Offset too large (>" + Restrictions.MAX_BIT_OFFSET + ")");
        }

        return sendWriteRequest("setbit", key, offset).getIntResult();
    }

    public int bitcount(String key, long start, long end) {
        if (start > Restrictions.MAX_BIT_OFFSET) {
            throw new SsdbException("Start offset too large (>" + Restrictions.MAX_BIT_OFFSET + ")");
        }
        if (end > Restrictions.MAX_BIT_OFFSET) {
            throw new SsdbException("End offset too large (>" + Restrictions.MAX_BIT_OFFSET + ")");
        }
        return sendRequest("bitcount", key, start, end).getIntResult();
    }

    public int countbit(String key, long start, long end) {
        if (start > Restrictions.MAX_BIT_OFFSET) {
            throw new SsdbException("Start offset too large (>" + Restrictions.MAX_BIT_OFFSET + ")");
        }
        if (end > Restrictions.MAX_BIT_OFFSET) {
            throw new SsdbException("End offset too large (>" + Restrictions.MAX_BIT_OFFSET + ")");
        }
        return sendRequest("countbit", key, start, end).getIntResult();
    }

    public String substr(String key, int start, int size) {
        return sendRequest("substr", key, start, size).firstBlock();
    }

    public String substr(String key, int start) {
        return sendRequest("substr", key, start).firstBlock();
    }

    public int strlen(String key) {
        return sendRequest("strlen", key).getIntResult();
    }

    public List<String> keys(String startExclude, String endInclude, int limit) {
        return combineBlocks(sendRequestToAll("keys", startExclude, endInclude, limit));
    }

    public List<String> rkeys(String startExclude, String endInclude, int limit) {
        return sendRequest("rkeys", startExclude, endInclude, limit).getBlocks();
    }

    public List<KeyValue> scan(String startExclude, String endInclude, int limit) {
        return combineKeyValues(sendRequestToAll("scan", startExclude, endInclude, limit));
    }

    public void scan(String prefix, int batchSize, Processor<KeyValue> keyConsumer) {
        String start = prefix;
        String end = prefix + (char) 255;

        List<KeyValue> result = scan(start, end, batchSize);

        while (!result.isEmpty() && start.startsWith(prefix)) {
            for (KeyValue keyValue : result) {
                keyConsumer.process(keyValue);
            }
            start = new String(result.get(result.size() - 1).getKey(), getCharset());
            result = scan(start, end, batchSize);
        }
    }

    public List<KeyValue> rscan(String startExclude, String endInclude, int limit) {
        return sendRequest("rscan", startExclude, endInclude, limit).getKeyValues();
    }

    public List<String> multiGet(String... keys) {

        if (keys == null || keys.length == 0) {
            return Collections.emptyList();
        }

        return multiGet(Arrays.asList(keys));
    }

    public List<byte[]> multiGetBytes(String... keys) {

        if (keys == null || keys.length == 0) {
            return Collections.emptyList();
        }

        return multiGetBytes(Arrays.asList(keys));
    }

    /**
     * 一次取多个 key。注意如果 Sharding 中包含一个以上的 Cluster，那么实际上是对每个 key 执行 get 查询。
     *
     * @param keys key 列表
     *
     * @return value 列表
     */
    public List<String> multiGet(List<String> keys) {

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result;

        if (getSharding().getClusters().size() == 1) {
            result = new ArrayList<>();
            String[] command = prependCommand("multi_get", keys);
            List<KeyValue> keyValues = sendRequest((Object[]) command).getKeyValues();
            for (KeyValue keyValue : keyValues) {
                result.add(keyValue.getValueString());
            }

        } else {
            result = new ArrayList<>();
            for (String key : keys) {
                result.add(get(key));
            }
        }

        return result;
    }

    /**
     * 一次取多个 key。注意如果 Sharding 中包含一个以上的 Cluster，那么实际上是对每个 key 执行 get 查询。
     *
     * @param keys key 列表
     *
     * @return value 列表
     */
    public List<byte[]> multiGetBytes(List<String> keys) {

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<byte[]> result;

        if (getSharding().getClusters().size() == 1) {
            result = new ArrayList<>();
            String[] command = prependCommand("multi_get", keys);
            List<KeyValue> keyValues = sendRequest((Object[]) command).getKeyValues();
            for (KeyValue keyValue : keyValues) {
                result.add(keyValue.getValue());
            }

        } else {
            result = new ArrayList<>();
            for (String key : keys) {
                result.add(getBytes(key));
            }
        }

        return result;
    }

    public void multiSet(String... keyValues) {

        if (keyValues == null || keyValues.length == 0) {
            return;
        }

        if (keyValues.length % 2 == 1) {
            throw new SsdbException("Length of parameters must be odd");
        }

        List<String[]> keyValuesList = splitKeyValues(keyValues);
        for (String[] keyValueArr : keyValuesList) {
            String[] command = prependCommand("multi_set", keyValueArr);
            sendWriteRequest((Object[]) command);
        }
    }

    public void multiSet(List<KeyValue> keyValues) {

        if (keyValues == null || keyValues.isEmpty()) {
            return;
        }

        // 每个元素对应一个 Cluster
        List<List<KeyValue>> keyValueLists = splitKeyValues(keyValues);

        for (List<KeyValue> keyValueList : keyValueLists) {
            Object[] command = new Object[keyValueList.size() * 2 + 1];
            command[0] = "multi_set";

            for (int i = 0; i < keyValueList.size(); i++) {
                KeyValue keyValue = keyValueList.get(i);
                command[i * 2 + 1] = keyValue.getKey();
                command[i * 2 + 2] = keyValue.getValue();
            }

            sendWriteRequest(command);
        }
    }

    //////////////////////////////////////////////////////////////// hashmap commands

    public void hset(String key, String propName, String propValue) {
        sendWriteRequest("hset", key, propName, propValue);
    }

    public String hget(String key, String propName) {
        return sendRequest("hget", key, propName).firstBlock();
    }

    public int hdel(String key, String propName) {
        return sendWriteRequest("hdel", key, propName).getIntResult();
    }

    public long hincr(String key, String propName) {
        return hincr(key, propName, 1);
    }

    public long hincr(String key, String propName, long incr) {
        return sendWriteRequest("hincr", key, propName, incr).getLongResult();
    }

    public boolean hexists(String key, String propName) {
        return sendRequest("hexists", key, propName).getIntResult() > 0;
    }

    public int hsize(String key) {
        return sendRequest("hsize", key).getIntResult();
    }

    public List<String> hlist(String startExclude, String endInclude, int limit) {
        return combineBlocks(sendRequestToAll("hlist", startExclude, endInclude, limit));
    }

    public List<String> hrlist(String startExclude, String endInclude, int limit) {
        return combineBlocks(sendRequestToAll("hrlist", startExclude, endInclude, limit));
    }

    public List<String> hkeys(String key, String startExclude, String endInclude, int limit) {
        return sendRequest("hkeys", key, startExclude, endInclude, limit).getBlocks();
    }

    public List<KeyValue> hgetall(String key) {
        return sendRequest("hgetall", key).getKeyValues();
    }

    public Map<String, String> hgetallmap(String key) {
        return sendRequest("hgetall", key).getBlocksAsStringMap(getCharset());
    }

    public List<KeyValue> hscan(String key, String startExclude, String endInclude, int limit) {
        return sendRequest("hscan", key, startExclude, endInclude, limit).getKeyValues();
    }

    public List<KeyValue> hrscan(String key, String startExclude, String endInclude, int limit) {
        return sendRequest("hrscan", key, startExclude, endInclude, limit).getKeyValues();
    }

    public int hclear(String key) {
        return sendWriteRequest("hclear", key).getIntResult();
    }

    public void multiHset(String key, String... props) {
        if (props.length % 2 == 1) {
            throw new SsdbException("Length of props must be odd");
        }

        String[] command = prependCommand("multi_hset", key, props);
        sendWriteRequest((Object[]) command);
    }

    public void multiHset(String key, List<KeyValue> props) {
        sendWriteRequest((Object[]) prependCommandKeyValue("multi_hset", key, props));
    }

    public List<KeyValue> multiHget(String key, String... propNames) {
        return multiHget(key, Arrays.asList(propNames));
    }

    public List<KeyValue> multiHget(String key, List<String> propNames) {
        return sendRequest((Object[]) prependCommand("multi_hget", key, propNames)).getKeyValues();
    }

    public void multiHdel(String key, String... propNames) {
        sendWriteRequest((Object[]) prependCommand("multi_hdel", key, propNames));
    }

    //////////////////////////////////////////////////////////////// sorted set

    public void zset(String key, String id, long score) {
        sendWriteRequest("zset", key, id, score);
    }

    public Long zget(String key, String id) {
        return sendRequest("zget", key, id).getLongResult();
    }

    public void zdel(String key, String id) {
        sendWriteRequest("zdel", key, id);
    }

    public long zincr(String key, String id, long incr) {
        return sendWriteRequest("zincr", key, id, incr).getLongResult();
    }

    public int zsize(String key) {
        return sendRequest("zsize", key).getIntResult();
    }

    public int zclear(String key) {
        return sendWriteRequest("zclear", key).getIntResult();
    }

    public List<String> zlist(String startExclude, String endInclude, int limit) {
        return sendRequest("zlist", startExclude, endInclude, limit).getBlocks();
    }

    public List<String> zkeys(String key, String keyStartExclude, Long scoreStartInclude, Long scoreEndInclude,
                              int limit) {
        return sendRequest("zscan", key,
            Str.ifBlank(keyStartExclude, ""),
            Num.ifNull(scoreStartInclude, ""),
            Num.ifNull(scoreEndInclude, ""),
            limit
        ).getIds();
    }

    /**
     * (这命令真别扭)
     *
     * @param key               zset 的 key
     * @param keyStartExclude   （可选）key 的开始值
     * @param scoreStartInclude （可选）score 的最小值，为空表示无限小(-inf)
     * @param scoreEndInclude   （可选）score 的最大值，为空表示无限大(+inf)
     * @param limit             最多返回记录数
     *
     * @return 查询结果
     */
    public List<IdScore> zscan(String key, String keyStartExclude, Long scoreStartInclude, Long scoreEndInclude,
                               int limit) {
        return sendRequest("zscan", key,
            Str.ifBlank(keyStartExclude, ""),
            Num.ifNull(scoreStartInclude, ""),
            Num.ifNull(scoreEndInclude, ""),
            limit
        ).getIdScores();
    }

    public List<IdScore> zrscan(String key, String keyStartExclude, Long scoreStartInclude, Long scoreEndInclude,
                                int limit) {
        return sendRequest("zrscan", key,
            Str.ifBlank(keyStartExclude, ""),
            Num.ifNull(scoreStartInclude, ""),
            Num.ifNull(scoreEndInclude, ""),
            limit
        ).getIdScores();
    }

    /**
     * 获取 id 的排名，从小到大，0 表示第一位
     * <p>
     * 注意，根据文档 http://www.ideawu.net/blog/archives/752.html ，该命令存在性能问题，请慎用
     *
     * @param key id 所处的 zset 的 key
     * @param id  id
     *
     * @return 排名，如果 id 不在 key 当中则返回 -1
     */
    public int zrank(String key, String id) {
        return sendRequest("zrank", key, id).getIntResult(-1);
    }

    // 同上，从大到小排名
    public int zrrank(String key, String id) {
        return sendRequest("zrrank", key, id).getIntResult(-1);
    }

    public List<IdScore> zrange(String key, int offset, int limit) {
        return sendRequest("zrange", key, offset, limit).getIdScores();
    }

    public List<IdScore> zrrange(String key, int offset, int limit) {
        return sendRequest("zrrange", key, offset, limit).getIdScores();
    }

    /**
     * 查询 score 在 minScoreInclude 与 maxScoreInclude 之间的 id 数量
     *
     * @param key             zset 的 key
     * @param minScoreInclude score 最小值（含），Integer.MIN_VALUE 表示无最小值
     * @param maxScoreInclude score 最大值（含），Integer.MAX_VALUE 表示无最大值
     *
     * @return score 在 minScoreInclude 与 maxScoreInclude 之间的 id 数量
     */
    public int zcount(String key, int minScoreInclude, int maxScoreInclude) {
        String strMin = minScoreInclude == Integer.MIN_VALUE ? "" : String.valueOf(minScoreInclude);
        String strMax = maxScoreInclude == Integer.MAX_VALUE ? "" : String.valueOf(maxScoreInclude);
        return sendRequest("zcount", key, strMin, strMax).getIntResult(0);
    }

    public Long zsum(String key, int minScoreInclude, int maxScoreInclude) {
        String strMin = minScoreInclude == Integer.MIN_VALUE ? "" : String.valueOf(minScoreInclude);
        String strMax = maxScoreInclude == Integer.MAX_VALUE ? "" : String.valueOf(maxScoreInclude);
        return sendRequest("zsum", key, strMin, strMax).getLongResult(0);
    }

    public long zavg(String key, int minScoreInclude, int maxScoreInclude) {
        String strMin = minScoreInclude == Integer.MIN_VALUE ? "" : String.valueOf(minScoreInclude);
        String strMax = maxScoreInclude == Integer.MAX_VALUE ? "" : String.valueOf(maxScoreInclude);
        return sendRequest("zavg", key, strMin, strMax).getLongResult(0);
    }

    /**
     * 删除指定排名范围内的 id
     *
     * @param key            zset 的 key
     * @param minRankInclude 最小排名（含），最小值为 0
     * @param maxRankInclude 最大排名（含），最大值为 zset 的大小
     *
     * @return 被删除的 id 的数量
     */
    public int zremrangebyrank(String key, int minRankInclude, int maxRankInclude) {
        return sendWriteRequest("zremrangebyrank", key, minRankInclude, maxRankInclude).getIntResult();
    }

    public int zremrangebyscore(String key, int minScoreInclude, int maxScoreInclude) {
        String strMin = minScoreInclude == Integer.MIN_VALUE ? "" : String.valueOf(minScoreInclude);
        String strMax = maxScoreInclude == Integer.MAX_VALUE ? "" : String.valueOf(maxScoreInclude);
        return sendWriteRequest("zremrangebyscore", key, strMin, strMax).getIntResult();
    }

    public List<IdScore> zpopFront(String key, int limit) {
        return sendWriteRequest("zpop_front", key, limit).getIdScores();
    }

    public List<IdScore> zpopBack(String key, int limit) {
        return sendWriteRequest("zpop_back", key, limit).getIdScores();
    }

    public long multiZset(String key, IdScore... idScores) {
        return multiZset(key, Arrays.asList(idScores));
    }

    public long multiZset(String key, List<IdScore> idScores) {
        return sendWriteRequest((Object[]) prependCommandIdScore("multi_zset", key, idScores)).getLongResult(0);
    }

    public List<IdScore> multiZget(String key, List<String> ids) {
        return sendRequest((Object[]) prependCommand("multi_zget", key, ids)).getIdScores();
    }

    public List<IdScore> multiZget(String key, String... ids) {
        return multiZget(key, Arrays.asList(ids));
    }

    public void multiZdel(String key, List<String> ids) {
        sendWriteRequest((Object[]) prependCommand("multi_zdel", key, ids));
    }

    //////////////////////////////////////////////////////////////

    public int qpushFront(String key, String... values) {
        return sendWriteRequest((Object[]) prependCommand("qpush_front", key, values)).getIntResult();
    }

    public int qpushFront(String key, byte[] bytes) {
        return sendWriteRequest("qpush_front", key, bytes).getIntResult();
    }

    public int qpushBack(String key, String... values) {
        return sendWriteRequest((Object[]) prependCommand("qpush_back", key, values)).getIntResult();
    }

    public int qpushBack(String key, byte[] bytes) {
        return sendWriteRequest("qpush_back", key, bytes).getIntResult();
    }

    public List<String> qpopFront(String key, int size) {
        return sendWriteRequest("qpop_front", key, size).getBlocks();
    }

    public List<String> qpopBack(String key, int size) {
        return sendWriteRequest("qpop_back", key, size).getBlocks();
    }

    public void qpopAllFront(String key, int batchSize, Processor<String> valueProcessor) {
        while (qsize(key) > 0) {
            List<String> values = qpopFront(key, batchSize);
            for (String value : values) {
                valueProcessor.process(value);
            }
        }
    }

    public void qpopAllBack(String key, int batchSize, Processor<String> valueProcessor) {
        while (qsize(key) > 0) {
            List<String> values = qpopBack(key, batchSize);
            for (String value : values) {
                valueProcessor.process(value);
            }
        }
    }

    public String qfront(String key) {
        return sendRequest("qfront", key).firstBlock();
    }

    public String qback(String key) {
        return sendRequest("qback", key).firstBlock();
    }

    public int qsize(String key) {
        return sendRequest("qsize", key).getIntResult();
    }

    public void qclear(String key) {
        sendWriteRequest("qclear", key);
    }

    public String qget(String key, int index) {
        return sendRequest("qget", key, index).firstBlock();
    }

    public byte[] qgetBytes(String key, int index) {
        return sendRequest("qget", key, index).getBytes();
    }

    public void qset(String key, int index, String value) {
        sendWriteRequest("qset", key, index, value);
    }

    public List<String> qrange(String key, int offset, int limit) {
        return sendRequest("qrange", key, offset, limit).getBlocks();
    }

    public List<String> qslice(String key, int startInclude, int endInclude) {
        return sendRequest("qslice", key, startInclude, endInclude).getBlocks();
    }

    public int qtrimFront(String key, int size) {
        return sendWriteRequest("qtrim_front", key, size).getIntResult();
    }

    public int qtrimBack(String key, int size) {
        return sendWriteRequest("qtrim_back", key, size).getIntResult();
    }

    /**
     * 列出指定区间的 queue/list 的 key 列表
     *
     * @param startKeyExclude 起始名字（不含，可选）
     * @param endKeyInclude   结束名字（含，可选）
     * @param limit           最多返回记录数
     *
     * @return 指定区间的 queue/list 的 key 列表
     */
    public List<String> qlist(String startKeyExclude, String endKeyInclude, int limit) {
        return sendRequest("qlist", startKeyExclude, endKeyInclude, limit).getBlocks();
    }

    public List<String> qrlist(String startKeyExclude, String endKeyInclude, int limit) {
        return sendRequest("qrlist", startKeyExclude, endKeyInclude, limit).getBlocks();
    }
}
