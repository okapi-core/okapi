package org.okapi.web.tsvector.factory;

import org.okapi.web.tsvector.TimeVector;

public interface TimeVectorFactory <T>{

    TimeVector createTimeVector(T base);
}
