import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import TodoList from '../views/TodoList.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: TodoList },
    { path: '/login', component: Login }
  ]
})

router.beforeEach((to) => {
  if (to.path === '/login') return true
  if (!localStorage.getItem('token')) return '/login'
  return true
})

export default router
