package com.netflixclone.exception;



public class InvalidRoleException extends RuntimeException {

    public InvalidRoleException(String message) {
        super(message);
    }
}