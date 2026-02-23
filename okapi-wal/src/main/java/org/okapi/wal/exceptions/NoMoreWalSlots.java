package org.okapi.wal.exceptions;

/**
 * Indicates WAL slots have wrapped around without free space to allocate a new segment.
 */
public class NoMoreWalSlots extends Exception {
  public NoMoreWalSlots(String message) {
    super(message);
  }
}
