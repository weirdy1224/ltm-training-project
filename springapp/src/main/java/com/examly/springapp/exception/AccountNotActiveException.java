package com.examly.springapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised after a CORRECT password when the account may not log in yet: the email address has not been
 * verified with the OTP (code EMAIL_NOT_VERIFIED), or an instructor is still waiting for an admin to
 * approve the account (code APPROVAL_PENDING). Answered with 403 and the code in the JSON body.
 *
 * @author Lokesh
 */
public class AccountNotActiveException extends AppException {
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String APPROVAL_PENDING = "APPROVAL_PENDING";

    private final String code;

    public AccountNotActiveException(String code, String message) {
        super(HttpStatus.FORBIDDEN, message);
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
