package org.okapi.metrics.common;


import org.okapi.metrics.common.pojo.Node;

public class ZkPaths {

  public static String clusterLock() {
    return "/okapi/cluster-state-lock";
  }

  public static String metricsProcessorLeader() {
    return "/okapi/metrics-processor-leader";
  }

  public static String metricsProxyLeader() {
    return "/okapi/metrics-proxy-leader";
  }

  public static String metricsProcessor(String id) {
    return metricsProcessorRoot() + "/" + id;
  }

  public static String metricsProcessorRoot() {
    return "/okapi/nodes/metrics-processor";
  }

  public static String shardOpPath() {
    return "/okapi/shard-op";
  }

  public static String clusterChangeOpPath() {
    return "/okapi/cluster-update-op";
  }

  public static String heartbeatPath(String id) {
    return "/okapi/heart-beats/" + id;
  }

  public static String failedCount(String id) {
    return "/okapi/heart-beats/failed/" + id;
  }

  public static String unhealthyNodes() {
    return "/okapi/metrics-processor-unhealthy";
  }

  public static String oldShards() {
    return "/okapi/old-shards";
  }

  public static String newShards() {
    return "/okapi/new-shards";
  }

  public static String newNodeConfig() {
    return "/okapi/latest-node-config";
  }

  public static String oldNodeConfig() {
    return "/okapi/old-node-config";
  }

  public static String lastCheckpointedHour(String nodeId) {
    return "/okapi/hourly/" + nodeId;
  }

  public static String getEphemeralNodeRoot(String nodeType){
    return "/okapi/nodes/" + nodeType;
  }

  public static String getEphemeralNodePath(String nodeType, int id){
    return getEphemeralNodeRoot(nodeType) + "/" + id;
  }
  public static String getEphemeralNodePath(String nodeType, Node node){
    return getEphemeralNodeRoot(nodeType) + "/" + node.id();
  }
}
