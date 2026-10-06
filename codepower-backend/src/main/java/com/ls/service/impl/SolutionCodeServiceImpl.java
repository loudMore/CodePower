/**
 * 文件说明：题解代码 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ls.domain.SolutionCode;
import com.ls.mapper.SolutionCodeMapper;
import com.ls.service.SolutionCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 题解代码服务实现类
 * @author ls
 */
@Service
public class SolutionCodeServiceImpl extends ServiceImpl<SolutionCodeMapper, SolutionCode> implements SolutionCodeService {

    @Autowired
    private SolutionCodeMapper solutionCodeMapper;

    @Override
    public List<SolutionCode> getByProblemId(Long problemId) {
        return solutionCodeMapper.selectByProblemId(problemId);
    }

    @Override
    public SolutionCode getByProblemIdAndLanguage(Long problemId, String language) {
        return solutionCodeMapper.selectByProblemIdAndLanguage(problemId, language);
    }

    @Override
    @Transactional
    public boolean createOfficialSolutions(Long problemId, Long userId, Map<String, String> solutionMap) {
        if (solutionMap == null || solutionMap.isEmpty()) {
            return false;
        }
        
        // 先删除旧的题解代码
        solutionCodeMapper.deleteByProblemId(problemId);

        List<SolutionCode> solutionCodes = new ArrayList<>();

        for (Map.Entry<String, String> entry : solutionMap.entrySet()) {
            String language = entry.getKey();
            String code = entry.getValue();

            // 跳过空代码
            if (code == null || code.trim().isEmpty()) {
                continue;
            }

            SolutionCode solutionCode = new SolutionCode();
            solutionCode.setProblemId(problemId);
            solutionCode.setLanguage(language);
            solutionCode.setCode(code);
            solutionCode.setIsOfficial(true);

            solutionCodes.add(solutionCode);
        }

        if (solutionCodes.isEmpty()) {
            return false;
        }

        return saveBatch(solutionCodes);
    }

    @Override
    public boolean deleteByProblemId(Long problemId) {
        return solutionCodeMapper.deleteByProblemId(problemId) > 0;
    }
} 