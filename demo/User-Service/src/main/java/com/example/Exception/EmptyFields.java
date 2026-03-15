package com.example.Exception;


public class EmptyFields extends GlobalException {
    public EmptyFields(String responseCodeNotFound, String message) {
        super(responseCodeNotFound, message);
    }
    public EmptyFields(String message) {
        super(message);
    }
}
