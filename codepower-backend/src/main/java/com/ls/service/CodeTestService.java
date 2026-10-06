/**
 * 文件说明：代码测试与在线评测 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import java.util.Map;

/**
 * 代码测试服务接口
 * 处理代码测试相关的业务逻辑
 */
public interface CodeTestService {
    
    /**
     * 测试代码
     * @param problemId 问题ID
     * @param code Base64编码的源代码
     * @param languageId 语言ID
     * @param stdin Base64编码的输入
     * @param expectedOutput Base64编码的期望输出
     * @param timeLimit 时间限制(ms)
     * @param memoryLimit 内存限制(KB)
     * @return 测试结果
     */
    Map<String, Object> testCode(Long problemId, String code, Integer languageId, String stdin, String expectedOutput, 
                               Integer timeLimit, Integer memoryLimit);
    
    /**
     * 根据语言名称获取Judge0语言ID
     * @param language 语言名称
     * @return Judge0语言ID
     */
    int getJudge0LanguageId(String language);
    
    /**
     * 调用Judge0 API执行代码
     * @param sourceCode Base64编码的源代码
     * @param languageId 语言ID
     * @param stdin Base64编码的输入
     * @param expectedOutput Base64编码的期望输出
     * @param timeLimit 时间限制(ms)
     * @param memoryLimit 内存限制(KB)
     * @return 执行结果
     */
    Map<String, Object> executeCodeWithJudge0(String sourceCode, int languageId, String stdin, String expectedOutput, 
                                           Integer timeLimit, Integer memoryLimit);

    /**
     * 获取评测集群运行状态，用于管理员后台观察节点健康、并发和失败情况。
     */
    Map<String, Object> getJudgeClusterStatus();
} 
