SET NAMES utf8mb4;

-- 灯组巡检异常处置模块建表脚本（幂等，供已有数据库在应用启动时补建表）

CREATE TABLE IF NOT EXISTS inspection_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '巡检批次ID',
    batch_no VARCHAR(64) NOT NULL UNIQUE COMMENT '批次号',
    inspector VARCHAR(50) NOT NULL COMMENT '巡检人(上报人)',
    zone_ids TEXT COMMENT '巡检分区ID列表(逗号分隔)',
    zone_names TEXT COMMENT '巡检分区名称快照(逗号分隔)',
    total_count INT NOT NULL DEFAULT 0 COMMENT '灯组总数',
    recorded_count INT NOT NULL DEFAULT 0 COMMENT '已登记数量',
    abnormal_count INT NOT NULL DEFAULT 0 COMMENT '异常数量(提交时生成)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0暂存(草稿) 1已提交',
    remark VARCHAR(500) COMMENT '批次备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(巡检开始时间)',
    submitted_at DATETIME COMMENT '提交时间(上报时间)',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_batch_no (batch_no),
    INDEX idx_inspector (inspector),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灯组巡检批次表';

CREATE TABLE IF NOT EXISTS inspection_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '巡检结果ID',
    batch_id BIGINT NOT NULL COMMENT '巡检批次ID',
    light_group_id BIGINT NOT NULL COMMENT '灯组ID',
    group_code VARCHAR(50) NOT NULL COMMENT '灯组编号快照',
    installation_location VARCHAR(200) COMMENT '安装位置快照',
    power INT COMMENT '功率快照(W)',
    zone_id BIGINT NOT NULL COMMENT '巡检开始时所属分区ID快照',
    zone_name VARCHAR(100) COMMENT '巡检开始时所属分区名称快照',
    inspect_result VARCHAR(20) COMMENT '巡检结果: NORMAL正常 OFF熄灭 FLICKER频闪 DIM亮度不足',
    site_description VARCHAR(1000) COMMENT '现场说明(异常必填)',
    recorded_at DATETIME COMMENT '登记时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_batch_id (batch_id),
    INDEX idx_light_group_id (light_group_id),
    INDEX idx_inspect_result (inspect_result),
    UNIQUE KEY uk_batch_light (batch_id, light_group_id),
    CONSTRAINT fk_item_batch FOREIGN KEY (batch_id) REFERENCES inspection_batch(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灯组巡检结果表(原始结果不可覆盖)';

CREATE TABLE IF NOT EXISTS inspection_exception (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '异常ID',
    batch_id BIGINT NOT NULL COMMENT '巡检批次ID',
    item_id BIGINT NOT NULL COMMENT '巡检结果ID',
    light_group_id BIGINT NOT NULL COMMENT '灯组ID',
    group_code VARCHAR(50) NOT NULL COMMENT '灯组编号快照',
    installation_location VARCHAR(200) COMMENT '安装位置快照',
    snapshot_zone_id BIGINT NOT NULL COMMENT '巡检时分区ID快照',
    snapshot_zone_name VARCHAR(100) COMMENT '巡检时分区名称快照',
    exception_type VARCHAR(20) NOT NULL COMMENT '异常类型: OFF熄灭 FLICKER频闪 DIM亮度不足',
    site_description VARCHAR(1000) NOT NULL COMMENT '现场说明(最新,含历次补充)',
    original_description VARCHAR(1000) NOT NULL COMMENT '首次上报的原始现场说明',
    reporter VARCHAR(50) NOT NULL COMMENT '上报人',
    reported_at DATETIME NOT NULL COMMENT '上报时间(批次提交时间)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING待复核 RETURNED已退回待补充 CONFIRMED已确认待处理 CLOSED已关闭',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    review_opinion VARCHAR(1000) COMMENT '确认异常时的处理意见',
    handle_deadline DATETIME COMMENT '处理时限',
    reviewer VARCHAR(50) COMMENT '确认人',
    reviewed_at DATETIME COMMENT '确认时间',
    handler VARCHAR(50) COMMENT '处理人',
    handle_result VARCHAR(2000) COMMENT '处理结果',
    handled_at DATETIME COMMENT '处理结果登记时间',
    closer VARCHAR(50) COMMENT '关闭人',
    closed_at DATETIME COMMENT '关闭时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_batch_id (batch_id),
    INDEX idx_item_id (item_id),
    INDEX idx_light_group_id (light_group_id),
    INDEX idx_status (status),
    CONSTRAINT fk_exception_batch FOREIGN KEY (batch_id) REFERENCES inspection_batch(id),
    CONSTRAINT fk_exception_item FOREIGN KEY (item_id) REFERENCES inspection_item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡检异常处置表';

CREATE TABLE IF NOT EXISTS inspection_exception_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    exception_id BIGINT NOT NULL COMMENT '异常ID',
    action VARCHAR(30) NOT NULL COMMENT '动作: SUBMIT上报 RETURN退回 SUPPLEMENT补充 CONFIRM确认 HANDLE登记处理结果 CLOSE关闭',
    content TEXT COMMENT '操作内容/意见说明',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    operated_at DATETIME NOT NULL COMMENT '操作时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_exception_id (exception_id),
    INDEX idx_operated_at (operated_at),
    CONSTRAINT fk_record_exception FOREIGN KEY (exception_id) REFERENCES inspection_exception(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡检异常处置流转记录表';
