package org.okapi.consistenthashing;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ConsistentHasher {
    public static String mapShard(int shard, List<String>nodes){
       var murmur = Hashing.murmur3_32_fixed();
       var hash = murmur.hashInt(shard);
       var consistentHash = Hashing.consistentHash(hash, nodes.size());
       return nodes.get(consistentHash);
    }

    public static int hash(String key, int buckets){
        var murmur = Hashing.murmur3_32_fixed();
        var hash = murmur.hashBytes(key.getBytes(StandardCharsets.UTF_8));
        return Hashing.consistentHash(hash, buckets) ;
    }
}
