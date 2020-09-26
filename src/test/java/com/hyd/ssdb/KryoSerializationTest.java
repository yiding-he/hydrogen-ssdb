package com.hyd.ssdb;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.hyd.ssdb.util.Bytes;
import com.hyd.ssdb.util.KeyValue;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * (description)
 * created at 2016/12/13
 *
 * @author yidin
 */
public class KryoSerializationTest extends BaseTest {

    public static class User {

        private int id;

        private String username;

        private Date registrationTime;

        public User() {
        }

        public User(int id, String username, Date registrationTime) {
            this.id = id;
            this.username = username;
            this.registrationTime = registrationTime;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Date getRegistrationTime() {
            return registrationTime;
        }

        public void setRegistrationTime(Date registrationTime) {
            this.registrationTime = registrationTime;
        }

        @Override
        public String toString() {
            return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", registrationTime=" + registrationTime +
                '}';
        }
    }

    private static final ThreadLocal<Kryo> KRYO = ThreadLocal.withInitial(Kryo::new);

    private static byte[] serialize(Object object) {
        ByteBufferOutput output = new ByteBufferOutput(10240);
        KRYO.get().writeObject(output, object);
        return output.toBytes();
    }

    private static <T> T deserialize(byte[] bytes, Class<T> type) {
        return KRYO.get().readObject(new ByteBufferInput(bytes), type);
    }

    @Test
    public void testSerialization() throws Exception {

        User user = new User(1, "admin", new Date());

        byte[] bytes = serialize(user);
        ssdbClient.set("kryo_user", bytes);
        System.out.println("Saved bytes: " + Bytes.toString(bytes));

        byte[] readBytes = ssdbClient.getBytes("kryo_user");
        System.out.println("Read bytes: " + Bytes.toString(readBytes));

        Assert.assertEquals(bytes.length, readBytes.length);
    }

    @Test
    public void testMultiHSetGet() throws Exception {

        User user = new User(1, "admin", new Date());
        System.out.println("user = " + user);
        byte[] bs = serialize(user);
        System.out.println("bs = " + Bytes.toString(bs));

        List<KeyValue> kvlist = singletonList(new KeyValue("user", bs));
        ssdbClient.multiHset("kvlist", kvlist);

        List<KeyValue> result = ssdbClient.multiHget("kvlist", "user");
        for (KeyValue kv : result) {
            String key = kv.getKeyString();
            byte[] resultBytes = kv.getValue();
            System.out.println("resultBytes = " + Bytes.toString(resultBytes));
            User resultUser = deserialize(resultBytes, User.class);
            System.out.println(key + ": resultUser = " + resultUser);
        }
    }
}
