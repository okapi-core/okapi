package org.okapi.web.service;

import lombok.AllArgsConstructor;
import org.okapi.data.exceptions.ResourceNotFoundException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.web.service.validation.RequestValidator;

@AllArgsConstructor
public abstract class AbstractValidatedCrudService<Ctx, ValidatedCtx, Req, Update, Res>
    implements CrudService<Ctx, Req, Update, Res> {

  RequestValidator<Ctx, ValidatedCtx, Req, Update> validator;

  @Override
  public Res create(Ctx ctx, Req request)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    return createAfterValidation(validator.validateCreate(ctx, request), request);
  }

  public abstract Res createAfterValidation(ValidatedCtx ctx, Req request)
      throws UnAuthorizedException, ResourceNotFoundException;

  @Override
  public Res read(Ctx ctx)
      throws ResourceNotFoundException, UnAuthorizedException, BadRequestException {
    return readAfterValidation(validator.validateRead(ctx));
  }

  public abstract Res readAfterValidation(ValidatedCtx ctx)
      throws ResourceNotFoundException, UnAuthorizedException, BadRequestException;

  @Override
  public Res update(Ctx ctx, Update request) throws Exception {
    return updateAfterValidation(validator.validateUpdate(ctx, request), request);
  }

  public abstract Res updateAfterValidation(ValidatedCtx ctx, Update request) throws Exception;

  @Override
  public void delete(Ctx ctx) throws ResourceNotFoundException, UnAuthorizedException, BadRequestException {
    deleteAfterValidation(validator.validateDelete(ctx));
  }

  public abstract void deleteAfterValidation(ValidatedCtx ctx)
      throws ResourceNotFoundException, UnAuthorizedException;
}
