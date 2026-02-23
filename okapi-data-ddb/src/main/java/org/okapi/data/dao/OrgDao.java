/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dao;

import java.util.Optional;
import org.okapi.data.dto.OrgDtoDdb;

public interface OrgDao {
  Optional<OrgDtoDdb> findById(String orgId);

  void save(OrgDtoDdb orgDto);
}
