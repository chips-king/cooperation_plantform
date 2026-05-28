import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import './styles/global.css';
import { createApp } from 'vue';

import App from './App.vue';
import { router } from './router';
import { createApplicationStore } from './stores';

// 创建前端应用实例，统一挂载路由、状态管理和组件库。
const app = createApp(App);

app.use(createApplicationStore());
app.use(router);
app.use(ElementPlus);
app.mount('#app');
