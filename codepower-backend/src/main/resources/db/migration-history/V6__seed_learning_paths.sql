-- =====================================================================
-- V6: 学习路线种子数据
-- 提供 5 条学习路线，每条含 5-8 个阶段，覆盖主流编程语言和算法方向
-- =====================================================================

-- ==================== 路线1: C语言基础入门 ====================
INSERT INTO `learning_paths` (`title`, `description`, `difficulty`, `language`, `estimated_hours`, `sort_order`, `status`) VALUES
('C语言基础入门', '从零开始学习C语言，掌握变量、控制流、函数、指针和内存管理等核心概念，为后续数据结构与算法学习打下坚实基础。', '简单', 'C', 40, 100, 1);

SET @path1 = LAST_INSERT_ID();

INSERT INTO `learning_path_stages` (`path_id`, `title`, `description`, `content`, `stage_order`, `estimated_minutes`) VALUES
(@path1, '开发环境搭建', '安装GCC编译器和VS Code，配置C语言开发环境',
'## 开发环境搭建\n\n### Windows 环境\n1. 下载并安装 **MinGW-w64**（推荐 MSYS2）\n2. 配置系统环境变量，将 `gcc` 加入 PATH\n3. 安装 VS Code 并安装 **C/C++** 扩展\n4. 验证：打开终端输入 `gcc --version`\n\n### 第一个程序\n```c\n#include <stdio.h>\nint main() {\n    printf("Hello, CodePower!\\n");\n    return 0;\n}\n```\n\n编译运行：`gcc hello.c -o hello && ./hello`', 1, 30),

(@path1, '变量与数据类型', '学习int、float、char等基本数据类型，掌握变量声明和类型转换',
'## 基本数据类型\n\n| 类型 | 大小 | 范围 |\n|------|------|------|\n| `int` | 4字节 | -2^31 ~ 2^31-1 |\n| `float` | 4字节 | 约6位有效数字 |\n| `double` | 8字节 | 约15位有效数字 |\n| `char` | 1字节 | -128 ~ 127 |\n\n### 变量声明\n```c\nint age = 20;\nfloat pi = 3.14f;\nchar grade = ''A'';\n```\n\n### 类型转换\n- 隐式转换：小类型 → 大类型自动提升\n- 显式转换：`(int)3.14` 结果为 `3`\n\n### 练习\n声明不同类型的变量并使用 `printf` 输出它们的值和大小。', 2, 45),

(@path1, '控制流语句', '掌握if-else、switch、for、while等控制结构',
'## 条件语句\n\n```c\nif (score >= 90) {\n    printf("优秀");\n} else if (score >= 60) {\n    printf("及格");\n} else {\n    printf("不及格");\n}\n```\n\n## 循环语句\n\n### for 循环\n```c\nfor (int i = 1; i <= 100; i++) {\n    sum += i;\n}\n```\n\n### while 循环\n```c\nwhile (n > 0) {\n    digit_count++;\n    n /= 10;\n}\n```\n\n### 练习题目\n1. 判断一个数是否为素数\n2. 打印九九乘法表\n3. 求斐波那契数列第n项', 3, 60),

(@path1, '函数', '学习函数定义、参数传递、返回值和递归',
'## 函数基础\n\n```c\n// 函数声明\nint max(int a, int b);\n\n// 函数定义\nint max(int a, int b) {\n    return a > b ? a : b;\n}\n```\n\n## 参数传递\n- **值传递**：函数内修改不影响实参\n- **指针传递**：通过指针可以修改实参\n\n```c\nvoid swap(int *a, int *b) {\n    int temp = *a;\n    *a = *b;\n    *b = temp;\n}\n```\n\n## 递归\n```c\nint factorial(int n) {\n    if (n <= 1) return 1;\n    return n * factorial(n - 1);\n}\n```\n\n### 练习\n1. 实现二分查找函数\n2. 用递归实现汉诺塔', 4, 60),

(@path1, '数组与字符串', '掌握一维数组、二维数组和字符串操作',
'## 一维数组\n```c\nint arr[5] = {1, 2, 3, 4, 5};\nfor (int i = 0; i < 5; i++) {\n    printf("%d ", arr[i]);\n}\n```\n\n## 二维数组\n```c\nint matrix[3][3] = {\n    {1, 2, 3},\n    {4, 5, 6},\n    {7, 8, 9}\n};\n```\n\n## 字符串\nC语言中字符串是以 `\\0` 结尾的字符数组。\n```c\nchar name[] = "CodePower";\nprintf("长度: %lu\\n", strlen(name));\n```\n\n常用函数：`strlen`, `strcpy`, `strcat`, `strcmp`\n\n### 练习\n1. 实现冒泡排序\n2. 字符串反转\n3. 统计字符出现次数', 5, 60),

