package com.deployfast.taskmanagement.exception;

/**
 * Exception levée lorsque les données métier ne sont pas valides.
 */
public class BusinessValidationException extends RuntimeException {
    
    private final String errorCode;

    public BusinessValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
    }

    public BusinessValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
