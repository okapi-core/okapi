package org.okapi.pages;

import org.okapi.streams.StreamIdentifier;

public interface PageMatchingCondition {
    boolean isMatch(StreamIdentifier streamIdentifier, long block);
}
