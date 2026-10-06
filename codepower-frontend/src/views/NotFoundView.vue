<!-- 404 页面 -->
<template>
  <div class="not-found-container">
    <div class="error-content">
      <div class="error-code">404</div>
      <div class="error-title">页面不存在</div>
      <div class="error-desc">很抱歉，您访问的页面不存在或已被移除</div>
      <div class="code-block">
        <div class="code-lines">
          <div class="code-line">
            <span class="code-number">1</span>
            <span class="code-content">
              <span class="keyword">function</span> <span class="function">findPage</span>(<span class="param">path</span>) {
            </span>
          </div>
          <div class="code-line">
            <span class="code-number">2</span>
            <span class="code-content indent">
              <span class="keyword">try</span> {
            </span>
          </div>
          <div class="code-line">
            <span class="code-number">3</span>
            <span class="code-content indent-2">
              <span class="keyword">const</span> <span class="variable">page</span> = <span class="function">searchInRoutes</span>(<span class="param">path</span>);
            </span>
          </div>
          <div class="code-line">
            <span class="code-number">4</span>
            <span class="code-content indent-2">
              <span class="keyword">return</span> <span class="variable">page</span>;
            </span>
          </div>
          <div class="code-line">
            <span class="code-number">5</span>
            <span class="code-content indent">
              } <span class="keyword">catch</span> (<span class="variable">error</span>) {
            </span>
          </div>
          <div class="code-line highlight">
            <span class="code-number">6</span>
            <span class="code-content indent-2">
              <span class="keyword">throw new</span> <span class="error">Error</span>(<span class="string">"404: Page not found"</span>);
            </span>
          </div>
          <div class="code-line">
            <span class="code-number">7</span>
            <span class="code-content indent">
              }
            </span>
          </div>
          <div class="code-line">
            <span class="code-number">8</span>
            <span class="code-content">
              }
            </span>
          </div>
        </div>
      </div>
      <div class="action-buttons">
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="goBack">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.not-found-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  padding: 20px;
}

.error-content {
  max-width: 800px;
  text-align: center;
}

.error-code {
  font-size: 120px;
  font-weight: bold;
  color: #409EFF;
  line-height: 1;
  margin-bottom: 10px;
  text-shadow: 5px 5px 0 rgba(0, 0, 0, 0.1);
}

.error-title {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 15px;
}

.error-desc {
  font-size: 18px;
  color: #606266;
  margin-bottom: 30px;
}

.code-block {
  background-color: #282c34;
  border-radius: 10px;
  padding: 20px;
  margin: 0 auto 30px;
  max-width: 600px;
  text-align: left;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.code-lines {
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.code-line {
  margin-bottom: 5px;
  color: #abb2bf;
  display: flex;
}

.code-line.highlight {
  background-color: rgba(255, 0, 0, 0.2);
  border-radius: 4px;
}

.code-number {
  color: #636d83;
  padding-right: 15px;
  user-select: none;
  width: 25px;
  text-align: right;
}

.code-content {
  flex: 1;
}

.indent {
  padding-left: 20px;
}

.indent-2 {
  padding-left: 40px;
}

.keyword {
  color: #c678dd;
}

.function {
  color: #61afef;
}

.variable {
  color: #e06c75;
}

.string {
  color: #98c379;
}

.param {
  color: #d19a66;
}

.error {
  color: #ff5370;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
}

@media (max-width: 768px) {
  .error-code {
    font-size: 80px;
  }
  
  .error-title {
    font-size: 24px;
  }
  
  .error-desc {
    font-size: 16px;
  }
  
  .code-block {
    font-size: 12px;
  }
}
</style> 