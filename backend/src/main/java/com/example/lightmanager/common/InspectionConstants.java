package com.example.lightmanager.common;

/**
 * 灯组巡检异常处置相关常量
 */
public final class InspectionConstants {

    private InspectionConstants() {}

    // 巡检结果
    public static final String RESULT_NORMAL = "NORMAL";
    public static final String RESULT_OFF = "OFF";
    public static final String RESULT_FLICKER = "FLICKER";
    public static final String RESULT_DIM = "DIM";

    // 批次状态
    public static final int BATCH_DRAFT = 0;
    public static final int BATCH_SUBMITTED = 1;

    // 异常处置状态
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CLOSED = "CLOSED";

    // 处置流转动作
    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_RETURN = "RETURN";
    public static final String ACTION_SUPPLEMENT = "SUPPLEMENT";
    public static final String ACTION_CONFIRM = "CONFIRM";
    public static final String ACTION_HANDLE = "HANDLE";
    public static final String ACTION_CLOSE = "CLOSE";
}
