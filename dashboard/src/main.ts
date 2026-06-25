import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import './styles/base.css';
import { startDashboardI18n } from './i18n';

createApp(App).use(createPinia()).use(router).mount('#app');
startDashboardI18n();
