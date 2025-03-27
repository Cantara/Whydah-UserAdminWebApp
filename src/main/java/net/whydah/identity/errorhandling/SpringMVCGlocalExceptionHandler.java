package net.whydah.identity.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.ws.rs.WebApplicationException;
import java.io.PrintWriter;
import java.io.StringWriter;


@ControllerAdvice
public class SpringMVCGlocalExceptionHandler {


	@ExceptionHandler(AppException.class)
	public ResponseEntity handleAppException(AppException ex) {
		return new ResponseEntity<String>(ExceptionConfig.handleSecurity(new ErrorMessage(ex)).toString(), ex.getStatus());
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity handle(NoHandlerFoundException ex){
		return new ResponseEntity<String>(ExceptionConfig.handleSecurity(new ErrorMessage(ex)).toString(), HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(Throwable.class)
	public ResponseEntity handle(Throwable ex){
		return toResponse(ex);
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


}