(@path1, '指针入门', '理解指针的概念，掌握指针与数组、函数的关系',
'## 什么是指针\n指针是存储内存地址的变量。\n```c\nint x = 42;\nint *p = &x;  // p指向x\nprintf("%d\\n", *p);  // 解引用：输出42\n```\n\n## 指针与数组\n数组名就是首元素的地址：\n```c\nint arr[] = {10, 20, 30};\nint *p = arr;\nprintf("%d\\n", *(p + 1));  // 20\n```\n\n## 动态内存分配\n```c\nint *arr = (int *)malloc(n * sizeof(int));\nif (arr == NULL) {\n    printf("内存分配失败\\n");\n    return -1;\n}\n// 使用...\nfree(arr);\n```\n\n### 练习\n1. 用指针遍历数组\n2. 实现动态数组', 6, 90);

-- ==================== 路线2: Python编程入门 ====================
INSERT INTO `learning_paths` (`title`, `description`, `difficulty`, `language`, `estimated_hours`, `sort_order`, `status`) VALUES
('Python编程入门', '零基础学习Python 3，从语法基础到面向对象编程，涵盖列表、字典、文件操作等实用技能，适合编程初学者。', '简单', 'Python', 35, 90, 1);

SET @path2 = LAST_INSERT_ID();

INSERT INTO `learning_path_stages` (`path_id`, `title`, `description`, `content`, `stage_order`, `estimated_minutes`) VALUES
(@path2, 'Python环境与基础语法', '安装Python，学习变量、输入输出和基本运算',
'## 安装 Python\n1. 从 [python.org](https://python.org) 下载 Python 3.10+\n2. 安装时勾选 **Add Python to PATH**\n3. 验证：`python --version`\n\n## 基础语法\n```python\n# 变量不需要声明类型\nname = "CodePower"\nage = 20\npi = 3.14\n\n# 输入输出\nuser_input = input("请输入姓名: ")\nprint(f"你好, {user_input}!")\n\n# 基本运算\nprint(10 // 3)   # 整除: 3\nprint(10 ** 2)   # 幂运算: 100\nprint(10 % 3)    # 取模: 1\n```\n\nPython 使用缩进（4个空格）表示代码块，不使用大括号。', 1, 40),

(@path2, '控制流与循环', '掌握if/elif/else、for、while和列表推导式',
'## 条件语句\n```python\nscore = 85\nif score >= 90:\n    grade = "A"\nelif score >= 80:\n    grade = "B"\nelse:\n    grade = "C"\n```\n\n## 循环\n```python\n# for 循环\nfor i in range(1, 11):\n    print(i)\n\n# while 循环\nn = 10\nwhile n > 0:\n    print(n)\n    n -= 1\n```\n\n## 列表推导式\n```python\nsquares = [x**2 for x in range(10)]\nevens = [x for x in range(20) if x % 2 == 0]\n```\n\n### 练习\n1. FizzBuzz 问题\n2. 猜数字游戏\n3. 打印杨辉三角', 2, 50),

(@path2, '函数与模块', '学习函数定义、参数类型、lambda表达式和模块导入',
'## 函数定义\n```python\ndef greet(name, greeting="你好"):\n    return f"{greeting}, {name}!"\n\nprint(greet("小明"))           # 你好, 小明!\nprint(greet("Tom", "Hello"))  # Hello, Tom!\n```\n\n## 可变参数\n```python\ndef calc_sum(*args):\n    return sum(args)\n\ndef print_info(**kwargs):\n    for k, v in kwargs.items():\n        print(f"{k}: {v}")\n```\n\n## Lambda 表达式\n```python\nsquare = lambda x: x ** 2\nnums = [3, 1, 4, 1, 5]\nnums.sort(key=lambda x: -x)  # 降序\n```\n\n## 模块\n```python\nimport math\nfrom collections import Counter\n```', 3, 50),

