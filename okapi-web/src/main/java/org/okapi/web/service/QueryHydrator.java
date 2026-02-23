package org.okapi.web.service;

import org.okapi.exceptions.BadRequestException;

import java.util.Map;

public interface QueryHydrator {
  String hydrate(String query, Map<String, Object> params) throws BadRequestException;
}
