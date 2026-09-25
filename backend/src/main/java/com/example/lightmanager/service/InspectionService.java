package com.example.lightmanager.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lightmanager.dto.BatchCreateRequestDTO;
import com.example.lightmanager.dto.BatchDetailDTO;
import com.example.lightmanager.dto.ExceptionActionRequestDTO;
import com.example.lightmanager.dto.ExceptionDetailDTO;
import com.example.lightmanager.dto.ExceptionPageRequestDTO;
import com.example.lightmanager.dto.SaveItemsRequestDTO;
import com.example.lightmanager.entity.InspectionBatch;
import com.example.lightmanager.entity.InspectionException;

public interface InspectionService {

    /** 创建巡检批次: 选择分区, 快照分区内灯组清单 */
    InspectionBatch createBatch(BatchCreateRequestDTO request);

    /** 暂存/补录单个或多个灯组巡检结果 */
    BatchDetailDTO saveItems(SaveItemsRequestDTO request);

    /** 提交批次: 生成异常清单, 原始结果固化不可再改 */
    InspectionBatch submitBatch(Long batchId);

    /** 批次分页 */
    IPage<InspectionBatch> getBatchPage(String batchNo, String inspector, Integer status,
                                        Integer pageNum, Integer pageSize);

    /** 批次详情(含已登记结果) */
    BatchDetailDTO getBatchDetail(Long batchId);

    /** 异常清单分页(含实时分区变化标记) */
    IPage<InspectionException> getExceptionPage(ExceptionPageRequestDTO request);

    /** 异常详情 + 完整处置记录 */
    ExceptionDetailDTO getExceptionDetail(Long exceptionId);

    /** 复核退回: 待复核 -> 已退回待补充 */
    ExceptionDetailDTO returnException(ExceptionActionRequestDTO request);

    /** 巡检人补充: 已退回 -> 待复核 */
    ExceptionDetailDTO supplement(ExceptionActionRequestDTO request);

    /** 复核确认: 待复核 -> 已确认(填处理意见+处理时限) */
    ExceptionDetailDTO confirm(ExceptionActionRequestDTO request);

    /** 登记处理结果: 已确认状态下登记, 状态不变 */
    ExceptionDetailDTO registerHandleResult(ExceptionActionRequestDTO request);

    /** 关闭: 已确认 -> 已关闭(终态) */
    ExceptionDetailDTO close(ExceptionActionRequestDTO request);
}
