package org.okapi.metrics.common;


import java.util.List;

public interface FleetMetadata {
  byte[] getData(String path) throws Exception;

  void create(String path) throws Exception;

  void createParentsIfNeeded(String path) throws Exception;

  void setData(String path, byte[] data) throws Exception;

  void incCounter(String path) throws Exception;

  void decCounter(String path) throws Exception;

  int getCounter(String path) throws Exception;

  boolean atomicWrite(List<String> paths, List<byte[]> data) throws  Exception;

  void createPathIfNotExists(String path) throws Exception;

  List<String> listChildren(String path) throws Exception;
}
