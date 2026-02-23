/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service;

public interface CrudService<Ctx, Req, UpdateReq, Res> {
  Res create(Ctx ctx, Req request) throws Exception;

  Res read(Ctx ctx) throws Exception;

  Res update(Ctx ctx, UpdateReq request) throws Exception;

  void delete(Ctx ctx) throws Exception;
}
