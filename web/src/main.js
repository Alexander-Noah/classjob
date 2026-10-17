/*
 * src main公共逻辑模块，供相关视图或服务复用。
 */
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
// 顺序是主题覆盖契约：组件库 → 基础布局 → 全局玻璃表面 → 按钮独立状态。
import './style.css'
import './glass-theme.css'
import './liquid-optics.css'
import './button-glass.css'
import App from './App.vue'
import router from './router'

createApp(App).use(router).use(ElementPlus, { locale: zhCn }).mount('#app')
