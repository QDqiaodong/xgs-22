package com.example.lightmanager.dto;

import jakarta.validation.constraints.NotNull;

public class BatchSubmitRequestDTO {

    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
}
