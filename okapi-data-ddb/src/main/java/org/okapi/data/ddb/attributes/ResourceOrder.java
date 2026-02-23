/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class ResourceOrder {
  List<String> resourceIds;

  public List<String> asList() {
    return Collections.unmodifiableList(resourceIds);
  }

  public ResourceOrder(List<String> resourceIds) {
    this.resourceIds = resourceIds;
  }

  public ResourceOrder() {
    resourceIds = new ArrayList<>();
  }

  public ResourceOrder add(String resourceId) {
    resourceIds.add(resourceId);
    return this;
  }

  public int size() {
    return resourceIds.size();
  }

  public static ResourceOrder from(List<String> resourceIds) {
    return new ResourceOrder(resourceIds);
  }

  public static ResourceOrder from(String... resourceIds) {
    ResourceOrder order = new ResourceOrder();
    for (String id : resourceIds) {
      order.add(id);
    }
    return order;
  }
}
