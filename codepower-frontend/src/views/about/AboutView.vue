<!-- 关于页面 — 平台介绍与功能说明 -->
<template>
  <div class="about-container">
    <div class="about-header">
      <h1>关于 Code Power</h1>
      <p class="subtitle">致力于构建高效的编程学习平台</p>
    </div>

    <el-card class="about-card">
      <h2>我们的使命</h2>
      <p>Code Power 致力于帮助程序员和计算机科学爱好者系统地提升他们的编程能力。我们提供结构化的学习路径、高质量的编程题目和实时竞赛环境，帮助用户在编程领域不断进步。</p>
      
      <h2>平台特色</h2>
      <div class="features-grid">
        <div class="feature-item">
          <el-icon color="#409EFF" size="30"><Compass /></el-icon>
          <div class="feature-content">
            <h3>系统化学习</h3>
            <p>根据难度和主题分类的题库和学习路径，循序渐进地提升能力。</p>
          </div>
        </div>
        <div class="feature-item">
          <el-icon color="#409EFF" size="30"><Monitor /></el-icon>
          <div class="feature-content">
            <h3>在线编程环境</h3>
            <p>多语言支持的在线IDE，无需本地环境即可编写和运行代码。</p>
          </div>
        </div>
        <div class="feature-item">
          <el-icon color="#409EFF" size="30"><TrophyBase /></el-icon>
          <div class="feature-content">
            <h3>竞赛演练</h3>
            <p>定期举办编程竞赛，模拟真实面试环境，提供实战机会。</p>
          </div>
        </div>
        <div class="feature-item">
          <el-icon color="#409EFF" size="30"><DataAnalysis /></el-icon>
          <div class="feature-content">
            <h3>学习分析</h3>
            <p>详细的学习统计和进度跟踪，帮助用户了解自己的优势和不足。</p>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="about-card">
      <h2>我们的团队</h2>
      <div class="team-grid">
        <div class="team-member" v-for="(member, index) in teamMembers" :key="index">
          <el-avatar :size="80" :src="member.avatar"></el-avatar>
          <h3>{{ member.name }}</h3>
          <p class="position">{{ member.position }}</p>
          <p class="bio">{{ member.bio }}</p>
        </div>
      </div>
    </el-card>

    <el-card class="about-card">
      <h2>联系我们</h2>
      <p>如果您有任何问题、建议或合作意向，请随时联系我们：</p>
      <div class="contact-grid">
        <div class="contact-item">
          <el-icon><Message /></el-icon>
          <span>邮箱：support@codepower.com</span>
        </div>
        <div class="contact-item">
          <el-icon><Phone /></el-icon>
          <span>电话：400-123-4567</span>
        </div>
        <div class="contact-item">
          <el-icon><Location /></el-icon>
          <span>地址：北京市海淀区中关村科技园</span>
        </div>
        <div class="contact-item">
          <el-icon><ChatDotRound /></el-icon>
          <span>微信公众号：CodePower</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Compass, Monitor, TrophyBase, DataAnalysis, Message, Phone, Location, ChatDotRound } from '@element-plus/icons-vue'

const teamMembers = ref([
  {
    name: '张三',
    position: '创始人 & CEO',
    bio: '前Google高级工程师，拥有10年以上软件开发经验，致力于编程教育创新。',
    avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
  },
  {
    name: '李四',
    position: '技术总监',
    bio: '曾在多家科技公司担任技术领导，精通算法和分布式系统设计。',
    avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
  },
  {
    name: '王五',
    position: '教育内容总监',
    bio: '计算机科学博士，拥有丰富的教学经验，专注于编程学习方法研究。',
    avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
  }
])
</script>

<style scoped>
.about-container {
  width: 100%;
  box-sizing: border-box;
  padding: 0;
}

.about-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 40px 20px;
  background: linear-gradient(135deg, #409EFF 0%, #2c78d4 100%);
  border-radius: 8px;
  color: white;
  position: relative;
  overflow: hidden;
  box-shadow: 0 10px 20px rgba(44, 120, 212, 0.1);
}

