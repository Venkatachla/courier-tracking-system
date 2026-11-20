package com.courier.tracking.client;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(String trackingNumber, Throwable cause) { super("Shipment " + trackingNumber + " was not found", cause); }
}
