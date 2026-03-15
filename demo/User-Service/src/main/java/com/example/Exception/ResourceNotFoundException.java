package com.example.Exception;


public class ResourceNotFoundException extends GlobalException{
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException() {
        super("Resource not found");
    }
}
