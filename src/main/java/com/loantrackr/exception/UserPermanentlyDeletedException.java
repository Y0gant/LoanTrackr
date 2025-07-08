package com.loantrackr.exception;

public class UserPermanentlyDeletedException extends RuntimeException {
    public UserPermanentlyDeletedException(String message) {
        super(message);
    }
}
