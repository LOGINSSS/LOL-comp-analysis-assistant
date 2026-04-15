import { createApp } from 'vue';
import { MotionPlugin } from '@vueuse/motion';

import App from './App.vue';
import './styles/index.css';
import './styles/avatar.css';

createApp(App).use(MotionPlugin).mount('#app');

