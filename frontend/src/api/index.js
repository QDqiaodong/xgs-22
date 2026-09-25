import request from '@/utils/request'

export const zoneApi = {
  getTree() {
    return request.get('/zone/tree')
  },
  getAll() {
    return request.get('/zone/list')
  },
  getById(id) {
    return request.get(`/zone/${id}`)
  },
  create(data) {
    return request.post('/zone', data)
  },
  update(data) {
    return request.put('/zone', data)
  },
  delete(id) {
    return request.delete(`/zone/${id}`)
  }
}

export const lightGroupApi = {
  getPage(params) {
    return request.get('/light-group/page', { params })
  },
  getById(id) {
    return request.get(`/light-group/${id}`)
  },
  getByZoneId(zoneId) {
    return request.get(`/light-group/by-zone/${zoneId}`)
  },
  create(data) {
    return request.post('/light-group', data)
  },
  update(data) {
    return request.put('/light-group', data)
  },
  delete(id) {
    return request.delete(`/light-group/${id}`)
  },
  exportByZoneId(zoneId) {
    return request.get(`/light-group/export/${zoneId}`, { responseType: 'blob' })
  },
  exportSelected(ids) {
    return request.get('/light-group/export-selected', { 
      params: { ids: ids.join(',') },
      responseType: 'blob' 
    })
  }
}

export const transferApi = {
  batchTransfer(data) {
    return request.post('/transfer/batch', data)
  },
  getLedgerPage(params) {
    return request.get('/transfer/ledger/page', { params })
  },
  getLedgerById(id) {
    return request.get(`/transfer/ledger/${id}`)
  },
  getLedgerDetails(id) {
    return request.get(`/transfer/ledger/${id}/details`)
  }
}

// ==================== 灯组巡检异常处置 ====================

export const inspectionApi = {
  // 批次
  createBatch(data) {
    return request.post('/inspection/batch', data)
  },
  saveItems(data) {
    return request.post('/inspection/items/save', data)
  },
  submitBatch(batchId) {
    return request.post('/inspection/batch/submit', { batchId })
  },
  getBatchPage(params) {
    return request.get('/inspection/batch/page', { params })
  },
  getBatchDetail(id) {
    return request.get(`/inspection/batch/${id}`)
  },
  // 异常清单
  getExceptionPage(params) {
    return request.get('/inspection/exception/page', { params })
  },
  getExceptionDetail(id) {
    return request.get(`/inspection/exception/${id}`)
  },
  returnException(data) {
    return request.post('/inspection/exception/return', data)
  },
  supplement(data) {
    return request.post('/inspection/exception/supplement', data)
  },
  confirm(data) {
    return request.post('/inspection/exception/confirm', data)
  },
  registerHandleResult(data) {
    return request.post('/inspection/exception/handle', data)
  },
  close(data) {
    return request.post('/inspection/exception/close', data)
  }
}