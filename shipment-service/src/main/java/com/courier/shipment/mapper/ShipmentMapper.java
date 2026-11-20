package com.courier.shipment.mapper;

import com.courier.shipment.dto.ShipmentResponse;
import com.courier.shipment.model.Shipment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {
    ShipmentResponse toResponse(Shipment shipment);
}
