/** 文件说明：日期时间工具，统一解析后端时间并格式化为页面展示文本。 */
const HAS_TIMEZONE = /(?:Z|[+-]\d{2}:?\d{2})$/i

const normalizeDateText = (value: string) => value.trim().replace(' ', 'T')

/**
 * 解析后端时间字段。
 * 后端有些字段带时区、有些是不带时区的本地时间，这里做兼容，避免页面显示差 8 小时。
 */
export function parseApiDate(value?: string | number | Date | null): Date | null {
  if (value === null || value === undefined || value === '') return null
  if (value instanceof Date) return Number.isFinite(value.getTime()) ? value : null
  if (typeof value === 'number') {
    const date = new Date(value)
    return Number.isFinite(date.getTime()) ? date : null
  }

  const raw = String(value).trim()
  if (!raw) return null

  if (HAS_TIMEZONE.test(raw)) {
    const date = new Date(raw)
    return Number.isFinite(date.getTime()) ? date : null
  }

  const normalized = normalizeDateText(raw)
  const localDate = new Date(normalized)
  if (!Number.isFinite(localDate.getTime())) return null

  const utcDate = new Date(`${normalized}Z`)
  if (!Number.isFinite(utcDate.getTime())) return localDate

  const now = Date.now()
  const localDelta = Math.abs(now - localDate.getTime())
  const utcDelta = Math.abs(now - utcDate.getTime())

  if (utcDelta < 3 * 60 * 60 * 1000 && utcDelta + 60 * 60 * 1000 < localDelta) {
    return utcDate
  }

  return localDate
}

/** 转成毫秒时间戳，排序竞赛和提交记录时使用。 */
export function apiDateTimeMs(value?: string | number | Date | null): number {
  return parseApiDate(value)?.getTime() ?? 0
}

/** 格式化完整日期时间。 */
export function formatApiDateTime(value?: string | number | Date | null): string {
  const date = parseApiDate(value)
  return date ? date.toLocaleString('zh-CN') : '-'
}

/** 格式化日期。 */
export function formatApiDate(value?: string | number | Date | null): string {
  const date = parseApiDate(value)
  return date ? date.toLocaleDateString('zh-CN') : '-'
}

/** 格式化相对时间，例如“刚刚”“3分钟前”。 */
export function formatRelativeTime(value?: string | number | Date | null): string {
  const date = parseApiDate(value)
  if (!date) return ''
  const diff = Math.max(0, Date.now() - date.getTime())
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}小时前`
  if (diff < 604_800_000) return `${Math.floor(diff / 86_400_000)}天前`
  return date.toLocaleDateString('zh-CN')
}
