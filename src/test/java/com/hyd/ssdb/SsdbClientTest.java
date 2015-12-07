package com.hyd.ssdb;

import com.hyd.ssdb.protocol.Request;
import com.hyd.ssdb.protocol.Response;
import com.hyd.ssdb.util.KeyValue;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

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
        Response response = this.ssdbClient.sendRequest(new Request("get name"));
        System.out.println(response.getHeader());
        System.out.println(response.getBlocks());
    }

    @Test
    public void testDbsize() throws Exception {
        System.out.println(ssdbClient.dbsize());
    }

    @Test
    public void testInfo() throws Exception {
        System.out.println(ssdbClient.info());
    }

    @Test
    public void testSetGet() throws Exception {
        ssdbClient.set("name", "123123123123123123123123");
        System.out.println(ssdbClient.get("name"));
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
    public void testTtl() throws Exception {
        ssdbClient.set("name", "ssdb");
        System.out.println(ssdbClient.ttl("name"));
        ssdbClient.expire("name", 120);
        System.out.println(ssdbClient.ttl("name"));
        System.out.println(ssdbClient.ttl("_key_not_exists_"));
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
    public void testZlist() throws Exception {
        ssdbClient.zclear("zkey");
        ssdbClient.zset("zkey", "user1", 123);
        List<String> keys = ssdbClient.zlist("", "", 100);
        assertTrue(keys.contains("zkey"));
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

        List<KeyValue> keyValues = ssdbClient.zrange("zkey", 0, 2);
        assertEquals(2, keyValues.size());
        assertEquals("user1", keyValues.get(0).getKey());
        assertEquals("456", keyValues.get(1).getValue());
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
}