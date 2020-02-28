package com.hyd.ssdb;

import static org.junit.Assert.*;

import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.protocol.Request;
import com.hyd.ssdb.protocol.Response;
import com.hyd.ssdb.util.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * (description)
 * created at 15-12-2
 *
 * @author Yiding
 */
public class SsdbClientTest extends BaseTest {

    ////////////////////////////////////////////////////////////////

    @Test
    public void testSendRequest() throws Exception {
        Response response = ssdbClient.sendRequest(new Request("get name"));
        System.out.println(response.getHead());
        System.out.println(response.getBlocks());
    }

    @Test
    public void testMultiGet() {
        ssdbClient.multiSet("key1", "value1", "key2", "value2", "key3", "value3");
        List<String> values = ssdbClient.multiGet("key1", "key2", "key3");
        assertEquals(Arrays.asList("value1", "value2", "value3"), values);
    }

    @Test
    public void testMultiGetBytes() throws Exception {
        Charset charset = StandardCharsets.UTF_16;

        byte[] hello1 = "你好".getBytes(charset);
        System.out.println("Bytes: " + Arrays.toString(hello1));

        KeyValue keyValue = new KeyValue("hello1", hello1, null);
        System.out.println("KeyValue: " + Arrays.toString(keyValue.getValue()));
        ssdbClient.multiSet(Collections.singletonList(keyValue));

        byte[] hello1_ = ssdbClient.getBytes("hello1");
        System.out.println("GetBytes: " + Arrays.toString(hello1_));

        List<byte[]> bytesList = ssdbClient.multiGetBytes("hello1");
        System.out.println("MultiGetBytes: " + Arrays.toString(bytesList.get(0)));

        assertEquals("你好", new String(bytesList.get(0), charset));
    }

    @Test
    public void testMultiGetBytes2() throws Exception {
        byte[] bytes = {-1, -2, -3, -4, -5};

        KeyValue keyValue = new KeyValue("bytes", bytes, null);
        ssdbClient.multiSet(Collections.singletonList(keyValue));

        List<byte[]> bytesList = ssdbClient.multiGetBytes("bytes");
        assertFalse(bytesList.isEmpty());
        assertArrayEquals(bytes, bytesList.get(0));
    }

    @Test
    public void testDbsize() throws Exception {
        Server server = ssdbClient.getSharding().getClusters().get(0).getServers().get(0);
        System.out.println("dbsize of " + server + ": " + ssdbClient.dbsize(server));
    }

    @Test
    public void testInfo() throws Exception {
        Server server = ssdbClient.getSharding().getClusters().get(0).getServers().get(0);
        System.out.println("info of " + server + ": " + ssdbClient.info(server));
    }

