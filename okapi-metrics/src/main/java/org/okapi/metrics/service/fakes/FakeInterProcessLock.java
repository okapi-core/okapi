package org.okapi.metrics.service.fakes;

import org.apache.curator.framework.recipes.locks.InterProcessLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FakeInterProcessLock implements InterProcessLock {
    Lock lock;
    public FakeInterProcessLock (){
        this.lock = new ReentrantLock();
    }
    @Override
    public void acquire() throws Exception {
        lock.lock();
    }

    @Override
    public boolean acquire(long time, TimeUnit unit) throws Exception {
        lock.tryLock(time, unit);
        return true;
    }

    @Override
    public void release() throws Exception {
        lock.unlock();
    }

    @Override
    public boolean isAcquiredInThisProcess() {
        throw new IllegalArgumentException("Not implemented");
    }
}
