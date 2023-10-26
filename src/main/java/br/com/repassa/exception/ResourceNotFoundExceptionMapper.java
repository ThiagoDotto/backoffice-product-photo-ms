package br.com.repassa.exception;

import br.com.backoffice_repassa_utils_lib.error.api.ApiError;
import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<NotFoundException>{

    @Override
    public Response toResponse(NotFoundException exception) {
        ApiError standardError = ApiError.createError(new RepassaException(PhotoError.ENDPOINT_NAO_VALIDO));
        ExceptionHandlerMapper exceptionHandlerMapper = new ExceptionHandlerMapper();
        return exceptionHandlerMapper.buildResponseEntity(standardError);
    }
}