(@path2, '列表、字典与集合', '掌握Python核心数据结构的使用方法',
'## 列表 (List)\n```python\nfruits = ["apple", "banana", "cherry"]\nfruits.append("date")\nfruits.insert(1, "blueberry")\nsliced = fruits[1:3]\n```\n\n## 字典 (Dict)\n```python\nstudent = {"name": "小明", "age": 20, "score": 95}\nstudent["grade"] = "A"\nfor key, value in student.items():\n    print(f"{key}: {value}")\n```\n\n## 集合 (Set)\n```python\na = {1, 2, 3, 4}\nb = {3, 4, 5, 6}\nprint(a & b)  # 交集: {3, 4}\nprint(a | b)  # 并集: {1, 2, 3, 4, 5, 6}\n```\n\n### 练习\n1. 统计文本中单词频率\n2. 两个列表的交集\n3. 字典按值排序', 4, 50),

(@path2, '文件操作与异常处理', '学习读写文件和try-except异常处理机制',
'## 文件读写\n```python\n# 写文件\nwith open("output.txt", "w", encoding="utf-8") as f:\n    f.write("Hello, CodePower!\\n")\n\n# 读文件\nwith open("output.txt", "r", encoding="utf-8") as f:\n    content = f.read()\n    print(content)\n```\n\n## 异常处理\n```python\ntry:\n    result = 10 / 0\nexcept ZeroDivisionError:\n    print("不能除以零")\nexcept Exception as e:\n    print(f"发生错误: {e}")\nfinally:\n    print("清理资源")\n```\n\n## 自定义异常\n```python\nclass AgeError(Exception):\n    pass\n\ndef set_age(age):\n    if age < 0 or age > 150:\n        raise AgeError(f"无效年龄: {age}")\n```', 5, 45),

(@path2, '面向对象编程', '学习类、继承、封装和多态',
'## 类的定义\n```python\nclass Student:\n    def __init__(self, name, score):\n        self.name = name\n        self.score = score\n    \n    def grade(self):\n        if self.score >= 90:\n            return "A"\n        elif self.score >= 60:\n            return "B"\n        return "C"\n    \n    def __str__(self):\n        return f"{self.name}: {self.grade()}"\n```\n\n## 继承\n```python\nclass GradStudent(Student):\n    def __init__(self, name, score, research):\n        super().__init__(name, score)\n        self.research = research\n```\n\n### 练习\n1. 设计一个银行账户类\n2. 实现图形类继承体系（Shape→Circle, Rectangle）', 6, 60);

-- ==================== 路线3: 数据结构与算法 ====================
INSERT INTO `learning_paths` (`title`, `description`, `difficulty`, `language`, `estimated_hours`, `sort_order`, `status`) VALUES
('数据结构与算法', '系统学习常用数据结构（数组、链表、栈、队列、树、图）和经典算法（排序、搜索、动态规划），提升编程核心竞争力。', '普通', '通用', 60, 80, 1);

SET @path3 = LAST_INSERT_ID();

INSERT INTO `learning_path_stages` (`path_id`, `title`, `description`, `content`, `stage_order`, `estimated_minutes`) VALUES
(@path3, '复杂度分析', '理解时间复杂度和空间复杂度的概念与计算方法',
'## 为什么要分析复杂度\n算法的好坏不能只看运行时间，需要一种与机器无关的度量方式。\n\n## 时间复杂度\n用 **大O记号** 表示算法运行时间随输入规模的增长趋势。\n\n| 复杂度 | 名称 | 示例 |\n|--------|------|------|\n| O(1) | 常数 | 数组随机访问 |\n| O(log n) | 对数 | 二分查找 |\n| O(n) | 线性 | 遍历数组 |\n| O(n log n) | 线性对数 | 归并排序 |\n| O(n²) | 平方 | 冒泡排序 |\n| O(2^n) | 指数 | 暴力枚举子集 |\n\n## 空间复杂度\n算法执行过程中额外使用的内存空间。\n\n### 练习\n分析以下代码的时间和空间复杂度：\n```\nfor i in range(n):\n    for j in range(i, n):\n        print(i, j)\n```', 1, 45),

