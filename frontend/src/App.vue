<template>
  <div>
    <nav v-if="auth.isAuthenticated">
      <div class="nav-brand">Библиотека</div>
      <div class="nav-links">
        <router-link to="/books">Книги</router-link>
        <router-link to="/loans">Выдачи</router-link>
        <router-link to="/fines">Штрафы</router-link>
        <router-link v-if="auth.isLibrarian" to="/readers">Читатели</router-link>
      </div>
      <div class="nav-user">
        <span>{{ auth.login }}</span>
        <button @click="handleLogout">Выйти</button>
      </div>
    </nav>
    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f0f2f5; }
nav {
  background: white;
  padding: 0 2rem;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}
.nav-brand { font-size: 18px; font-weight: 600; color: #333; }
.nav-links { display: flex; gap: 1.5rem; }
.nav-links a { text-decoration: none; color: #666; font-size: 15px; }
.nav-links a.router-link-active { color: #4f6ef7; font-weight: 500; }
.nav-user { display: flex; align-items: center; gap: 1rem; }
.nav-user span { color: #666; font-size: 14px; }
.nav-user button {
  padding: 6px 14px; background: #f0f2f5;
  border: none; border-radius: 6px; cursor: pointer; font-size: 14px;
}
main { padding: 2rem; }
</style>
