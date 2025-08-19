package org.okapi.metrics.common;

import org.apache.curator.framework.recipes.locks.InterProcessLock;

public interface ZkResources {
    InterProcessLock clusterLock();
    boolean isLeader() throws Exception;
}
