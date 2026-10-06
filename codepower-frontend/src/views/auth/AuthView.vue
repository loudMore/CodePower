<!-- 认证页面布局 — 登录/注册/忘记密码的外层容器 -->
<template>
  <div class="auth-container" @mousemove="updateMousePosition">
    <!-- 背景浮动代码元素 -->
    <div class="background-code-elements">
      <div
        class="bg-code-element"
        v-for="(code, index) in visibleBackgroundElements"
        :key="`bg-${index}`"
        :style="{ 
          left: code.left + '%', 
          top: code.top + '%', 
          color: code.color,
          opacity: code.opacity,
          fontSize: code.size + 'px',
          transform: `rotate(${code.rotation}deg)`,
          animationDuration: code.duration + 's'
        }"
      >
        {{ code.text }}
      </div>
    </div>

    <!-- 主要代码窗口元素 -->
    <div class="floating-elements">
      <div
        class="code-element"
        v-for="(code, index) in visibleCodeElements"
        :key="index"
        :style="getCodeElementStyle(code, index)"
        :class="{ 'pulse-animation': index % 2 === 0, 'scale-animation': index % 2 === 1 }"
      >
        <div class="code-header" :style="{ backgroundColor: code.headerColor }">
          <span class="lang-label">{{ code.lang }}</span>
          <div class="dots"><span></span><span></span><span></span></div>
        </div>
        <pre><code><span class="typing-text" :style="{ color: code.color }">{{ code.displayedText }}</span></code></pre>
      </div>
    </div>

    <!-- 路由视图用于承载登录、注册、找回密码卡片 -->
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue';

// 动画逻辑（与 LoginView.vue 中完全相同）
const mouseX = ref(0);
const mouseY = ref(0);
const visibleBackgroundElements = reactive([]);
const visibleCodeElements = reactive([]);
const recentlyRemovedPositions = [];
const MAX_POSITION_HISTORY = 5;
const MAX_WINDOWS = 4;
const MAX_BG_ELEMENTS = 40;
let codeTypingTimers = {};
let bgElementTimer = null;
let codeWindowTimer = null;

