package com.example.lightmanager.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lightmanager.common.BusinessException;
import com.example.lightmanager.common.InspectionConstants;
import com.example.lightmanager.dto.BatchCreateRequestDTO;
import com.example.lightmanager.dto.BatchDetailDTO;
import com.example.lightmanager.dto.ExceptionActionRequestDTO;
import com.example.lightmanager.dto.ExceptionDetailDTO;
import com.example.lightmanager.dto.ExceptionPageRequestDTO;
import com.example.lightmanager.dto.ItemResultDTO;
import com.example.lightmanager.dto.SaveItemsRequestDTO;
import com.example.lightmanager.entity.GarageZone;
import com.example.lightmanager.entity.InspectionBatch;
import com.example.lightmanager.entity.InspectionException;
import com.example.lightmanager.entity.InspectionExceptionRecord;
import com.example.lightmanager.entity.InspectionItem;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.mapper.GarageZoneMapper;
import com.example.lightmanager.mapper.InspectionBatchMapper;
import com.example.lightmanager.mapper.InspectionExceptionMapper;
import com.example.lightmanager.mapper.InspectionExceptionRecordMapper;
import com.example.lightmanager.mapper.InspectionItemMapper;
import com.example.lightmanager.mapper.LightGroupMapper;
import com.example.lightmanager.service.InspectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InspectionServiceImpl implements InspectionService {

    private final InspectionBatchMapper batchMapper;
    private final InspectionItemMapper itemMapper;
    private final InspectionExceptionMapper exceptionMapper;
    private final InspectionExceptionRecordMapper recordMapper;
    private final LightGroupMapper lightGroupMapper;
    private final GarageZoneMapper garageZoneMapper;

    public InspectionServiceImpl(InspectionBatchMapper batchMapper,
                                 InspectionItemMapper itemMapper,
                                 InspectionExceptionMapper exceptionMapper,
                                 InspectionExceptionRecordMapper recordMapper,
                                 LightGroupMapper lightGroupMapper,
                                 GarageZoneMapper garageZoneMapper) {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.exceptionMapper = exceptionMapper;
        this.recordMapper = recordMapper;
        this.lightGroupMapper = lightGroupMapper;
        this.garageZoneMapper = garageZoneMapper;
    }

    private static final Set<String> VALID_RESULTS = Set.of(
            InspectionConstants.RESULT_NORMAL,
            InspectionConstants.RESULT_OFF,
            InspectionConstants.RESULT_FLICKER,
            InspectionConstants.RESULT_DIM);

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ==================== 批次 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionBatch createBatch(BatchCreateRequestDTO request) {
        // 校验分区
        List<Long> zoneIds = request.getZoneIds().stream().distinct().collect(Collectors.toList());
        List<GarageZone> zones = garageZoneMapper.selectAllZones().stream()
                .filter(z -> zoneIds.contains(z.getId()))
                .collect(Collectors.toList());
        if (zones.size() != zoneIds.size()) {
            throw new BusinessException(400, "部分巡检分区不存在或已停用");
        }
        Map<Long, String> zoneNameMap = zones.stream()
                .collect(Collectors.toMap(GarageZone::getId, GarageZone::getZoneName));

        // 快照分区内灯组(巡检开始时点)
        List<LightGroup> lights = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long zoneId : zoneIds) {
            for (LightGroup lg : lightGroupMapper.selectByZoneId(zoneId)) {
                if (seen.add(lg.getId())) {
                    lights.add(lg);
                }
            }
        }
        if (lights.isEmpty()) {
            throw new BusinessException(400, "所选分区下没有可巡检的灯组");
        }

        LocalDateTime now = LocalDateTime.now();
        InspectionBatch batch = new InspectionBatch();
        batch.setBatchNo(generateBatchNo());
        batch.setInspector(request.getInspector().trim());
        batch.setZoneIds(zoneIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        batch.setZoneNames(zoneIds.stream()
                .map(id -> zoneNameMap.getOrDefault(id, String.valueOf(id)))
                .collect(Collectors.joining(",")));
        batch.setTotalCount(lights.size());
        batch.setRecordedCount(0);
        batch.setAbnormalCount(0);
        batch.setStatus(InspectionConstants.BATCH_DRAFT);
        batch.setRemark(request.getRemark());
        batch.setCreatedAt(now);
        batchMapper.insert(batch);

        // 预生成全部灯组的巡检结果行(结果为空), 固化巡检开始时的分区/灯组快照
        for (LightGroup lg : lights) {
            InspectionItem item = new InspectionItem();
            item.setBatchId(batch.getId());
            item.setLightGroupId(lg.getId());
            item.setGroupCode(lg.getGroupCode());
            item.setInstallationLocation(lg.getInstallationLocation());
            item.setPower(lg.getPower());
            item.setZoneId(lg.getZoneId());
            item.setZoneName(zoneNameMap.getOrDefault(lg.getZoneId(),
                    lg.getZoneName() != null ? lg.getZoneName() : String.valueOf(lg.getZoneId())));
            item.setInspectResult(null);
            item.setSiteDescription(null);
            item.setRecordedAt(null);
            item.setCreatedAt(now);
            itemMapper.insert(item);
        }

        return batch;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchDetailDTO saveItems(SaveItemsRequestDTO request) {
        InspectionBatch batch = requireBatch(request.getBatchId());
        if (batch.getStatus() != InspectionConstants.BATCH_DRAFT) {
            throw new BusinessException(400, "批次已提交，原始巡检结果不能再修改或补录");
        }

        List<InspectionItem> existing = itemMapper.selectByBatchId(batch.getId());
        Map<Long, InspectionItem> itemMap = existing.stream()
                .collect(Collectors.toMap(InspectionItem::getLightGroupId, it -> it, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();
        for (ItemResultDTO dto : request.getItems()) {
            if (!VALID_RESULTS.contains(dto.getInspectResult())) {
                throw new BusinessException(400, "巡检结果非法，只能为 NORMAL/OFF/FLICKER/DIM");
            }
            if (!InspectionConstants.RESULT_NORMAL.equals(dto.getInspectResult())
                    && (dto.getSiteDescription() == null || dto.getSiteDescription().trim().isEmpty())) {
                throw new BusinessException(400, "灯组异常时必须填写现场说明");
            }
            InspectionItem item = itemMap.get(dto.getLightGroupId());
            if (item == null) {
                throw new BusinessException(400, "该灯组不属于本巡检批次，无法登记");
            }
            item.setInspectResult(dto.getInspectResult());
            item.setSiteDescription(InspectionConstants.RESULT_NORMAL.equals(dto.getInspectResult())
                    ? null : dto.getSiteDescription().trim());
            item.setRecordedAt(now);
            int rows = itemMapper.upsertDraftItem(item);
            if (rows == 0) {
                // 理论不会发生(批次已校验为暂存), 兜底防止原始结果被覆盖
                throw new BusinessException(409, "批次状态已变化，登记结果未保存，请刷新后重试");
            }
        }

        batch.setRecordedCount(itemMapper.countRecordedByBatchId(batch.getId()));
        batchMapper.updateById(batch);

        return getBatchDetail(batch.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionBatch submitBatch(Long batchId) {
        InspectionBatch batch = requireBatch(batchId);
        if (batch.getStatus() != InspectionConstants.BATCH_DRAFT) {
            throw new BusinessException(400, "批次已提交，不能重复提交");
        }

        List<InspectionItem> items = itemMapper.selectByBatchId(batchId);
        List<InspectionItem> unrecorded = items.stream()
                .filter(it -> it.getInspectResult() == null)
                .collect(Collectors.toList());
        if (!unrecorded.isEmpty()) {
            String codes = unrecorded.stream()
                    .limit(10)
                    .map(InspectionItem::getGroupCode)
                    .collect(Collectors.joining("、"));
            throw new BusinessException(400,
                    "还有 " + unrecorded.size() + " 个灯组未登记巡检结果（如 " + codes + "），请补录后再提交");
        }

        LocalDateTime now = LocalDateTime.now();

        // 生成异常清单: 快照分区以巡检结果表为准, 不受后续划转影响
        List<InspectionItem> abnormalItems = items.stream()
                .filter(it -> !InspectionConstants.RESULT_NORMAL.equals(it.getInspectResult()))
                .collect(Collectors.toList());

        for (InspectionItem it : abnormalItems) {
            InspectionException ex = new InspectionException();
            ex.setBatchId(batchId);
            ex.setItemId(it.getId());
            ex.setLightGroupId(it.getLightGroupId());
            ex.setGroupCode(it.getGroupCode());
            ex.setInstallationLocation(it.getInstallationLocation());
            ex.setSnapshotZoneId(it.getZoneId());
            ex.setSnapshotZoneName(it.getZoneName());
            ex.setExceptionType(it.getInspectResult());
            ex.setSiteDescription(it.getSiteDescription());
            ex.setOriginalDescription(it.getSiteDescription());
            ex.setReporter(batch.getInspector());
            ex.setReportedAt(now);
            ex.setStatus(InspectionConstants.STATUS_PENDING);
            ex.setVersion(0);
            ex.setCreatedAt(now);
            exceptionMapper.insert(ex);

            // 上报即形成第一条处置记录
            insertRecord(ex.getId(), InspectionConstants.ACTION_SUBMIT,
                    "异常上报：[" + typeLabel(it.getInspectResult()) + "] " + it.getSiteDescription(),
                    batch.getInspector(), now);
        }

        batch.setStatus(InspectionConstants.BATCH_SUBMITTED);
        batch.setSubmittedAt(now);
        batch.setRecordedCount(items.size());
        batch.setAbnormalCount(abnormalItems.size());
        batchMapper.updateById(batch);

        return batch;
    }

    @Override
    public IPage<InspectionBatch> getBatchPage(String batchNo, String inspector, Integer status,
                                               Integer pageNum, Integer pageSize) {
        Page<InspectionBatch> page = new Page<>(pageNum, pageSize);
        return batchMapper.selectBatchPage(page, batchNo, inspector, status);
    }

    @Override
    public BatchDetailDTO getBatchDetail(Long batchId) {
        InspectionBatch batch = requireBatch(batchId);
        List<InspectionItem> items = itemMapper.selectByBatchId(batchId);
        return new BatchDetailDTO(batch, items);
    }

    // ==================== 异常清单 / 详情 ====================

    @Override
    public IPage<InspectionException> getExceptionPage(ExceptionPageRequestDTO request) {
        Page<InspectionException> page = new Page<>(request.getPageNum(), request.getPageSize());
        return exceptionMapper.selectExceptionPage(page, request.getBatchNo(), request.getGroupCode(),
                request.getExceptionType(), request.getStatus(), request.getReporter(),
                request.getZoneChanged());
    }

    @Override
    public ExceptionDetailDTO getExceptionDetail(Long exceptionId) {
        InspectionException exception = exceptionMapper.selectDetailById(exceptionId);
        if (exception == null) {
            throw new BusinessException(404, "异常记录不存在");
        }
        List<InspectionExceptionRecord> records = recordMapper.selectByExceptionId(exceptionId);
        return new ExceptionDetailDTO(exception, records);
    }

    // ==================== 受控状态流转 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExceptionDetailDTO returnException(ExceptionActionRequestDTO request) {
        InspectionException ex = lockException(request.getExceptionId());
        if (InspectionConstants.STATUS_CLOSED.equals(ex.getStatus())) {
            throw closedConflict(ex);
        }
        if (!InspectionConstants.STATUS_PENDING.equals(ex.getStatus())) {
            throw stateConflict(ex, "当前不是待复核状态，不能退回");
        }
        if (request.getReturnReason() == null || request.getReturnReason().trim().isEmpty()) {
            throw new BusinessException(400, "退回时必须填写退回原因");
        }
        checkVersion(ex, request.getVersion());

        LocalDateTime now = LocalDateTime.now();
        int rows = exceptionMapper.returnException(ex.getId(), ex.getVersion(), now);
        ensureChanged(rows, ex);
        insertRecord(ex.getId(), InspectionConstants.ACTION_RETURN,
                "退回补充：" + request.getReturnReason().trim(), request.getOperator().trim(), now);

        return getExceptionDetail(ex.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExceptionDetailDTO supplement(ExceptionActionRequestDTO request) {
        InspectionException ex = lockException(request.getExceptionId());
        if (InspectionConstants.STATUS_CLOSED.equals(ex.getStatus())) {
            throw closedConflict(ex);
        }
        if (!InspectionConstants.STATUS_RETURNED.equals(ex.getStatus())) {
            throw stateConflict(ex, "当前不是待补充状态，不能提交补充说明");
        }
        if (request.getSupplementDescription() == null || request.getSupplementDescription().trim().isEmpty()) {
            throw new BusinessException(400, "补充说明不能为空");
        }
        checkVersion(ex, request.getVersion());

        LocalDateTime now = LocalDateTime.now();
        String newDescription = request.getSupplementDescription().trim();
        int rows = exceptionMapper.supplementException(ex.getId(), ex.getVersion(), newDescription, now);
        ensureChanged(rows, ex);
        insertRecord(ex.getId(), InspectionConstants.ACTION_SUPPLEMENT,
                "补充现场说明：" + newDescription, request.getOperator().trim(), now);

        return getExceptionDetail(ex.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExceptionDetailDTO confirm(ExceptionActionRequestDTO request) {
        InspectionException ex = lockException(request.getExceptionId());
        if (InspectionConstants.STATUS_CLOSED.equals(ex.getStatus())) {
            throw closedConflict(ex);
        }
        if (!InspectionConstants.STATUS_PENDING.equals(ex.getStatus())) {
            throw stateConflict(ex, "当前不是待复核状态，不能确认异常");
        }
        if (request.getReviewOpinion() == null || request.getReviewOpinion().trim().isEmpty()) {
            throw new BusinessException(400, "确认异常时必须填写处理意见");
        }
        if (request.getHandleDeadline() == null) {
            throw new BusinessException(400, "确认异常时必须填写处理时限");
        }
        if (request.getHandleDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "处理时限不能早于当前时间");
        }
        checkVersion(ex, request.getVersion());

        LocalDateTime now = LocalDateTime.now();
        int rows = exceptionMapper.confirmException(ex.getId(), ex.getVersion(),
                request.getReviewOpinion().trim(), request.getHandleDeadline(),
                request.getOperator().trim(), now);
        ensureChanged(rows, ex);
        insertRecord(ex.getId(), InspectionConstants.ACTION_CONFIRM,
                "确认异常，处理意见：" + request.getReviewOpinion().trim()
                        + "；处理时限：" + request.getHandleDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                request.getOperator().trim(), now);

        return getExceptionDetail(ex.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExceptionDetailDTO registerHandleResult(ExceptionActionRequestDTO request) {
        InspectionException ex = lockException(request.getExceptionId());
        if (InspectionConstants.STATUS_CLOSED.equals(ex.getStatus())) {
            throw closedConflict(ex);
        }
        if (!InspectionConstants.STATUS_CONFIRMED.equals(ex.getStatus())) {
            throw stateConflict(ex, "异常尚未确认，不能登记处理结果");
        }
        if (request.getHandleResult() == null || request.getHandleResult().trim().isEmpty()) {
            throw new BusinessException(400, "处理结果不能为空");
        }
        checkVersion(ex, request.getVersion());

        LocalDateTime now = LocalDateTime.now();
        int rows = exceptionMapper.registerHandleResult(ex.getId(), ex.getVersion(),
                request.getOperator().trim(), request.getHandleResult().trim(), now);
        ensureChanged(rows, ex);
        insertRecord(ex.getId(), InspectionConstants.ACTION_HANDLE,
                "登记处理结果：" + request.getHandleResult().trim(),
                request.getOperator().trim(), now);

        return getExceptionDetail(ex.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExceptionDetailDTO close(ExceptionActionRequestDTO request) {
        InspectionException ex = lockException(request.getExceptionId());
        if (InspectionConstants.STATUS_CLOSED.equals(ex.getStatus())) {
            throw new BusinessException(409, "异常已关闭，关闭后的记录只能查看，不能再改回处理中");
        }
        if (!InspectionConstants.STATUS_CONFIRMED.equals(ex.getStatus())) {
            throw stateConflict(ex, "仅已确认的异常可以关闭");
        }
        checkVersion(ex, request.getVersion());

        LocalDateTime now = LocalDateTime.now();
        int rows = exceptionMapper.closeException(ex.getId(), ex.getVersion(),
                request.getOperator().trim(), now);
        ensureChanged(rows, ex);
        String resultNote = ex.getHandleResult() != null ? "（处理结果：" + ex.getHandleResult() + "）" : "（未单独登记处理结果）";
        insertRecord(ex.getId(), InspectionConstants.ACTION_CLOSE,
                "处置关闭" + resultNote, request.getOperator().trim(), now);

        return getExceptionDetail(ex.getId());
    }

    // ==================== 辅助方法 ====================

    private InspectionBatch requireBatch(Long batchId) {
        if (batchId == null) {
            throw new BusinessException(400, "批次ID不能为空");
        }
        InspectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(404, "巡检批次不存在");
        }
        return batch;
    }

    /** 读取异常当前状态; 并发安全由各流转方法内的 状态+版本 条件更新(行级排他锁)保证 */
    private InspectionException lockException(Long id) {
        if (id == null) {
            throw new BusinessException(400, "异常ID不能为空");
        }
        InspectionException ex = exceptionMapper.selectById(id);
        if (ex == null) {
            throw new BusinessException(404, "异常记录不存在");
        }
        return ex;
    }

    /** 乐观锁版本校验: 版本落后说明页面内容已被他人改动 */
    private void checkVersion(InspectionException ex, Integer clientVersion) {
        if (clientVersion != null && !clientVersion.equals(ex.getVersion())) {
            throw new BusinessException(409,
                    "该异常已被其他人更新（当前状态：" + statusLabel(ex.getStatus())
                            + "，最新版本号：" + ex.getVersion() + "），您的提交未生效，请刷新查看最新内容后再操作");
        }
    }

    /** 条件更新影响行数为 0: 并发下状态/版本已被先到的提交改变 */
    private void ensureChanged(int rows, InspectionException ex) {
        if (rows == 0) {
            InspectionException latest = exceptionMapper.selectDetailById(ex.getId());
            String latestStatus = latest != null ? statusLabel(latest.getStatus()) : "未知";
            throw new BusinessException(409,
                    "记录已被其他人先提交（当前状态：" + latestStatus + "），本次操作未生效、未重复生成处理记录，请刷新查看最新内容");
        }
    }

    private BusinessException stateConflict(InspectionException ex, String message) {
        return new BusinessException(409, message + "（当前状态：" + statusLabel(ex.getStatus()) + "），请刷新后重试");
    }

    private BusinessException closedConflict(InspectionException ex) {
        return new BusinessException(409, "异常已关闭，关闭后的记录只能查看，不能再改回处理中");
    }

    private void insertRecord(Long exceptionId, String action, String content,
                              String operator, LocalDateTime operatedAt) {
        InspectionExceptionRecord record = new InspectionExceptionRecord();
        record.setExceptionId(exceptionId);
        record.setAction(action);
        record.setContent(content);
        record.setOperator(operator);
        record.setOperatedAt(operatedAt);
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
    }

    private String generateBatchNo() {
        return "IN" + LocalDateTime.now().format(NO_FMT)
                + String.format("%04d", (int) (Math.random() * 10000));
    }

    private String typeLabel(String type) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(InspectionConstants.RESULT_OFF, "熄灭");
        labels.put(InspectionConstants.RESULT_FLICKER, "频闪");
        labels.put(InspectionConstants.RESULT_DIM, "亮度不足");
        return labels.getOrDefault(type, type);
    }

    private String statusLabel(String status) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(InspectionConstants.STATUS_PENDING, "待复核");
        labels.put(InspectionConstants.STATUS_RETURNED, "已退回待补充");
        labels.put(InspectionConstants.STATUS_CONFIRMED, "已确认待处理");
        labels.put(InspectionConstants.STATUS_CLOSED, "已关闭");
        return labels.getOrDefault(status, status);
    }
}
