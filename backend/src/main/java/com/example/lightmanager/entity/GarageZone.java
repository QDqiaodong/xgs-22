package com.example.lightmanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.List;

@TableName("garage_zone")
public class GarageZone {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String zoneCode;
    
    private String zoneName;
    
    private Long parentId;
    
    private Integer level;
    
    private Integer sortOrder;
    
    private Integer status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private List<GarageZone> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<GarageZone> getChildren() { return children; }
    public void setChildren(List<GarageZone> children) { this.children = children; }
}