const backgroundCodeElements = [
  { text: 'Quick Sort', left: 5, top: 8, color: '#61afef', size: 18, rotation: -5, duration: 15, id: 'bg-1' },
  { text: 'Merge Sort', left: 80, top: 12, color: '#e06c75', size: 19, rotation: 3, duration: 18, id: 'bg-2' },
  { text: 'Binary Search', left: 15, top: 30, color: '#98c379', size: 17, rotation: 2, duration: 16, id: 'bg-3' },
  { text: 'Depth-First Search', left: 85, top: 40, color: '#61afef', size: 16, rotation: -2, duration: 17, id: 'bg-4' },
  { text: 'Dynamic Programming', left: 25, top: 60, color: '#c678dd', size: 17, rotation: 4, duration: 15, id: 'bg-5' },
  { text: 'Greedy Algorithm', left: 70, top: 75, color: '#e5c07b', size: 18, rotation: -3, duration: 14, id: 'bg-6' },
  { text: 'LinkedList<T>', left: 12, top: 80, color: '#56b6c2', size: 16, rotation: 2, duration: 16, id: 'bg-7' },
  { text: 'Tree', left: 60, top: 20, color: '#98c379', size: 20, rotation: -1, duration: 18, id: 'bg-8' },
  { text: 'HashMap<K,V>', left: 35, top: 90, color: '#56b6c2', size: 17, rotation: 3, duration: 15, id: 'bg-9' },
  { text: 'Stack<T>', left: 45, top: 5, color: '#e06c75', size: 18, rotation: -4, duration: 17, id: 'bg-10' },
  { text: 'Queue<T>', left: 90, top: 55, color: '#56b6c2', size: 16, rotation: 2, duration: 16, id: 'bg-11' },
  { text: 'Graph', left: 20, top: 45, color: '#c678dd', size: 19, rotation: -2, duration: 18, id: 'bg-12' },
  { text: 'Hello World!', left: 55, top: 25, color: '#61afef', size: 20, rotation: 0, duration: 14, id: 'bg-13' },
  { text: '你好，编程世界！', left: 30, top: 70, color: '#e06c75', size: 19, rotation: 2, duration: 16, id: 'bg-14' },
  { text: 'Code Power!', left: 75, top: 85, color: '#98c379', size: 22, rotation: -1, duration: 15, id: 'bg-15' },
  { text: '学习编程，改变世界', left: 15, top: 15, color: '#c678dd', size: 18, rotation: 3, duration: 17, id: 'bg-16' },
  { text: 'for (int i=0;i<n;i++)', left: 67, top: 38, color: '#61afef', size: 16, rotation: -3, duration: 16, id: 'bg-17' },
  { text: '#include <algorithm>', left: 58, top: 78, color: '#e06c75', size: 16, rotation: -2, duration: 17, id: 'bg-19' },
  { text: 'O(n log n)', left: 38, top: 72, color: '#56b6c2', size: 18, rotation: -2, duration: 16, id: 'bg-23' },
  { text: 'O(1)', left: 88, top: 5, color: '#98c379', size: 20, rotation: 0, duration: 14, id: 'bg-24' },
  { text: 'Dijkstra', left: 10, top: 50, color: '#e5c07b', size: 19, rotation: 3, duration: 20, id: 'bg-25' },
  { text: 'A* Search', left: 80, top: 60, color: '#61afef', size: 18, rotation: -4, duration: 18, id: 'bg-26' },
  { text: 'BFS', left: 50, top: 80, color: '#98c379', size: 22, rotation: 2, duration: 15, id: 'bg-27' },
  { text: 'KMP Algorithm', left: 5, top: 20, color: '#c678dd', size: 17, rotation: -3, duration: 22, id: 'bg-28' },
  { text: 'Trie', left: 90, top: 30, color: '#56b6c2', size: 20, rotation: 1, duration: 16, id: 'bg-29' },
  { text: 'Heap', left: 40, top: 40, color: '#e06c75', size: 21, rotation: -2, duration: 17, id: 'bg-30' },
  { text: 'B-Tree', left: 20, top: 90, color: '#61afef', size: 18, rotation: 4, duration: 19, id: 'bg-31' },
  { text: 'Recursion', left: 75, top: 5, color: '#98c379', size: 19, rotation: -1, duration: 21, id: 'bg-32' },
  { text: 'Lambda →', left: 30, top: 10, color: '#e5c07b', size: 20, rotation: 2, duration: 14, id: 'bg-33' },
  { text: 'async/await', left: 65, top: 90, color: '#c678dd', size: 18, rotation: -3, duration: 18, id: 'bg-34' },
  { text: 'Polymorphism', left: 5, top: 70, color: '#56b6c2', size: 17, rotation: 3, duration: 20, id: 'bg-35' },
  { text: 'Vue.js', left: 85, top: 75, color: '#42b883', size: 20, rotation: -2, duration: 16, id: 'bg-36' },
  { text: 'React', left: 50, top: 50, color: '#61dafb', size: 21, rotation: 1, duration: 19, id: 'bg-37' },
  { text: 'Keep Coding', left: 20, top: 25, color: '#e06c75', size: 22, rotation: -1, duration: 15, id: 'bg-38' },
  { text: 'Bug Hunter', left: 80, top: 45, color: '#e5c07b', size: 19, rotation: 2, duration: 18, id: 'bg-39' },
  { text: 'Solve & Conquer', left: 50, top: 95, color: '#61afef', size: 18, rotation: -3, duration: 20, id: 'bg-40' },
];

