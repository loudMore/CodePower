/**
 * 文件说明：地区解析 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

/** 地区标准化与 IP 默认定位服务。 */
public interface RegionService {

    /**
     * 将用户输入、城市名或第三方 IP 归属地结果归一到省级行政区。
     * 无法识别时返回 null，避免地图统计出现“南京省”这类脏数据。
     */
    String normalizeRegion(String rawRegion);

    /** 根据当前 HTTP 请求的客户端 IP 推断省级地区，失败时返回 null。 */
    String resolveCurrentRequestRegion();
}
