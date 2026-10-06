/**
 * Redis 缓存统一封装。
 *
 * 项目采用三级缓存架构：Caffeine（JVM本地，60min）→ Redis（分布式）→ MySQL（持久化）。
 * 所有 Redis 操作经此类统一管理，序列化方式：Key=String，Value=Jackson JSON（自动嵌入类型信息）。
 *
 * 缓存策略概览（常见 TTL）：
 *   竞赛排名 15s、题目列表 30min、推荐结果 2h、提交限频 1.5s、通知未读 30s、图表数据 5min
 */
package com.ls.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具服务 — Redis不可用时静默降级
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.debug("Redis set failed, skip cache: {}", e.getMessage());
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.debug("Redis set with duration failed, skip cache: {}", e.getMessage());
        }
    }

    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.debug("Redis get failed, skip cache: {}", e.getMessage());
            return null;
        }
    }

    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("Redis delete failed: {}", e.getMessage());
        }
    }

    public void deleteKeys(List<String> keys) {
        try {
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.debug("Redis deleteKeys failed: {}", e.getMessage());
        }
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }

    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            return 0L;
        }
    }

    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            return 0L;
        }
    }

    public Long incrementAndExpire(String key, long delta, long timeout, TimeUnit unit) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            if (value != null && value == delta) {
                redisTemplate.expire(key, timeout, unit);
            }
            return value;
        } catch (Exception e) {
            log.debug("Redis incrementAndExpire failed: {}", e.getMessage());
            return null;
        }
    }

    public Object hashGet(String key, String field) {
        try {
            return redisTemplate.opsForHash().get(key, field);
        } catch (Exception e) {
            log.debug("Redis hashGet failed, skip cache: {}", e.getMessage());
            return null;
        }
    }

    public List<Object> hashMultiGet(String key, Collection<String> fields) {
        try {
            if (fields == null || fields.isEmpty()) {
                return List.of();
            }
            List<Object> hashFields = fields.stream().map(field -> (Object) field).toList();
            return redisTemplate.opsForHash().multiGet(key, hashFields);
        } catch (Exception e) {
            log.debug("Redis hashMultiGet failed, skip cache: {}", e.getMessage());
            return List.of();
        }
    }

    public void hashPut(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
        } catch (Exception e) {
            log.debug("Redis hashPut failed, skip cache: {}", e.getMessage());
        }
    }

    public void hashPutAll(String key, Map<String, Object> values) {
        try {
            if (values != null && !values.isEmpty()) {
                redisTemplate.opsForHash().putAll(key, values);
            }
        } catch (Exception e) {
            log.debug("Redis hashPutAll failed, skip cache: {}", e.getMessage());
        }
    }

    public Long setAdd(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.debug("Redis setAdd failed, skip cache: {}", e.getMessage());
            return 0L;
        }
    }

    public Object setPop(String key) {
        try {
            return redisTemplate.opsForSet().pop(key);
        } catch (Exception e) {
            log.debug("Redis setPop failed, skip cache: {}", e.getMessage());
            return null;
        }
    }

    public List<Object> setPop(String key, long count) {
        try {
            if (count <= 0) {
                return List.of();
            }
            return redisTemplate.opsForSet().pop(key, count);
        } catch (Exception e) {
            log.debug("Redis setPop(count) failed, skip cache: {}", e.getMessage());
            return List.of();
        }
    }

    public Set<Object> setMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.debug("Redis setMembers failed, skip cache: {}", e.getMessage());
            return Set.of();
        }
    }

    public void zSetAdd(String key, Object value, double score) {
        try {
            redisTemplate.opsForZSet().add(key, value, score);
        } catch (Exception e) {
            log.debug("Redis zSetAdd failed: {}", e.getMessage());
        }
    }

    public void zSetIncrBy(String key, Object value, double delta) {
        try {
            redisTemplate.opsForZSet().incrementScore(key, value, delta);
        } catch (Exception e) {
            log.debug("Redis zSetIncrBy failed: {}", e.getMessage());
        }
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.debug("Redis expire failed: {}", e.getMessage());
        }
    }

    public void deleteByPattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.debug("Redis deleteByPattern failed: {}", e.getMessage());
        }
    }
}
