/** 应用入口 — 创建 Vue 实例、注册全局插件 */
import './assets/main.css'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 添加全局样式，修正 Element Plus 布局容器默认宽度对页面内容的影响。
const globalStyle = document.createElement('style')
globalStyle.innerHTML = `
  .el-main, .el-header, .el-footer {
    width: 100% !important;
    max-width: 100% !important;
    padding: 0 !important;
  }
  
  .el-main[width], .el-header[width], .el-footer[width] {
    width: 100% !important;
  }
`
document.head.appendChild(globalStyle)

// 创建 Vue 应用。
const app = createApp(App)

// 注册所有 Element Plus 图标，页面里可直接使用图标组件。
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册路由和 UI 组件库。
app.use(router)
app.use(ElementPlus)

// 挂载到 index.html 中的 #app。
app.mount('#app')
