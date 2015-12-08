package com.hyd.ssdb;

import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.conf.Sharding;
import com.hyd.ssdb.protocol.Response;
import com.hyd.ssdb.util.IdScore;
import com.hyd.ssdb.util.KeyValue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 包含连接池的客户端类，对于一个 SSDB 服务器只需要创建一个 SsdbClient 客户端。
 * <p/>
 * 应用关闭时，需要调用 {@link #close()} 方法释放资源。
 *
 * @author Yiding
 */
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
        super(Sharding.fromSingleServer(host, port));
    }

    public SsdbClient(String host, int port, String pass) throws SsdbException {
        super(Sharding.fromSingleServer(host, port, pass));
    }

    //////////////////////////////////////////////////////////////

    public SsdbClient(String host, int port, String pass, int soTimeout, int poolMaxTotal) throws SsdbException {
        super(Sharding.fromSingleServer(host, port, pass, soTimeout, poolMaxTotal));
    }

    public SsdbClient(Sharding sharding) {
        super(sharding);
    }

    public SsdbClient(List<Server> servers) {
        super(Sharding.fromServerList(servers));
    }

    public SsdbClient(Server server) {
        super(Sharding.fromSingleServer(server));
    }

    public static SsdbClient fromSingleCluster(List<Server> servers) {
        return new SsdbClient(Sharding.fromServerList(servers));
    }

    public static SsdbClient fromClusters(List<Cluster> clusters) {
        return new SsdbClient(new Sharding(clusters));
    }

    //////////////////////////////////////////////////////////////

    public long dbsize() {
        Response response = sendRequest("dbsize");
        return Long.parseLong(response.firstBlock());
    }

    public String info() {
        Response response = sendRequest("info");
        return response.joinBlocks('\n');
    }

    //////////////////////////////////////////////////////////////// key value commands

    public String get(String key) {
        return sendRequest("get", key).firstBlock();
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
            sendWriteRequest((Object[]) prependCommand("multi_del", keys));
        }
    }

    public void del(List<String> keys) {
        if (keys.size() == 1) {
            sendWriteRequest("del", keys.get(0));
        } else {
            sendWriteRequest((Object[]) prependCommand("multi_del", keys.toArray(new String[keys.size()])));
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
        return sendRequest("keys", startExclude, endInclude, limit).getBlocks();
    }

    public List<String> rkeys(String startExclude, String endInclude, int limit) {
        return sendRequest("rkeys", startExclude, endInclude, limit).getBlocks();
    }

    public List<KeyValue> scan(String startExclude, String endInclude, int limit) {
        return sendRequest("scan", startExclude, endInclude, limit).getKeyValues();
    }

    public List<KeyValue> rscan(String startExclude, String endInclude, int limit) {
        return sendRequest("rscan", startExclude, endInclude, limit).getKeyValues();
    }

    public void multiSet(String... keyValues) {
        if (keyValues.length % 2 == 1) {
            throw new SsdbException("Length of parameters must be odd");
        }

        String[] command = prependCommand("multi_set", keyValues);
        sendWriteRequest((Object[]) command);
    }

    public void multiSet(List<KeyValue> keyValues) {
        String[] command = new String[keyValues.size() * 2 + 1];
        command[0] = "multi_set";

        for (int i = 0; i < keyValues.size(); i++) {
            KeyValue keyValue = keyValues.get(i);
            command[i * 2 + 1] = keyValue.getKey();
            command[i * 2 + 2] = keyValue.getValue();
        }

        sendWriteRequest((Object[]) command);
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
        return sendRequest("hexists", propName).getIntResult() > 0;
    }

    public int hsize(String key) {
        return sendRequest("hsize", key).getIntResult();
    }

    public List<KeyValue> hlist(String key, String startExclude, String endInclude, int limit) {
        return sendRequest("hlist", key, startExclude, endInclude, limit).getKeyValues();
    }

    public List<KeyValue> hrlist(String key, String startExclude, String endInclude, int limit) {
        return sendRequest("hrlist", key, startExclude, endInclude, limit).getKeyValues();
    }

    public List<String> hkeys(String key, String startExclude, String endInclude, int limit) {
        return sendRequest("hkeys", key, startExclude, endInclude, limit).getBlocks();
    }

    public List<KeyValue> hgetall(String key) {
        return sendRequest("hgtall", key).getKeyValues();
    }

    public Map<String, String> hgetallmap(String key) {
        return sendRequest("hgtall", key).getBlocksAsMap();
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

    public long zget(String key, String id) {
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

    public List<String> zkeys(String key, String startExclude, String endInclude, int limit) {
        return sendRequest("zkeys", key, startExclude, endInclude, limit).getBlocks();
    }

    public List<IdScore> zscan(String key, String startExclude, String endInclude, int limit) {
        return sendRequest("zscan", key, startExclude, endInclude, limit).getIdScores();
    }

    public List<IdScore> zrscan(String key, String startExclude, String endInclude, int limit) {
        return sendRequest("zrscan", key, startExclude, endInclude, limit).getIdScores();
    }

    /**
     * 获取 id 的排名，从小到大，0 表示第一位
     *
     * @param key id 所处的 zset 的 key
     * @param id  id
     *
     * @return 排名，如果 id 不在 key 当中则返回 -1
     */
    public int zrank(String key, String id) {
        return sendRequest("zrank", key, id).getIntResult();
    }

    // 同上，从大到小排名
    public int zrrank(String key, String id) {
        return sendRequest("zrrank", key, id).getIntResult();
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
        return sendRequest("zcount", key, strMin, strMax).getIntResult();
    }

    public long zsum(String key, int minScoreInclude, int maxScoreInclude) {
        String strMin = minScoreInclude == Integer.MIN_VALUE ? "" : String.valueOf(minScoreInclude);
        String strMax = maxScoreInclude == Integer.MAX_VALUE ? "" : String.valueOf(maxScoreInclude);
        return sendRequest("zsum", key, strMin, strMax).getLongResult();
    }

    public long zavg(String key, int minScoreInclude, int maxScoreInclude) {
        String strMin = minScoreInclude == Integer.MIN_VALUE ? "" : String.valueOf(minScoreInclude);
        String strMax = maxScoreInclude == Integer.MAX_VALUE ? "" : String.valueOf(maxScoreInclude);
        return sendRequest("zavg", key, strMin, strMax).getLongResult();
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

    public void multiZset(String key, List<IdScore> idScores) {
        sendWriteRequest(prependCommandIdScore("multi_zset", key, idScores));
    }

    public List<IdScore> multiZget(String key, List<String> ids) {
        return sendRequest(prependCommand("multi_zget", key, ids)).getIdScores();
    }

    public void multiZdel(String key, List<String> ids) {
        sendWriteRequest(prependCommand("multi_zdel", key, ids));
    }
}
