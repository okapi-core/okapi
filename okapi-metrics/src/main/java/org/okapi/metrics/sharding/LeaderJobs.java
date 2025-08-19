package org.okapi.metrics.sharding;

public interface LeaderJobs {
    /**
     * Iterate through the nodes, check heartbeats of each node. If a node has a missing heartbeat, mark it as unhealthy.
      */
    void checkFleetHealth() throws Exception;
    void checkShardMovementStatus() throws Exception;
}
