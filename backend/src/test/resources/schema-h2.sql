-- H2 (MySQL 兼容模式) 测试库结构, 与主 schema.sql 保持表/列一致

CREATE TABLE IF NOT EXISTS garage_zone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_code VARCHAR(50) NOT NULL UNIQUE,
    zone_name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS light_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_code VARCHAR(50) NOT NULL UNIQUE,
    power INT NOT NULL,
    zone_id BIGINT NOT NULL,
    installation_location VARCHAR(200),
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_light_zone FOREIGN KEY (zone_id) REFERENCES garage_zone(id)
);

CREATE TABLE IF NOT EXISTS batch_transfer_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no VARCHAR(64) NOT NULL UNIQUE,
    operator VARCHAR(50) NOT NULL,
    transfer_time DATETIME NOT NULL,
    source_zone_ids VARCHAR(1000),
    target_zone_id BIGINT NOT NULL,
    light_count INT NOT NULL,
    description VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ledger_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ledger_id BIGINT NOT NULL,
    light_group_id BIGINT NOT NULL,
    source_zone_id BIGINT NOT NULL,
    target_zone_id BIGINT NOT NULL,
    CONSTRAINT fk_detail_ledger FOREIGN KEY (ledger_id) REFERENCES batch_transfer_ledger(id),
    CONSTRAINT fk_detail_light FOREIGN KEY (light_group_id) REFERENCES light_group(id)
);

CREATE TABLE IF NOT EXISTS inspection_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no VARCHAR(64) NOT NULL UNIQUE,
    inspector VARCHAR(50) NOT NULL,
    zone_ids CLOB,
    zone_names CLOB,
    total_count INT NOT NULL DEFAULT 0,
    recorded_count INT NOT NULL DEFAULT 0,
    abnormal_count INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    submitted_at DATETIME,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inspection_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    light_group_id BIGINT NOT NULL,
    group_code VARCHAR(50) NOT NULL,
    installation_location VARCHAR(200),
    power INT,
    zone_id BIGINT NOT NULL,
    zone_name VARCHAR(100),
    inspect_result VARCHAR(20),
    site_description VARCHAR(1000),
    recorded_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_light UNIQUE (batch_id, light_group_id),
    CONSTRAINT fk_item_batch FOREIGN KEY (batch_id) REFERENCES inspection_batch(id)
);

CREATE TABLE IF NOT EXISTS inspection_exception (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    light_group_id BIGINT NOT NULL,
    group_code VARCHAR(50) NOT NULL,
    installation_location VARCHAR(200),
    snapshot_zone_id BIGINT NOT NULL,
    snapshot_zone_name VARCHAR(100),
    exception_type VARCHAR(20) NOT NULL,
    site_description VARCHAR(1000) NOT NULL,
    original_description VARCHAR(1000) NOT NULL,
    reporter VARCHAR(50) NOT NULL,
    reported_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    version INT NOT NULL DEFAULT 0,
    review_opinion VARCHAR(1000),
    handle_deadline DATETIME,
    reviewer VARCHAR(50),
    reviewed_at DATETIME,
    handler VARCHAR(50),
    handle_result VARCHAR(2000),
    handled_at DATETIME,
    closer VARCHAR(50),
    closed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exception_batch FOREIGN KEY (batch_id) REFERENCES inspection_batch(id),
    CONSTRAINT fk_exception_item FOREIGN KEY (item_id) REFERENCES inspection_item(id)
);

CREATE TABLE IF NOT EXISTS inspection_exception_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exception_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    content CLOB,
    operator VARCHAR(50) NOT NULL,
    operated_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_record_exception FOREIGN KEY (exception_id) REFERENCES inspection_exception(id)
);
