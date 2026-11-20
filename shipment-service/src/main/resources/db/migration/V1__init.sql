CREATE TABLE shipments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tracking_number VARCHAR(20) NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_name VARCHAR(120) NOT NULL,
    receiver_address VARCHAR(500) NOT NULL,
    receiver_phone VARCHAR(30) NOT NULL,
    weight DECIMAL(10,3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    assigned_courier_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY idx_shipments_tracking_number (tracking_number),
    KEY idx_shipments_sender_id (sender_id),
    KEY idx_shipments_assigned_courier_id (assigned_courier_id),
    KEY idx_shipments_status (status)
);
