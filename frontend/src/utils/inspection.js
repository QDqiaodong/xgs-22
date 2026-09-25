// 灯组巡检异常处置 - 枚举与展示常量(与后端 InspectionConstants 对应)

export const RESULT_OPTIONS = [
  { value: 'NORMAL', label: '正常', type: 'success' },
  { value: 'OFF', label: '熄灭', type: 'danger' },
  { value: 'FLICKER', label: '频闪', type: 'warning' },
  { value: 'DIM', label: '亮度不足', type: 'warning' }
]

export const ABNORMAL_RESULTS = ['OFF', 'FLICKER', 'DIM']

const RESULT_LABEL_MAP = RESULT_OPTIONS.reduce((m, i) => { m[i.value] = i; return m }, {})

export function resultLabel(value) {
  return RESULT_LABEL_MAP[value]?.label || value || '未登记'
}

export function resultTagType(value) {
  return RESULT_LABEL_MAP[value]?.type || 'info'
}

export function isAbnormal(value) {
  return ABNORMAL_RESULTS.includes(value)
}

// 异常处置状态
export const STATUS_OPTIONS = [
  { value: 'PENDING', label: '待复核', type: 'warning' },
  { value: 'RETURNED', label: '已退回待补充', type: 'danger' },
  { value: 'CONFIRMED', label: '已确认待处理', type: 'primary' },
  { value: 'CLOSED', label: '已关闭', type: 'info' }
]

const STATUS_LABEL_MAP = STATUS_OPTIONS.reduce((m, i) => { m[i.value] = i; return m }, {})

export function statusLabel(value) {
  return STATUS_LABEL_MAP[value]?.label || value
}

export function statusTagType(value) {
  return STATUS_LABEL_MAP[value]?.type || 'info'
}

// 处置流转动作
export const ACTION_LABELS = {
  SUBMIT: { label: '异常上报', type: 'warning' },
  RETURN: { label: '退回补充', type: 'danger' },
  SUPPLEMENT: { label: '补充说明', type: 'warning' },
  CONFIRM: { label: '确认异常', type: 'primary' },
  HANDLE: { label: '登记处理结果', type: 'primary' },
  CLOSE: { label: '处置关闭', type: 'success' }
}

export function actionLabel(action) {
  return ACTION_LABELS[action]?.label || action
}

export function actionTagType(action) {
  return ACTION_LABELS[action]?.type || 'info'
}

// 批次状态
export function batchStatusLabel(status) {
  return status === 1 ? '已提交' : '暂存中'
}

export function batchStatusType(status) {
  return status === 1 ? 'success' : 'warning'
}