.about-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M11 18c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm48 25c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm-43-7c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm63 31c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zM34 90c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm56-76c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zM12 86c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm28-65c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm23-11c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-6 60c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm29 22c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zM32 63c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm57-13c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-9-21c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM60 91c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM35 41c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM12 60c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2z' fill='%23ffffff' fill-opacity='0.05' fill-rule='evenodd'/%3E%3C/svg%3E");
  opacity: 0.7;
}

.about-header h1 {
  font-size: 36px;
  color: white;
  margin-bottom: 12px;
  font-weight: 700;
  position: relative;
  z-index: 1;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.subtitle {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.9);
  position: relative;
  z-index: 1;
  font-weight: 300;
}

.about-card {
  margin-bottom: 40px;
  overflow: visible;
  width: 100%;
  border-radius: 8px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05) !important;
}

.about-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1) !important;
}

.about-card h2 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 25px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 15px;
  font-weight: 600;
  position: relative;
}

.about-card h2:after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 50px;
  height: 3px;
  background: linear-gradient(90deg, #409EFF, #2c78d4);
  border-radius: 3px;
}

.about-card p {
  color: #606266;
  line-height: 1.8;
  margin-bottom: 25px;
  font-size: 15px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 30px;
  width: 100%;
  margin-top: 30px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  background-color: #f9fafc;
  padding: 25px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.feature-item:hover {
  background-color: #f0f5ff;
  transform: translateY(-5px);
}

.feature-item .el-icon {
  background-color: rgba(64, 158, 255, 0.1);
  padding: 15px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.feature-item:hover .el-icon {
  background-color: rgba(64, 158, 255, 0.2);
  transform: scale(1.1);
}

.feature-content {
  flex: 1;
}

.feature-content h3 {
  font-size: 18px;
  color: #303133;
  margin: 0 0 12px 0;
  font-weight: 600;
}

.feature-content p {
  color: #606266;
  line-height: 1.8;
  margin: 0;
  font-size: 15px;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
  width: 100%;
  margin-top: 30px;
}

.team-member {
  text-align: center;
  background-color: #f9fafc;
  padding: 30px 20px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.team-member:hover {
  background-color: #f0f5ff;
  transform: translateY(-5px);
}

.team-member .el-avatar {
  border: 4px solid white;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.team-member:hover .el-avatar {
  transform: scale(1.1);
}

.team-member h3 {
  font-size: 20px;
  color: #303133;
  margin: 20px 0 5px 0;
  font-weight: 600;
}

.position {
  color: #409EFF;
  font-weight: 500;
  margin: 0 0 15px 0;
  font-size: 15px;
}

.bio {
  color: #606266;
  line-height: 1.8;
  font-size: 15px;
}

.contact-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 25px;
  width: 100%;
  margin-top: 30px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 15px;
  background-color: #f9fafc;
  padding: 20px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.contact-item:hover {
  background-color: #f0f5ff;
  transform: translateY(-5px);
}

.contact-item .el-icon {
  color: #409EFF;
  background-color: rgba(64, 158, 255, 0.1);
  padding: 12px;
  border-radius: 50%;
  font-size: 20px;
  transition: all 0.3s ease;
}

.contact-item:hover .el-icon {
  background-color: rgba(64, 158, 255, 0.2);
  transform: scale(1.1);
}

.contact-item span {
  font-weight: 500;
  color: #303133;
}

@media (max-width: 768px) {
  .about-header h1 {
    font-size: 28px;
  }
  
  .subtitle {
    font-size: 16px;
  }
  
  .about-card h2 {
    font-size: 20px;
  }
  
  .features-grid,
  .contact-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .team-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .feature-item,
  .contact-item,
  .team-member {
    padding: 20px 15px;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .team-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .features-grid {
    grid-template-columns: 1fr;
  }
}
</style> 