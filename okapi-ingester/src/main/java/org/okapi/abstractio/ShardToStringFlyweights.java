package org.okapi.abstractio;


import org.okapi.CommonConfig;

import java.util.HashMap;
import java.util.Map;

public class ShardToStringFlyweights {
    Map<Integer, String> shardToString;
    
    public ShardToStringFlyweights(){
        this.shardToString = new HashMap<>();
        for(int i = 0; i < CommonConfig.N_SHARDS; i++){
            this.shardToString.put(i, Integer.toString(i));
        }
    }

    public String toString(int shard){
        return this.shardToString.get(shard);
    }
}
