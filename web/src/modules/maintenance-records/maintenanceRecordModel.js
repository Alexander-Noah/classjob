const TEXT_FALLBACK = '未填写'

const firstPresent = (record, fields, fallback = TEXT_FALLBACK) => {
  for (const field of fields) {
    const value = record?.[field]
    if (value !== undefined && value !== null && String(value).trim() !== '') return value
  }
  return fallback
}

/**
 * Convert the legacy repair response shape into the MaintenanceRecord DTO.
 * Only the fields owned by the maintenance-record contract are returned.
 */
export function normalizeMaintenanceRecord(record = {}) {
  return {
    id: record?.id ?? record?.recordId ?? '',
    recordNo: firstPresent(record, ['recordNo', 'record_no', 'orderNo', 'order_no', 'ticket', 'recordCode']),
    lab: firstPresent(record, ['lab', 'labName', 'lab_name', 'laboratoryName', 'laboratory_name']),
    device: firstPresent(record, ['device', 'deviceName', 'device_name']),
    maintenanceType: firstPresent(record, ['maintenanceType', 'maintenance_type', 'faultType', 'fault_type', 'type']),
    maintenanceDate: firstPresent(record, [
      'maintenanceDate',
      'maintenance_date',
      'createdAt',
      'created_at',
      'submittedAt',
      'submitted_at',
    ]),
    maintainer: firstPresent(record, [
      'maintainer',
      'maintainerName',
      'maintainer_name',
      'assigneeName',
      'assignee_name',
      'assignee',
    ]),
    content: firstPresent(record, ['content', 'description']),
    result: firstPresent(record, ['result', 'resultDesc', 'result_desc']),
    remark: firstPresent(record, ['remark', 'contact']),
    updatedAt: firstPresent(record, ['updatedAt', 'updated_at', 'lastUpdate', 'last_update']),
  }
}
