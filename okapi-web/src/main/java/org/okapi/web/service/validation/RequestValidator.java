package org.okapi.web.service.validation;

import org.okapi.data.exceptions.ResourceNotFoundException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;

public interface RequestValidator<Ctx, ValidatedCtx, Req, UpdateReq> {
  ValidatedCtx validateCreate(Ctx ctx, Req request)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException;

  ValidatedCtx validateRead(Ctx ctx)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException;

  ValidatedCtx validateUpdate(Ctx ctx, UpdateReq request)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException;

  ValidatedCtx validateDelete(Ctx ctx)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException;

}
