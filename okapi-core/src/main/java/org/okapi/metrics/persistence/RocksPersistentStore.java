package org.okapi.metrics.persistence;

import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.WriteBackRequest;
import org.okapi.metrics.stats.Statistics;

import java.nio.file.Path;

public class RocksPersistentStore implements PersistentStore {
    Path root;
    SharedMessageBox<WriteBackRequest> requests;

    public RocksPersistentStore(Path root){
        this.root = root;
    }

    @Override
    public Statistics get(String key) {
        throw new RuntimeException();
    }

    @Override
    public void put(String key, Statistics statistics) {

    }
}
