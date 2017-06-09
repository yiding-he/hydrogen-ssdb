package com.hyd.ssdb.conn;

/**
 * 服务器监视线程。不论多少个 SsdbClient 实例，都共用一个监视线程。监视线程仅监视
 * Cluster 中无法连接的服务器，直到它们恢复为止。只要端口能连上就当服务器已上线。
 * 本类不关心一个服务器是属于哪个 SsdbClient 或属于哪个 Sharding。
 *
 * @author yidin
 */
public class ServerMonitorDaemon {

    private static final Thread daemonThread = new Thread(new MonitorTask());

    {

    }

    //////////////////////////////////////////////////////////////

    private static class MonitorTask implements Runnable {

        @Override
        public void run() {

        }
    }
}
