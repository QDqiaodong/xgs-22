package com.example.lightmanager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 暂存: 提交一个或多个灯组的登记结果, 可反复补录
 */
public class SaveItemsRequestDTO {

    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    @NotEmpty(message = "巡检结果不能为空")
    @Valid
    private List<ItemResultDTO> items;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public List<ItemResultDTO> getItems() { return items; }
    public void setItems(List<ItemResultDTO> items) { this.items = items; }
}