(@path3, '链表', '掌握单链表、双链表的实现和常见操作',
'## 单链表\n```java\nclass ListNode {\n    int val;\n    ListNode next;\n    ListNode(int val) { this.val = val; }\n}\n```\n\n## 核心操作\n- **插入**：修改指针指向，O(1)\n- **删除**：跳过目标节点，O(1)\n- **查找**：从头遍历，O(n)\n\n## 经典题目\n1. **反转链表**：三指针法\n2. **合并两个有序链表**：归并思想\n3. **检测环**：快慢指针法\n4. **找中间节点**：快慢指针\n\n## 技巧\n- 使用 **哨兵节点(dummy)** 简化边界处理\n- 快慢指针解决一类链表问题\n\n### 练习\n实现链表的反转和合并操作。', 2, 60),

(@path3, '栈与队列', '理解栈和队列的特性，掌握单调栈和BFS等应用',
'## 栈 (Stack) — LIFO\n```python\nstack = []\nstack.append(1)   # 入栈\nstack.append(2)\ntop = stack.pop()  # 出栈: 2\n```\n\n**应用场景**：\n- 括号匹配\n- 表达式求值\n- 函数调用栈\n- 单调栈：求下一个更大元素\n\n## 队列 (Queue) — FIFO\n```python\nfrom collections import deque\nq = deque()\nq.append(1)      # 入队\nq.append(2)\nfront = q.popleft()  # 出队: 1\n```\n\n**应用场景**：\n- BFS 广度优先搜索\n- 任务调度\n- 滑动窗口最大值（单调队列）\n\n### 练习\n1. 用栈实现括号匹配\n2. 用两个栈实现队列\n3. 最小栈', 3, 60),

(@path3, '树与二叉树', '学习二叉树的遍历、BST和常见树形问题',
'## 二叉树节点\n```java\nclass TreeNode {\n    int val;\n    TreeNode left, right;\n}\n```\n\n## 遍历方式\n- **前序**：根 → 左 → 右\n- **中序**：左 → 根 → 右（BST中为有序）\n- **后序**：左 → 右 → 根\n- **层序**：使用队列 BFS\n\n## 二叉搜索树 (BST)\n- 左子树所有节点 < 根 < 右子树所有节点\n- 查找/插入/删除：O(log n) 平均，O(n) 最坏\n\n## 经典问题\n1. 求树的最大深度\n2. 判断是否平衡二叉树\n3. 最近公共祖先 (LCA)\n4. 二叉树的序列化与反序列化\n\n### 练习\n实现二叉树的三种遍历（递归 + 迭代）。', 4, 90),

(@path3, '排序算法', '掌握常见排序算法的原理、实现和比较',
'## 排序算法对比\n\n| 算法 | 平均 | 最坏 | 空间 | 稳定 |\n|------|------|------|------|------|\n| 冒泡 | O(n²) | O(n²) | O(1) | ✅ |\n| 选择 | O(n²) | O(n²) | O(1) | ❌ |\n| 插入 | O(n²) | O(n²) | O(1) | ✅ |\n| 归并 | O(n log n) | O(n log n) | O(n) | ✅ |\n| 快排 | O(n log n) | O(n²) | O(log n) | ❌ |\n| 堆排 | O(n log n) | O(n log n) | O(1) | ❌ |\n\n## 快速排序\n```python\ndef quicksort(arr, lo, hi):\n    if lo >= hi:\n        return\n    pivot = arr[hi]\n    i = lo\n    for j in range(lo, hi):\n        if arr[j] < pivot:\n            arr[i], arr[j] = arr[j], arr[i]\n            i += 1\n    arr[i], arr[hi] = arr[hi], arr[i]\n    quicksort(arr, lo, i - 1)\n    quicksort(arr, i + 1, hi)\n```\n\n### 练习\n1. 手写归并排序\n2. 求第K大的数', 5, 75),

(@path3, '搜索与回溯', '掌握DFS、BFS和回溯法解决组合问题',
'## 深度优先搜索 (DFS)\n```python\ndef dfs(graph, node, visited):\n    visited.add(node)\n    for neighbor in graph[node]:\n        if neighbor not in visited:\n            dfs(graph, neighbor, visited)\n```\n\n## 广度优先搜索 (BFS)\n```python\ndef bfs(graph, start):\n    queue = deque([start])\n    visited = {start}\n    while queue:\n        node = queue.popleft()\n        for neighbor in graph[node]:\n            if neighbor not in visited:\n                visited.add(neighbor)\n                queue.append(neighbor)\n```\n\n## 回溯法模板\n```python\ndef backtrack(path, choices):\n    if 满足条件:\n        result.append(path[:])\n        return\n    for choice in choices:\n        path.append(choice)\n        backtrack(path, 剩余选择)\n        path.pop()  # 撤销选择\n```\n\n### 练习\n1. 全排列\n2. N皇后\n3. 组合总和', 6, 90),

