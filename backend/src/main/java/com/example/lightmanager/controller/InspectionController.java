package com.example.lightmanager.controller;

import com.example.lightmanager.dto.BatchCreateRequestDTO;
import com.example.lightmanager.dto.BatchDetailDTO;
import com.example.lightmanager.dto.BatchSubmitRequestDTO;
import com.example.lightmanager.dto.ExceptionActionRequestDTO;
import com.example.lightmanager.dto.ExceptionDetailDTO;
import com.example.lightmanager.dto.ExceptionPageRequestDTO;
import com.example.lightmanager.dto.ResponseDTO;
import com.example.lightmanager.dto.SaveItemsRequestDTO;
import com.example.lightmanager.entity.InspectionBatch;
import com.example.lightmanager.entity.InspectionException;
import com.example.lightmanager.service.InspectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inspection")
@CrossOrigin(origins = "*")
public class InspectionController {

    private final InspectionService inspectionService;

    public InspectionController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    // ==================== 巡检批次 ====================

    @PostMapping("/batch")
    public ResponseDTO<InspectionBatch> createBatch(@Valid @RequestBody BatchCreateRequestDTO request) {
        return ResponseDTO.success(inspectionService.createBatch(request));
    }

    /** 暂存/补录灯组巡检结果 */
    @PostMapping("/items/save")
    public ResponseDTO<BatchDetailDTO> saveItems(@Valid @RequestBody SaveItemsRequestDTO request) {
        return ResponseDTO.success(inspectionService.saveItems(request));
    }

    /** 提交批次, 生成异常清单 */
    @PostMapping("/batch/submit")
    public ResponseDTO<InspectionBatch> submitBatch(@Valid @RequestBody BatchSubmitRequestDTO request) {
        return ResponseDTO.success(inspectionService.submitBatch(request.getBatchId()));
    }

    @GetMapping("/batch/page")
    public ResponseDTO<List<InspectionBatch>> getBatchPage(
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String inspector,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        var pageInfo = inspectionService.getBatchPage(batchNo, inspector, status, pageNum, pageSize);
        return ResponseDTO.success(pageInfo.getRecords(), pageInfo.getTotal());
    }

    @GetMapping("/batch/{id}")
    public ResponseDTO<BatchDetailDTO> getBatchDetail(@PathVariable Long id) {
        return ResponseDTO.success(inspectionService.getBatchDetail(id));
    }

    // ==================== 异常清单 / 处置 ====================

    @GetMapping("/exception/page")
    public ResponseDTO<List<InspectionException>> getExceptionPage(ExceptionPageRequestDTO request) {
        var pageInfo = inspectionService.getExceptionPage(request);
        return ResponseDTO.success(pageInfo.getRecords(), pageInfo.getTotal());
    }

    @GetMapping("/exception/{id}")
    public ResponseDTO<ExceptionDetailDTO> getExceptionDetail(@PathVariable Long id) {
        return ResponseDTO.success(inspectionService.getExceptionDetail(id));
    }

    /** 复核退回 */
    @PostMapping("/exception/return")
    public ResponseDTO<ExceptionDetailDTO> returnException(@Valid @RequestBody ExceptionActionRequestDTO request) {
        return ResponseDTO.success(inspectionService.returnException(request));
    }

    /** 巡检人补充说明 */
    @PostMapping("/exception/supplement")
    public ResponseDTO<ExceptionDetailDTO> supplement(@Valid @RequestBody ExceptionActionRequestDTO request) {
        return ResponseDTO.success(inspectionService.supplement(request));
    }

    /** 复核确认(处理意见 + 处理时限) */
    @PostMapping("/exception/confirm")
    public ResponseDTO<ExceptionDetailDTO> confirm(@Valid @RequestBody ExceptionActionRequestDTO request) {
        return ResponseDTO.success(inspectionService.confirm(request));
    }

    /** 登记处理结果 */
    @PostMapping("/exception/handle")
    public ResponseDTO<ExceptionDetailDTO> registerHandleResult(@Valid @RequestBody ExceptionActionRequestDTO request) {
        return ResponseDTO.success(inspectionService.registerHandleResult(request));
    }

    /** 关闭(终态) */
    @PostMapping("/exception/close")
    public ResponseDTO<ExceptionDetailDTO> close(@Valid @RequestBody ExceptionActionRequestDTO request) {
        return ResponseDTO.success(inspectionService.close(request));
    }
}
