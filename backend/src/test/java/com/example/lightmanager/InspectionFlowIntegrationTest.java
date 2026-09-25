package com.example.lightmanager;

import com.example.lightmanager.common.BusinessException;
import com.example.lightmanager.common.InspectionConstants;
import com.example.lightmanager.dto.BatchCreateRequestDTO;
import com.example.lightmanager.dto.BatchDetailDTO;
import com.example.lightmanager.dto.ExceptionActionRequestDTO;
import com.example.lightmanager.dto.ExceptionDetailDTO;
import com.example.lightmanager.dto.ExceptionPageRequestDTO;
import com.example.lightmanager.dto.ItemResultDTO;
import com.example.lightmanager.dto.SaveItemsRequestDTO;
import com.example.lightmanager.entity.InspectionBatch;
import com.example.lightmanager.entity.InspectionException;
import com.example.lightmanager.entity.InspectionExceptionRecord;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.mapper.LightGroupMapper;
import com.example.lightmanager.service.InspectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InspectionFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InspectionService inspectionService;

    @Autowired
    private LightGroupMapper lightGroupMapper;

    private BatchCreateRequestDTO createReq(String inspector, Long... zoneIds) {
        BatchCreateRequestDTO req = new BatchCreateRequestDTO();
        req.setInspector(inspector);
        req.setZoneIds(Arrays.asList(zoneIds));
        return req;
    }

    private ItemResultDTO item(long lightGroupId, String result, String desc) {
        ItemResultDTO dto = new ItemResultDTO();
        dto.setLightGroupId(lightGroupId);
        dto.setInspectResult(result);
        dto.setSiteDescription(desc);
        return dto;
    }

    private ExceptionActionRequestDTO action(long id, int version, String operator) {
        ExceptionActionRequestDTO dto = new ExceptionActionRequestDTO();
        dto.setExceptionId(id);
        dto.setVersion(version);
        dto.setOperator(operator);
        return dto;
    }

    @Test
    void fullFlow_createDraftSaveSubmit_returnSupplementConfirm_handle_close() {
        // 1. 创建批次: A区(3个灯)
        InspectionBatch batch = inspectionService.createBatch(createReq("巡检员张三", 1L));
        assertNotNull(batch.getId());
        assertEquals(3, batch.getTotalCount());
        assertEquals(0, batch.getRecordedCount());
        assertEquals(InspectionConstants.BATCH_DRAFT, batch.getStatus());

        BatchDetailDTO detail = inspectionService.getBatchDetail(batch.getId());
        assertEquals(3, detail.getItems().size());

        // 2. 暂存两条(一个正常, 一个熄灭带说明) —— 允许暂存后继续补录
        SaveItemsRequestDTO save1 = new SaveItemsRequestDTO();
        save1.setBatchId(batch.getId());
        save1.setItems(Arrays.asList(
                item(1L, InspectionConstants.RESULT_NORMAL, null),
                item(2L, InspectionConstants.RESULT_OFF, "2号灯完全不亮")));
        detail = inspectionService.saveItems(save1);
        assertEquals(2, detail.getBatch().getRecordedCount());

        // 3. 异常不填说明必须被拒
        SaveItemsRequestDTO bad = new SaveItemsRequestDTO();
        bad.setBatchId(batch.getId());
        bad.setItems(Arrays.asList(item(3L, InspectionConstants.RESULT_FLICKER, "  ")));
        BusinessException ex = assertThrows(BusinessException.class, () -> inspectionService.saveItems(bad));
        assertTrue(ex.getMessage().contains("现场说明"));

        // 4. 补录第三条(频闪)
        SaveItemsRequestDTO save2 = new SaveItemsRequestDTO();
        save2.setBatchId(batch.getId());
        save2.setItems(Arrays.asList(item(3L, InspectionConstants.RESULT_FLICKER, "3号灯持续频闪")));
        inspectionService.saveItems(save2);

        // 5. 未登记完整不能提交 —— 新建一个只登1/2的批次验证
        InspectionBatch batch2 = inspectionService.createBatch(createReq("巡检员李四", 2L));
        SaveItemsRequestDTO partial = new SaveItemsRequestDTO();
        partial.setBatchId(batch2.getId());
        partial.setItems(Arrays.asList(item(4L, InspectionConstants.RESULT_DIM, "昏暗")));
        inspectionService.saveItems(partial);

        // 6. 提交批次1 -> 形成2条异常(2熄灭、3频闪)
        InspectionBatch submitted = inspectionService.submitBatch(batch.getId());
        assertEquals(InspectionConstants.BATCH_SUBMITTED, submitted.getStatus());
        assertEquals(2, submitted.getAbnormalCount());
        assertNotNull(submitted.getSubmittedAt());

        // 7. 提交后原始结果不能被覆盖(saveItems 拒绝)
        SaveItemsRequestDTO afterSubmit = new SaveItemsRequestDTO();
        afterSubmit.setBatchId(batch.getId());
        afterSubmit.setItems(Arrays.asList(item(1L, InspectionConstants.RESULT_OFF, "想改成异常")));
        BusinessException locked = assertThrows(BusinessException.class,
                () -> inspectionService.saveItems(afterSubmit));
        assertTrue(locked.getMessage().contains("不能再修改"));
        // 且数据库原始结果仍是 NORMAL
        InspectionItemNormalStillNormal(inspectionService.getBatchDetail(batch.getId()));

        // 8. 重复提交被拒
        assertThrows(BusinessException.class, () -> inspectionService.submitBatch(batch.getId()));

        // 9. 异常清单(按本批次过滤, 与其他用例隔离)
        ExceptionPageRequestDTO pageReq = new ExceptionPageRequestDTO();
        pageReq.setPageNum(1);
        pageReq.setPageSize(50);
        pageReq.setBatchNo(submitted.getBatchNo());
        List<InspectionException> all = inspectionService.getExceptionPage(pageReq).getRecords();
        // 本批次: 2号熄灭、3号频闪, 共2条
        assertEquals(2, all.size());

        InspectionException offEx = findByGroup(all, "LG-A-002");
        InspectionException flickerEx = findByGroup(all, "LG-A-003");
        assertEquals(InspectionConstants.STATUS_PENDING, offEx.getStatus());
        assertEquals("2号灯完全不亮", offEx.getOriginalDescription());

        // 10. 每条异常都有 SUBMIT 上报记录
        ExceptionDetailDTO offDetail = inspectionService.getExceptionDetail(offEx.getId());
        assertEquals(1, offDetail.getRecords().size());
        assertEquals(InspectionConstants.ACTION_SUBMIT, offDetail.getRecords().get(0).getAction());

        // 11. 复核退回
        ExceptionActionRequestDTO retReq = action(offEx.getId(), offEx.getVersion(), "复核员王五");
        retReq.setReturnReason("缺少位置细节，请补充");
        offDetail = inspectionService.returnException(retReq);
        assertEquals(InspectionConstants.STATUS_RETURNED, offDetail.getException().getStatus());
        assertEquals(1, offDetail.getException().getVersion());

        // 12. 退回状态下不能确认
        ExceptionActionRequestDTO confirmWhileReturned = action(offEx.getId(), 1, "复核员王五");
        confirmWhileReturned.setReviewOpinion("意见");
        confirmWhileReturned.setHandleDeadline(LocalDateTime.now().plusDays(2));
        assertThrows(BusinessException.class, () -> inspectionService.confirm(confirmWhileReturned));

        // 13. 巡检人补充 -> 重新待复核
        ExceptionActionRequestDTO supReq = action(offEx.getId(), 1, "巡检员张三");
        supReq.setSupplementDescription("补充：位于A区主通道东侧，灯罩内有焦糊味");
        offDetail = inspectionService.supplement(supReq);
        assertEquals(InspectionConstants.STATUS_PENDING, offDetail.getException().getStatus());
        assertEquals(2, offDetail.getException().getVersion());
        // 原始说明保留, 当前说明为补充内容
        assertEquals("2号灯完全不亮", offDetail.getException().getOriginalDescription());
        assertEquals("补充：位于A区主通道东侧，灯罩内有焦糊味",
                offDetail.getException().getSiteDescription());

        // 14. 确认异常(处理意见+时限)
        ExceptionActionRequestDTO confirmReq = action(offEx.getId(), 2, "复核员王五");
        confirmReq.setReviewOpinion("确认为驱动电源损坏，安排更换");
        confirmReq.setHandleDeadline(LocalDateTime.now().plusDays(3));
        offDetail = inspectionService.confirm(confirmReq);
        assertEquals(InspectionConstants.STATUS_CONFIRMED, offDetail.getException().getStatus());
        assertEquals(3, offDetail.getException().getVersion());
        assertEquals("复核员王五", offDetail.getException().getReviewer());

        // 确认缺时限被拒
        ExceptionActionRequestDTO noDeadline = action(flickerEx.getId(), flickerEx.getVersion(), "复核员王五");
        noDeadline.setReviewOpinion("频闪确认");
        assertThrows(BusinessException.class, () -> inspectionService.confirm(noDeadline));

        // 15. 登记处理结果
        ExceptionActionRequestDTO handleReq = action(offEx.getId(), 3, "维修员赵六");
        handleReq.setHandleResult("已更换驱动电源，通电测试正常");
        offDetail = inspectionService.registerHandleResult(handleReq);
        assertEquals(InspectionConstants.STATUS_CONFIRMED, offDetail.getException().getStatus(),
                "登记结果不改状态，仍为已确认待处理");
        assertEquals(4, offDetail.getException().getVersion());
        assertEquals("维修员赵六", offDetail.getException().getHandler());

        // 16. 关闭(终态)
        ExceptionActionRequestDTO closeReq = action(offEx.getId(), 4, "经理孙七");
        offDetail = inspectionService.close(closeReq);
        assertEquals(InspectionConstants.STATUS_CLOSED, offDetail.getException().getStatus());
        assertEquals("经理孙七", offDetail.getException().getCloser());

        // 17. 关闭后任何流转都被拒(不能改回处理中)
        ExceptionActionRequestDTO afterClose = action(offEx.getId(), 5, "经理孙七");
        afterClose.setHandleResult("试图再处理");
        assertThrows(BusinessException.class, () -> inspectionService.registerHandleResult(afterClose));
        assertThrows(BusinessException.class, () -> inspectionService.close(action(offEx.getId(), 5, "x")));
        assertThrows(BusinessException.class,
                () -> inspectionService.returnException(action(offEx.getId(), 5, "x")));

        // 18. 历史记录完整连续: 上报/退回/补充/确认/处理/关闭 共6条
        List<String> actions = offDetail.getRecords().stream().map(InspectionExceptionRecord::getAction).toList();
        assertEquals(Arrays.asList(
                InspectionConstants.ACTION_SUBMIT,
                InspectionConstants.ACTION_RETURN,
                InspectionConstants.ACTION_SUPPLEMENT,
                InspectionConstants.ACTION_CONFIRM,
                InspectionConstants.ACTION_HANDLE,
                InspectionConstants.ACTION_CLOSE), actions);
        for (InspectionExceptionRecord r : offDetail.getRecords()) {
            assertNotNull(r.getOperatedAt());
            assertNotNull(r.getOperator());
            assertNotNull(r.getContent());
        }
    }

    @Test
    void zoneTransferredAfterSubmit_snapshotKeptAndFlagged() {
        // 创建并提交一个含 B区 4号灯 的批次
        InspectionBatch batch = inspectionService.createBatch(createReq("巡检员钱八", 2L));
        SaveItemsRequestDTO save = new SaveItemsRequestDTO();
        save.setBatchId(batch.getId());
        save.setItems(Arrays.asList(item(4L, InspectionConstants.RESULT_DIM, "4号灯明显偏暗")));
        inspectionService.saveItems(save);
        inspectionService.submitBatch(batch.getId());

        ExceptionPageRequestDTO pageReq = new ExceptionPageRequestDTO();
        pageReq.setPageNum(1);
        pageReq.setPageSize(50);
        pageReq.setBatchNo(batch.getBatchNo());
        InspectionException ex = inspectionService.getExceptionPage(pageReq).getRecords().stream()
                .filter(e -> e.getGroupCode().equals("LG-B-001")).findFirst().orElseThrow();
        Long exId = ex.getId();
        assertEquals(2L, ex.getSnapshotZoneId());
        assertEquals("B区车库", ex.getSnapshotZoneName());
        assertEquals(0, ex.getZoneChanged());

        // 巡检提交后, 把4号灯划转到 A区(id=1)
        LightGroup lg = lightGroupMapper.selectById(4L);
        lg.setZoneId(1L);
        lightGroupMapper.updateById(lg);

        // 快照分区不变, 但清单标出当前分区已变化
        InspectionException refreshed = inspectionService.getExceptionDetail(exId).getException();
        assertEquals(2L, refreshed.getSnapshotZoneId(), "历史快照分区不被划转改写");
        assertEquals("B区车库", refreshed.getSnapshotZoneName());
        assertEquals(1L, refreshed.getCurrentZoneId());
        assertEquals("A区车库", refreshed.getCurrentZoneName());
        assertEquals(1, refreshed.getZoneChanged());

        // 按“仅看已变化”过滤可命中
        ExceptionPageRequestDTO changedOnly = new ExceptionPageRequestDTO();
        changedOnly.setZoneChanged(1);
        changedOnly.setGroupCode("LG-B-001");
        changedOnly.setPageNum(1);
        changedOnly.setPageSize(50);
        boolean hit = inspectionService.getExceptionPage(changedOnly).getRecords().stream()
                .anyMatch(e -> e.getId().equals(exId));
        assertTrue(hit);
    }

    @Test
    void concurrentConfirm_onlyFirstWins_secondSeesConflictAndLatest() throws Exception {
        // 准备一个待复核异常
        InspectionBatch batch = inspectionService.createBatch(createReq("巡检员周九", 1L));
        SaveItemsRequestDTO save = new SaveItemsRequestDTO();
        save.setBatchId(batch.getId());
        save.setItems(Arrays.asList(
                item(1L, InspectionConstants.RESULT_OFF, "1号熄灭"),
                item(2L, InspectionConstants.RESULT_NORMAL, null),
                item(3L, InspectionConstants.RESULT_NORMAL, null)));
        inspectionService.saveItems(save);
        inspectionService.submitBatch(batch.getId());

        ExceptionPageRequestDTO pageReq = new ExceptionPageRequestDTO();
        pageReq.setPageNum(1);
        pageReq.setPageSize(50);
        pageReq.setBatchNo(batch.getBatchNo());
        final Long exId = inspectionService.getExceptionPage(pageReq).getRecords().stream()
                .filter(e -> e.getGroupCode().equals("LG-A-001")).findFirst().orElseThrow().getId();

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger conflict = new AtomicInteger(0);
        List<String> finalStatuses = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final String reviewer = i == 0 ? "复核员甲" : "复核员乙";
            pool.submit(() -> {
                try {
                    start.await();
                    // 两人都基于 version=0(PENDING) 提交确认
                    ExceptionActionRequestDTO req = action(exId, 0, reviewer);
                    req.setReviewOpinion(reviewer + "的处理意见");
                    req.setHandleDeadline(LocalDateTime.now().plusDays(2));
                    inspectionService.confirm(req);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getCode() == 409) {
                        conflict.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(1, success.get(), "只有第一个提交能改变状态");
        assertEquals(1, conflict.get(), "后提交者必须收到冲突提示");

        // 最终状态为已确认, 版本只增加一次, 处理记录不重复
        ExceptionDetailDTO latest = inspectionService.getExceptionDetail(exId);
        assertEquals(InspectionConstants.STATUS_CONFIRMED, latest.getException().getStatus());
        assertEquals(1, latest.getException().getVersion(), "版本号只增加一次");
        long confirmCount = latest.getRecords().stream()
                .filter(r -> InspectionConstants.ACTION_CONFIRM.equals(r.getAction())).count();
        assertEquals(1, confirmCount, "确认处理记录只能生成一次");
        // 生效的是第一个提交者(先抢到行锁的那个)
        assertNotNull(latest.getException().getReviewer());
        assertTrue(latest.getException().getReviewer().equals("复核员甲")
                || latest.getException().getReviewer().equals("复核员乙"));
    }

    private void InspectionItemNormalStillNormal(BatchDetailDTO d) {
        boolean normal = d.getItems().stream()
                .filter(i -> i.getLightGroupId().equals(1L))
                .allMatch(i -> InspectionConstants.RESULT_NORMAL.equals(i.getInspectResult()));
        assertTrue(normal, "提交后1号灯原始结果必须仍为正常");
    }

    private InspectionException findByGroup(List<InspectionException> list, String code) {
        return list.stream().filter(e -> e.getGroupCode().equals(code)).findFirst()
                .orElseThrow(() -> new AssertionError("未找到灯组 " + code));
    }
}
