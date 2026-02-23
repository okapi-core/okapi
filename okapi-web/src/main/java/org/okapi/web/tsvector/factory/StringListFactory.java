package org.okapi.web.tsvector.factory;

import java.util.List;

public interface StringListFactory<T> {
    List<String> getStringList(T body);
}
