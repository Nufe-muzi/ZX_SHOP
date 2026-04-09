<template>
  <Teleport to="body">
    <!-- 可拖拽的AI小球 -->
    <div
      v-if="!isExpanded"
      ref="ballRef"
      :style="{ left: position.x + 'px', top: position.y + 'px' }"
       class="fixed w-16 h-16 rounded-full bg-gradient-to-br from-orange-400 to-orange-600 shadow-lg cursor-move z-50 flex items-center justify-center text-white hover:scale-110 transition-transform"
      @mousedown="startDrag"
      @click="expandChat"
    >
      <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
              d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
      </svg>
    </div>

    <!-- 展开的聊天窗口 -->
    <Transition name="scale">
      <div
        v-if="isExpanded"
        class="fixed bottom-6 right-6 w-96 h-[600px] bg-white rounded-2xl shadow-2xl z-50 flex flex-col overflow-hidden"
      >
        <!-- 头部 -->
         <div class="bg-gradient-to-r from-orange-400 to-orange-600 p-4 text-white flex justify-between items-center">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                      d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
            </div>
            <div>
              <h3 class="font-semibold">AI 智能助手</h3>
              <p class="text-xs text-white/80">在线</p>
            </div>
          </div>
          <button @click="collapseChat" class="hover:bg-white/20 rounded-full p-2 transition">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- 聊天内容 -->
        <div class="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50">
          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="[
              'flex',
              message.type === 'user' ? 'justify-end' : 'justify-start'
            ]"
          >
            <div
              :class="[
                'max-w-[80%] rounded-2xl px-4 py-2',
                message.type === 'user'
                   ? 'bg-gradient-to-r from-orange-400 to-orange-600 text-white'
                  : 'bg-white text-gray-800 shadow-sm'
              ]"
            >
              {{ message.text }}
            </div>
          </div>
        </div>

        <!-- 输入框 -->
        <div class="p-4 bg-white border-t border-gray-200">
          <div class="flex gap-2">
            <input
              v-model="inputMessage"
              type="text"
              placeholder="输入消息..."
               class="flex-1 px-4 py-2 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-orange-500"
              @keypress.enter="sendMessage"
            />
            <button
              @click="sendMessage"
               class="bg-gradient-to-r from-orange-400 to-orange-600 text-white rounded-full p-2 hover:shadow-lg transition"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                      d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'

const isExpanded = ref(false)
const ballRef = ref<HTMLElement | null>(null)
const position = reactive({ x: 0, y: 0 })
const isDragging = ref(false)
const dragOffset = reactive({ x: 0, y: 0 })
const inputMessage = ref('')
const messages = ref([
  { type: 'ai', text: '你好！我是AI智能助手，有什么可以帮助你的吗？' }
])

onMounted(() => {
  // 初始位置：右下角
  position.x = window.innerWidth - 100
  position.y = window.innerHeight - 100
})

const startDrag = (e: MouseEvent) => {
  isDragging.value = true
  dragOffset.x = e.clientX - position.x
  dragOffset.y = e.clientY - position.y

  const handleMouseMove = (e: MouseEvent) => {
    if (isDragging.value) {
      position.x = e.clientX - dragOffset.x
      position.y = e.clientY - dragOffset.y
    }
  }

  const handleMouseUp = () => {
    isDragging.value = false
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

const expandChat = () => {
  if (!isDragging.value) {
    isExpanded.value = true
  }
}

const collapseChat = () => {
  isExpanded.value = false
}

const sendMessage = () => {
  if (inputMessage.value.trim()) {
    messages.value.push({
      type: 'user',
      text: inputMessage.value
    })

    // 模拟AI回复
    setTimeout(() => {
      messages.value.push({
        type: 'ai',
        text: getAIResponse(inputMessage.value)
      })
    }, 500)

    inputMessage.value = ''
  }
}

const getAIResponse = (userMessage: string) => {
  const message = userMessage.toLowerCase()
  if (message.includes('推荐') || message.includes('商品')) {
    return '根据您的浏览历史，我推荐您查看我们的新款电子产品，特别是智能手表系列！'
  } else if (message.includes('价格') || message.includes('优惠')) {
    return '现在有限时优惠活动，使用优惠码 SAVE20 可享受8折优惠！'
  } else if (message.includes('配送') || message.includes('物流')) {
    return '我们提供全国包邮服务，通常3-5个工作日送达。'
  }
  return '感谢您的咨询！我会尽力为您提供帮助。'
}
</script>

<style scoped>
.scale-enter-active,
.scale-leave-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.scale-enter-from,
.scale-leave-to {
  opacity: 0;
  transform: scale(0.8);
}
</style>
