<template>
  <div class="pt-24 pb-16 min-h-screen bg-gray-50">
    <div class="max-w-7xl mx-auto px-4">
      <h1 class="text-4xl font-bold mb-8">个人中心</h1>
      <!-- 未登录显示登录提示 -->
      <div v-if="!isLoggedIn" class="max-w-md mx-auto bg-white rounded-xl p-8 shadow-sm text-center">
        <h2 class="text-2xl font-bold mb-4">需要登录</h2>
        <p class="text-gray-600 mb-6">请先登录您的账户才能访问个人中心</p>
        <router-link 
          to="/login" 
          class="inline-block bg-gradient-to-r from-orange-400 to-orange-600 text-white px-8 py-3 rounded-lg font-medium hover:shadow-lg transition"
        >
          立即登录
        </router-link>
      </div>

      <!-- 已登录显示个人中心内容 -->
      <div v-else class="grid grid-cols-1 lg:grid-cols-4 gap-8">

        <!-- 侧边栏 -->
        <div class="lg:col-span-1">
          <div class="bg-white rounded-xl p-6 shadow-sm">
            <div class="text-center mb-6">
               <div class="w-24 h-24 bg-gradient-to-br from-orange-400 to-orange-600 rounded-full mx-auto mb-4 flex items-center justify-center text-white text-3xl font-bold">
                {{ userInitial }}
              </div>
              <h2 class="text-xl font-bold">{{ userName }}</h2>
            </div>
            <nav class="space-y-2 mb-6">
              <button
                v-for="tab in tabs"
                :key="tab.id"
                @click="activeTab = tab.id"
                :class="[
                  'w-full text-left px-4 py-2 rounded-lg transition',
                   activeTab === tab.id
                     ? 'bg-orange-50 text-orange-500 font-medium'
                     : 'hover:bg-gray-50'
                ]"
              >
                {{ tab.name }}
              </button>
            </nav>
            <button 
              @click="handleLogout"
              class="w-full px-4 py-3 rounded-lg border border-orange-500 text-orange-500 hover:bg-orange-50 transition-colors mt-4"
            >
              退出登录
            </button>
          </div>
        </div>

        <!-- 主内容区 -->
        <div class="lg:col-span-3">
          <!-- 个人信息 -->
          <div v-if="activeTab === 'info'" class="bg-white rounded-xl p-8 shadow-sm">
            <h2 class="text-2xl font-bold mb-6">个人信息</h2>
            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
                  <input
                    v-model="userName"
                    type="text"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">邮箱</label>
                  <input
                    type="email"
                    value="user@example.com"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg"
                  />
                </div>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">手机号</label>
                <input
                  type="tel"
                  value="138****8888"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg"
                />
              </div>
               <button class="bg-gradient-to-r from-orange-400 to-orange-600 text-white px-6 py-2 rounded-lg hover:shadow-lg transition">
                保存修改
              </button>
            </div>
          </div>

          <!-- 订单历史 -->
          <div v-if="activeTab === 'orders'" class="space-y-4">
            <div
              v-for="order in orders"
              :key="order.id"
              class="bg-white rounded-xl p-6 shadow-sm"
            >
              <div class="flex justify-between items-start mb-4">
                <div>
                  <h3 class="font-semibold">订单号: {{ order.id }}</h3>
                  <p class="text-sm text-gray-600">{{ order.date }}</p>
                </div>
                <span
                  :class="[
                    'px-3 py-1 rounded-full text-sm font-medium',
                    order.status === '已完成' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                  ]"
                >
                  {{ order.status }}
                </span>
              </div>
              <div class="space-y-2">
                <div
                  v-for="item in order.items"
                  :key="item.name"
                  class="flex justify-between text-sm"
                >
                  <span>{{ item.name }} × {{ item.quantity }}</span>
                  <span class="font-medium">¥{{ item.price }}</span>
                </div>
              </div>
              <div class="mt-4 pt-4 border-t flex justify-between items-center">
                <span class="font-bold">总计: ¥{{ order.total }}</span>
                 <button class="text-orange-500 hover:underline">查看详情</button>
              </div>
            </div>
          </div>

           <!-- 统计数据 -->
           <div v-if="activeTab === 'stats'" class="space-y-6">
             <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
               <div class="bg-white rounded-xl p-6 shadow-sm text-center">
                  <div class="text-4xl font-bold text-orange-500 mb-2">12</div>
                 <div class="text-gray-600">总订单数</div>
               </div>
               <div class="bg-white rounded-xl p-6 shadow-sm text-center">
                  <div class="text-4xl font-bold text-orange-500 mb-2">¥23,456</div>
                 <div class="text-gray-600">累计消费</div>
               </div>
               <div class="bg-white rounded-xl p-6 shadow-sm text-center">
                  <div class="text-4xl font-bold text-orange-500 mb-2">856</div>
                 <div class="text-gray-600">积分余额</div>
               </div>
             </div>
             
             <!-- 月度消费条形图 -->
             <div class="bg-white rounded-xl p-8 shadow-sm">
               <h3 class="text-xl font-bold mb-6">近6个月消费统计</h3>
               <div class="h-80">
                 <div class="flex items-end h-64 gap-8 px-4">
                   <div 
                     v-for="item in monthlyStats" 
                     :key="item.month"
                     class="flex-1 flex flex-col items-center justify-end h-full"
                   >
                     <div 
                       class="w-full bg-gradient-to-t from-orange-500 to-orange-400 rounded-t-lg transition-all duration-500 hover:opacity-80 cursor-pointer relative group"
                       :style="{ height: `${getBarHeight(item.amount)}%` }"
                     >
                       <div class="absolute -top-8 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                         ¥{{ item.amount.toLocaleString() }}
                       </div>
                     </div>
                     <div class="mt-4 text-sm text-gray-600 font-medium">
                       {{ item.month }}
                     </div>
                   </div>
                 </div>
                 <!-- Y轴刻度 -->
                 <div class="flex justify-between mt-2 px-4 text-xs text-gray-400">
                   <span>0</span>
                   <span>¥1000</span>
                   <span>¥2000</span>
                   <span>¥3000</span>
                   <span>¥4000</span>
                   <span>¥5000</span>
                 </div>
               </div>
             </div>
           </div>
         </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const isLoggedIn = ref(false)
