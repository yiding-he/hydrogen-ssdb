package com.hyd.ssdb.conn;

import com.hyd.ssdb.*;
import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.protocol.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 对 Socket 的包装。一旦发送或读取内容失败，Connection
 * 会将 available 置为 false，然后会被从连接池中去掉。
 *
 * @author Yiding
 */
public class Connection {

    private Socket socket;      // 网络连接套接字

    private String pass;        // 连接成功后发送认证口令

    private boolean available;  // 是否已经不再可用

    private int buffer;         // 读取数据时缓存区的长度

    private Map<String, Object> properties = new HashMap<>();   // 其他属性

    public Connection(Server server) {
        this(server.getHost(), server.getPort(), server.getPass(),
            server.getSocketConfig().getSoTimeout(), server.getSocketConfig().getSoBufferSize());
    }

    public Connection(String host, int port, String pass, int soTimeout, int soBuffer) {
        try {
            this.socket = new Socket(host, port);
            this.socket.setSoTimeout(soTimeout);
            this.pass = pass;
            this.buffer = soBuffer;
            this.available = true;
            this.properties.put("host", host);
            this.properties.put("port", port);

            if (this.pass != null) {
                auth();
            }
        } catch (IOException e) {
            throw new SsdbSocketFailedException(e);
        }
    }

    private void auth() {
        send(new Request("auth", this.pass).toBytes());
        Response response = receivePacket();
        String header = response.getHead().toString();
        if (!header.equals("ok")) {
            throw new SsdbAuthFailedException();
        }
    }

    public String getHost() {
        return this.getProperty("host");
    }

    public int getPort() {
        return (Integer) this.getProperty("port");
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String propName) {
        return (T) this.properties.get(propName);
    }

    public void setProperty(String propName, Object propValue) {
        this.properties.put(propName, propValue);
    }

    public boolean hasProperty(String propName) {
        return this.properties.containsKey(propName);
    }

    public void removeProperty(String propName) {
        this.properties.remove(propName);
    }

    public void send(byte[] bytes) {
        try {
            OutputStream outputStream = this.socket.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e) {
            this.available = false;
            throw new SsdbSocketFailedException(e);
        }
    }

    public Response receivePacket() {
        return receivePacket(AbstractClient.DEFAULT_CHARSET);
    }

    public Response receivePacket(Charset charset) {

        ByteArrayOutputStream bos = null;
        Response response = new Response(charset);

        try {
            InputStream inputStream = this.socket.getInputStream();
            bos = new ByteArrayOutputStream(10240);
            StringBuilder numSb = new StringBuilder();

            byte b;
            int dataLength = 0, dataCounter = 0;
            int blockStatus = 0; // 0=ready, 1=receiving_length, 2=receiving_data, 3=data_finished
            int responseStatus = 0; //0=ready, 1=head_received

            byte[] bs = new byte[this.buffer];

            while (true) {
                int len = inputStream.read(bs);
                if (len == -1) {
                    break;
                }

                for (int i = 0; i < len; i++) {
                    b = bs[i];
                    if (b == '\n') {
                        if (blockStatus == 0) {
                            return response;  // 方法唯一的正确出口

                        } else if (blockStatus == 1) {
                            dataLength = Integer.parseInt(numSb.toString());
                            bos.reset();
                            numSb.setLength(0);

                            // 如果数据长度为 0，则跳过状态2
                            if (dataLength == 0) {
                                blockStatus = 3;
                            } else {
                                blockStatus = 2;
                            }

                        } else if (blockStatus == 2) {
                            bos.write(b);
                            dataCounter += 1;
                            if (dataCounter >= dataLength) {
                                blockStatus = 3;
                                dataCounter = 0;
                            }

                        } else { // blockStatus == 3
                            blockStatus = 0;

                            Block block = new Block(bos.toByteArray());
                            if (responseStatus == 0) {
                                response.setHead(block);
                                responseStatus = 1;
                            } else {
                                response.addBodyBlock(block);
                            }
                        }
                    } else {
                        bos.write(b);

                        if (blockStatus == 0) {
                            blockStatus = 1;
                            numSb.append((char) b);
                        } else if (blockStatus == 1) {
                            numSb.append((char) b);
                        } else if (blockStatus == 2) {
                            dataCounter += 1;
                            if (dataCounter >= dataLength) {
                                blockStatus = 3;
                                dataCounter = 0;
                            }
                        } else { // status == 3 包已读取完毕，此时必须收到 \n
                            throw new SsdbException("Illegal packet: " + Arrays.toString(bos.toByteArray()));
                        }
                    }
                }
            }

            throw new SsdbException("Invalid packet");
        } catch (SocketTimeoutException e) {
            this.available = false;
            throw new SsdbSocketFailedException("Socket timed out, already read: " +
                    (bos == null ? "" : new String(bos.toByteArray())), e);
        } catch (IOException e) {
            this.available = false;
            throw new SsdbSocketFailedException(e);
        } catch (SsdbException e) {
            this.available = false;
            throw e;
        }
    }

    public void close() throws IOException {
        this.socket.close();
    }

    public boolean isAvailable() {
        return this.available;
    }

    @Override
    public String toString() {
        return "Connection{host='" + getHost() + "',port=" + getPort() + ",available=" + this.available + "}";
    }
}
