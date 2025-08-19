package org.okapi.data.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CachedData <T>{
    long cacheTime;
    T data;
}
