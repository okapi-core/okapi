package org.okapi.metrics.service.fakes;

import org.okapi.metrics.common.ZkResources;
import lombok.Setter;
import org.apache.curator.framework.recipes.locks.InterProcessLock;

public class FakeZkResources implements ZkResources {
    FakeInterProcessLock clusterLock;
    @Setter
    boolean isLeader;

    public FakeZkResources() {
        clusterLock = new FakeInterProcessLock();
    }

    @Override
    public InterProcessLock clusterLock() {
        return clusterLock;
    }

    @Override
    public boolean isLeader() throws Exception {
        return isLeader;
    }
}
