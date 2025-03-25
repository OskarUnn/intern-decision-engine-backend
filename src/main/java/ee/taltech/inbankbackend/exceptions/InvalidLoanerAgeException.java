package ee.taltech.inbankbackend.exceptions;

/**
 * Thrown when loaner age is invalid.
 */
public class InvalidLoanerAgeException extends Throwable {
    private final String message;
    private final Throwable cause;

    public InvalidLoanerAgeException(String message) {
        this(message, null);
    }

    public InvalidLoanerAgeException(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
