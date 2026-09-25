package com.example.lightmanager.dto;

import com.example.lightmanager.entity.InspectionBatch;
import com.example.lightmanager.entity.InspectionItem;

import java.util.List;

/**
 * 批次详情: 批次信息 + 分区内全部灯组(含已登记结果), 供暂存后继续补录
 */
public class BatchDetailDTO {

    private InspectionBatch batch;

    private List<InspectionItem> items;

    public BatchDetailDTO() {}

    public BatchDetailDTO(InspectionBatch batch, List<InspectionItem> items) {
        this.batch = batch;
        this.items = items;
    }

    public InspectionBatch getBatch() { return batch; }
    public void setBatch(InspectionBatch batch) { this.batch = batch; }
    public List<InspectionItem> getItems() { return items; }
    public void setItems(List<InspectionItem> items) { this.items = items; }
}
