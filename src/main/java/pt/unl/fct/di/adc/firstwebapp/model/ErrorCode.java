package pt.unl.fct.di.adc.firstwebapp.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Canonical API error codes per spec.
 * Keep the JSON "status" as a string, e.g. "9906".
 */
public enum ErrorCode {
    INVALID_CREDENTIALS("9900", "The username-password pair is not valid"),
    USER_ALREADY_EXISTS("9901", "Error in creating an account because the username already exists"),
    USER_NOT_FOUND("9902", "The username referred in the operation doesn't exist in registered accounts"),
    INVALID_TOKEN("9903", "The operation is called with an invalid token (wrong format for example)"),
    TOKEN_EXPIRED("9904", "The operation is called with a token that is expired"),
    UNAUTHORIZED("9905", "The operation is not allowed for the user role"),
    INVALID_INPUT("9906", "The call is using input data not following the correct specification"),
    FORBIDDEN("9907", "The operation generated a forbidden error by other reason");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = Objects.requireNonNull(code, "code");
        this.defaultMessage = Objects.requireNonNull(defaultMessage, "defaultMessage");
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    /**
     * Returns the matching ErrorCode for a numeric string like "9906".
     *
     * @throws IllegalArgumentException if no matching code exists
     */
    public static ErrorCode fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("code is blank");
        }
        final String normalized = code.trim();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown error code: " + normalized));
    }
}
