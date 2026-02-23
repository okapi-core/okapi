/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes;

import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EdgeSeq {

  List<OutgoingEdge> accepted;

  public List<OutgoingEdge> accepted() {
    return accepted;
  }
}
