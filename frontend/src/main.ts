import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';

import './css/variables.css';
import 'remixicon/fonts/remixicon.css';
import 'leaflet/dist/leaflet.css';

const app = createApp(App);
app.use(createPinia());
app.mount('#app');