const userName = ref('张三')
const activeTab = ref('info')

onMounted(() => {
  // 检查localStorage中是否有登录信息
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      if (user.isLoggedIn && user.username) {
        isLoggedIn.value = true
        userName.value = user.username === 'test' ? '张三' : user.username
      }
    } catch (e) {
      console.error('Failed to parse user info', e)
    }
  }
  
  if (!isLoggedIn.value) {
    setTimeout(() => {
      router.push('/login')
    }, 1000)
  }
})

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  isLoggedIn.value = false
  router.push('/login')
}

const userInitial = computed(() => userName.value.charAt(0))

const tabs = [
  { id: 'info', name: '个人信息' },
  { id: 'orders', name: '订单历史' },
  { id: 'stats', name: '消费统计' }
]

 const orders = ref([
   {
     id: '202604090001',
     date: '2026-04-05',
     status: '已完成',
     items: [
       { name: '智能手表 Pro', quantity: 1, price: 2999 }
     ],
     total: 2999
   },
   {
     id: '202604090002',
     date: '2026-04-01',
     status: '配送中',
     items: [
       { name: '无线耳机', quantity: 1, price: 1299 },
       { name: '便携充电宝', quantity: 2, price: 598 }
     ],
     total: 1897
   }
 ])

 // 月度消费统计数据
 const monthlyStats = ref([
   { month: '11月', amount: 1850 },
   { month: '12月', amount: 3240 },
   { month: '1月', amount: 2180 },
   { month: '2月', amount: 4850 },
   { month: '3月', amount: 2960 },
   { month: '4月', amount: 4120 },
 ])

 // 计算条形图高度百分比
 const getBarHeight = (amount: number): number => {
   const maxAmount = 5000
   return Math.min((amount / maxAmount) * 100, 100)
 }
</script>
