package com.example.lightmanager.dto;

import com.example.lightmanager.entity.InspectionException;
import com.example.lightmanager.entity.InspectionExceptionRecord;

import java.util.List;

/**
 * 异常处置详情: 异常当前状态 + 连续处置记录(退回/补充/确认/处理/关闭)
 */
public class ExceptionDetailDTO {

    private InspectionException exception;

    private List<InspectionExceptionRecord> records;

    public ExceptionDetailDTO() {}

    public ExceptionDetailDTO(InspectionException exception, List<InspectionExceptionRecord> records) {
        this.exception = exception;
        this.records = records;
    }

    public InspectionException getException() { return exception; }
    public void setException(InspectionException exception) { this.exception = exception; }
    public List<InspectionExceptionRecord> getRecords() { return records; }
    public void setRecords(List<InspectionExceptionRecord> records) { this.records = records; }
}