    @Test
    public void testSetStringWithReturn() throws Exception {
        String key = "str_with_return";
        ssdbClient.set(key, "123\n\r\t456");
        System.out.println(ssdbClient.get(key)
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));
    }

    @Test
    public void testSetGet() throws Exception {
        ssdbClient.set("name", "111");
        assertEquals("111", ssdbClient.get("name"));

        ssdbClient.set("value", "-1");
        assertEquals("-1", ssdbClient.get("value"));
    }

    @Test
    public void testSetnx() throws Exception {
        String key = "key" + System.currentTimeMillis();
        assertEquals(1, ssdbClient.setnx(key, "1"));     // 第一次设置成功
        assertEquals(0, ssdbClient.setnx(key, "1"));     // 因为值已经存在，第二次设置失败
    }

    @Test
    public void testExpire() throws Exception {
        ssdbClient.set("name", "ssdb");
        assertEquals("ssdb", ssdbClient.get("name"));

        ssdbClient.expire("name", 1);
        Thread.sleep(1500);
        assertNull(ssdbClient.get("name"));
    }

    @Test
    public void testExpireCommand() throws Exception {
        System.out.println(ssdbClient.expire("invalid_key", 10));
    }

    @Test
    public void testHexists() throws Exception {
        ssdbClient.hset("hname", "prop", "value");
        assertTrue(ssdbClient.hexists("hname", "prop"));
    }

    @Test
    public void testTtl() throws Exception {
        ssdbClient.set("name", "ssdb");
        System.out.println(ssdbClient.ttl("name"));
        ssdbClient.expire("name", 120);
        System.out.println(ssdbClient.ttl("name"));
        System.out.println(ssdbClient.ttl("_key_not_exists_"));
    }

    @Test
    public void testQbytes() throws Exception {
        ssdbClient.qclear("queue");
        ssdbClient.qpushFront("queue", "123");
        System.out.println(ssdbClient.qget("queue", 0));
        System.out.println(new String(ssdbClient.qgetBytes("queue", 0)));
    }

    @Test
    public void testSetx() throws Exception {
        ssdbClient.setx("key", "value", 3);
        assertEquals("value", ssdbClient.get("key"));
        Thread.sleep(3100);
        assertFalse(ssdbClient.exists("key"));
        assertNull(ssdbClient.get("key"));
    }

    @Test
    public void testGetSet() throws Exception {
        ssdbClient.set("key", "old_value");
        assertEquals("old_value", ssdbClient.getset("key", "new_value"));
        assertEquals("new_value", ssdbClient.get("key"));
    }

    @Test
    public void testSetGetBytes() throws Exception {
        ssdbClient.set("bytes", new byte[]{50, 51, 52, 62});
        System.out.println(Bytes.toString(ssdbClient.getBytes("bytes")));
    }

    @Test
    public void testQpushFrontBytes() throws Exception {
        ssdbClient.qclear("queue");
        ssdbClient.qpushFront("queue", new byte[]{50, 51, 52, 62});
        System.out.println(Arrays.toString(ssdbClient.qgetBytes("queue", 0)));
    }

    @Test
    public void testIncr() throws Exception {
        ssdbClient.set("counter", 123);
        assertEquals(223, ssdbClient.incr("counter", 100));
    }

    @Test
    public void testDelExists() throws Exception {
        ssdbClient.set("name1", 123);
        assertTrue(ssdbClient.exists("name1"));
        ssdbClient.set("name2", 456);
        assertTrue(ssdbClient.exists("name2"));

        ssdbClient.del("name1", "name2");

        assertFalse(ssdbClient.exists("name1"));
        assertFalse(ssdbClient.exists("name2"));
    }

    @Test
    public void testDelExists2() throws Exception {
        ssdbClient.set("name1", 123);
        assertTrue(ssdbClient.exists("name1"));
        ssdbClient.set("name2", 456);
        assertTrue(ssdbClient.exists("name2"));

        ssdbClient.del(Arrays.asList("name1", "name2"));

        assertFalse(ssdbClient.exists("name1"));
        assertFalse(ssdbClient.exists("name2"));
    }

    @Test
    public void testGetbit() throws Exception {
        ssdbClient.set("bitkey", "1");  // 0011 0001

        String str = "";
        for (int i = 0; i < 8; i++) {
            str += ssdbClient.getbit("bitkey", i);
        }
        System.out.println(str);

/*
        assertEquals(0, ssdbClient.getbit("bitkey", 0));
        assertEquals(0, ssdbClient.getbit("bitkey", 1));
        assertEquals(1, ssdbClient.getbit("bitkey", 2));
        assertEquals(1, ssdbClient.getbit("bitkey", 3));
*/
    }

    @Test
    public void testGetByte() throws Exception {
        byte[] bytes = {-1, -2, -3, -4, -5};
        ssdbClient.set("bytes", bytes);
        assertArrayEquals(bytes, ssdbClient.getBytes("bytes"));
    }

    @Test
    public void testSubStr() throws Exception {
        ssdbClient.set("key", "hello, everyone");
        assertEquals("every", ssdbClient.substr("key", 7, 5));
        assertEquals("one", ssdbClient.substr("key", -3));
    }

    @Test
    public void testStrLen() throws Exception {
        String str = "hello, everyone";
        ssdbClient.set("key", str);
        assertEquals(str.length(), ssdbClient.strlen("key"));
        assertEquals(0, ssdbClient.strlen("invlaid_key"));
    }

    @Test
    public void testKeys() throws Exception {
        ssdbClient.set("_key0", 0);
        ssdbClient.set("_key1", 1);
        ssdbClient.set("_key2", 2);
        ssdbClient.set("_key3", 3);
        ssdbClient.set("_key4", 4);

        List<String> keys = ssdbClient.keys("_key0", "_key9", 9);
        assertNotNull(keys);
        assertFalse(keys.isEmpty());
        assertEquals(4, keys.size());  // "_key0" 不包含在内
        assertEquals("_key1", keys.get(0));
        assertEquals("_key4", keys.get(3));
    }

    @Test
    public void testRkeys() throws Exception {
        ssdbClient.set("_key0", 0);
        ssdbClient.set("_key1", 1);
        ssdbClient.set("_key2", 2);
        ssdbClient.set("_key3", 3);
        ssdbClient.set("_key4", 4);

        List<String> keys = ssdbClient.rkeys("_key5", "_key1", 9);
        assertNotNull(keys);
        assertFalse(keys.isEmpty());
        assertEquals(4, keys.size());  // "_key0" 不包含在内
        assertEquals("_key4", keys.get(0));
        assertEquals("_key1", keys.get(3));
    }

    @Test
    public void testScan() throws Exception {
        ssdbClient.set("_key0", 0);
        ssdbClient.set("_key1", 1);
        ssdbClient.set("_key2", 2);
        ssdbClient.set("_key3", 3);
        ssdbClient.set("_key4", 4);

        List<KeyValue> keyValues = ssdbClient.scan("_key0", "_key9", 9);
        assertEquals(4, keyValues.size());
        for (KeyValue keyValue : keyValues) {
            System.out.println(keyValue);
        }
    }

    @Test
    public void testScanAllKeys() throws Exception {
        List<KeyValue> keyValues = ssdbClient.scan("", "", 100);
        for (KeyValue keyValue : keyValues) {
            System.out.println(keyValue);
        }
    }

    @Test
    public void testScanWithProcessor() throws Exception {
        String prefix = "ScoreValue:431200_BIG_UNION_2016:range:Examno:705130040:target:Quest";
        final AtomicInteger counter = new AtomicInteger();

        ssdbClient.scan(prefix, 100, new Processor<KeyValue>() {
            @Override
            public void process(KeyValue keyValue) {
                if (counter.incrementAndGet() > 1000) {
                    throw new RuntimeException("STOP");
                }
                System.out.println(keyValue);
            }
        });
    }

    @Test
    public void testMultiSet() throws Exception {
        ssdbClient.del("__key1", "__key2", "__key3");

        List<KeyValue> keyValues = Arrays.asList(
                new KeyValue("__key1", "value1"),
                new KeyValue("__key2", "value2"),
                new KeyValue("__key3", "value3")
        );

        ssdbClient.multiSet(keyValues);

        assertEquals("value1", ssdbClient.get("__key1"));
        assertEquals("value2", ssdbClient.get("__key2"));
        assertEquals("value3", ssdbClient.get("__key3"));
    }

    @Test
    public void testMultiSet2() throws Exception {
        ssdbClient.del("__key4", "__key5", "__key6");
        ssdbClient.multiSet("__key4", "value4", "__key5", "value5", "__key6", "value6");

        assertEquals("value4", ssdbClient.get("__key4"));
        assertEquals("value5", ssdbClient.get("__key5"));
        assertEquals("value6", ssdbClient.get("__key6"));
    }

    @Test
    public void testHashMap() throws Exception {
        String key = "multiKey";
        ssdbClient.del(key);
        ssdbClient.hset(key, "prop1", "value1");
        ssdbClient.hset(key, "prop2", "value2");

        assertEquals("value1", ssdbClient.hget(key, "prop1"));
        assertEquals("value2", ssdbClient.hget(key, "prop2"));
    }

    @Test
    public void testMultiHset() throws Exception {
        String key = "multiKey";
        ssdbClient.del(key);
        ssdbClient.multiHset(key, "prop1", "value1", "prop2", "value2");

        assertEquals("value1", ssdbClient.hget(key, "prop1"));
        assertEquals("value2", ssdbClient.hget(key, "prop2"));

        List<String> keys = ssdbClient.hkeys(key, "", "", 100);
        assertEquals(2, keys.size());
    }

    @Test
    public void testZsetgetdelsize() throws Exception {
        ssdbClient.zclear("zkey");
        ssdbClient.zset("zkey", "user1", 123);
        ssdbClient.zset("zkey", "user2", 456);
        ssdbClient.zset("zkey", "user3", 789);

        assertEquals(3, ssdbClient.zsize("zkey"));
    }

    @Test
    public void testMultiZget() throws Exception {
        ssdbClient.zclear("zkey");
        long setCount = ssdbClient.multiZset("zkey", Arrays.asList(
                new IdScore("user1", -1),
                new IdScore("user2", 0),
                new IdScore("user3", 1)
        ));

        assertEquals(3, setCount);

        List<IdScore> idScores = ssdbClient.multiZget("zkey", "user1", "user2", "user3");
        assertEquals(3, idScores.size());
    }

    @Test
    public void testZsetMinusValue() throws Exception {
        ssdbClient.zclear("zkey");
        ssdbClient.zset("zkey", "user1", -1);
        assertEquals(new Long(-1), ssdbClient.zget("zkey", "user1"));

        System.out.println(ssdbClient.zget("zkey", "user2"));
    }

    @Test
    public void testZlist() throws Exception {
        ssdbClient.zset("zkey:1", "user1", 123);
        List<String> keys = ssdbClient.zlist("zkey:", "", 100);
        assertTrue(keys.contains("zkey:1"));
    }

    @Test
    public void testZrank() throws Exception {
        ssdbClient.zclear("zkey");
        ssdbClient.zset("zkey", "user1", 123);
        ssdbClient.zset("zkey", "user2", 456);
        ssdbClient.zset("zkey", "user3", 789);

        assertEquals(0, ssdbClient.zrank("zkey", "user1"));
        assertEquals(1, ssdbClient.zrank("zkey", "user2"));
        assertEquals(-1, ssdbClient.zrank("zkey", "user4"));
    }

    @Test
    public void testZrange() throws Exception {
        ssdbClient.zclear("zkey");
        ssdbClient.zset("zkey", "user1", 123);
        ssdbClient.zset("zkey", "user2", 456);
        ssdbClient.zset("zkey", "user3", 789);

        List<IdScore> keyValues = ssdbClient.zrange("zkey", 0, 2);
        assertEquals(2, keyValues.size());
        assertEquals("user1", keyValues.get(0).getId());
        assertEquals(456, keyValues.get(1).getScore());
    }

    @Test
    public void testZcount() throws Exception {
        ssdbClient.zclear("zkey");
        ssdbClient.zset("zkey", "user1", 123);
        ssdbClient.zset("zkey", "user2", 456);
        ssdbClient.zset("zkey", "user3", 789);

        assertEquals(1, ssdbClient.zcount("zkey", 100, 200));
        assertEquals(2, ssdbClient.zcount("zkey", 100, 500));
        assertEquals(3, ssdbClient.zcount("zkey", Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void testList() throws Exception {
        ssdbClient.qclear("list");
        ssdbClient.qpushFront("list", "one", "two", "three");
        assertEquals(3, ssdbClient.qsize("list"));
        assertEquals("one", ssdbClient.qback("list"));
        assertEquals("three", ssdbClient.qfront("list"));

        ssdbClient.qpushFront("list", "four");
        assertEquals("four", ssdbClient.qfront("list"));

        ssdbClient.qpushBack("list", "zero");
        assertEquals("zero", ssdbClient.qback("list"));

        List<String> rangedList = ssdbClient.qrange("list", 0, 2);
        assertEquals(Arrays.asList("four", "three"), rangedList);
        assertEquals(Collections.singletonList("zero"), ssdbClient.qpopBack("list", 1));
    }

    @Test
    public void testHlist() throws Exception {
        ssdbClient.hset("h1", "name", "name_of_h1");
        ssdbClient.hset("h2", "name", "name_of_h2");
        List<String> keys = ssdbClient.hlist("h0", "h9", -1);
        assertEquals(Arrays.asList("h1", "h2"), keys);
    }

    @Test
    public void testHrlist() throws Exception {
        ssdbClient.hset("h1", "name", "name_of_h1");
        ssdbClient.hset("h2", "name", "name_of_h2");
        List<String> keys = ssdbClient.hrlist("h9", "h0", -1);
        assertEquals(Arrays.asList("h2", "h1"), keys);
    }

    @Test
    public void testPopAllFront() throws Exception {
        ssdbClient.qpushFront("q1", "1");
        ssdbClient.qpushFront("q1", "2");
        ssdbClient.qpushFront("q1", "3");
        ssdbClient.qpushFront("q1", "4");
        ssdbClient.qpushFront("q1", "5");
        ssdbClient.qpushFront("q1", "6");

        ssdbClient.qpopAllFront("q1", 5, new Processor<String>() {
            @Override
            public void process(String s) {
                System.out.println(s);
            }
        });
    }

    @Test
    public void testPopAllBack() throws Exception {
        ssdbClient.qpushFront("q1", "1");
        ssdbClient.qpushFront("q1", "2");
        ssdbClient.qpushFront("q1", "3");
        ssdbClient.qpushFront("q1", "4");
        ssdbClient.qpushFront("q1", "5");
        ssdbClient.qpushFront("q1", "6");

        ssdbClient.qpopAllBack("q1", 5, new Processor<String>() {
            @Override
            public void process(String s) {
                System.out.println(s);
            }
        });
    }

    @Test
    public void testZget() throws Exception {
        ssdbClient.zclear("yuwen");
        ssdbClient.zclear("shuxue");
        ssdbClient.zset("yuwen", "zhangsan", 100);
        ssdbClient.zset("yuwen", "lisi", 101);
        assertEquals(100, ssdbClient.zget("yuwen", "zhangsan").longValue());
        assertNull(ssdbClient.zget("yuwen", "wangwu"));
        assertNull(ssdbClient.zget("shuxue", "wangwu"));
    }

    @Test
    public void testZscan() throws Exception {
        ssdbClient.zclear("zkey");
        ssdbClient.zset("zkey", "a", 99);
        ssdbClient.zset("zkey", "b", 100);
        ssdbClient.zset("zkey", "c", 101);

        List<String> zkeys = ssdbClient.zkeys("zkey", null, 1L, 100L, 10);
        assertEquals(2, zkeys.size());
        assertEquals("a", zkeys.get(0));
        assertEquals("b", zkeys.get(1));

        // zscan
        assertEquals(2, ssdbClient.zscan("zkey", null, 1L, 100L, 10).size());
        assertEquals(1, ssdbClient.zscan("zkey", "a", 99L, 100L, 10).size());
        assertEquals(0, ssdbClient.zscan("zkey", null, 200L, 300L, 10).size());

        // zrscan
        assertEquals(1, ssdbClient.zrscan("zkey", "b", null, null, 10).size());
    }
}