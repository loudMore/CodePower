/** 内存缓存工具 — 带 TTL 过期的 Map 缓存 */
export type MemoryCacheEntry<T> = {
  value: T
  expiresAt: number
}

const memoryCache = new Map<string, MemoryCacheEntry<unknown>>()

/** 读取缓存；过期则立即删除并返回 null。 */
export function getMemoryCache<T>(key: string): T | null {
  const entry = memoryCache.get(key)
  if (!entry) return null
  if (entry.expiresAt <= Date.now()) {
    memoryCache.delete(key)
    return null
  }
  return entry.value as T
}

/** 写入缓存，并记录绝对过期时间。 */
export function setMemoryCache<T>(key: string, value: T, ttlMs: number) {
  memoryCache.set(key, {
    value,
    expiresAt: Date.now() + ttlMs
  })
}

/** 删除单个缓存键。 */
export function deleteMemoryCache(key: string) {
  memoryCache.delete(key)
}

/** 按前缀批量删除缓存，题库更新后可清掉相关 GET 缓存。 */
export function deleteMemoryCacheByPrefix(prefix: string) {
  for (const key of memoryCache.keys()) {
    if (key.startsWith(prefix)) {
      memoryCache.delete(key)
    }
  }
}
