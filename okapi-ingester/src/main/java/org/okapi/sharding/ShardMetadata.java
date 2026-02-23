package org.okapi.sharding;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ShardMetadata {
  private long epoch;
  private String owner; // nodeId
  private ShardState state; // STEADY / MOVING
  private String target; // nodeId or null
  private Long handoffOffset;

  @Builder(toBuilder = true)
  public ShardMetadata(
      long epoch, String owner, ShardState state, String target, Long handoffOffset) {
    setEpoch(epoch);
    setOwner(owner);
    setState(state);
    setTarget(target);
    setHandoffOffset(handoffOffset);
  }

  private void setEpoch(long epoch) {
    this.epoch = epoch;
  }

  private void setOwner(String owner) {
    if (owner == null || owner.isEmpty()) {
      throw new IllegalArgumentException("owner cannot be null or empty");
    }
    this.owner = owner;
  }

  private void setState(ShardState state) {
    this.state = state;
  }

  private void setTarget(String target) {
    if (state == ShardState.MOVING) {
      if (target == null || target.isEmpty()) {
        throw new IllegalArgumentException("target cannot be null or empty when state is MOVING");
      }
    }
    this.target = target;
  }

  private void setHandoffOffset(Long handoffOffset) {
    this.handoffOffset = handoffOffset;
  }
}