(@path3, '动态规划', '理解DP思想，掌握常见DP问题的状态定义和转移方程',
'## 动态规划核心思想\n将大问题拆解为子问题，保存子问题的解避免重复计算。\n\n## 解题步骤\n1. **定义状态**：dp[i] 代表什么\n2. **状态转移方程**：dp[i] 如何由之前的状态推导\n3. **初始条件**：dp[0] 或 dp[1] 的值\n4. **计算顺序**：从小到大还是从大到小\n\n## 经典问题\n\n### 爬楼梯\n```python\ndp[i] = dp[i-1] + dp[i-2]\n```\n\n### 0-1背包\n```python\nfor i in range(n):\n    for j in range(W, w[i]-1, -1):\n        dp[j] = max(dp[j], dp[j-w[i]] + v[i])\n```\n\n### 最长公共子序列\n```python\nif s1[i] == s2[j]:\n    dp[i][j] = dp[i-1][j-1] + 1\nelse:\n    dp[i][j] = max(dp[i-1][j], dp[i][j-1])\n```\n\n### 练习\n1. 最长递增子序列\n2. 编辑距离\n3. 硬币找零', 7, 120);

-- ==================== 路线4: Java面向对象编程 ====================
INSERT INTO `learning_paths` (`title`, `description`, `difficulty`, `language`, `estimated_hours`, `sort_order`, `status`) VALUES
('Java面向对象编程', '深入学习Java语言特性和面向对象设计，掌握封装、继承、多态、接口、泛型和常用集合框架，为Spring Boot开发做准备。', '普通', 'Java', 50, 70, 1);

SET @path4 = LAST_INSERT_ID();

INSERT INTO `learning_path_stages` (`path_id`, `title`, `description`, `content`, `stage_order`, `estimated_minutes`) VALUES
(@path4, 'Java基础与开发环境', '安装JDK，理解Java程序的编译运行过程',
'## 安装 JDK\n1. 下载 JDK 17+ (推荐 Amazon Corretto 或 Eclipse Temurin)\n2. 配置 `JAVA_HOME` 环境变量\n3. 验证：`java -version` 和 `javac -version`\n\n## Java程序结构\n```java\npublic class Hello {\n    public static void main(String[] args) {\n        System.out.println("Hello, CodePower!");\n    }\n}\n```\n\n编译运行：\n```\njavac Hello.java\njava Hello\n```\n\n## Java特点\n- **跨平台**：编译为字节码，JVM执行\n- **强类型**：变量必须声明类型\n- **面向对象**：一切皆对象\n- **自动内存管理**：垃圾回收 (GC)', 1, 40),

(@path4, '类与对象', '掌握类的定义、构造方法、访问修饰符和this关键字',
'## 类的定义\n```java\npublic class Student {\n    private String name;\n    private int age;\n    \n    public Student(String name, int age) {\n        this.name = name;\n        this.age = age;\n    }\n    \n    public String getName() { return name; }\n    public void setName(String name) { this.name = name; }\n    \n    @Override\n    public String toString() {\n        return name + " (" + age + "岁)";\n    }\n}\n```\n\n## 访问修饰符\n| 修饰符 | 类内 | 包内 | 子类 | 全局 |\n|--------|------|------|------|------|\n| private | ✅ | ❌ | ❌ | ❌ |\n| default | ✅ | ✅ | ❌ | ❌ |\n| protected | ✅ | ✅ | ✅ | ❌ |\n| public | ✅ | ✅ | ✅ | ✅ |', 2, 60),

(@path4, '继承与多态', '理解继承机制、方法重写、抽象类和多态的应用',
'## 继承\n```java\npublic class Animal {\n    protected String name;\n    public void speak() {\n        System.out.println("...");\n    }\n}\n\npublic class Dog extends Animal {\n    @Override\n    public void speak() {\n        System.out.println("汪汪!");\n    }\n}\n```\n\n## 多态\n```java\nAnimal a = new Dog();  // 向上转型\na.speak();  // 输出 "汪汪!" — 运行时绑定\n```\n\n## 抽象类\n```java\npublic abstract class Shape {\n    public abstract double area();\n    public void describe() {\n        System.out.println("面积: " + area());\n    }\n}\n```\n\n### 练习\n设计一个图形类体系：Shape → Circle, Rectangle, Triangle，实现面积计算。', 3, 75),

