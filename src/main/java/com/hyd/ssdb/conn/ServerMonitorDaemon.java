package com.hyd.ssdb.conn;

import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 服务器监视线程。不论多少个 SsdbClient 实例，都共用一个监视线程。监视线程仅监视
 * Cluster 中无法连接的服务器，直到它们恢复为止。只要端口能连上就当服务器已上线。
 * 本类不关心一个服务器是属于哪个 SsdbClient 或属于哪个 Sharding。
 *
 * @author yidin
 */
public class ServerMonitorDaemon {

    private static final Logger LOG = LoggerFactory.getLogger(ServerMonitorDaemon.class);

    public static final int DEFAULT_INTERVAL = 15000;

    //////////////////////////////////////////////////////////////

    private static int interval = DEFAULT_INTERVAL;

    private static final Map<Server, List<Cluster>> invalidServers = new ConcurrentHashMap<Server, List<Cluster>>();

    private static final Thread daemonThread;

    private static final Set<Server> checkingServers = new HashSet<Server>();

    private static ThreadPoolExecutor checkerThreadPool =
            (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                checkerThreadPool.shutdownNow();
            }
        }));

        daemonThread = new Thread(new MonitorTask());
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

    //////////////////////////////////////////////////////////////

    public static List<Server> getCurrentMonitoringServers() {
        return Collections.unmodifiableList(new ArrayList<Server>(invalidServers.keySet()));
    }

    public static int getInterval() {
        return interval;
    }

    /**
     * 设置检查时间间隔。如果要监视的服务器数量很多，可以适当增加间隔。缺省值为 {@link #DEFAULT_INTERVAL}
     *
     * @param interval 检查时间间隔（ms）
     */
    public static void setInterval(int interval) {
        ServerMonitorDaemon.interval = interval;
    }

    static {
    }

    /**
     * 添加一个要进行监视的无效服务器
     *
     * @param server  无法连接的服务器
     * @param cluster server 所属的 Cluster 对象。
     *                1. 当 server 恢复运作时，将调用 cluster 的 {@link Cluster#markValid(Server)} 方法；
     *                2. 当 server 被从 cluster 中移除时，不再进行监视。
     */
    public synchronized static void addInvalidServer(Server server, Cluster cluster) {
        if (!invalidServers.containsKey(server)) {
            invalidServers.put(server, new ArrayList<Cluster>());
        }

        List<Cluster> clusters = invalidServers.get(server);
        if (!clusters.contains(cluster)) {
            clusters.add(cluster);
        }
    }

    /**
     * 检查服务器是否可以连接
     *
     * @param server 要检查的服务器
     *
     * @return 如果可以连接则返回 true
     *
     * @throws Exception 如果出现错误
     */
    public static boolean isServerAvailable(Server server) throws Exception {
        Socket socket = null;
        try {
            InetSocketAddress address = new InetSocketAddress(server.getHost(), server.getPort());
            int timeout = server.getSocketConfig().getSoTimeout();

            socket = new Socket();
            socket.connect(address, timeout);

            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    //////////////////////////////////////////////////////////////

    private static class MonitorTask implements Runnable {

        @Override
        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    runSafe();
                    Thread.sleep(interval);
                } catch (Exception e) {
                    LOG.error("Error monitoring invalid servers", e);
                }
            }

        }

        private void runSafe() throws Exception {

            // 无需监视的服务器列表（在当前线程中删除）
            final Set<Server> noMonitoringServers = new HashSet<Server>();

            for (final Server server : invalidServers.keySet()) {

                final List<Cluster> clusters = invalidServers.get(server);

                // 删除不再包含该 Server 的 Cluster
                Iterator<Cluster> itClusters = clusters.iterator();
                while (itClusters.hasNext()) {
                    Cluster cluster = itClusters.next();
                    if (!cluster.containsServer(server)) {
                        itClusters.remove();
                    }
                }

                // 所有 Cluster 都没有包含该 Server，无需再监视了
                if (clusters.isEmpty()) {
                    noMonitoringServers.add(server);
                    continue;
                }

                // Server 正在检查当中，无需重复添加任务
                if (checkingServers.contains(server)) {
                    continue;
                } else {
                    checkingServers.add(server);
                }

                // 添加检查任务
                checkerThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (isServerAvailable(server)) {
                                for (Cluster cluster : clusters) {
                                    cluster.markValid(server);
                                }
                                invalidServers.remove(server);
                            }
                        } catch (Exception e) {
                            LOG.error("Error checking server", e);
                        } finally {
                            checkingServers.remove(server);
                        }
                    }
                });
            }

            // 从监视列表中移除
            for (Server noMonitoringServer : noMonitoringServers) {
                invalidServers.remove(noMonitoringServer);
            }
        }

    }
}
