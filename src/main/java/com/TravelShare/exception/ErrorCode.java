package com.TravelShare.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    TRIP_NOT_EXISTED(1009, "Trip not existed", HttpStatus.NOT_FOUND),
    CURRENCY_EXISTED(1010, "Currency existed", HttpStatus.BAD_REQUEST),
    CURRENCY_NOT_EXISTED(1011, "Currency not existed", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_EXISTED(1011, "Category not existed", HttpStatus.NOT_FOUND),
    EXPENSE_NOT_EXISTED(1012, "Expense not existed", HttpStatus.NOT_FOUND),
    EXPENSE_EXISTED(1013, "Expense existed", HttpStatus.BAD_REQUEST),
    EXPENSE_SPLIT_EXISTED(1014, "Expense split existed", HttpStatus.BAD_REQUEST),
    EXPENSE_SPLIT_NOT_EXISTED(1015, "Expense split not existed", HttpStatus.NOT_FOUND),
    MEDIA_NOT_EXISTED(1016, "Media not existed", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED(1017, "File upload failed", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE(1018, "File type unsupport", HttpStatus.BAD_REQUEST),
    INVITATION_NOT_FOUND(1019, "Invitation not found", HttpStatus.NOT_FOUND),
    PARTICIPANT_NOT_EXISTED(1020, "Participant not existed", HttpStatus.NOT_FOUND),
    INVALID_SPLIT_TYPE(1021, "Invalid split type", HttpStatus.NOT_FOUND),
    INVALID_REQUEST(1022, "Invalid request", HttpStatus.NOT_FOUND),
    FILE_NOT_FOUND(1022, "File not found", HttpStatus.NOT_FOUND)
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
