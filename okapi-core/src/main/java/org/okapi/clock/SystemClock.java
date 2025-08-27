package org.okapi.clock;

public class SystemClock implements Clock{
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public Clock setTime(long time) {
    throw new RuntimeException("Not permitted, maybe you want to use FakeClock instead ?");
    }

    @Override
    public long getTime() {
        return System.currentTimeMillis();
    }
}
