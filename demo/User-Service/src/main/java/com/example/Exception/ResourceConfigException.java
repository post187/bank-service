package com.example.Exception;

public class ResourceConfigException extends  GlobalException{
    public ResourceConfigException() {
        super("Resource already present on the server!!!");
    }
    public ResourceConfigException(String message) {
        super(message);
    }
}
