package org.okapi.web.auth;

import org.okapi.data.dao.RelationGraphDao;

public interface GraphTx {
  void doTx(RelationGraphDao relationGraphDao);
}
