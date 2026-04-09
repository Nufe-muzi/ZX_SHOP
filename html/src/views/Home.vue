<template>
  <div class="pt-5">
    <!-- Hero Section -->
    <section class="relative h-[600px] text-white">
      <div class="absolute inset-0 bg-cover bg-center" 
           style="background-image: url('/startbj.jpg')">
        <div class="absolute inset-0 bg-black/40"></div>
      </div>
      <div class="relative max-w-7xl mx-auto px-4 h-full flex items-center">
        <div class="max-w-2xl">
          <h1 class="text-6xl font-bold mb-6">AI 智能商城</h1>
          <p class="text-xl mb-8 text-white/90">
            体验由人工智能驱动的个性化购物推荐，发现最适合你的产品，额外现金回馈等你来拿
          </p>
          <button @click="scrollToHotProducts" class="bg-white text-gray-900 px-8 py-3 rounded-full font-semibold hover:shadow-xl transition">
            开始购物
          </button>
        </div>
      </div>
    </section>

    <!-- 热门商品 -->
    <section id="hot-products" class="max-w-7xl mx-auto px-4 py-16">
      <h2 class="text-3xl font-bold mb-8">热门商品</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <div
          v-for="product in products"
          :key="product.id"
          class="group bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-xl transition-all duration-300 cursor-pointer"
        >
          <div class="aspect-square overflow-hidden bg-gray-100">
            <img
              :src="product.image"
              :alt="product.name"
              class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
            />
          </div>
          <div class="p-6">
            <h3 class="text-xl font-semibold mb-2">{{ product.name }}</h3>
            <p class="text-gray-600 mb-4">{{ product.description }}</p>
            <div class="flex justify-between items-center">
              <span class="text-2xl font-bold text-orange-500">¥{{ product.price }}</span>
              <button
                @click="addToCart(product)"
                class="bg-gradient-to-r from-orange-400 to-orange-600 text-white px-6 py-2 rounded-full hover:shadow-lg transition"
              >
                加入购物车
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>

  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useCartStore } from '../stores/cart'
import { ElMessageBox, ElMessage } from 'element-plus'

const cartStore = useCartStore()

const products = ref([
  {
    id: 1,
    name: '智能手表 Pro',
    description: '健康监测，运动追踪',
    price: 2999,
    image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500'
  },
  {
    id: 2,
    name: '无线耳机',
    description: '降噪技术，高保真音质',
    price: 1299,
    image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500'
  },
  {
    id: 3,
    name: '智能音箱',
    description: '语音控制，智能家居中枢',
    price: 899,
    image: 'https://images.unsplash.com/photo-1543512214-318c7553f230?w=500'
  },
  {
    id: 4,
    name: '运动相机',
    description: '4K录制，防水防震',
    price: 1899,
    image: 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=500'
  },
  {
    id: 5,
    name: '机械键盘',
    description: 'RGB背光，青轴手感',
    price: 599,
    image: 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500'
  },
  {
    id: 6,
    name: '便携充电宝',
    description: '20000mAh，快充支持',
    price: 299,
    image: 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=500'
  }
])

const scrollToHotProducts = () => {
  document.getElementById('hot-products')?.scrollIntoView({ behavior: 'smooth' })
}

 const addToCart = (product: any) => {
   cartStore.addToCart(product)
   ElMessageBox.alert(`${product.name} 已加入购物车！`, '添加成功', {
     confirmButtonText: '确定',
     confirmButtonClass: 'bg-gradient-to-r from-orange-400 to-orange-600 text-white border-none',
     callback: action => {
       ElMessage({
         type: 'success',
         message: `已成功添加商品到购物车`,
         offset: 100,
         showClose: false,
         duration: 1500,
         position: 'top-right'
       })
     }
   })
 }
</script>


