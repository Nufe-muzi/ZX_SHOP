<template>
  <!-- 只有首页显示顶部搜索框 -->
  <nav v-if="$route.path === '/'" class="fixed top-0 left-0 right-0 z-40 bg-white border-b border-gray-200">
    <div class="max-w-7xl mx-auto px-4">
      <!-- 顶部搜索框 -->
      <div class="py-1">
        <div class="flex items-center border-2 border-orange-500 rounded-full overflow-hidden">
          <div class="px-4 text-gray-500 bg-white">
            <span>宝贝 ▾</span>
          </div>
          <div class="h-8 w-px bg-gray-300 mx-2"></div>
          <input
            v-model="searchQuery"
            @keyup.enter="handleSearch"
            placeholder="搜索商品..."
            class="flex-1 px-4 py-2 focus:outline-none"
          />
          <button
            @click="handleSearch"
            class="bg-gradient-to-r from-orange-400 to-orange-600 text-white px-8 py-2 font-semibold hover:shadow-lg transition"
          >
            搜索
          </button>
        </div>
      </div>
    </div>
  </nav>

  <!-- 右侧纵向导航 - 始终显示 -->
  <div :class="[$route.path === '/' ? 'fixed top-14' : 'fixed top-0', 'right-4 bottom-0 z-30 flex items-center justify-center']">
    <div class="w-12 bg-white border border-gray-200 rounded-3xl overflow-hidden shadow-sm">
      <div class="flex flex-col">
        <RouterLink
          v-for="link in navLinks"
          :key="link.path"
          :to="link.path"
          class="flex flex-col items-center gap-0.5 text-xs text-gray-700 hover:text-orange-500 px-1 py-3 transition-colors relative"
          active-class="text-orange-500 bg-orange-50/50"
        >
          <svg v-if="link.icon" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path :d="link.icon" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" />
          </svg>
          <span>{{ link.name }}</span>
          <span v-if="link.name === '购物车' && cartStore.totalItems > 0" 
                class="absolute top-2 right-1 bg-orange-500 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center">
            {{ cartStore.totalItems }}
          </span>
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'

const cartStore = useCartStore()
const router = useRouter()
const route = useRoute()
const searchQuery = ref('')

// 检查登录状态
const isLoggedIn = computed(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      return user.isLoggedIn
    } catch (e) {
      return false
    }
  }
  return false
})

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    // 这里可以跳转到搜索结果页面，当前简化处理，返回首页并提示
    router.push('/')
    alert(`搜索: ${searchQuery.value}`)
  }
}

 const navLinks = computed(() => {
   const links = [
     { name: '首页', path: '/', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
     { name: isLoggedIn.value ? '我的' : '登录', path: isLoggedIn.value ? '/profile' : '/login', icon: isLoggedIn.value ? 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z' : 'M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1' },
     { name: '购物车', path: '/cart', icon: 'M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 004 0z' }
   ]
   return links
 })
</script>
