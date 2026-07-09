package com.example.lightmanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ledger_detail")
public class LedgerDetail {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long ledgerId;
    
    private Long lightGroupId;
    
    private Long sourceZoneId;
    
    private Long targetZoneId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public Long getLightGroupId() { return lightGroupId; }
    public void setLightGroupId(Long lightGroupId) { this.lightGroupId = lightGroupId; }
    public Long getSourceZoneId() { return sourceZoneId; }
    public void setSourceZoneId(Long sourceZoneId) { this.sourceZoneId = sourceZoneId; }
    public Long getTargetZoneId() { return targetZoneId; }
    public void setTargetZoneId(Long targetZoneId) { this.targetZoneId = targetZoneId; }
}