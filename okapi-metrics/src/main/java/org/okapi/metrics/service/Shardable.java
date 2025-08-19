package org.okapi.metrics.service;

/** 2pc lifecycle for a resource that can be scaled up and down on demand. */
public interface Shardable {

  void onShardMovePrepare() throws Exception;

  void onShardMoveRollback() throws Exception;

  void onShardMoveCommit() throws Exception;

  void afterShardMoveCommit() throws Exception;

  void afterShardMoveRollback() throws Exception;
}
