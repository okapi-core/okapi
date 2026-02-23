/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.auth;

import org.okapi.data.dao.RelationGraphDao;

public interface GraphTx {
  void doTx(RelationGraphDao relationGraphDao);
}
