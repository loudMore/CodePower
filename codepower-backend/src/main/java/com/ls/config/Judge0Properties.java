/**
 * 文件说明：J ud ge0 配置类，负责系统框架、中间件或外部服务的参数装配。
 */
package com.ls.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Judge0 多节点评测集群配置 */
@Data
@Component
@ConfigurationProperties(prefix = "judge0")
public class Judge0Properties {
    private Api api = new Api();
    private List<Node> nodes = new ArrayList<>();

    @Data
    public static class Api {
        private String url = "http://127.0.0.1:2358";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 30000;
        private int maxProcessesAndThreads = 2;
        private int jvmMaxProcessesAndThreads = 32;
        private int maxMemoryKb = 512000;
        private double wallTimeLimitSeconds = 3.0;
        private double cpuExtraTimeSeconds = 0.5;
        private int maxConcurrentSubmissions = 3;
        private int acquireTimeoutMs = 10000;
        private boolean enablePerProcessAndThreadTimeLimit = false;
        private boolean enablePerProcessAndThreadMemoryLimit = false;
    }

    @Data
    public static class Node {
        private String name;
        private String url;
        private boolean enabled = true;
        private int weight = 1;
        private int maxConcurrent = 3;
        private int workers = 4;
        private String authHeader = "";
        private String authToken = "";
    }
}
