package com.courier.tracking.mapper;

import com.courier.tracking.dto.TimelineEntryResponse;
import com.courier.tracking.dto.TrackingHistoryResponse;
import com.courier.tracking.model.TrackingEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TrackingEventMapper {
    @Mapping(source = "occurredAt", target = "timestamp")
    TimelineEntryResponse toTimelineEntry(TrackingEvent event);

    @Mapping(source = "occurredAt", target = "timestamp")
    TrackingHistoryResponse toHistory(TrackingEvent event);
}
