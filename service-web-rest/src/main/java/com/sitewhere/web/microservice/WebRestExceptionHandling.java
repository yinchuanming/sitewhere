package com.sitewhere.web.microservice;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.sitewhere.grpc.model.security.NotAuthorizedException;
import com.sitewhere.grpc.model.security.UnauthenticatedException;
import com.sitewhere.microservice.security.JwtExpiredException;
import com.sitewhere.rest.ISiteWhereWebConstants;
import com.sitewhere.spi.SiteWhereSystemException;
import com.sitewhere.spi.error.ErrorCode;
import com.sitewhere.spi.error.ResourceExistsException;
import com.sitewhere.spi.tenant.TenantNotAvailableException;

/**
 * Common handler for exceptions generated while processing REST requests.
 * 
 * @author Derek
 */
@ControllerAdvice
public class WebRestExceptionHandling extends ResponseEntityExceptionHandler {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /**
     * Handles exception thrown when a tenant operation is requested on an
     * unavailable tenant.
     * 
     * @param e
     * @param response
     */
    @ExceptionHandler(value = { TenantNotAvailableException.class })
    protected void handleTenantNotAvailable(TenantNotAvailableException e, HttpServletResponse response) {
	LOGGER.error("Operation invoked on unavailable tenant.", e);
	try {
	    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "The requested tenant is not available.");
	} catch (IOException e1) {
	    LOGGER.error(e1);
	}
    }

    /**
     * Handles exceptions where a new resource is to be created, but an existing
     * resource exists with the given key.
     * 
     * @param e
     * @param response
     */
    @ExceptionHandler(value = { ResourceExistsException.class })
    protected void handleResourceExists(ResourceExistsException e, HttpServletResponse response) {
	try {
	    sendErrorResponse(e, e.getCode(), HttpServletResponse.SC_CONFLICT, response);
	    LOGGER.error("Resource with same key already exists.", e);
	} catch (IOException e1) {
	    e1.printStackTrace();
	}
    }

    /**
     * Handle unauthorized requests.
     * 
     * @param e
     * @param request
     * @return
     */
    @ExceptionHandler(value = { NotAuthorizedException.class })
    protected ResponseEntity<Object> handleNotAuthorized(NotAuthorizedException e, WebRequest request) {
	return handleExceptionInternal(e, e.getMessage(), new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    /**
     * Handles unauthenticated access.
     * 
     * @param e
     * @param response
     */
    @ExceptionHandler(value = { UnauthenticatedException.class })
    protected ResponseEntity<Object> handleUnauthenticated(UnauthenticatedException e, WebRequest request) {
	return handleExceptionInternal(e, e.getMessage(), new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    /**
     * Handles expired JWT credentials.
     * 
     * @param e
     * @param response
     */
    @ExceptionHandler(value = { JwtExpiredException.class })
    protected ResponseEntity<Object> handleJwtExpired(JwtExpiredException e, WebRequest request) {
	return handleExceptionInternal(e, e.getMessage(), new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    /**
     * Handles a system exception by setting the HTML response code and response
     * headers.
     * 
     * @param e
     * @param response
     */
    @ExceptionHandler(value = { SiteWhereSystemException.class })
    protected ResponseEntity<Object> handleSystemException(SiteWhereSystemException e, WebRequest request) {
	String combined = e.getCode() + ":" + e.getMessage();
	HttpHeaders headers = new HttpHeaders();
	headers.add(ISiteWhereWebConstants.HEADER_SITEWHERE_ERROR, e.getMessage());
	headers.add(ISiteWhereWebConstants.HEADER_SITEWHERE_ERROR_CODE, String.valueOf(e.getCode()));
	HttpStatus responseCode = (e.hasHttpResponseCode()) ? HttpStatus.valueOf(e.getHttpResponseCode())
		: HttpStatus.BAD_REQUEST;
	return handleExceptionInternal(e, combined, headers, responseCode, request);
    }

    /**
     * Handles uncaught runtime exceptions such as null pointers.
     * 
     * @param e
     * @param response
     */
    @ExceptionHandler(value = { RuntimeException.class })
    protected ResponseEntity<Object> handleRuntimeException(RuntimeException e, WebRequest request) {
	LOGGER.error("Showing internal server error due to unhandled runtime exception.", e);
	return handleExceptionInternal(e, e.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Send error response including SiteWhere headers.
     * 
     * @param e
     * @param errorCode
     * @param responseCode
     * @param response
     * @throws IOException
     */
    protected void sendErrorResponse(Exception e, ErrorCode errorCode, int responseCode, HttpServletResponse response)
	    throws IOException {
	response.setHeader(ISiteWhereWebConstants.HEADER_SITEWHERE_ERROR, errorCode.getMessage());
	response.setHeader(ISiteWhereWebConstants.HEADER_SITEWHERE_ERROR_CODE, String.valueOf(errorCode.getCode()));
	response.sendError(responseCode, errorCode.getMessage());
    }
}