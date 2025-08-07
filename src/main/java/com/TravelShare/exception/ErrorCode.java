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
    USER_NOT_EXISTED(1005, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    TRIP_NOT_EXISTED(1009, "Trip not existed", HttpStatus.NOT_FOUND),
    GROUP_NOT_EXISTED(1010, "Group not existed", HttpStatus.NOT_FOUND),
    CURRENCY_EXISTED(1011, "Currency existed", HttpStatus.BAD_REQUEST),
    CURRENCY_NOT_EXISTED(1012, "Currency not existed", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_EXISTED(1013, "Category not existed", HttpStatus.NOT_FOUND),
    EXPENSE_NOT_EXISTED(1014, "Expense not existed", HttpStatus.NOT_FOUND),
    EXPENSE_EXISTED(1015, "Expense existed", HttpStatus.BAD_REQUEST),
    EXPENSE_SPLIT_EXISTED(1016, "Expense split existed", HttpStatus.BAD_REQUEST),
    EXPENSE_SPLIT_NOT_EXISTED(1017, "Expense split not existed", HttpStatus.NOT_FOUND),
    MEDIA_NOT_EXISTED(1018, "Media not existed", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED(1019, "File upload failed", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE(1020, "File type unsupport", HttpStatus.BAD_REQUEST),
    INVITATION_NOT_FOUND(1021, "Invitation not found", HttpStatus.NOT_FOUND),
    PARTICIPANT_NOT_EXISTED(1022, "Participant not existed", HttpStatus.NOT_FOUND),
    INVALID_SPLIT_TYPE(1023, "Invalid split type", HttpStatus.NOT_FOUND),
    INVALID_REQUEST(1024, "Invalid request", HttpStatus.NOT_FOUND),
    FILE_NOT_FOUND(1025, "File not found", HttpStatus.NOT_FOUND),
    SETTLEMENT_NOT_FOUND(1026, "Settlement not found", HttpStatus.NOT_FOUND),
    INVALID_TOKEN_TYPE(1027, "Invalid token type", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_DEVICE(1028, "UNAUTHORIZED DEVICE", HttpStatus.NOT_FOUND),
    USER_NOT_ACTIVE(1029, "Email not active, pls check your email!", HttpStatus.FORBIDDEN),
    TOKEN_USED(1030, "Your token used", HttpStatus.BAD_REQUEST),
    TOKEN_EXPIRED(1031, "Your token expired", HttpStatus.BAD_REQUEST),
    PARTICIPANT_ALREADY_LINKED(1032, "This participant already linked", HttpStatus.BAD_REQUEST),
    USER_NOT_IN_GROUP(1033, "Bạn không có ở trong nhóm này rồi", HttpStatus.NOT_FOUND),
    USER_ALREADY_IN_GROUP(1034, "Bạn đã ở trong nhóm này rồi", HttpStatus.NOT_FOUND),
    CANNOT_LEAVE_TRIP_AS_LAST_ADMIN(1035, "Your is last admin in Trip, can't leave", HttpStatus.BAD_REQUEST),
    CANNOT_LEAVE_AS_LAST_ADMIN(1036, "Your is last admin in Group, can't leave", HttpStatus.BAD_REQUEST),
    CANNOT_REMOVE_LAST_ADMIN(1037, "Your is last admin in Group, can't update role", HttpStatus.BAD_REQUEST),
    CANNOT_DEMOTE_LAST_ADMIN(1038, "Your is last admin in Group, can't demote", HttpStatus.BAD_REQUEST),
    NOT_GROUP_ADMIN(1039, "You are not admin of this group", HttpStatus.FORBIDDEN),
    EMAIL_NOT_EXISTED(1040, "Email không tồn tại trong hệ thống !!!", HttpStatus.NOT_FOUND),
    PASSWORD_NOT_MATCH(1041, "Password not match", HttpStatus.BAD_REQUEST),
    CATEGORY_EXISTED(1042, "Danh mục đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_ALREADY_EXISTS(1043, "Tên đăng nhập đã tồn tại !!! Hãy nhập tên đăng nhập khác", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(1044, "Email đã tồn tại !!! Hãy nhập email khác", HttpStatus.BAD_REQUEST),
    PHONE_NUMBER_ALREADY_EXISTS(1045, "Số điện thoại đã tồn tại !!! Hãy nhập số điện thoại khác", HttpStatus.BAD_REQUEST),
    JOIN_CODE_NOT_EXISTED(1046, "Mã tham gia không đúng, vui lòng kiểm tra lại!!!", HttpStatus.NOT_FOUND),
    NOT_GROUP_MEMBER(1047, "You are not member of this group", HttpStatus.FORBIDDEN),
    CANNOT_DELETE_GROUP_CATEGORY(1048, "Cannot delete group category", HttpStatus.BAD_REQUEST),
    INVALID_CATEGORY_TYPE(1049, "Invalid category type", HttpStatus.BAD_REQUEST),
    CANNOT_UPDATE_SYSTEM_CATEGORY(1050, "Cannot update system category", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_SYSTEM_CATEGORY(1051, "Cannot delete system category", HttpStatus.BAD_REQUEST),
    CANNOT_UPDATE_CATEGORY_TYPE(1052, "Cannot update category of this type", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_CATEGORY_TYPE(1053, "Cannot delete category of this type", HttpStatus.BAD_REQUEST),
    REQUEST_NOT_EXISTED(1054, "Yêu cầu không tồn tại", HttpStatus.NOT_FOUND),
    REQUEST_ALREADY_HANDLED(1055, "Yêu cầu đã được xử lý", HttpStatus.BAD_REQUEST),
    PARTICIPANT_NOT_IN_GROUP(1056, "Người dùng này không tồn tại trong Group", HttpStatus.BAD_REQUEST),
    ALREADY_INVITED(1057, "Đã gửi lời mời", HttpStatus.BAD_REQUEST),
    TOKEN_NOT_EXISTED(1031, "Token của bạn không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    NO_ADMIN_FOUND(1032, "Không tìm thấy admin", HttpStatus.NOT_FOUND),
    // Expense Finalization error codes
    FINALIZATION_NOT_FOUND(1058, "Yêu cầu tất toán không tồn tại", HttpStatus.NOT_FOUND),
    FINALIZATION_ALREADY_PENDING(1059, "Đã có yêu cầu tất toán đang chờ xử lý", HttpStatus.BAD_REQUEST),
    FINALIZATION_ALREADY_PROCESSED(1060, "Yêu cầu tất toán đã được xử lý", HttpStatus.BAD_REQUEST),
    EXPENSE_LOCKED(1061, "Chi phí đã bị khóa, không thể chỉnh sửa", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
