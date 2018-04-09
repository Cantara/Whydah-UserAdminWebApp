package net.whydah.identity.errorhandling;

/**
 * Thrown when authentication fails.
 */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
    }

    public AuthenticationFailedException(String message) {
        super(message);
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationFailedException(Throwable cause) {
        super(cause);
    }
}
