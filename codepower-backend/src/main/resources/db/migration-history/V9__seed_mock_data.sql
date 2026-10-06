-- V9: 预置模拟数据 — 用户、提交记录、竞赛

-- ========== 模拟用户 (密码均为 Test123456) ==========
INSERT INTO users (username, email, password, role, status) VALUES
('zhangsan',   'zhangsan@example.com',   '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'NORMAL_USER',  1),
('lisi',       'lisi@example.com',       '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'NORMAL_USER',  1),
('wangwu',     'wangwu@example.com',     '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'SENIOR_USER',  1),
('zhaoliu',    'zhaoliu@example.com',    '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'NORMAL_USER',  1),
('sunqi',      'sunqi@example.com',      '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'NORMAL_USER',  1),
('zhouba',     'zhouba@example.com',     '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'SENIOR_USER',  1),
('wujiu',      'wujiu@example.com',      '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'NORMAL_USER',  1),
('zhengshi',   'zhengshi@example.com',   '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'NORMAL_USER',  1),
('codeMaster', 'codemaster@example.com', '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'SENIOR_USER',  1),
('algoKing',   'algoking@example.com',   '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'NORMAL_USER',  1),
('admin',      'admin@codepower.com',    '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'ADMIN',        1);

-- ========== 用户个人资料 ==========
INSERT INTO user_profiles (user_id, region, bio, avatar_url) VALUES
((SELECT id FROM users WHERE username='zhangsan'),   '北京', '热爱算法的大三学生',            '/avatars/avatar-2.svg'),
((SELECT id FROM users WHERE username='lisi'),       '上海', '全栈开发者，专注后端',           '/avatars/avatar-3.svg'),
((SELECT id FROM users WHERE username='wangwu'),     '深圳', '5年Java开发经验，ACM金牌',       '/avatars/avatar-4.svg'),
((SELECT id FROM users WHERE username='zhaoliu'),    '广州', '前端工程师，正在学习算法',        '/avatars/avatar-5.svg'),
((SELECT id FROM users WHERE username='sunqi'),      '杭州', '研一在读，研究方向NLP',          '/avatars/avatar-6.svg'),
((SELECT id FROM users WHERE username='zhouba'),     '成都', '刷题爱好者，LeetCode 500+',      '/avatars/avatar-7.svg'),
((SELECT id FROM users WHERE username='wujiu'),      '武汉', '大二学生，从C++开始学起',        '/avatars/avatar-8.svg'),
((SELECT id FROM users WHERE username='zhengshi'),   '南京', 'Go语言忠实粉丝',                '/avatars/avatar-1.svg'),
((SELECT id FROM users WHERE username='codeMaster'), '西安', '竞赛选手，打过3届ICPC',          '/avatars/avatar-2.svg'),
((SELECT id FROM users WHERE username='algoKing'),   '重庆', '坚持每日一题',                  '/avatars/avatar-3.svg'),
((SELECT id FROM users WHERE username='admin'),      '北京', '系统管理员',                    '/avatars/avatar-1.svg');

-- ========== 提交记录 ==========
-- 使用变量简化
SET @u_zs  = (SELECT id FROM users WHERE username='zhangsan');
SET @u_ls  = (SELECT id FROM users WHERE username='lisi');
SET @u_ww  = (SELECT id FROM users WHERE username='wangwu');
SET @u_zl  = (SELECT id FROM users WHERE username='zhaoliu');
SET @u_sq  = (SELECT id FROM users WHERE username='sunqi');
SET @u_zb  = (SELECT id FROM users WHERE username='zhouba');
SET @u_wj  = (SELECT id FROM users WHERE username='wujiu');
SET @u_zs2 = (SELECT id FROM users WHERE username='zhengshi');
SET @u_cm  = (SELECT id FROM users WHERE username='codeMaster');
SET @u_ak  = (SELECT id FROM users WHERE username='algoKing');

-- zhangsan 的提交 (刷了15题)
INSERT INTO submissions (problem_id, user_id, code, language, status, score, execution_time, memory_used, created_at) VALUES
(1, @u_zs, 'class Solution { public int[] twoSum(int[] nums, int target) { return new int[]{0,1}; } }', 'java', 'ACCEPTED', 100, 12, 38400, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(2, @u_zs, '#include<iostream>\nusing namespace std;\nint main(){int n;cin>>n;return 0;}', 'cpp', 'ACCEPTED', 100, 4, 3200, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(3, @u_zs, 'def maxSubArray(nums): return max(nums)', 'python', 'WRONG_ANSWER', 25, 32, 14200, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(3, @u_zs, 'def maxSubArray(nums):\n  dp=nums[0]\n  res=dp\n  for i in range(1,len(nums)):\n    dp=max(dp+nums[i],nums[i])\n    res=max(res,dp)\n  return res', 'python', 'ACCEPTED', 100, 45, 14800, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4, @u_zs, 'class Solution { public int climbStairs(int n) { if(n<=2) return n; int a=1,b=2; for(int i=3;i<=n;i++){int c=a+b;a=b;b=c;} return b; } }', 'java', 'ACCEPTED', 100, 8, 37600, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(5, @u_zs, 'def isValid(s): stack=[]; m={"(":")","{":"}","[":"]"}; return True', 'python', 'WRONG_ANSWER', 50, 28, 14000, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(6, @u_zs, 'int search(int* nums, int n, int target){int l=0,r=n-1;while(l<=r){int m=(l+r)/2;if(nums[m]==target)return m;if(nums[m]<target)l=m+1;else r=m-1;}return -1;}', 'c', 'ACCEPTED', 100, 2, 2800, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(9, @u_zs, 'class Solution { public int maxProfit(int[] p) { int min=p[0],max=0; for(int i=1;i<p.length;i++){min=Math.min(min,p[i]);max=Math.max(max,p[i]-min);} return max; } }', 'java', 'ACCEPTED', 100, 10, 38000, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(10, @u_zs, 'def lengthOfLongestSubstring(s): pass', 'python', 'WRONG_ANSWER', 0, 25, 14000, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(10, @u_zs, 'def lengthOfLongestSubstring(s):\n  seen={}\n  l=res=0\n  for r,c in enumerate(s):\n    if c in seen and seen[c]>=l: l=seen[c]+1\n    seen[c]=r\n    res=max(res,r-l+1)\n  return res', 'python', 'ACCEPTED', 100, 35, 15000, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(21, @u_zs, 'def fib(n):\n  a,b=0,1\n  for _ in range(n): a,b=b,a+b\n  return a', 'python', 'ACCEPTED', 100, 22, 14000, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(22, @u_zs, 'class Solution { public boolean isPalindrome(int x) { if(x<0)return false; int rev=0,orig=x; while(x>0){rev=rev*10+x%10;x/=10;} return rev==orig; } }', 'java', 'ACCEPTED', 100, 9, 37800, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(25, @u_zs, 'def reverseString(s): return s[::-1]', 'python', 'ACCEPTED', 100, 20, 14200, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(26, @u_zs, '#include<iostream>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 5, 3400, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(27, @u_zs, 'def convert(n, base):\n  digits="0123456789ABCDEF"\n  res=""\n  while n>0: res=digits[n%base]+res; n//=base\n  return res or "0"', 'python', 'ACCEPTED', 100, 24, 14000, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- lisi 的提交 (刷了10题)
INSERT INTO submissions (problem_id, user_id, code, language, status, score, execution_time, memory_used, created_at) VALUES
(1, @u_ls, 'class Solution { public int[] twoSum(int[] n, int t) { return null; } }', 'java', 'WRONG_ANSWER', 0, 15, 38800, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(1, @u_ls, 'import java.util.*; class Solution { public int[] twoSum(int[] n, int t) { Map<Integer,Integer> m=new HashMap<>(); for(int i=0;i<n.length;i++){if(m.containsKey(t-n[i]))return new int[]{m.get(t-n[i]),i};m.put(n[i],i);} return null; } }', 'java', 'ACCEPTED', 100, 11, 39200, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(4, @u_ls, 'public class Main { public static void main(String[] args) { System.out.println(3); } }', 'java', 'ACCEPTED', 100, 8, 37200, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(5, @u_ls, 'function isValid(s){const st=[];const m={"(":")","{":"}","[":"]"};for(const c of s){if(m[c])st.push(m[c]);else if(st.pop()!==c)return false;}return st.length===0;}', 'javascript', 'ACCEPTED', 100, 55, 42000, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(9, @u_ls, 'const maxProfit=p=>{let min=p[0],max=0;p.forEach(v=>{min=Math.min(min,v);max=Math.max(max,v-min);});return max;}', 'javascript', 'ACCEPTED', 100, 60, 43200, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(21, @u_ls, 'const fib=n=>{let a=0,b=1;for(let i=0;i<n;i++){[a,b]=[b,a+b];}return a;}', 'javascript', 'ACCEPTED', 100, 50, 41000, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(22, @u_ls, 'const isPalindrome=x=>x>=0&&String(x)===String(x).split("").reverse().join("")', 'javascript', 'ACCEPTED', 100, 58, 43800, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(25, @u_ls, 'const rev=s=>s.split("").reverse().join("")', 'javascript', 'ACCEPTED', 100, 48, 41500, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(11, @u_ls, 'function threeSum(nums){}', 'javascript', 'WRONG_ANSWER', 0, 52, 42800, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(12, @u_ls, 'const longestPalindrome=s=>{let res="";for(let i=0;i<s.length;i++){for(let j=i;j<s.length;j++){const sub=s.slice(i,j+1);if(sub===sub.split("").reverse().join("")&&sub.length>res.length)res=sub;}}return res;}', 'javascript', 'TIME_LIMIT_EXCEEDED', 25, 2005, 45000, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- wangwu 的提交 (大佬，刷了20题全AC)
INSERT INTO submissions (problem_id, user_id, code, language, status, score, execution_time, memory_used, created_at) VALUES
(1,  @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){int n,t;cin>>n>>t;vector<int>a(n);for(auto&x:a)cin>>x;unordered_map<int,int>m;for(int i=0;i<n;i++){if(m.count(t-a[i])){cout<<m[t-a[i]]<<" "<<i;return 0;}m[a[i]]=i;}return 0;}', 'cpp', 'ACCEPTED', 100, 3, 3400, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2,  @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){int n;cin>>n;vector<int>a(n);for(auto&x:a)cin>>x;reverse(a.begin(),a.end());for(int x:a)cout<<x<<" ";return 0;}', 'cpp', 'ACCEPTED', 100, 2, 3200, DATE_SUB(NOW(), INTERVAL 19 DAY)),
(3,  @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){int n;cin>>n;vector<int>a(n);for(auto&x:a)cin>>x;int dp=a[0],res=dp;for(int i=1;i<n;i++){dp=max(dp+a[i],a[i]);res=max(res,dp);}cout<<res;return 0;}', 'cpp', 'ACCEPTED', 100, 3, 3200, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(10, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){string s;cin>>s;int n=s.size(),res=0,l=0;unordered_map<char,int>m;for(int r=0;r<n;r++){if(m.count(s[r])&&m[s[r]]>=l)l=m[s[r]]+1;m[s[r]]=r;res=max(res,r-l+1);}cout<<res;return 0;}', 'cpp', 'ACCEPTED', 100, 3, 3600, DATE_SUB(NOW(), INTERVAL 17 DAY)),
(11, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 4, 3400, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(12, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 5, 3600, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(13, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 4, 3200, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(14, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 3, 3400, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(15, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 4, 3600, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(16, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 3, 3200, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(17, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 8, 4200, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(18, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 5, 3800, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(19, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 4, 3400, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(20, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 6, 3600, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(33, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 5, 3400, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(34, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 4, 3200, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(35, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 6, 3800, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(36, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 120, 8400, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(39, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 15, 4200, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(40, @u_ww, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 6, 3600, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- zhaoliu 的提交 (初学者，5题)
INSERT INTO submissions (problem_id, user_id, code, language, status, score, execution_time, memory_used, created_at) VALUES
(21, @u_zl, 'print(sum(range(11)))', 'python', 'WRONG_ANSWER', 0, 25, 14000, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(21, @u_zl, 'def fib(n):\n  if n<=1: return n\n  return fib(n-1)+fib(n-2)', 'python', 'TIME_LIMIT_EXCEEDED', 25, 2010, 28000, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(21, @u_zl, 'def fib(n):\n  a,b=0,1\n  for _ in range(n): a,b=b,a+b\n  return a', 'python', 'ACCEPTED', 100, 24, 14200, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(22, @u_zl, 'n=int(input()); print(str(n)==str(n)[::-1])', 'python', 'WRONG_ANSWER', 50, 22, 14000, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(22, @u_zl, 'n=int(input())\nif n<0: print("false")\nelse: print("true" if str(n)==str(n)[::-1] else "false")', 'python', 'ACCEPTED', 100, 22, 14200, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(25, @u_zl, 's=input(); print(s[::-1])', 'python', 'ACCEPTED', 100, 20, 14000, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1,  @u_zl, 'n,t=map(int,input().split())\na=list(map(int,input().split()))\nfor i in range(n):\n  for j in range(i+1,n):\n    if a[i]+a[j]==t: print(i,j); exit()', 'python', 'ACCEPTED', 100, 280, 14600, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- sunqi 的提交 (8题)
INSERT INTO submissions (problem_id, user_id, code, language, status, score, execution_time, memory_used, created_at) VALUES
(1,  @u_sq, 'def two_sum(nums,target):\n  d={}\n  for i,n in enumerate(nums):\n    if target-n in d: return [d[target-n],i]\n    d[n]=i', 'python', 'ACCEPTED', 100, 30, 15000, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2,  @u_sq, 'n=int(input()); a=list(map(int,input().split())); print(*a[::-1])', 'python', 'ACCEPTED', 100, 22, 14200, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(4,  @u_sq, 'n=int(input()); a,b=1,2\nfor _ in range(n-2): a,b=b,a+b\nprint(b if n>2 else n)', 'python', 'ACCEPTED', 100, 24, 14000, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(6,  @u_sq, 'import bisect\nn,t=map(int,input().split())\na=list(map(int,input().split()))\ni=bisect.bisect_left(a,t)\nprint(i if i<n and a[i]==t else -1)', 'python', 'WRONG_ANSWER', 50, 26, 14400, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(9,  @u_sq, 'n=int(input());a=list(map(int,input().split()));mn=a[0];mx=0\nfor x in a[1:]:mn=min(mn,x);mx=max(mx,x-mn)\nprint(mx)', 'python', 'ACCEPTED', 100, 28, 14600, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(21, @u_sq, 'n=int(input())\na,b=0,1\nfor _ in range(n):a,b=b,a+b\nprint(a)', 'python', 'ACCEPTED', 100, 22, 14000, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(23, @u_sq, 'import math;n=int(input());print(len([i for i in range(2,n) if all(i%j for j in range(2,int(math.sqrt(i))+1))]))', 'python', 'ACCEPTED', 100, 180, 15200, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(24, @u_sq, 'import math;a,b=map(int,input().split());g=math.gcd(a,b);print(g,a*b//g)', 'python', 'ACCEPTED', 100, 22, 14000, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- codeMaster 的提交 (大量，15题)
INSERT INTO submissions (problem_id, user_id, code, language, status, score, execution_time, memory_used, created_at) VALUES
(1,  @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 2, 3200, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(2,  @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 2, 3000, DATE_SUB(NOW(), INTERVAL 17 DAY)),
(3,  @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 2, 3200, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(10, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 3, 3400, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(11, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 3, 3400, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(12, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 4, 3600, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(17, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 8, 4200, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(18, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 5, 3800, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(33, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 4, 3400, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(35, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 5, 3600, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(36, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 150, 8800, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(40, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 5, 3600, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(41, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 8, 4000, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(48, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 6, 3800, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(49, @u_cm, '#include<bits/stdc++.h>\nusing namespace std;int main(){return 0;}', 'cpp', 'ACCEPTED', 100, 4, 3400, DATE_SUB(NOW(), INTERVAL 4 DAY));

-- algoKing 的提交 (6题)
INSERT INTO submissions (problem_id, user_id, code, language, status, score, execution_time, memory_used, created_at) VALUES
(1,  @u_ak, 'def two_sum(nums,t):\n  d={}\n  for i,n in enumerate(nums):\n    if t-n in d: return [d[t-n],i]\n    d[n]=i', 'python', 'ACCEPTED', 100, 32, 15200, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4,  @u_ak, 'def climb(n): a,b=1,2; exec("a,b=b,a+b;"*(n-2)); return b if n>2 else n', 'python', 'ACCEPTED', 100, 26, 14400, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(21, @u_ak, 'n=int(input());a,b=0,1\nfor _ in range(n):a,b=b,a+b\nprint(a)', 'python', 'ACCEPTED', 100, 22, 14000, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(22, @u_ak, 'n=int(input());print("true" if str(n)==str(n)[::-1] and n>=0 else "false")', 'python', 'ACCEPTED', 100, 24, 14200, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3,  @u_ak, 'def maxSubArray(nums):\n  dp=res=nums[0]\n  for x in nums[1:]:\n    dp=max(dp+x,x)\n    res=max(res,dp)\n  return res', 'python', 'ACCEPTED', 100, 28, 14600, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10, @u_ak, 'def f(s):\n  seen={};l=res=0\n  for r,c in enumerate(s):\n    if c in seen and seen[c]>=l: l=seen[c]+1\n    seen[c]=r; res=max(res,r-l+1)\n  return res', 'python', 'ACCEPTED', 100, 30, 14800, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- zhouba 的提交 (12题)
INSERT INTO submissions (problem_id, user_id, code, language, status, score, execution_time, memory_used, created_at) VALUES
(1,  @u_zb, 'import java.util.*;class Main{public static void main(String[]a){Scanner sc=new Scanner(System.in);int n=sc.nextInt(),t=sc.nextInt();int[]arr=new int[n];for(int i=0;i<n;i++)arr[i]=sc.nextInt();Map<Integer,Integer>m=new HashMap<>();for(int i=0;i<n;i++){if(m.containsKey(t-arr[i])){System.out.println(m.get(t-arr[i])+" "+i);return;}m.put(arr[i],i);}}}', 'java', 'ACCEPTED', 100, 14, 39200, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(2,  @u_zb, 'import java.util.*;class Main{public static void main(String[]a){Scanner sc=new Scanner(System.in);int n=sc.nextInt();int[]arr=new int[n];for(int i=0;i<n;i++)arr[i]=sc.nextInt();for(int i=n-1;i>=0;i--)System.out.print(arr[i]+" ");}}', 'java', 'ACCEPTED', 100, 12, 38000, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(3,  @u_zb, 'import java.util.*;class Main{public static void main(String[]a){Scanner sc=new Scanner(System.in);int n=sc.nextInt();int[]arr=new int[n];for(int i=0;i<n;i++)arr[i]=sc.nextInt();int dp=arr[0],res=dp;for(int i=1;i<n;i++){dp=Math.max(dp+arr[i],arr[i]);res=Math.max(res,dp);}System.out.println(res);}}', 'java', 'ACCEPTED', 100, 13, 38400, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4,  @u_zb, 'class Main{public static void main(String[]a){int n=Integer.parseInt(a.length>0?a[0]:"3");if(n<=2){System.out.println(n);return;}int x=1,y=2;for(int i=3;i<=n;i++){int z=x+y;x=y;y=z;}System.out.println(y);}}', 'java', 'WRONG_ANSWER', 25, 10, 37600, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(5,  @u_zb, 'import java.util.*;class Main{public static void main(String[]a){}}', 'java', 'ACCEPTED', 100, 9, 37200, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(6,  @u_zb, 'import java.util.*;class Main{public static void main(String[]a){}}', 'java', 'ACCEPTED', 100, 10, 37400, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(9,  @u_zb, 'import java.util.*;class Main{public static void main(String[]a){}}', 'java', 'ACCEPTED', 100, 11, 37800, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(10, @u_zb, 'import java.util.*;class Main{public static void main(String[]a){}}', 'java', 'ACCEPTED', 100, 12, 38200, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(13, @u_zb, 'import java.util.*;class Main{public static void main(String[]a){}}', 'java', 'ACCEPTED', 100, 14, 39000, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(15, @u_zb, 'import java.util.*;class Main{public static void main(String[]a){}}', 'java', 'ACCEPTED', 100, 13, 38600, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(16, @u_zb, 'import java.util.*;class Main{public static void main(String[]a){}}', 'java', 'ACCEPTED', 100, 11, 38000, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(19, @u_zb, 'import java.util.*;class Main{public static void main(String[]a){}}', 'java', 'ACCEPTED', 100, 12, 38200, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- wujiu 的提交 (3题)
INSERT INTO submissions (problem_id, user_id, code, language, status, score, execution_time, memory_used, created_at) VALUES
(21, @u_wj, '#include<iostream>\nusing namespace std;int main(){int n;cin>>n;int a=0,b=1;for(int i=0;i<n;i++){int c=a+b;a=b;b=c;}cout<<a;return 0;}', 'cpp', 'ACCEPTED', 100, 3, 3200, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(22, @u_wj, '#include<iostream>\nusing namespace std;int main(){int n;cin>>n;if(n<0){cout<<"false";return 0;}int r=0,o=n;while(n>0){r=r*10+n%10;n/=10;}cout<<(r==o?"true":"false");return 0;}', 'cpp', 'ACCEPTED', 100, 2, 3000, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(25, @u_wj, '#include<iostream>\n#include<algorithm>\nusing namespace std;int main(){string s;cin>>s;reverse(s.begin(),s.end());cout<<s;return 0;}', 'cpp', 'ACCEPTED', 100, 2, 3200, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- zhengshi 的提交 (4题，Go语言)
INSERT INTO submissions (problem_id, user_id, code, language, status, score, execution_time, memory_used, created_at) VALUES
(1,  @u_zs2, 'package main\nimport "fmt"\nfunc main(){fmt.Println("0 1")}', 'go', 'WRONG_ANSWER', 25, 8, 6400, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1,  @u_zs2, 'package main\nimport "fmt"\nfunc main(){\n  nums:=[]int{2,7,11,15}\n  target:=9\n  m:=map[int]int{}\n  for i,n:=range nums{\n    if j,ok:=m[target-n];ok{fmt.Println(j,i);return}\n    m[n]=i\n  }\n}', 'go', 'ACCEPTED', 100, 6, 5800, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(21, @u_zs2, 'package main\nimport "fmt"\nfunc main(){\n  n:=10;a,b:=0,1\n  for i:=0;i<n;i++{a,b=b,a+b}\n  fmt.Println(a)\n}', 'go', 'ACCEPTED', 100, 5, 5200, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(25, @u_zs2, 'package main\nimport "fmt"\nfunc main(){\n  s:="hello"\n  r:=[]rune(s)\n  for i,j:=0,len(r)-1;i<j;i,j=i+1,j-1{r[i],r[j]=r[j],r[i]}\n  fmt.Println(string(r))\n}', 'go', 'ACCEPTED', 100, 4, 5000, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ========== 竞赛 ==========
INSERT INTO contests (title, description, type, start_time, end_time, duration_minutes, creator_id, max_participants, status) VALUES
('新手入门赛 #1', '适合刚开始学习编程的同学，包含基础算法题目。题目难度为简单到普通，涵盖基础数据类型、循环、条件判断等知识点。', 'PRACTICE',
  DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 120,
  (SELECT id FROM users WHERE username='admin'), 100, 'ENDED'),

('周赛 第1期 — 算法基础', '本周周赛主题为基础算法，包含排序、查找、字符串处理等题目。请在规定时间内完成尽可能多的题目，按总分排名。', 'RATED',
  DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY) + INTERVAL 150 MINUTE, 150,
  (SELECT id FROM users WHERE username='admin'), 200, 'UPCOMING'),

('期中模拟考试', '模拟期中考试环境，涵盖数据结构与算法课程前半学期的核心内容，包括链表、栈、队列、二叉树等数据结构题目。', 'EXAM',
  DATE_ADD(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY) + INTERVAL 180 MINUTE, 180,
  (SELECT id FROM users WHERE username='admin'), 50, 'UPCOMING');

-- 竞赛题目关联
SET @c1 = (SELECT id FROM contests WHERE title='新手入门赛 #1');
SET @c2 = (SELECT id FROM contests WHERE title='周赛 第1期 — 算法基础');
SET @c3 = (SELECT id FROM contests WHERE title='期中模拟考试');

INSERT INTO contest_problems (contest_id, problem_id, sort_order, score) VALUES
(@c1, 21, 1, 100), (@c1, 22, 2, 100), (@c1, 25, 3, 100), (@c1, 4, 4, 150), (@c1, 1, 5, 200),
(@c2, 1, 1, 100), (@c2, 3, 2, 150), (@c2, 6, 3, 150), (@c2, 10, 4, 200), (@c2, 12, 5, 250),
(@c3, 7, 1, 100), (@c3, 14, 2, 150), (@c3, 37, 3, 150), (@c3, 38, 4, 200), (@c3, 39, 5, 200), (@c3, 40, 6, 200);

-- 竞赛报名
INSERT INTO contest_registrations (contest_id, user_id) VALUES
(@c1, @u_zs), (@c1, @u_ls), (@c1, @u_zl), (@c1, @u_sq), (@c1, @u_wj), (@c1, @u_ak),
(@c2, @u_zs), (@c2, @u_ww), (@c2, @u_zb), (@c2, @u_cm), (@c2, @u_ak), (@c2, @u_sq),
(@c3, @u_zs), (@c3, @u_ls), (@c3, @u_ww), (@c3, @u_zl), (@c3, @u_sq);

-- ========== 用户能力数据 (基于提交记录) ==========
-- 为活跃用户插入能力值
INSERT INTO user_abilities (user_id, tag_id, ability_score, solved_count, attempt_count) VALUES
(@u_zs,  1, 78, 8, 10),  -- 数组
(@u_zs,  2, 65, 3, 4),   -- 字符串
(@u_zs,  3, 72, 4, 4),   -- 动态规划
(@u_zs,  5, 60, 2, 2),   -- 数学
(@u_ww,  1, 95, 12, 12), -- 数组
(@u_ww,  2, 92, 5, 5),   -- 字符串
(@u_ww,  3, 90, 6, 6),   -- 动态规划
(@u_ww,  4, 88, 3, 3),   -- 二叉树
(@u_ww,  5, 85, 2, 2),   -- 数学
(@u_ww,  8, 82, 2, 2),   -- 图论
(@u_cm,  1, 92, 10, 10), -- 数组
(@u_cm,  2, 88, 4, 4),   -- 字符串
(@u_cm,  3, 85, 4, 4),   -- 动态规划
(@u_cm,  8, 80, 3, 3),   -- 图论
(@u_zb,  1, 80, 7, 8),   -- 数组
(@u_zb,  2, 70, 3, 3),   -- 字符串
(@u_zb,  3, 75, 4, 4),   -- 动态规划
(@u_sq,  1, 70, 5, 6),   -- 数组
(@u_sq,  5, 72, 3, 3),   -- 数学
(@u_sq,  3, 55, 2, 2),   -- 动态规划
(@u_ak,  1, 68, 4, 4),   -- 数组
(@u_ak,  3, 62, 2, 2),   -- 动态规划
(@u_ak,  2, 60, 2, 2);   -- 字符串
