package org.okapi.swim.ping;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;

@Service
public class MemberList {
  Map<String, Member> members = new HashMap<>();
  Lock modifierLock = new ReentrantLock();

  public void addMember(Member member) {
    try {
      modifierLock.lock();
      this.members.put(member.getNodeId(), member);
    } finally {
      modifierLock.unlock();
    }
  }

  public Member getMember(String nodeId) {
    try {
      modifierLock.lock();
      return this.members.get(nodeId);
    } finally {
      modifierLock.unlock();
    }
  }

  public void remove(String nodeId) {
    try {
      modifierLock.lock();
      this.members.remove(nodeId);
    } finally {
      modifierLock.unlock();
    }
  }

  public List<Member> sampleK(int k) {
    try {
      modifierLock.lock();
      var keys = new ArrayList<>(members.keySet());
      Collections.shuffle(keys);
      int take = Math.min(k, keys.size());
      var sample = new ArrayList<Member>(take);
      for (int i = 0; i < take; i++) {
        sample.add(this.members.get(keys.get(i)));
      }
      return sample;
    } finally {
      modifierLock.unlock();
    }
  }

  public List<Member> sampleKExcluding(Collection<String> exclude, int k) {
    try {
      modifierLock.lock();
      var keys = new ArrayList<>(members.keySet());
      keys.removeAll(exclude);
      Collections.shuffle(keys);
      int take = Math.min(k, keys.size());
      var sample = new ArrayList<Member>(take);
      for (int i = 0; i < take; i++) {
        sample.add(this.members.get(keys.get(i)));
      }
      return sample;
    } finally {
      modifierLock.unlock();
    }
  }

  public Member sample() {
    try {
      modifierLock.lock();
      var sampled = members.keySet().stream().toList();
      var idx = new ArrayList<Integer>();
      for (int i = 0; i < sampled.size(); i++) {
        idx.add(i);
      }
      Collections.shuffle(idx);
      return this.members.get(sampled.getFirst());
    } finally {
      modifierLock.unlock();
    }
  }

  public List<Member> getAllMembers() {
    try {
      modifierLock.lock();
      return new ArrayList<>(this.members.values());
    } finally {
      modifierLock.unlock();
    }
  }
}
