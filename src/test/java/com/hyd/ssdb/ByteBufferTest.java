package com.hyd.ssdb;

import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * (description)
 * created at 15-12-2
 *
 * @author Yiding
 */
public class ByteBufferTest {

    @Test
    public void testByteBuffer() throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        System.out.println(byteBuffer.array().length);
        System.out.println(byteBuffer.position());

        byteBuffer.put((byte)1);
        byteBuffer.put((byte)2);
        byteBuffer.put((byte)3);
        System.out.println(byteBuffer.array().length);
        System.out.println(byteBuffer.position());
    }
}
