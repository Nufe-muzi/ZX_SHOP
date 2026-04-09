<template>
  <div class="min-h-screen pt-24 pb-16 bg-gray-50 flex items-center justify-center">
    <div class="w-full max-w-md">
      <div class="bg-white rounded-2xl p-8 shadow-lg">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold mb-2">欢迎回来</h1>
          <p class="text-gray-600">登录您的账户，继续购物</p>
        </div>

        <form @submit.prevent="handleLogin" class="space-y-6">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
            <input
              v-model="loginForm.username"
              type="text"
              placeholder="请输入用户名"
              class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
            <input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              @keyup.enter="handleLogin"
              class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition"
            />
          </div>

          <div class="flex items-center justify-between">
            <label class="flex items-center">
              <input type="checkbox" v-model="rememberMe" class="rounded text-orange-500 focus:ring-orange-500">
              <span class="ml-2 text-sm text-gray-600">记住我</span>
            </label>
            <a href="#" class="text-sm text-orange-500 hover:underline">忘记密码？</a>
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full bg-gradient-to-r from-orange-400 to-orange-600 text-white py-3 rounded-xl font-medium hover:shadow-lg transition-all duration-300 disabled:opacity-70 disabled:cursor-not-allowed"
          >
            {{ loading ? '登录中...' : '登录' }}
          </button>

          <div class="text-center text-sm text-gray-600">
            还没有账号？<a href="#" class="text-orange-500 hover:underline">立即注册</a>
          </div>
        </form>

        <!-- 测试用户提示 -->
        <div class="mt-6 p-4 bg-orange-50 rounded-lg">
          <p class="text-sm text-orange-800">
            <span class="font-medium">测试账号：</span><br>
            用户名：test<br>
            密码：123456
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loginForm = ref({
  username: 'test',
  password: '123456'
})

const rememberMe = ref(true)
const loading = ref(false)

// 模拟后端登录接口
const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true

  try {
    // 模拟POST请求到后端
    const response = await new Promise<{success: boolean; message: string; token?: string}>((resolve) => {
      setTimeout(() => {
        // 测试用户验证
        if (loginForm.value.username === 'test' && loginForm.value.password === '123456') {
          resolve({
            success: true,
            message: '登录成功',
            token: 'test-token-' + Date.now()
          })
        } else {
          resolve({
            success: false,
            message: '用户名或密码错误'
          })
        }
      }, 800)
    })

    if (response.success) {
      // 保存token和用户信息到localStorage
      localStorage.setItem('token', response.token!)
      localStorage.setItem('user', JSON.stringify({
        username: loginForm.value.username,
        isLoggedIn: true
      }))

      ElMessage.success(response.message)
      // 跳转到个人中心
      router.push('/profile')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error('登录失败，请稍后重试')
    console.error('Login error:', error)
  } finally {
    loading.value = false
  }
}
</script>