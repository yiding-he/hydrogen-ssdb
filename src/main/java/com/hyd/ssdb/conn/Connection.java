package com.hyd.ssdb.conn;

import com.hyd.ssdb.SsdbException;
import com.hyd.ssdb.SsdbSocketFailedException;
import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.protocol.Block;
import com.hyd.ssdb.protocol.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 对 Socket 的包装。一旦发送或读取内容失败，Connection
 * 会将 available 置为 false，然后会被从连接池中去掉。
 *
 * @author Yiding
 */
public class Connection {

    private Socket socket;      // 网络连接套接字

    private boolean available;  // 是否已经不再可用

    private Map<String, Object> properties = new HashMap<String, Object>();   // 其他属性

    public Connection(Server server) {
        this(server.getHost(), server.getPort(), server.getSocketConfig().getSoTimeout());
    }

    public Connection(String host, int port, int soTimeout) {
        try {
            this.socket = new Socket(host, port);
            this.socket.setSoTimeout(soTimeout);
            this.available = true;
            this.properties.put("host", host);
            this.properties.put("port", port);
        } catch (IOException e) {
            throw new SsdbSocketFailedException(e);
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

    public Response receivePacket2() {

        ByteArrayOutputStream bos = null;
        Response response = new Response();

        try {
            InputStream inputStream = this.socket.getInputStream();
            bos = new ByteArrayOutputStream(10240);
            StringBuilder numSb = new StringBuilder();


            int b;
            int dataLength = 0, dataCounter = 0;
            int blockStatus = 0; // 0=ready, 1=receiving_length, 2=receiving_data, 3=data_finished
            int responseStatus = 0; //0=ready, 1=head_received
            while ((b = inputStream.read()) != -1) {

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
                    } else { // status == 3
                        blockStatus = 0;

                        if (responseStatus == 0) {
                            response.setHead(new Block(bos.toByteArray()));
                            responseStatus = 1;
                        } else {
                            response.addBodyBlock(new Block(bos.toByteArray()));
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
