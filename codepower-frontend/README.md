## CodePower 前端说明

本目录是 CodePower 的 Vue 3 前端工程，主要负责题库列表、做题 IDE、AI 辅导、竞赛、个人中心、社区和管理后台页面。
所有后端请求统一从 `src/api/index.ts` 发出；开发环境由 `vite.config.ts` 把 `/api` 和 `/ws` 代理到本地 Spring Boot，线上环境由 OpenResty/Nginx 做同样的反向代理。

## 本地开发

```bash
npm install
npm run dev
```

默认开发端口由 Vite 分配，页面中的 `/api/**` 请求会代理到 `http://localhost:8080`。
如果本地后端端口不同，需要同步修改 `vite.config.ts` 中的 `server.proxy`。

## 构建部署

```bash
npm run build
```

构建产物在 `dist/` 目录。线上部署时把 `dist/` 发布到 OpenResty/Nginx 站点根目录，并保证：

- `/api/` 反向代理到 Spring Boot 后端；
- `/ws/` 反向代理到后端 WebSocket 服务；
- 前端路由使用 history 模式，刷新子页面时需要回退到 `index.html`；
- 静态资源更新后若浏览器仍缓存旧 chunk，`src/router/index.ts` 会在加载失败时自动刷新当前路由兜底。