const codeWindowsPool = [
  { text: '// 快速排序算法\nint partition(int arr[], int low, int high) {\n  int pivot = arr[high];\n  int i = (low - 1);\n  for (int j = low; j <= high - 1; j++) {\n    if (arr[j] < pivot) {\n      i++;\n      swap(&arr[i], &arr[j]);\n    }\n  }\n  swap(&arr[i + 1], &arr[high]);\n  return (i + 1);\n}', displayedText: '', color: '#61afef', headerColor: '#193549', lang: 'C++', z: 1, id: 'win-1' },
  { text: '# 斐波那契数列\ndef fibonacci(n):\n    a, b = 0, 1\n    for _ in range(n):\n        a, b = b, a + b\n    return a\n\n# 打印前10个斐波那契数\nfor i in range(10):\n    print(fibonacci(i))', displayedText: '', color: '#4584b6', headerColor: '#203a51', lang: 'Python', z: 2, id: 'win-3' },
  { text: '// Code Power 在线编程平台\npublic class CodePower {\n    public static void main(String[] args) {\n        System.out.println("Code Power - 助你成为更好的程序员");\n    }\n}', displayedText: '', color: '#f89820', headerColor: '#5d3a0c', lang: 'Java', z: 2, id: 'win-4' },
  { text: '// 冒泡排序\nvoid bubbleSort(int arr[], int n) {\n    for (int i = 0; i < n-1; i++)\n        for (int j = 0; j < n-i-1; j++)\n            if (arr[j] > arr[j+1])\n                swap(arr[j], arr[j+1]);\n}', displayedText: '', color: '#659bd3', headerColor: '#193549', lang: 'C++', z: 1, id: 'win-5' },
];

const updateMousePosition = (event) => {
  mouseX.value = event.clientX;
  mouseY.value = event.clientY;
};

const getCodeElementStyle = (code, index) => {
  const windowCenterX = window.innerWidth / 2;
  const windowCenterY = window.innerHeight / 2;
  const dx = (mouseX.value - windowCenterX) / windowCenterX;
  const dy = (mouseY.value - windowCenterY) / windowCenterY;
  const maxMovement = 10;
  const movementX = dx * maxMovement * (index % 2 === 0 ? 1 : -1);
  const movementY = dy * maxMovement * (index % 3 === 0 ? 1 : -1);
  return {
    left: `calc(${code.left}% + ${movementX}px)`,
    top: `calc(${code.top}% + ${movementY}px)`,
    zIndex: code.z,
    transform: `perspective(1000px) rotateX(${dy * 3}deg) rotateY(${-dx * 3}deg)`,
    transition: 'all 0.5s cubic-bezier(0.23, 1, 0.32, 1)',
    opacity: code.opacity || 1
  };
};

const getRandomPosition = () => {
  const positionZones = [
    { left: 5, top: 10 }, { left: 18, top: 15 }, { left: 8, top: 25 },
    { left: 35, top: 8 }, { left: 50, top: 12 }, { left: 65, top: 8 },
    { left: 82, top: 15 }, { left: 93, top: 10 }, { left: 85, top: 25 },
    { left: 5, top: 45 }, { left: 15, top: 55 },
    { left: 85, top: 45 }, { left: 95, top: 55 },
    { left: 8, top: 75 }, { left: 20, top: 80 }, { left: 5, top: 88 },
    { left: 35, top: 85 }, { left: 50, top: 80 }, { left: 65, top: 85 },
    { left: 83, top: 80 }, { left: 93, top: 75 }, { left: 85, top: 88 },
  ];
  
  const exclusionRadius = 35; 
  const usedZones = visibleCodeElements.map(el => positionZones.findIndex(zone => Math.abs(zone.left - el.left) < exclusionRadius && Math.abs(zone.top - el.top) < exclusionRadius)).filter(idx => idx !== -1);
  
  const recentlyUsedZones = recentlyRemovedPositions.slice(-3).map(recentPos => positionZones.findIndex(zone => Math.abs(zone.left - recentPos.left) < exclusionRadius && Math.abs(zone.top - recentPos.top) < exclusionRadius)).filter(idx => idx !== -1);
  
  const availableZones = positionZones.filter((_, idx) => !usedZones.includes(idx) && !recentlyUsedZones.includes(idx));
  
  let basePos;
  if (availableZones.length > 0) {
    basePos = availableZones[Math.floor(Math.random() * availableZones.length)];
  } else {
    const lessRecentlyUsedZones = positionZones.filter((_, idx) => !usedZones.includes(idx));
    if(lessRecentlyUsedZones.length > 0){
      basePos = lessRecentlyUsedZones[Math.floor(Math.random() * lessRecentlyUsedZones.length)];
    } else {
      basePos = positionZones[Math.floor(Math.random() * positionZones.length)];
    }
  }

  return {
    left: basePos.left + Math.random() * 4 - 2,
    top: basePos.top + Math.random() * 4 - 2,
  };
};