(@path4, '接口与泛型', '掌握接口定义、默认方法和泛型的使用',
'## 接口\n```java\npublic interface Comparable<T> {\n    int compareTo(T other);\n}\n\npublic class Student implements Comparable<Student> {\n    @Override\n    public int compareTo(Student other) {\n        return this.score - other.score;\n    }\n}\n```\n\n## 泛型\n```java\npublic class Pair<K, V> {\n    private K key;\n    private V value;\n    \n    public Pair(K key, V value) {\n        this.key = key;\n        this.value = value;\n    }\n}\n\nPair<String, Integer> p = new Pair<>("age", 20);\n```\n\n## 泛型通配符\n- `<? extends T>` — 上界，只读\n- `<? super T>` — 下界，只写', 4, 60),

(@path4, '集合框架', '掌握List、Set、Map等集合接口和常用实现类',
'## 集合体系\n```\nCollection\n├── List (有序, 可重复)\n│   ├── ArrayList\n│   └── LinkedList\n├── Set (无序, 不重复)\n│   ├── HashSet\n│   └── TreeSet\nMap (键值对)\n├── HashMap\n├── TreeMap\n└── LinkedHashMap\n```\n\n## 常用操作\n```java\nList<String> list = new ArrayList<>();\nlist.add("Java");\nlist.add("Python");\nlist.forEach(System.out::println);\n\nMap<String, Integer> map = new HashMap<>();\nmap.put("Alice", 95);\nmap.getOrDefault("Bob", 0);\n\n// Stream API\nlist.stream()\n    .filter(s -> s.length() > 4)\n    .sorted()\n    .collect(Collectors.toList());\n```\n\n### 练习\n1. 用HashMap统计词频\n2. 用TreeMap实现排行榜', 5, 60),

(@path4, '异常处理与IO', '学习Java异常体系、try-with-resources和文件操作',
'## 异常体系\n```\nThrowable\n├── Error (系统级, 不需要捕获)\n└── Exception\n    ├── RuntimeException (运行时, 可选捕获)\n    │   ├── NullPointerException\n    │   ├── IndexOutOfBoundsException\n    │   └── IllegalArgumentException\n    └── IOException (编译时, 必须处理)\n```\n\n## try-with-resources\n```java\ntry (BufferedReader br = new BufferedReader(\n        new FileReader("data.txt"))) {\n    String line;\n    while ((line = br.readLine()) != null) {\n        System.out.println(line);\n    }\n} catch (IOException e) {\n    e.printStackTrace();\n}\n```\n\n## NIO\n```java\nString content = Files.readString(Path.of("data.txt"));\nFiles.writeString(Path.of("out.txt"), "Hello");\n```', 6, 60);

-- ==================== 路线5: 算法竞赛入门 ====================
INSERT INTO `learning_paths` (`title`, `description`, `difficulty`, `language`, `estimated_hours`, `sort_order`, `status`) VALUES
('算法竞赛入门', '面向ACM/ICPC和蓝桥杯等编程竞赛的训练路线，涵盖数论、图论、高级数据结构和常见竞赛技巧，提升算法解题能力。', '困难', 'C++', 80, 60, 1);

SET @path5 = LAST_INSERT_ID();

INSERT INTO `learning_path_stages` (`path_id`, `title`, `description`, `content`, `stage_order`, `estimated_minutes`) VALUES
(@path5, '竞赛环境与输入输出', '配置竞赛开发环境，掌握快速IO和调试技巧',
'## 竞赛环境\n- **推荐IDE**: VS Code / CLion / Dev-C++\n- **编译参数**: `g++ -O2 -std=c++17 solution.cpp -o sol`\n\n## 快速IO\n```cpp\n#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n    \n    int n;\n    cin >> n;\n    // ...\n    return 0;\n}\n```\n\n## 常用模板\n```cpp\ntypedef long long ll;\ntypedef pair<int,int> pii;\nconst int INF = 0x3f3f3f3f;\nconst int MOD = 1e9 + 7;\n```\n\n## 调试技巧\n- 使用文件重定向：`freopen("in.txt", "r", stdin);`\n- 对拍：写暴力解法与优化解法对比\n- 边界测试：n=0, n=1, 最大值', 1, 40),

