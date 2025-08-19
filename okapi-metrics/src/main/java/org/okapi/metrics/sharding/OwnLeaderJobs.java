package org.okapi.metrics.sharding;

public class OwnLeaderJobs implements LeaderJobs {
  @Override
  public void checkFleetHealth() throws Exception {}

  @Override
  public void checkShardMovementStatus() throws Exception {}
}
