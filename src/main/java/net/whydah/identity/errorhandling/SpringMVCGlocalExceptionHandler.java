package net.whydah.identity.errorhandling;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.exoreaction.notification.SlackNotificationFacade;


@ControllerAdvice
public class SpringMVCGlocalExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(SpringMVCGlocalExceptionHandler.class);
	private String serviceName="UAWA";

	@ExceptionHandler(AppException.class)
	public ResponseEntity handleAppException(AppException ex, HttpServletRequest request) {

		// Send to Slack as WARNING (expected/handled exceptions)
		try {
			Map<String, Object> context = buildExceptionContext(ex, request);
			SlackNotificationFacade.handleExceptionAsWarning(
					ex, 
					request.getRequestURI(), 
					"Application exception in " + serviceName, 
					context
					);
		} catch (Exception slackEx) {
			logger.error("Failed to send Slack notification", slackEx);
		}

		return new ResponseEntity<String>(ExceptionConfig.handleSecurity(new ErrorMessage(ex)).toString(), ex.getStatus());
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity handle(NoHandlerFoundException ex){
		return new ResponseEntity<String>(ExceptionConfig.handleSecurity(new ErrorMessage(ex)).toString(), HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(Throwable.class)
	public ResponseEntity<String> handleThrowable(Throwable ex, HttpServletRequest request) {
		logger.error("Unhandled exception at {}", request.getRequestURI(), ex);

		// Send to Slack as ALARM (unexpected/unhandled exceptions)
		try {
			Map<String, Object> context = buildExceptionContext(ex, request);

			// This sends to the "errors" channel with full stack trace
			SlackNotificationFacade.handleException(
					ex, 
					request.getRequestURI(), 
					"Unhandled exception in " + serviceName, 
					context
					);

		} catch (Exception slackEx) {
			logger.error("Failed to send Slack notification", slackEx);
		}

		return toResponse(ex);
	}
	
	@ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(
            AccessDeniedException ex, 
            HttpServletRequest request) {
        
        logger.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        
        // Send security issues to Slack
        try {
            Map<String, Object> context = buildSecurityContext(ex, request);
            SlackNotificationFacade.handleExceptionAsWarning(
                ex,
                request.getRequestURI(),
                "Access denied - Security alert in " + serviceName,
                context
            );
        } catch (Exception slackEx) {
            logger.error("Failed to send Slack notification", slackEx);
        }
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.FORBIDDEN.value());
        errorMessage.setMessage("Access denied");
        
        return new ResponseEntity<>(
            ExceptionConfig.handleSecurity(errorMessage).toString(),
            HttpStatus.FORBIDDEN
        );
    }

	private ResponseEntity toResponse(Throwable ex) {

		ErrorMessage errorMessage = new ErrorMessage();		
		setHttpStatus(ex, errorMessage);
		errorMessage.setCode(9999);
		errorMessage.setMessage(ex.getMessage());
		StringWriter errorStackTrace = new StringWriter();
		ex.printStackTrace(new PrintWriter(errorStackTrace));
		errorMessage.setDeveloperMessage(errorStackTrace.toString());
		errorMessage.setLink("");
		errorMessage = ExceptionConfig.handleSecurity(errorMessage);
		return new ResponseEntity<String>(errorMessage.toString(), HttpStatus.valueOf(errorMessage.status));

	}

	private void setHttpStatus(Throwable ex, ErrorMessage errorMessage) {
		if (ex instanceof WebApplicationException exception) {
			errorMessage.setStatus(exception.getResponse().getStatus());
		} else {
			errorMessage.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()); //defaults to internal server error 500
		}
	}

	private Map<String, Object> buildExceptionContext(Throwable ex, HttpServletRequest request) {
		Map<String, Object> context = new HashMap<>();

		// Request information
		if (request != null) {
			context.put("method", request.getMethod());
			context.put("uri", request.getRequestURI());
			context.put("queryString", request.getQueryString());
			context.put("remoteAddr", request.getRemoteAddr());
			context.put("userAgent", request.getHeader("User-Agent"));

			// User info if available
			if (request.getUserPrincipal() != null) {
				context.put("user", request.getUserPrincipal().getName());
			}

			// Session info if available
			if (request.getSession(false) != null) {
				context.put("sessionId", request.getSession().getId());
			}
		}

		// Exception information
		context.put("exceptionType", ex.getClass().getName());
		context.put("exceptionMessage", ex.getMessage());

		// AppException specific fields
		if (ex instanceof AppException) {
			AppException appEx = (AppException) ex;
			context.put("errorCode", appEx.getCode());
			context.put("httpStatus", appEx.getStatus().value());
			if (appEx.getDeveloperMessage() != null) {
				context.put("developerMessage", appEx.getDeveloperMessage());
			}
		}

		// Service context
		context.put("service", serviceName);
		context.put("timestamp", Instant.now().toString());

		return context;
	}
	
	private Map<String, Object> buildSecurityContext(Throwable ex, HttpServletRequest request) {
        Map<String, Object> context = new HashMap<>();
        
        if (request != null) {
            context.put("uri", request.getRequestURI());
            context.put("method", request.getMethod());
            context.put("remoteAddr", request.getRemoteAddr());
            context.put("userAgent", request.getHeader("User-Agent"));
            context.put("referer", request.getHeader("Referer"));
            context.put("origin", request.getHeader("Origin"));
            
            // User attempting access
            if (request.getUserPrincipal() != null) {
                context.put("attemptedUser", request.getUserPrincipal().getName());
            }
            
            // Session info
            if (request.getSession(false) != null) {
                context.put("sessionId", request.getSession().getId());
            }
        }
        
        context.put("service", serviceName);
        context.put("securityEvent", "ACCESS_DENIED");
        context.put("timestamp", Instant.now().toString());
        
        return context;
    }


}
