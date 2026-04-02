# LOLCAA Frontend

这是 LOLCAA 的前端原型，采用 Vue 3 + Vite + Element Plus 构建，整体风格偏暗色游戏工具面板，重点围绕“红色方最后一选”分析台展开。

## 技术栈

- Vue 3
- Vite
- Element Plus
- TypeScript
- Axios
- Sass

## 目录说明

- `src/App.vue`：当前首版主界面
- `src/api/`：接口封装
- `src/data/champions.ts`：前端英雄候选池
- `src/types/lastPick.ts`：请求与响应类型
- `src/styles/`：全局暗色主题样式

## 本地启动

```bash
cd "D:\ASTUDY\TheRoadOfGold\project\LOL comp analysis assistant\frontend"
npm install
npm run dev
```

默认开发端口为 `5173`。

## 后端联调说明

当前前端默认请求地址为：

- `POST /api/api/last-pick/analyze`

这是为了匹配当前后端 `server.servlet.context-path=/api` 与控制器映射的实际组合。如果后端后续将接口整理为更干净的路径，只需要修改：

- `VITE_API_BASE_URL`

例如：

```bash
VITE_API_BASE_URL=/api
```

## 首版页面内容

- 左侧：ban / ally / enemy / role 输入区
- 中间：推荐结果与最终评分
- 下方：对线、协同、阵容、威胁的分项解释
- 保留原始 JSON 详情，方便后续接大模型解释层

## 视觉约束

- 使用暗色主题
- 不使用 emoji
- 以英雄联盟工具面板风格为主，避免过度装饰和 AI 感

