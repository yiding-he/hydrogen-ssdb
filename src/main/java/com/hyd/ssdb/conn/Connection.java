package com.hyd.ssdb.conn;

import com.hyd.ssdb.SsdbException;
import com.hyd.ssdb.conf.Server;

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

    private Server server;

    private Socket socket;

    private boolean available;

    private Map<String, Object> properties = new HashMap<String, Object>();

    public Connection(Server server) throws IOException {
        this.socket = new Socket(server.getHost(), server.getPort());
        this.socket.setSoTimeout(server.getSocketConfig().getSoTimeout());
        this.available = true;
    }

    public Connection(String host, int port, int soTimeout) throws IOException {
        this.socket = new Socket(host, port);
        this.socket.setSoTimeout(soTimeout);
        this.available = true;
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

    public void send(byte[] bytes) throws IOException {
        try {
            OutputStream outputStream = this.socket.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e) {
            this.available = false;
            throw e;
        }
    }

    public byte[] receivePacket() throws IOException {

        ByteArrayOutputStream bos = null;

        try {
            InputStream inputStream = this.socket.getInputStream();
            bos = new ByteArrayOutputStream(10240);
            StringBuilder numSb = new StringBuilder();

            int b;
            int dataLength = 0, dataCounter = 0;
            int status = 0; // 0=ready, 1=receiving_length, 2=receiving_data, 3=data_finished
            while ((b = inputStream.read()) != -1) {
                bos.write(b);

                if (b == '\n') {
                    if (status == 0) {
                        return bos.toByteArray();  // 方法唯一的正确出口
                    } else if (status == 1) {
                        dataLength = Integer.parseInt(numSb.toString());
                        numSb.setLength(0);
                        status = 2;
                    } else if (status == 2) {
                        dataCounter += 1;
                        if (dataCounter >= dataLength) {
                            status = 3;
                            dataCounter = 0;
                        }
                    } else { // status == 3
                        status = 0;
                    }
                } else {
                    if (status == 0) {
                        status = 1;
                        numSb.append((char)b);
                    } else if (status == 1) {
                        numSb.append((char)b);
                    } else if (status == 2) {
                        dataCounter += 1;
                        if (dataCounter >= dataLength) {
                            status = 3;
                            dataCounter = 0;
                        }
                    } else { // status == 3
                        throw new SsdbException("Illegal packet: " + Arrays.toString(bos.toByteArray()));
                    }
                }
            }

            throw new SsdbException("Invalid packet");
        } catch (SocketTimeoutException e) {
            this.available = false;
            throw new SsdbException("Socket timed out, already read: " +
                    (bos == null? "": new String(bos.toByteArray())));
        } catch (IOException e) {
            this.available = false;
            throw e;
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
}
