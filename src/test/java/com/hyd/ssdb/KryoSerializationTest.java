package com.hyd.ssdb;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.hyd.ssdb.util.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

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
    }

    @Test
    public void testSerialization() throws Exception {
        User user = new User(1, "admin", new Date());

        Kryo kryo = new Kryo();
        ByteBufferOutput output = new ByteBufferOutput(10240);
        kryo.writeObject(output, user);
        byte[] bytes = output.toBytes();
        System.out.println("Saved bytes: " + Bytes.toString(bytes));

        ssdbClient.set("kryo_user", bytes);

        //////////////////////////////////////////////////////////////

        byte[] readBytes = ssdbClient.getBytes("kryo_user");
        System.out.println("Read  bytes: " + Bytes.toString(readBytes));
        Assert.assertEquals(bytes.length, readBytes.length);
    }


}
