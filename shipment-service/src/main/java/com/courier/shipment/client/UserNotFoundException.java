package com.courier.shipment.client;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id, Throwable cause) { super("User " + id + " was not found", cause); }
}
