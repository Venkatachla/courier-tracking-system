package com.courier.tracking.client;

public class ShipmentServiceUnavailableException extends RuntimeException {
    public ShipmentServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
}
