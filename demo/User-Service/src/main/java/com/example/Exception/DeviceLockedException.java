package com.example.Exception;

public class DeviceLockedException extends RuntimeException{
    public DeviceLockedException(String message) {
        super(message);
    }
}
