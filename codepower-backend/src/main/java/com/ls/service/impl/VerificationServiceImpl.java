/**
 * 文件说明：登录前人机验证 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ls.domain.dto.VerificationChallenge;
import com.ls.domain.dto.VerificationResult;
import com.ls.service.VerificationService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 编程题人机验证服务。
 * 登录前生成一道短代码推理题，答对后发放短期验证令牌。
 */
@Service
public class VerificationServiceImpl implements VerificationService {

    private final Cache<String, String> solutionCache = Caffeine.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .maximumSize(2000)
            .build();

    private final Cache<String, String> tokenCache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .maximumSize(2000)
            .build();

    private final Cache<String, Integer> attemptCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(5000)
            .build();

    private final Random random = new Random();

    /** 创建验证挑战，缓存正确答案并返回题面和选项。 */
    @Override
    public VerificationChallenge createChallenge() {
        String challengeId = UUID.randomUUID().toString();

        CodePuzzle puzzle = generateCodePuzzle();

        solutionCache.put(challengeId, puzzle.answer);

        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setChallengeId(challengeId);
        challenge.setPrompt(puzzle.code);
        challenge.setOptions(puzzle.options);
        return challenge;
    }

    /** 校验用户选择，答对后生成一次性验证令牌。 */
    @Override
    public VerificationResult verify(String challengeId, List<String> selection) {
        VerificationResult result = new VerificationResult();

        Integer attempts = attemptCache.getIfPresent(challengeId);
        if (attempts != null && attempts >= 5) {
            solutionCache.invalidate(challengeId);
            result.setSuccess(false);
            result.setMessage("尝试次数过多，请刷新重试");
            return result;
        }
        attemptCache.put(challengeId, (attempts == null ? 0 : attempts) + 1);

        String solution = solutionCache.getIfPresent(challengeId);
        if (solution == null) {
            result.setSuccess(false);
            result.setMessage("验证已过期，请重新获取");
            return result;
        }

        String userAnswer = (selection != null && !selection.isEmpty()) ? selection.get(0) : "";

        if (solution.equals(userAnswer)) {
            solutionCache.invalidate(challengeId);
            attemptCache.invalidate(challengeId);
            String token = UUID.randomUUID().toString();
            tokenCache.put(token, "VALID");
            result.setSuccess(true);
            result.setMessage("验证成功");
            result.setVerificationToken(token);
        } else {
            result.setSuccess(false);
            result.setMessage("回答错误，请再试一次");
        }
        return result;
    }

