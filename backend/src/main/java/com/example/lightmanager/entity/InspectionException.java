package com.example.lightmanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@TableName("inspection_exception")
public class InspectionException {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;

    private Long itemId;

    private Long lightGroupId;

    private String groupCode;

    private String installationLocation;

    private Long snapshotZoneId;

    private String snapshotZoneName;

    /** OFF熄灭 FLICKER频闪 DIM亮度不足 */
    private String exceptionType;

    /** 现场说明(最新, 含历次补充) */
    private String siteDescription;

    /** 首次上报的原始现场说明 */
    private String originalDescription;

    private String reporter;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportedAt;

    /** PENDING待复核 RETURNED已退回待补充 CONFIRMED已确认待处理 CLOSED已关闭 */
    private String status;

    /** 乐观锁版本号: 所有状态流转均按 id + version + 前置状态 条件更新, 保证并发下只有一次生效 */
    private Integer version;

    private String reviewOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleDeadline;

    private String reviewer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;

    private String handler;

    private String handleResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handledAt;

    private String closer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /** 以下为查询时实时比对的非持久化展示字段 */
    @TableField(exist = false)
    private Long currentZoneId;

    @TableField(exist = false)
    private String currentZoneName;

    @TableField(exist = false)
    private Integer zoneChanged;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getLightGroupId() { return lightGroupId; }
    public void setLightGroupId(Long lightGroupId) { this.lightGroupId = lightGroupId; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getInstallationLocation() { return installationLocation; }
    public void setInstallationLocation(String installationLocation) { this.installationLocation = installationLocation; }
    public Long getSnapshotZoneId() { return snapshotZoneId; }
    public void setSnapshotZoneId(Long snapshotZoneId) { this.snapshotZoneId = snapshotZoneId; }
    public String getSnapshotZoneName() { return snapshotZoneName; }
    public void setSnapshotZoneName(String snapshotZoneName) { this.snapshotZoneName = snapshotZoneName; }
    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
    public String getSiteDescription() { return siteDescription; }
    public void setSiteDescription(String siteDescription) { this.siteDescription = siteDescription; }
    public String getOriginalDescription() { return originalDescription; }
    public void setOriginalDescription(String originalDescription) { this.originalDescription = originalDescription; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getReviewOpinion() { return reviewOpinion; }
    public void setReviewOpinion(String reviewOpinion) { this.reviewOpinion = reviewOpinion; }
    public LocalDateTime getHandleDeadline() { return handleDeadline; }
    public void setHandleDeadline(LocalDateTime handleDeadline) { this.handleDeadline = handleDeadline; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }
    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }
    public String getCloser() { return closer; }
    public void setCloser(String closer) { this.closer = closer; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getCurrentZoneId() { return currentZoneId; }
    public void setCurrentZoneId(Long currentZoneId) { this.currentZoneId = currentZoneId; }
    public String getCurrentZoneName() { return currentZoneName; }
    public void setCurrentZoneName(String currentZoneName) { this.currentZoneName = currentZoneName; }
    public Integer getZoneChanged() { return zoneChanged; }
    public void setZoneChanged(Integer zoneChanged) { this.zoneChanged = zoneChanged; }
}