const typeCodeText = (codeElement, index) => {
  const fullText = codeElement.text;
  let currentPos = 0;
  if (codeTypingTimers[codeElement.id]) clearInterval(codeTypingTimers[codeElement.id]);
  codeElement.displayedText = '';
  codeTypingTimers[codeElement.id] = setInterval(() => {
    if (currentPos < fullText.length) {
      const char = fullText.charAt(currentPos);
      codeElement.displayedText += char;
      currentPos++;
      if (['\n', ' ', ';', '{', '}', '(', ')'].includes(char) && currentPos < fullText.length) {
        codeElement.displayedText += fullText.charAt(currentPos);
        currentPos++;
      }
    } else {
      clearInterval(codeTypingTimers[codeElement.id]);
      setTimeout(() => {
        if (visibleCodeElements.includes(codeElement)) typeCodeText(codeElement, index);
      }, 10000);
    }
  }, 30);
};

const fadeInElement = (element) => {
  let opacity = 0;
  const fadeInterval = setInterval(() => {
    opacity += 0.05;
    if (opacity >= 0.8) {
      opacity = 0.8;
      clearInterval(fadeInterval);
      setTimeout(() => {
        if (visibleBackgroundElements.includes(element)) fadeOutAndRemoveElement(element, 'background');
      }, 3000 + Math.random() * 2000);
    }
    element.opacity = opacity;
  }, 50);
};

const fadeOutAndRemoveElement = (element, type) => {
  let opacity = element.opacity || 1;
  const fadeInterval = setInterval(() => {
    opacity -= 0.04;
    if (opacity <= 0) {
      opacity = 0;
      clearInterval(fadeInterval);
      const list = type === 'background' ? visibleBackgroundElements : visibleCodeElements;
      const index = list.indexOf(element);
      if (index !== -1) list.splice(index, 1);
      if (type === 'window' && codeTypingTimers[element.id]) {
        clearInterval(codeTypingTimers[element.id]);
        delete codeTypingTimers[element.id];
      }
    }
    element.opacity = opacity;
  }, 40);
};

const addBackgroundElement = () => {
  if (visibleBackgroundElements.length >= MAX_BG_ELEMENTS) {
    fadeOutAndRemoveElement(visibleBackgroundElements[0], 'background');
  }
  const availableElements = backgroundCodeElements.filter(el => !visibleBackgroundElements.some(vis => vis.id === el.id));
  if (availableElements.length > 0) {
    const element = { ...availableElements[Math.floor(Math.random() * availableElements.length)] };
    element.left += (Math.random() * 6 - 3);
    element.top += (Math.random() * 6 - 3);
    element.opacity = 0;
    visibleBackgroundElements.push(element);
    setTimeout(() => fadeInElement(element), 50);
  }
};

const addNewCodeWindow = () => {
  const availableWindows = codeWindowsPool.filter(win => !visibleCodeElements.some(vis => vis.id === win.id));
  if (availableWindows.length > 0) {
    const newWindow = { ...availableWindows[Math.floor(Math.random() * availableWindows.length)] };
    const pos = getRandomPosition();
    newWindow.left = pos.left;
    newWindow.top = pos.top;
    newWindow.displayedText = '';
    newWindow.opacity = 0;
    visibleCodeElements.push(newWindow);
    let opacity = 0;
    const fadeInInterval = setInterval(() => {
      opacity += 0.05;
      if (opacity >= 1) {
        opacity = 1;
        clearInterval(fadeInInterval);
        setTimeout(() => typeCodeText(newWindow, visibleCodeElements.length - 1), 300);
      }
      newWindow.opacity = opacity;
    }, 50);
  }
};

