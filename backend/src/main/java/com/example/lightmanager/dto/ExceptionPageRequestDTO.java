package com.example.lightmanager.dto;

public class ExceptionPageRequestDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String batchNo;

    private String groupCode;

    private String exceptionType;

    private String status;

    private String reporter;

    /** 1只看分区已变化 0只看未变化 null全部 */
    private Integer zoneChanged;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public Integer getZoneChanged() { return zoneChanged; }
    public void setZoneChanged(Integer zoneChanged) { this.zoneChanged = zoneChanged; }
}
