package com.example.lightmanager.service.impl;

import com.example.lightmanager.dto.LedgerPageRequestDTO;
import com.example.lightmanager.dto.TransferRequestDTO;
import com.example.lightmanager.entity.BatchTransferLedger;
import com.example.lightmanager.entity.LedgerDetail;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.mapper.BatchTransferLedgerMapper;
import com.example.lightmanager.mapper.LedgerDetailMapper;
import com.example.lightmanager.mapper.LightGroupMapper;
import com.example.lightmanager.service.BatchTransferService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BatchTransferServiceImpl implements BatchTransferService {
    
    private final LightGroupMapper lightGroupMapper;
    private final BatchTransferLedgerMapper batchTransferLedgerMapper;
    private final LedgerDetailMapper ledgerDetailMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public BatchTransferServiceImpl(LightGroupMapper lightGroupMapper, 
                                    BatchTransferLedgerMapper batchTransferLedgerMapper,
                                    LedgerDetailMapper ledgerDetailMapper,
                                    RedisTemplate<String, Object> redisTemplate) {
        this.lightGroupMapper = lightGroupMapper;
        this.batchTransferLedgerMapper = batchTransferLedgerMapper;
        this.ledgerDetailMapper = ledgerDetailMapper;
        this.redisTemplate = redisTemplate;
    }
    
    private static final String LIGHT_GROUP_POWER_KEY = "light_group:power:";
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchTransferLedger batchTransfer(TransferRequestDTO request) {
        List<Long> lightGroupIds = request.getLightGroupIds();
        Long targetZoneId = request.getTargetZoneId();
        
        List<LightGroup> lightGroups = lightGroupMapper.selectByIds(lightGroupIds);
        if (lightGroups.isEmpty()) {
            throw new RuntimeException("未找到指定的灯组");
        }
        
        List<Long> sourceZoneIds = lightGroups.stream()
                .map(LightGroup::getZoneId)
                .distinct()
                .collect(Collectors.toList());
        
        String sourceZoneIdsStr = sourceZoneIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        
        String batchNo = generateBatchNo();
        
        BatchTransferLedger ledger = new BatchTransferLedger();
        ledger.setBatchNo(batchNo);
        ledger.setOperator(request.getOperator() != null ? request.getOperator() : "system");
        ledger.setTransferTime(LocalDateTime.now());
        ledger.setSourceZoneIds(sourceZoneIdsStr);
        ledger.setTargetZoneId(targetZoneId);
        ledger.setLightCount(lightGroups.size());
        ledger.setDescription(request.getDescription());
        batchTransferLedgerMapper.insert(ledger);
        
        List<LedgerDetail> details = new ArrayList<>();
        for (LightGroup lg : lightGroups) {
            LedgerDetail detail = new LedgerDetail();
            detail.setLedgerId(ledger.getId());
            detail.setLightGroupId(lg.getId());
            detail.setSourceZoneId(lg.getZoneId());
            detail.setTargetZoneId(targetZoneId);
            details.add(detail);
            
            String oldKey = LIGHT_GROUP_POWER_KEY + lg.getZoneId();
            redisTemplate.opsForZSet().remove(oldKey, lg.getGroupCode());
            
            String newKey = LIGHT_GROUP_POWER_KEY + targetZoneId;
            redisTemplate.opsForZSet().add(newKey, lg.getGroupCode(), lg.getPower());
        }
        
        if (!details.isEmpty()) {
            ledgerDetailMapper.batchInsert(details);
        }
        
        lightGroupMapper.batchUpdateZoneId(lightGroupIds, targetZoneId);
        
        return ledger;
    }
    
    @Override
    public IPage<BatchTransferLedger> getLedgerPage(LedgerPageRequestDTO request) {
        Page<BatchTransferLedger> page = new Page<>(request.getPageNum(), request.getPageSize());
        return batchTransferLedgerMapper.selectByConditionsPage(page,
                request.getOperator(), request.getStartTime(), 
                request.getEndTime(), request.getBatchNo());
    }
    
    @Override
    public BatchTransferLedger getLedgerById(Long id) {
        return batchTransferLedgerMapper.selectById(id);
    }
    
    @Override
    public List<LedgerDetail> getLedgerDetails(Long ledgerId) {
        return ledgerDetailMapper.selectByLedgerId(ledgerId);
    }
    
    private String generateBatchNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 10000);
        return "BT" + timestamp + String.format("%04d", random);
    }
}