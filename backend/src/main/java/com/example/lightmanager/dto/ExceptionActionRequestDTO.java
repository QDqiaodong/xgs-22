package com.example.lightmanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 异常处置动作请求(退回/补充/确认/登记处理结果/关闭通用)。
 * version 为打开页面时看到的版本号, 用于乐观锁并发冲突检测。
 */
public class ExceptionActionRequestDTO {

    @NotNull(message = "异常ID不能为空")
    private Long exceptionId;

    /** 前端当前持有版本号; 与库中不一致则拒绝并返回最新内容 */
    @NotNull(message = "版本号不能为空")
    private Integer version;

    @NotNull(message = "操作人不能为空")
    private String operator;

    /** 确认异常时的处理意见 */
    private String reviewOpinion;

    /** 确认异常时的处理时限 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleDeadline;

    /** 退回原因 */
    private String returnReason;

    /** 补充说明(被退回后重新填报的现场说明) */
    private String supplementDescription;

    /** 处理结果 */
    private String handleResult;

    public Long getExceptionId() { return exceptionId; }
    public void setExceptionId(Long exceptionId) { this.exceptionId = exceptionId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getReviewOpinion() { return reviewOpinion; }
    public void setReviewOpinion(String reviewOpinion) { this.reviewOpinion = reviewOpinion; }
    public LocalDateTime getHandleDeadline() { return handleDeadline; }
    public void setHandleDeadline(LocalDateTime handleDeadline) { this.handleDeadline = handleDeadline; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public String getSupplementDescription() { return supplementDescription; }
    public void setSupplementDescription(String supplementDescription) { this.supplementDescription = supplementDescription; }
    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
}
