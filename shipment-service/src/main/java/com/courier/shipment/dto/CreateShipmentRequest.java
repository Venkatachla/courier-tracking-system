package com.courier.shipment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateShipmentRequest(
        @NotBlank @Size(max = 120) String receiverName,
        @NotBlank @Size(max = 500) String receiverAddress,
        @NotBlank @Size(max = 30) String receiverPhone,
        @NotNull @DecimalMin(value = "0.001") BigDecimal weight) {}
