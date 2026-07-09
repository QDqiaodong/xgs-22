package com.example.lightmanager.controller;

import com.example.lightmanager.dto.LedgerPageRequestDTO;
import com.example.lightmanager.dto.ResponseDTO;
import com.example.lightmanager.dto.TransferRequestDTO;
import com.example.lightmanager.entity.BatchTransferLedger;
import com.example.lightmanager.entity.LedgerDetail;
import com.example.lightmanager.service.BatchTransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfer")
@CrossOrigin(origins = "*")
public class BatchTransferController {
    
    private final BatchTransferService batchTransferService;
    
    public BatchTransferController(BatchTransferService batchTransferService) {
        this.batchTransferService = batchTransferService;
    }
    
    @PostMapping("/batch")
    public ResponseDTO<BatchTransferLedger> batchTransfer(@Valid @RequestBody TransferRequestDTO request) {
        return ResponseDTO.success(batchTransferService.batchTransfer(request));
    }
    
    @GetMapping("/ledger/page")
    public ResponseDTO<List<BatchTransferLedger>> getLedgerPage(LedgerPageRequestDTO request) {
        var pageInfo = batchTransferService.getLedgerPage(request);
        return ResponseDTO.success(pageInfo.getRecords(), pageInfo.getTotal());
    }
    
    @GetMapping("/ledger/{id}")
    public ResponseDTO<BatchTransferLedger> getLedgerById(@PathVariable Long id) {
        return ResponseDTO.success(batchTransferService.getLedgerById(id));
    }
    
    @GetMapping("/ledger/{id}/details")
    public ResponseDTO<List<LedgerDetail>> getLedgerDetails(@PathVariable Long id) {
        return ResponseDTO.success(batchTransferService.getLedgerDetails(id));
    }
}