package com.example.lightmanager.service;

import com.example.lightmanager.dto.LedgerPageRequestDTO;
import com.example.lightmanager.dto.TransferRequestDTO;
import com.example.lightmanager.entity.BatchTransferLedger;
import com.example.lightmanager.entity.LedgerDetail;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface BatchTransferService {
    
    BatchTransferLedger batchTransfer(TransferRequestDTO request);
    
    IPage<BatchTransferLedger> getLedgerPage(LedgerPageRequestDTO request);
    
    BatchTransferLedger getLedgerById(Long id);
    
    List<LedgerDetail> getLedgerDetails(Long ledgerId);
}