(@path5, '数论基础', '掌握素数、GCD、快速幂和模运算等数论算法',
'## 素数筛\n```cpp\nvector<bool> sieve(int n) {\n    vector<bool> is_prime(n+1, true);\n    is_prime[0] = is_prime[1] = false;\n    for (int i = 2; i * i <= n; i++)\n        if (is_prime[i])\n            for (int j = i*i; j <= n; j += i)\n                is_prime[j] = false;\n    return is_prime;\n}\n```\n\n## 快速幂\n```cpp\nll power(ll base, ll exp, ll mod) {\n    ll result = 1;\n    base %= mod;\n    while (exp > 0) {\n        if (exp & 1) result = result * base % mod;\n        base = base * base % mod;\n        exp >>= 1;\n    }\n    return result;\n}\n```\n\n## GCD\n```cpp\nint gcd(int a, int b) {\n    return b ? gcd(b, a % b) : a;\n}\n```\n\n### 练习\n1. 质因数分解\n2. 欧拉函数\n3. 逆元计算', 2, 90),

(@path5, '图论算法', '学习最短路径、最小生成树和拓扑排序',
'## 图的表示\n```cpp\n// 邻接表\nvector<vector<pair<int,int>>> adj(n);\nadj[u].push_back({v, w});\n```\n\n## 最短路径\n\n### Dijkstra (非负权)\n```cpp\npriority_queue<pii, vector<pii>, greater<>> pq;\ndist[src] = 0;\npq.push({0, src});\nwhile (!pq.empty()) {\n    auto [d, u] = pq.top(); pq.pop();\n    if (d > dist[u]) continue;\n    for (auto [v, w] : adj[u]) {\n        if (dist[u] + w < dist[v]) {\n            dist[v] = dist[u] + w;\n            pq.push({dist[v], v});\n        }\n    }\n}\n```\n\n## 最小生成树\n- **Kruskal**: 排序边 + 并查集\n- **Prim**: 类似 Dijkstra\n\n## 拓扑排序\n入度为0的节点入队，BFS。\n\n### 练习\n1. 单源最短路\n2. 最小生成树\n3. 判断是否为DAG', 3, 120),

(@path5, '高级数据结构', '掌握并查集、线段树和树状数组',
'## 并查集\n```cpp\nint parent[MAXN], rank_[MAXN];\nvoid init(int n) {\n    for (int i = 0; i < n; i++)\n        parent[i] = i, rank_[i] = 0;\n}\nint find(int x) {\n    return parent[x] == x ? x : parent[x] = find(parent[x]);\n}\nvoid unite(int x, int y) {\n    x = find(x); y = find(y);\n    if (x == y) return;\n    if (rank_[x] < rank_[y]) swap(x, y);\n    parent[y] = x;\n    if (rank_[x] == rank_[y]) rank_[x]++;\n}\n```\n\n## 树状数组 (BIT)\n- 单点修改 + 区间查询：O(log n)\n- 前缀和的高效维护\n\n## 线段树\n- 区间修改 + 区间查询\n- 支持懒标记 (lazy propagation)\n\n### 练习\n1. 求连通分量数\n2. 区间求和\n3. 区间最大值', 4, 120),

(@path5, '动态规划进阶', '学习区间DP、状压DP和数位DP等竞赛常见技巧',
'## 区间DP\n```\ndp[i][j] = 区间 [i,j] 的最优解\n枚举分割点 k: dp[i][j] = min(dp[i][k] + dp[k+1][j] + cost)\n```\n典型：矩阵链乘法、石子合并\n\n## 状压DP\n用二进制表示集合状态：\n```cpp\nfor (int mask = 0; mask < (1<<n); mask++)\n    for (int i = 0; i < n; i++)\n        if (mask & (1<<i))\n            dp[mask] = min(dp[mask], dp[mask^(1<<i)] + cost[i]);\n```\n典型：旅行商问题 (TSP)\n\n## 数位DP\n按位枚举，维护是否贴上界。\n典型：统计区间 [L,R] 中满足条件的数的个数\n\n### 练习\n1. 石子合并\n2. 最短 Hamilton 路径\n3. 不含连续1的二进制数', 5, 120);
