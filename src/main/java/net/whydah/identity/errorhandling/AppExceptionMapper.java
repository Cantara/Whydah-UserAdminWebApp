package net.whydah.identity.errorhandling;
//package net.whydah.sso.errorhandling;
//
//import jakarta.ws.rs.core.MediaType;
//import jakarta.ws.rs.core.Response;
//import jakarta.ws.rs.ext.ExceptionMapper;
//import jakarta.ws.rs.ext.Provider;
//
//@Provider
//public class AppExceptionMapper implements ExceptionMapper<AppException> {
//
//	public Response toResponse(AppException ex) {
//		return Response.status(ex.getStatus().value())
//				.entity(new ErrorMessage(ex))
//				.type(MediaType.APPLICATION_JSON).
//				build();
//	}
//
//}
