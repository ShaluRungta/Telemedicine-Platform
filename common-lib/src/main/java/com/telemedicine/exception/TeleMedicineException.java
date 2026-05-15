package com.telemedicine.exception;

public class TeleMedicineException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;

    public TeleMedicineException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public TeleMedicineException(String message, Throwable cause, int statusCode, String errorCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class NotFoundException extends TeleMedicineException {
        public NotFoundException(String message) {
            super(message, 404, "NOT_FOUND");
        }
    }

    public static class BadRequestException extends TeleMedicineException {
        public BadRequestException(String message) {
            super(message, 400, "BAD_REQUEST");
        }
    }

    public static class UnauthorizedException extends TeleMedicineException {
        public UnauthorizedException(String message) {
            super(message, 401, "UNAUTHORIZED");
        }
    }

    public static class ForbiddenException extends TeleMedicineException {
        public ForbiddenException(String message) {
            super(message, 403, "FORBIDDEN");
        }
    }

    public static class ConflictException extends TeleMedicineException {
        public ConflictException(String message) {
            super(message, 409, "CONFLICT");
        }
    }
}
