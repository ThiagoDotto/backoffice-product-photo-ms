package br.com.repassa.exception;

import br.com.backoffice_repassa_utils_lib.error.api.ApiError;
import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.NoSuchElementException;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Provider
public class ExceptionHandlerMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlerMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error(exception.getMessage(), exception);


        if (exception instanceof RepassaException repassaException) {
            ApiError error = ApiError.createError(repassaException);
            return buildResponseEntity(error);
        }

        if (exception instanceof ConstraintViolationException constraintViolationException) {
            ApiError beanValidationError = ApiError.createBeanValidationError(constraintViolationException);
            return buildResponseEntity(beanValidationError);
        }

        if (exception instanceof NoSuchElementException noSuchElementException) {
            ApiError defaultError = ApiError.createStandardError(OK, noSuchElementException);
            return buildResponseEntity(defaultError);
        }

        if (exception instanceof ServiceUnavailableException serviceUnavailableException) {
            ApiError standardError = ApiError.createStandardError(SERVICE_UNAVAILABLE, serviceUnavailableException);
            return this.buildResponseEntity(standardError);
        }

        if (exception instanceof TimeoutException timeoutException) {
            ApiError internalError = ApiError.createInternalError(timeoutException);
            return this.buildResponseEntity(internalError);
        }


        if (exception instanceof IllegalArgumentException illegalArgumentException) {
            ApiError badRequest = ApiError.createBadRequest(illegalArgumentException.getMessage());
            return this.buildResponseEntity(badRequest);
        }

        if (exception instanceof NotFoundException notFoundException) {
            ApiError standardError = ApiError.createStandardError(NOT_FOUND, notFoundException);
            return this.buildResponseEntity(standardError);
        }

        if (exception instanceof WebApplicationException webApplicationException) {
            Response originalErrorResponse = webApplicationException.getResponse();
            ApiError defaultError = ApiError.createStandardError(
                    Response.Status.fromStatusCode(originalErrorResponse.getStatus()),
                    webApplicationException);

            return this.buildResponseEntity(defaultError);
        }

        ApiError internalError = ApiError.createInternalError(exception.getMessage());
        return buildResponseEntity(internalError);
    }

    public Response buildResponseEntity(ApiError error) {
        return Response
                .status(error.getStatus())
                .entity(error)
                .build();
    }

}
