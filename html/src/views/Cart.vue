<template>
  <div class="pt-24 pb-16 min-h-screen bg-gray-50">
    <div class="max-w-7xl mx-auto px-4">
      <h1 class="text-4xl font-bold mb-8">购物车</h1>

      <div v-if="cartStore.items.length === 0" class="text-center py-16">
        <svg class="w-24 h-24 mx-auto text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
          <p class="text-xl text-gray-600 mb-4">购物车是空的</p>
          <RouterLink to="/" class="text-orange-500 hover:underline">
          去购物 →
        </RouterLink>
      </div>

      <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- 商品列表 -->
        <div class="lg:col-span-2 space-y-4">
          <div
            v-for="item in cartStore.items"
            :key="item.id"
            class="bg-white rounded-xl p-6 flex gap-6 shadow-sm"
          >
            <img
              :src="item.image"
              :alt="item.name"
              class="w-24 h-24 object-cover rounded-lg"
            />
            <div class="flex-1">
              <h3 class="text-lg font-semibold mb-2">{{ item.name }}</h3>
                <p class="text-2xl font-bold text-orange-500 mb-4">¥{{ item.price }}</p>
              <div class="flex items-center gap-4">
                <div class="flex items-center gap-2 border border-gray-300 rounded-lg">
                  <button
                    @click="cartStore.updateQuantity(item.id, item.quantity - 1)"
                    class="px-3 py-1 hover:bg-gray-100"
                  >
                    -
                  </button>
                  <span class="px-4">{{ item.quantity }}</span>
                  <button
                    @click="cartStore.updateQuantity(item.id, item.quantity + 1)"
                    class="px-3 py-1 hover:bg-gray-100"
                  >
                    +
                  </button>
                </div>
                <button
                  @click="cartStore.removeFromCart(item.id)"
                  class="text-red-600 hover:text-red-700"
                >
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 订单摘要 -->
        <div class="lg:col-span-1">
          <div class="bg-white rounded-xl p-6 shadow-sm sticky top-24">
            <h2 class="text-xl font-bold mb-6">订单摘要</h2>
            
            <div class="space-y-4 mb-6">
              <div class="flex justify-between">
                <span class="text-gray-600">小计</span>
                <span class="font-semibold">¥{{ cartStore.subtotal.toFixed(2) }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-600">运费</span>
                <span class="font-semibold">免费</span>
              </div>
              <div class="border-t pt-4">
                <div class="flex justify-between text-lg">
                  <span class="font-bold">总计</span>
                   <span class="font-bold text-orange-500">¥{{ cartStore.subtotal.toFixed(2) }}</span>
                </div>
              </div>
             </div>

             <button
              @click="checkout"
              class="w-full bg-gradient-to-r from-orange-400 to-orange-600 text-white py-3 rounded-lg font-semibold hover:shadow-lg transition"
            >
              去结算
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useCartStore } from '../stores/cart'

const cartStore = useCartStore()

const checkout = () => {
  alert('正在跳转到结算页面...')
  // 这里可以跳转到支付页面
}
</script>
