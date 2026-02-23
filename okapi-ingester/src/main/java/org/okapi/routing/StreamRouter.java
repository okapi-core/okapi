package org.okapi.routing;

import org.okapi.streams.StreamIdentifier;

public interface StreamRouter<Id> {

  String getNodesForReading(StreamIdentifier<Id> streamIdentifier, long idxBlock);
}
