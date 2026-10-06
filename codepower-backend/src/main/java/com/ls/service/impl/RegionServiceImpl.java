/**
 * 文件说明：R eg io n 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ls.service.RegionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
    private static final List<String> GEO_ENDPOINTS = List.of(
            "https://whois.pconline.com.cn/ipJson.jsp?ip=%s&json=true",
            "http://whois.pconline.com.cn/ipJson.jsp?ip=%s&json=true",
            "https://ip-api.com/json/%s?fields=status,country,regionName,city,message&lang=zh-CN",
            "http://ip-api.com/json/%s?fields=status,country,regionName,city,message&lang=zh-CN"
    );

    private static final List<String> PROVINCES = List.of(
            "北京市", "天津市", "河北省", "山西省", "内蒙古自治区", "辽宁省", "吉林省", "黑龙江省",
            "上海市", "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省", "河南省",
            "湖北省", "湖南省", "广东省", "广西壮族自治区", "海南省", "重庆市", "四川省",
            "贵州省", "云南省", "西藏自治区", "陕西省", "甘肃省", "青海省",
            "宁夏回族自治区", "新疆维吾尔自治区", "香港特别行政区", "澳门特别行政区", "台湾省"
    );

    private static final Map<String, String> CITY_TO_PROVINCE = Map.ofEntries(
            Map.entry("北京", "北京市"),
            Map.entry("天津", "天津市"),
            Map.entry("上海", "上海市"),
            Map.entry("重庆", "重庆市"),
            Map.entry("石家庄", "河北省"),
            Map.entry("唐山", "河北省"),
            Map.entry("秦皇岛", "河北省"),
            Map.entry("太原", "山西省"),
            Map.entry("大同", "山西省"),
            Map.entry("呼和浩特", "内蒙古自治区"),
            Map.entry("包头", "内蒙古自治区"),
            Map.entry("沈阳", "辽宁省"),
            Map.entry("大连", "辽宁省"),
            Map.entry("长春", "吉林省"),
            Map.entry("吉林市", "吉林省"),
            Map.entry("哈尔滨", "黑龙江省"),
            Map.entry("南京", "江苏省"),
            Map.entry("苏州", "江苏省"),
            Map.entry("无锡", "江苏省"),
            Map.entry("杭州", "浙江省"),
            Map.entry("宁波", "浙江省"),
            Map.entry("合肥", "安徽省"),
            Map.entry("芜湖", "安徽省"),
            Map.entry("福州", "福建省"),
            Map.entry("厦门", "福建省"),
            Map.entry("南昌", "江西省"),
            Map.entry("济南", "山东省"),
            Map.entry("青岛", "山东省"),
            Map.entry("郑州", "河南省"),
            Map.entry("洛阳", "河南省"),
            Map.entry("武汉", "湖北省"),
            Map.entry("长沙", "湖南省"),
            Map.entry("广州", "广东省"),
            Map.entry("深圳", "广东省"),
            Map.entry("东莞", "广东省"),
            Map.entry("佛山", "广东省"),
            Map.entry("南宁", "广西壮族自治区"),
            Map.entry("桂林", "广西壮族自治区"),
            Map.entry("海口", "海南省"),
            Map.entry("三亚", "海南省"),
            Map.entry("成都", "四川省"),
            Map.entry("绵阳", "四川省"),
            Map.entry("贵阳", "贵州省"),
            Map.entry("昆明", "云南省"),
            Map.entry("拉萨", "西藏自治区"),
            Map.entry("西安", "陕西省"),
            Map.entry("兰州", "甘肃省"),
            Map.entry("西宁", "青海省"),
            Map.entry("银川", "宁夏回族自治区"),
            Map.entry("乌鲁木齐", "新疆维吾尔自治区"),
            Map.entry("香港", "香港特别行政区"),
            Map.entry("澳门", "澳门特别行政区"),
            Map.entry("台北", "台湾省"),
            Map.entry("台湾", "台湾省")
    );

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    @Override
    public String normalizeRegion(String rawRegion) {
        if (rawRegion == null) {
            return null;
        }

        String normalized = rawRegion.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        normalized = normalized.replace(" ", "")
                .replace("中华人民共和国", "")
                .replace("中国", "");

        for (String province : PROVINCES) {
            if (normalized.equals(province) || normalized.equals(toShortName(province))) {
                return province;
            }
        }

        for (String province : PROVINCES) {
            String shortName = toShortName(province);
            if (normalized.contains(province) || normalized.contains(shortName)) {
                return province;
            }
        }

        for (Map.Entry<String, String> entry : CITY_TO_PROVINCE.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    @Override
    public String resolveCurrentRequestRegion() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return resolveRegionFromRequest(attributes.getRequest());
    }

    String resolveRegionFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String clientIp = resolveClientIp(request);
        if (clientIp == null) {
            return null;
        }

        return lookupRegionByIp(clientIp);
    }

    private String resolveClientIp(HttpServletRequest request) {
        for (String headerName : List.of(
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR")) {
            String ip = extractFirstIp(request.getHeader(headerName));
            if (ip != null) {
                return ip;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return isUsableIp(remoteAddr) ? remoteAddr : null;
    }

    private String extractFirstIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        for (String item : headerValue.split(",")) {
            String candidate = item.trim();
            if (isUsableIp(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isUsableIp(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }

        String normalized = ip.trim().toLowerCase(Locale.ROOT);
        if ("127.0.0.1".equals(normalized) || "::1".equals(normalized) || "0:0:0:0:0:0:0:1".equals(normalized)) {
            return false;
        }
        if (normalized.startsWith("fc") || normalized.startsWith("fd") || normalized.startsWith("fe80")) {
            return false;
        }
        if (normalized.startsWith("10.") || normalized.startsWith("192.168.")) {
            return false;
        }
        if (normalized.startsWith("172.")) {
            String[] parts = normalized.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) {
                        return false;
                    }
                } catch (NumberFormatException ignore) {
                    return false;
                }
            }
        }
        return true;
    }

    private String lookupRegionByIp(String ip) {
        String encodedIp = URLEncoder.encode(ip, StandardCharsets.UTF_8);
        for (String endpoint : GEO_ENDPOINTS) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(String.format(endpoint, encodedIp)))
                        .timeout(REQUEST_TIMEOUT)
                        .header("User-Agent", "CodePower/1.0")
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String region = parseRegionResponse(response.body());
                    if (region != null) {
                        return region;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (IOException e) {
                log.debug("IP region lookup failed: ip={}, endpoint={}, error={}", ip, endpoint, e.getMessage());
            } catch (Exception e) {
                log.debug("IP region lookup failed: ip={}, endpoint={}, error={}", ip, endpoint, e.getMessage());
            }
        }
        return null;
    }

    private String parseRegionResponse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null || root.isNull()) {
                return null;
            }
            if (root.hasNonNull("status") && "fail".equalsIgnoreCase(root.path("status").asText())) {
                return null;
            }

            for (String key : List.of("pro", "province", "regionName", "region", "region_name", "city")) {
                String value = textValue(root, key);
                String region = normalizeRegion(value);
                if (region != null) {
                    return region;
                }
            }
        } catch (Exception e) {
            log.debug("IP region response parse failed: {}", e.getMessage());
        }
        return null;
    }

    private String textValue(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String toShortName(String province) {
        return switch (province) {
            case "北京市", "天津市", "上海市", "重庆市" -> province.substring(0, 2);
            case "香港特别行政区", "澳门特别行政区" -> province.substring(0, 2);
            case "内蒙古自治区" -> "内蒙古";
            case "广西壮族自治区" -> "广西";
            case "西藏自治区" -> "西藏";
            case "宁夏回族自治区" -> "宁夏";
            case "新疆维吾尔自治区" -> "新疆";
            default -> province.replace("省", "").replace("市", "");
        };
    }
}
