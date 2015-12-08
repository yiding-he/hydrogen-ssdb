package com.hyd.ssdb.conf;

/**
 * 单点故障处理策略
 * created at 15-12-8
 *
 * @author Yiding
 */
public enum SPOFStrategy {

    /**
     * 自动扩展上一个节点的 key 空间
     */
    AutoExpandStrategy,

    /**
     * 保留 key 空间，等待故障节点恢复
     */
    PreserveKeySpaceStrategy
}
