CREATE TABLE tracking_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(36) NULL,
    tracking_number VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    location VARCHAR(200),
    description VARCHAR(1000),
    updated_by BIGINT,
    occurred_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tracking_events_event_id (event_id),
    KEY idx_tracking_events_tracking_number (tracking_number),
    KEY idx_tracking_events_tracking_occurred (tracking_number, occurred_at)
);