    /** 登录时校验验证令牌，校验成功后立即失效，防止重复使用。 */
    @Override
    public boolean validateVerificationToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        boolean isValid = tokenCache.getIfPresent(token) != null;
        if (isValid) {
            tokenCache.invalidate(token);
        }
        return isValid;
    }

    /** 随机选择一种代码题类型，降低固定题型被脚本绕过的概率。 */
    private CodePuzzle generateCodePuzzle() {
        int type = random.nextInt(8);
        return switch (type) {
            case 0 -> generateArithmeticPuzzle();
            case 1 -> generateLoopPuzzle();
            case 2 -> generateConditionalPuzzle();
            case 3 -> generateStringPuzzle();
            case 4 -> generateArrayPuzzle();
            case 5 -> generateBitPuzzle();
            case 6 -> generateModuloPuzzle();
            case 7 -> generateSwapPuzzle();
            default -> generateArithmeticPuzzle();
        };
    }

    private CodePuzzle generateArithmeticPuzzle() {
        int a = random.nextInt(20) + 1;
        int b = random.nextInt(20) + 1;
        int op = random.nextInt(3);
        String opStr;
        int answer;
        switch (op) {
            case 0 -> { opStr = "+"; answer = a + b; }
            case 1 -> { opStr = "*"; answer = a * b; }
            default -> { opStr = "-"; answer = a - b; }
        }
        String code = String.format("x = %d\ny = %d\nprint(x %s y)", a, b, opStr);
        return buildPuzzle(code, String.valueOf(answer));
    }

    private CodePuzzle generateLoopPuzzle() {
        int n = random.nextInt(6) + 3;
        int start = random.nextInt(3);
        int sum = 0;
        for (int i = start; i < n; i++) sum += i;
        String code = String.format("total = 0\nfor i in range(%d, %d):\n    total += i\nprint(total)", start, n);
        return buildPuzzle(code, String.valueOf(sum));
    }

    private CodePuzzle generateConditionalPuzzle() {
        int x = random.nextInt(30) + 1;
        int threshold = random.nextInt(20) + 5;
        String result = x > threshold ? "big" : "small";
        String code = String.format("x = %d\nif x > %d:\n    print(\"big\")\nelse:\n    print(\"small\")", x, threshold);
        return buildPuzzle(code, result);
    }

    private CodePuzzle generateStringPuzzle() {
        String[] words = {"hello", "world", "code", "python", "power", "algo", "data", "test"};
        String word = words[random.nextInt(words.length)];
        int op = random.nextInt(3);
        String code;
        String answer;
        switch (op) {
            case 0 -> {
                code = String.format("s = \"%s\"\nprint(len(s))", word);
                answer = String.valueOf(word.length());
            }
            case 1 -> {
                code = String.format("s = \"%s\"\nprint(s[0])", word);
                answer = String.valueOf(word.charAt(0));
            }
            default -> {
                code = String.format("s = \"%s\"\nprint(s.upper())", word);
                answer = word.toUpperCase();
            }
        }
        return buildPuzzle(code, answer);
    }

    private CodePuzzle generateArrayPuzzle() {
        int len = random.nextInt(4) + 3;
        int[] arr = new int[len];
        for (int i = 0; i < len; i++) arr[i] = random.nextInt(10) + 1;

        int op = random.nextInt(3);
        String arrStr = Arrays.toString(arr);
        String code;
        String answer;
        switch (op) {
            case 0 -> {
                int sum = Arrays.stream(arr).sum();
                code = String.format("a = %s\nprint(sum(a))", arrStr);
                answer = String.valueOf(sum);
            }
            case 1 -> {
                int max = Arrays.stream(arr).max().orElse(0);
                code = String.format("a = %s\nprint(max(a))", arrStr);
                answer = String.valueOf(max);
            }
            default -> {
                code = String.format("a = %s\nprint(len(a))", arrStr);
                answer = String.valueOf(len);
            }
        }
        return buildPuzzle(code, answer);
    }

    private CodePuzzle generateBitPuzzle() {
        int val = (random.nextInt(8) + 1) * 2;
        int shift = random.nextInt(2) + 1;
        boolean leftShift = random.nextBoolean();
        int answer = leftShift ? val << shift : val >> shift;
        String op = leftShift ? "<<" : ">>";
        String code = String.format("x = %d\nprint(x %s %d)", val, op, shift);
        return buildPuzzle(code, String.valueOf(answer));
    }

    private CodePuzzle generateModuloPuzzle() {
        int a = random.nextInt(30) + 10;
        int b = random.nextInt(7) + 2;
        int answer = a % b;
        String code = String.format("print(%d %% %d)", a, b);
        return buildPuzzle(code, String.valueOf(answer));
    }

    private CodePuzzle generateSwapPuzzle() {
        int a = random.nextInt(20) + 1;
        int b = random.nextInt(20) + 1;
        while (b == a) b = random.nextInt(20) + 1;
        String code = String.format("a = %d\nb = %d\na, b = b, a\nprint(a, b)", a, b);
        String answer = b + " " + a;
        return buildPuzzle(code, answer);
    }

    private CodePuzzle buildPuzzle(String code, String correctAnswer) {
        Set<String> optionSet = new LinkedHashSet<>();
        optionSet.add(correctAnswer);

        int attempts = 0;
        while (optionSet.size() < 4 && attempts++ < 20) {
            String wrong = generateWrongAnswer(correctAnswer);
            optionSet.add(wrong);
        }
        int fallbackIndex = 1;
        while (optionSet.size() < 4) {
            optionSet.add(generateFallbackWrongAnswer(correctAnswer, fallbackIndex++));
        }

        List<String> options = new ArrayList<>(optionSet);
        Collections.shuffle(options);

        CodePuzzle puzzle = new CodePuzzle();
        puzzle.code = code;
        puzzle.answer = correctAnswer;
        puzzle.options = options;
        return puzzle;
    }

    private String generateWrongAnswer(String correct) {
        try {
            int val = Integer.parseInt(correct);
            int offset = random.nextInt(5) + 1;
            if (random.nextBoolean()) offset = -offset;
            int wrong = val + offset;
            if (wrong == val) wrong = val + 1;
            return String.valueOf(wrong);
        } catch (NumberFormatException e) {
            if (correct.contains(" ")) {
                String[] parts = correct.split(" ");
                if (parts.length == 2) {
                    String[] candidates = {
                            parts[1] + " " + parts[0],
                            parts[0] + " " + parts[0],
                            parts[1] + " " + parts[1],
                            "0 0"
                    };
                    return candidates[random.nextInt(candidates.length)];
                }
            }
            if (correct.length() <= 3) {
                char[] chars = correct.toCharArray();
                if (chars.length > 0) {
                    chars[0] = (char) (chars[0] + random.nextInt(3) + 1);
                    String w = new String(chars);
                    if (!w.equals(correct)) return w;
                }
            }
            String[] fallbacks = {"error", "None", "0", "undefined", "null", "true", "false"};
            String f;
            do {
                f = fallbacks[random.nextInt(fallbacks.length)];
            } while (f.equals(correct));
            return f;
        }
    }

    private String generateFallbackWrongAnswer(String correct, int index) {
        try {
            int val = Integer.parseInt(correct);
            return String.valueOf(val + index + 10);
        } catch (NumberFormatException e) {
            if (correct.contains(" ")) {
                return (index + 20) + " " + (index + 30);
            }
            return "option" + index;
        }
    }

    private static class CodePuzzle {
        String code;
        String answer;
        List<String> options;
    }
}
