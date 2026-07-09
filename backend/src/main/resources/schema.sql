SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS light_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE light_manager;

CREATE TABLE IF NOT EXISTS garage_zone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分区ID',
    zone_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分区编码',
    zone_name VARCHAR(100) NOT NULL COMMENT '分区名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分区ID',
    level INT DEFAULT 1 COMMENT '层级',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车库分区表';

CREATE TABLE IF NOT EXISTS light_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '灯组ID',
    group_code VARCHAR(50) NOT NULL UNIQUE COMMENT '灯组编号',
    power INT NOT NULL COMMENT '功率(W)',
    zone_id BIGINT NOT NULL COMMENT '所属分区ID',
    installation_location VARCHAR(200) COMMENT '安装位置',
    status TINYINT DEFAULT 1 COMMENT '状态: 0停用 1启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_zone_id (zone_id),
    INDEX idx_group_code (group_code),
    INDEX idx_status (status),
    CONSTRAINT fk_light_zone FOREIGN KEY (zone_id) REFERENCES garage_zone(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='照明灯组表';

CREATE TABLE IF NOT EXISTS batch_transfer_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '台账ID',
    batch_no VARCHAR(64) NOT NULL UNIQUE COMMENT '批次号',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    transfer_time DATETIME NOT NULL COMMENT '划转时间',
    source_zone_ids TEXT COMMENT '源分区ID列表',
    target_zone_id BIGINT NOT NULL COMMENT '目标分区ID',
    light_count INT NOT NULL COMMENT '划转灯组数量',
    description VARCHAR(500) COMMENT '操作描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_batch_no (batch_no),
    INDEX idx_transfer_time (transfer_time),
    INDEX idx_operator (operator)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批量操作台账表';

CREATE TABLE IF NOT EXISTS ledger_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '明细ID',
    ledger_id BIGINT NOT NULL COMMENT '台账ID',
    light_group_id BIGINT NOT NULL COMMENT '灯组ID',
    source_zone_id BIGINT NOT NULL COMMENT '原所属分区ID',
    target_zone_id BIGINT NOT NULL COMMENT '新所属分区ID',
    INDEX idx_ledger_id (ledger_id),
    INDEX idx_light_group_id (light_group_id),
    CONSTRAINT fk_detail_ledger FOREIGN KEY (ledger_id) REFERENCES batch_transfer_ledger(id),
    CONSTRAINT fk_detail_light FOREIGN KEY (light_group_id) REFERENCES light_group(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='台账明细表';

INSERT IGNORE INTO garage_zone (zone_code, zone_name, parent_id, level, sort_order, status) VALUES
('ZONE-A', 'A区车库', 0, 1, 1, 1),
('ZONE-B', 'B区车库', 0, 1, 2, 1),
('ZONE-C', 'C区车库', 0, 1, 3, 1),
('ZONE-A1', 'A区-一层', 1, 2, 1, 1),
('ZONE-A2', 'A区-二层', 1, 2, 2, 1),
('ZONE-B1', 'B区-一层', 2, 2, 1, 1),
('ZONE-B2', 'B区-二层', 2, 2, 2, 1);

INSERT IGNORE INTO light_group (group_code, power, zone_id, installation_location, status) VALUES
('LG-A1-001', 30, 4, 'A区一层-入口处', 1),
('LG-A1-002', 30, 4, 'A区一层-主通道', 1),
('LG-A1-003', 25, 4, 'A区一层-停车位A1', 1),
('LG-A1-004', 25, 4, 'A区一层-停车位A2', 1),
('LG-A1-005', 30, 4, 'A区一层-电梯口', 1),
('LG-A2-001', 30, 5, 'A区二层-入口处', 1),
('LG-A2-002', 30, 5, 'A区二层-主通道', 1),
('LG-A2-003', 25, 5, 'A区二层-停车位B1', 1),
('LG-A2-004', 25, 5, 'A区二层-停车位B2', 1),
('LG-A2-005', 30, 5, 'A区二层-电梯口', 1),
('LG-B1-001', 30, 6, 'B区一层-入口处', 1),
('LG-B1-002', 30, 6, 'B区一层-主通道', 1),
('LG-B1-003', 25, 6, 'B区一层-停车位C1', 1),
('LG-B1-004', 25, 6, 'B区一层-停车位C2', 1),
('LG-B2-001', 30, 7, 'B区二层-入口处', 1),
('LG-B2-002', 30, 7, 'B区二层-主通道', 1),
('LG-B2-003', 25, 7, 'B区二层-停车位D1', 1),
('LG-C-001', 30, 3, 'C区车库-入口处', 1),
('LG-C-002', 30, 3, 'C区车库-主通道', 1),
('LG-C-003', 25, 3, 'C区车库-停车位E1', 1);
