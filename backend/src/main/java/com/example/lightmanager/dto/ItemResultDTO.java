package com.example.lightmanager.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 单个灯组的巡检登记结果(逐个登记/暂存补录)
 */
public class ItemResultDTO {

    @NotNull(message = "灯组ID不能为空")
    private Long lightGroupId;

    /** NORMAL正常 OFF熄灭 FLICKER频闪 DIM亮度不足 */
    @NotNull(message = "巡检结果不能为空")
    private String inspectResult;

    /** 异常时必填(OFF/FLICKER/DIM) */
    private String siteDescription;

    public Long getLightGroupId() { return lightGroupId; }
    public void setLightGroupId(Long lightGroupId) { this.lightGroupId = lightGroupId; }
    public String getInspectResult() { return inspectResult; }
    public void setInspectResult(String inspectResult) { this.inspectResult = inspectResult; }
    public String getSiteDescription() { return siteDescription; }
    public void setSiteDescription(String siteDescription) { this.siteDescription = siteDescription; }
}
