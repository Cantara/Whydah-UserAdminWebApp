package net.whydah.identity.errorhandling;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_XML)
public class AppExceptionMapper implements ExceptionMapper<AppException> {

	public Response toResponse(AppException ex) {
		
		return Response.status(ex.getStatus())
				.entity(ExceptionConfig.handleSecurity(new ErrorMessage(ex)).toString())
				.type(MediaType.APPLICATION_JSON).
				build();
	}

}
