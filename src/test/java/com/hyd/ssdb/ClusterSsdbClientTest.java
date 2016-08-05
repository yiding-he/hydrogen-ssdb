package com.hyd.ssdb;

import com.hyd.ssdb.util.KeyValue;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.*;

/**
 * (description)
 * created at 16/08/05
 *
 * @author yiding_he
 */
public class ClusterSsdbClientTest extends ClusterBaseTest {

    @Test
    public void testMultiGetSet() throws Exception {
        ssdbClient.multiSet("key1", "value1", "key2", "value2", "key3", "value3");
        assertEquals("value1", ssdbClient.get("key1"));
        assertEquals("value2", ssdbClient.get("key2"));
        assertEquals("value3", ssdbClient.get("key3"));
    }

    @Test
    public void testMultiGetSet2() throws Exception {
        ssdbClient.multiSet(Arrays.asList(
                new KeyValue("key1", "value1"),
                new KeyValue("key2", "value2"),
                new KeyValue("key3", "value3")
        ));
        assertEquals("value1", ssdbClient.get("key1"));
        assertEquals("value2", ssdbClient.get("key2"));
        assertEquals("value3", ssdbClient.get("key3"));
    }

    @Test
    public void testDelKeys() throws Exception {
        ssdbClient.multiSet("key1", "value1", "key2", "value2", "key3", "value3");
        assertNotNull(ssdbClient.get("key1"));
        assertNotNull(ssdbClient.get("key2"));
        assertNotNull(ssdbClient.get("key3"));

        ssdbClient.del("key1", "key2", "key3");
        assertNull(ssdbClient.get("key1"));
        assertNull(ssdbClient.get("key2"));
        assertNull(ssdbClient.get("key3"));
    }

    @Test
    public void testDelKeys2() throws Exception {
        ssdbClient.multiSet("key1", "value1", "key2", "value2", "key3", "value3");
        assertNotNull(ssdbClient.get("key1"));
        assertNotNull(ssdbClient.get("key2"));
        assertNotNull(ssdbClient.get("key3"));

        ssdbClient.del(Arrays.asList("key1", "key2", "key3"));
        assertNull(ssdbClient.get("key1"));
        assertNull(ssdbClient.get("key2"));
        assertNull(ssdbClient.get("key3"));
    }
}
