package com.courier.tracking.event;

public class InvalidShipmentEventException extends RuntimeException {
    public InvalidShipmentEventException(String message) { super(message); }
}