const addCodeWindow = () => {
  if (visibleCodeElements.length >= MAX_WINDOWS) {
    const oldestWindow = visibleCodeElements[0];
    recentlyRemovedPositions.push({ left: oldestWindow.left, top: oldestWindow.top });
    if (recentlyRemovedPositions.length > MAX_POSITION_HISTORY) recentlyRemovedPositions.shift();
    fadeOutAndRemoveElement(oldestWindow, 'window');
    setTimeout(addNewCodeWindow, 800);
  } else {
    addNewCodeWindow();
  }
};

const startAnimations = () => {
  for (let i = 0; i < 15; i++) setTimeout(() => addBackgroundElement(), i * 150);
  for (let i = 0; i < 3; i++) setTimeout(() => addNewCodeWindow(), 1000 + i * 1200);
  bgElementTimer = setInterval(addBackgroundElement, 800);
  codeWindowTimer = setInterval(addCodeWindow, 7000);
};

onMounted(() => {
  window.addEventListener('mousemove', updateMousePosition);
  startAnimations();
});

onUnmounted(() => {
  window.removeEventListener('mousemove', updateMousePosition);
  clearInterval(bgElementTimer);
  clearInterval(codeWindowTimer);
  Object.values(codeTypingTimers).forEach(timer => clearInterval(timer as number));
});
</script>

<style scoped>
/* 动画和背景样式 */
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 120px);
  background-color: #f4f6f8;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%239C92AC' fill-opacity='0.08'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.background-code-elements, .floating-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.floating-elements {
  z-index: 2;
}

.bg-code-element {
  position: absolute;
  font-family: 'Fira Code', 'Consolas', monospace;
  white-space: nowrap;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
  animation: bgFloat 20s infinite ease-in-out;
  transition: opacity 0.8s ease-in-out;
}

@keyframes bgFloat {
  0%, 100% { transform: translateY(0) rotate(var(--rotation, 0deg)); }
  25% { transform: translateY(-15px) rotate(calc(var(--rotation, 0deg) + 2deg)); }
  50% { transform: translateY(10px) rotate(calc(var(--rotation, 0deg) - 1deg)); }
  75% { transform: translateY(-5px) rotate(calc(var(--rotation, 0deg) + 1deg)); }
}

.code-element {
  position: absolute;
  width: 320px;
  font-family: 'Fira Code', 'Consolas', 'Courier New', monospace;
  background-color: rgba(30, 30, 30, 0.85);
  border-radius: 8px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  animation: gentleFloat 12s infinite ease-in-out;
  margin: 0;
  transition: all 0.8s cubic-bezier(0.23, 1, 0.32, 1);
  will-change: opacity, transform;
  z-index: 2;
}

.code-header {
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.lang-label {
  font-size: 12px;
  font-weight: bold;
  color: #f0f0f0;
}

.dots {
  display: flex;
  gap: 4px;
}

.dots span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #ff5f56;
}

.dots span:nth-child(2) { background-color: #ffbd2e; }
.dots span:nth-child(3) { background-color: #27c93f; }

.code-element pre {
  margin: 0;
  padding: 12px;
  background: transparent;
  white-space: pre-wrap;
}

.code-element code {
  font-family: 'Fira Code', 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.typing-text {
  display: inline-block;
  white-space: pre-wrap;
  border-right: 2px solid;
  animation: blinkCursor 0.8s step-end infinite;
  font-size: 12px;
  line-height: 1.4;
}

@keyframes blinkCursor {
  from, to { border-color: transparent; }
  50% { border-color: currentColor; }
}

@keyframes gentleFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

/* 路由切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.4s cubic-bezier(0.55, 0, 0.1, 1);
}

.fade-enter-from {
  opacity: 0;
  transform: scale(0.95) translateY(20px);
}

.fade-leave-to {
  opacity: 0;
  transform: scale(1.05) translateY(-20px);
}
</style> 