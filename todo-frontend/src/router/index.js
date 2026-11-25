import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import TodoList from '../views/TodoList.vue'
import CalendarView from '../views/CalendarView.vue'
import StatsView from '../views/StatsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: TodoList },
    { path: '/calendar', component: CalendarView },
    { path: '/stats', component: StatsView },
    { path: '/login', component: Login }
  ]
})

router.beforeEach((to) => {
  if (to.path === '/login') return true
  if (!localStorage.getItem('token')) return '/login'
  return true
})

export default router
