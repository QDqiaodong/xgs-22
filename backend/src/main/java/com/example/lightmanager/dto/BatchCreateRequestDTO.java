package com.example.lightmanager.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BatchCreateRequestDTO {

    @NotNull(message = "巡检人不能为空")
    private String inspector;

    @NotEmpty(message = "请至少选择一个巡检分区")
    private List<Long> zoneIds;

    private String remark;

    public String getInspector() { return inspector; }
    public void setInspector(String inspector) { this.inspector = inspector; }
    public List<Long> getZoneIds() { return zoneIds; }
    public void setZoneIds(List<Long> zoneIds) { this.zoneIds = zoneIds; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
