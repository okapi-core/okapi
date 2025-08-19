package org.okapi.metrics.storage.fakes;

import org.okapi.metrics.storage.BitWriter;
import lombok.Getter;

public class StringValueWriter implements BitWriter {
    @Getter
    StringBuilder sb = new StringBuilder();

    @Override
    public void writeBit(boolean bit) {
        if(bit)sb.append('1');
        else sb.append('0');
    }

    @Override
    public boolean canWrite(int bits) {
        return true;
    }

}
