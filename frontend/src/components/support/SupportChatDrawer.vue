<script setup>
import { ref, watch, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { createSupportSocket } from '../../utils/supportSocket'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'support-read'])

const visible = ref(false)
const conversation = ref(null)
const messages = ref([])
const loading = ref(false)
const sending = ref(false)
const connectionState = ref('idle')
const draft = ref('')
const chatBodyRef = ref(null)
let supportSocket = null
let previousBodyOverflow = ''

const formatTime = (value) => value ? String(value).replace('T', ' ').slice(11, 16) : ''
const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 10) : ''
const senderName = (item) => item?.senderRealName || item?.senderName || (item?.senderRole === 'ADMIN' ? '客服' : '我')

const scrollToBottom = async () => {
  await nextTick()
  if (!chatBodyRef.value) return
  chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
}

const disconnectSocket = () => {
  supportSocket?.disconnect()
  supportSocket = null
  connectionState.value = 'idle'
}

const closeDrawer = () => {
  emit('update:modelValue', false)
}

const emitSupportRead = () => {
  if (!conversation.value?.id) return
  emit('support-read', {
    conversationId: conversation.value.id,
    messageType: 'SUPPORT_MESSAGE_REPLIED',
    bizType: 'SUPPORT_CONVERSATION',
  })
}

const lockBodyScroll = () => {
  previousBodyOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
}

const unlockBodyScroll = () => {
  document.body.style.overflow = previousBodyOverflow
}

const ensureSocket = () => {
  if (!supportSocket) {
    supportSocket = createSupportSocket({
      onOpen: () => { connectionState.value = 'connected' },
      onClose: () => { connectionState.value = 'disconnected' },
      onError: () => { connectionState.value = 'disconnected' },
      onMessage: async (payload) => {
        if (payload.type === 'ERROR') {
          ElMessage.error(payload.message || '客服连接异常')
          return
        }
        if (payload.type === 'MESSAGE_CREATED') {
          const message = payload.payload
          if (message?.conversationId === conversation.value?.id && !messages.value.some(item => item.id === message.id)) {
            messages.value.push(message)
            await scrollToBottom()
            if (visible.value && message.senderRole === 'ADMIN') {
              await markRead()
            }
          }
        }
        if (payload.type === 'CONVERSATION_UPDATED') {
          const updated = payload.payload
          if (updated?.id === conversation.value?.id) {
            conversation.value = updated
          }
        }
      },
    })
  }
  if (!supportSocket.isConnected()) {
    connectionState.value = 'connecting'
  }
  return supportSocket.connect()
}

const markRead = async () => {
  if (!conversation.value?.id) return false
  const unreadAdminMessages = messages.value.some(item => item.senderRole === 'ADMIN' && item.readStatus === 0)
  conversation.value = await request.put(`/support/conversations/${conversation.value.id}/read`)
  if (unreadAdminMessages) {
    emitSupportRead()
  }
  return unreadAdminMessages
}

const loadMessages = async () => {
  if (!conversation.value?.id) return
  messages.value = await request.get(`/support/conversations/${conversation.value.id}/messages`)
  await scrollToBottom()
  await markRead()
}

const ensureConversation = async () => {
  loading.value = true
  try {
    conversation.value = await request.post('/support/conversations', {})
    ensureSocket()
    await loadMessages()
  } finally {
    loading.value = false
  }
}

const sendMessage = async () => {
  if (!conversation.value?.id) return
  if (!draft.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }
  sending.value = true
  try {
    try {
      await ensureSocket()
    } catch {
      ElMessage.error('客服连接失败，请稍后重试')
      return
    }
    const sent = supportSocket.send({
      type: 'SEND_MESSAGE',
      conversationId: conversation.value.id,
      content: draft.value.trim(),
    })
    if (!sent) {
      ElMessage.warning('客服暂未连接成功，请稍后重试')
      return
    }
    draft.value = ''
  } finally {
    sending.value = false
  }
}

watch(() => props.modelValue, async (value) => {
  visible.value = value
  if (value) {
    lockBodyScroll()
    await ensureConversation()
    return
  }
  unlockBodyScroll()
  disconnectSocket()
}, { immediate: true })

watch(visible, (value) => {
  emit('update:modelValue', value)
})

onBeforeUnmount(() => {
  unlockBodyScroll()
  disconnectSocket()
})
</script>

<template>
  <transition name="support-drawer">
    <div v-if="visible" class="support-overlay" @click.self="closeDrawer">
      <div class="support-drawer">

        <!-- Header -->
        <div class="support-header">
          <div class="support-header-avatar">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 18v-6a9 9 0 0 1 18 0v6"/>
              <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3z"/>
              <path d="M3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/>
            </svg>
          </div>
          <div class="support-header-info">
            <div class="support-title">联系客服</div>
            <div class="support-status">
              <span class="support-status-dot" :class="connectionState"></span>
              <span>{{ connectionState === 'connected' ? '在线' : connectionState === 'connecting' ? '连接中…' : '已断开' }}</span>
            </div>
          </div>
          <button class="support-close" @click="closeDrawer" aria-label="关闭">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <!-- Body -->
        <div ref="chatBodyRef" v-loading="loading" class="support-body">
          <div v-if="!loading && messages.length === 0" class="support-empty">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" style="opacity:.35;margin-bottom:10px">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <p>先发条消息开始咨询吧</p>
          </div>

          <template v-for="(item, index) in messages" :key="item.id">
            <!-- Date divider -->
            <div
              v-if="index === 0 || formatDate(item.createTime) !== formatDate(messages[index - 1].createTime)"
              class="support-date-divider"
            >
              <span>{{ formatDate(item.createTime) }}</span>
            </div>

            <!-- Message bubble -->
            <div class="support-message" :class="item.senderRole === 'USER' ? 'self' : 'other'">
              <!-- Avatar for admin -->
              <div v-if="item.senderRole !== 'USER'" class="support-avatar">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3 18v-6a9 9 0 0 1 18 0v6"/>
                  <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3z"/>
                  <path d="M3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/>
                </svg>
              </div>
              <div class="support-bubble-group">
                <div class="support-sender">{{ senderName(item) }}</div>
                <div class="support-bubble">{{ item.content }}</div>
                <div class="support-time">{{ formatTime(item.createTime) }}</div>
              </div>
            </div>
          </template>
        </div>

        <!-- Footer -->
        <div class="support-footer">
          <div class="support-input-wrap" :class="{ disabled: conversation?.status === 'CLOSED' }">
            <textarea
              v-model="draft"
              class="support-input"
              rows="3"
              :disabled="conversation?.status === 'CLOSED'"
              placeholder="请输入咨询内容…"
              @keyup.ctrl.enter="sendMessage"
            ></textarea>
            <button
              class="support-send"
              :disabled="sending || conversation?.status === 'CLOSED'"
              @click="sendMessage"
              aria-label="发送"
            >
              <svg v-if="!sending" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-left:2px">
                <line x1="22" y1="2" x2="11" y2="13"/>
                <polygon points="22 2 15 22 11 13 2 9 22 2"/>
              </svg>
              <svg v-else width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" class="spin">
                <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
              </svg>
            </button>
          </div>
          <div class="support-hint">
            <span v-if="conversation?.status === 'CLOSED'" class="support-closed-tag">会话已关闭</span>
            <span v-else>Ctrl + Enter 发送</span>
          </div>
        </div>

      </div>
    </div>
  </transition>
</template>

<style scoped>
/* ── Overlay & drawer ── */
.support-overlay {
  position: fixed;
  inset: 0;
  z-index: 250;
  background: rgba(0, 0, 0, .32);
  backdrop-filter: blur(2px);
}

.support-drawer {
  position: absolute;
  top: 0;
  right: 0;
  width: min(420px, 100vw);
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 40px rgba(0, 0, 0, .12);
}

/* ── Header ── */
.support-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid #f0eeeb;
  background: #fff;
}

.support-header-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #e8f0fe;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #1a73e8;
}

.support-header-info {
  flex: 1;
  min-width: 0;
}

.support-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.3;
}

.support-status {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 3px;
  font-size: 12px;
  color: #888;
}

.support-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #d0d0d0;
  flex-shrink: 0;
  transition: background .3s;
}
.support-status-dot.connected { background: #2da44e; }
.support-status-dot.connecting { background: #f0a500; }
.support-status-dot.disconnected { background: #d93025; }

.support-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #ebebeb;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #666;
  flex-shrink: 0;
  transition: background .15s;
}
.support-close:hover { background: #efefef; }

/* ── Body ── */
.support-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 16px;
  background: #f7f6f4;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.support-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #aaa;
  font-size: 13px;
  padding: 40px 0;
}

/* ── Date divider ── */
.support-date-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 4px 0;
}
.support-date-divider span {
  font-size: 11px;
  color: #999;
  background: #ece9e5;
  padding: 3px 10px;
  border-radius: 20px;
}

/* ── Message row ── */
.support-message {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 85%;
}
.support-message.self {
  margin-left: auto;
  flex-direction: row-reverse;
}

.support-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #e8f0fe;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-bottom: 18px;
  color: #1a73e8;
}

.support-bubble-group {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.support-message.self .support-bubble-group {
  align-items: flex-end;
}

.support-sender {
  font-size: 11px;
  color: #aaa;
  padding: 0 2px;
}

.support-bubble {
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.65;
  word-break: break-word;
  white-space: pre-wrap;
  background: #fff;
  color: #1a1a1a;
  border: 1px solid #ebebeb;
  border-radius: 16px 16px 16px 4px;
}
.support-message.self .support-bubble {
  background: #1a73e8;
  color: #fff;
  border: none;
  border-radius: 16px 16px 4px 16px;
}

.support-time {
  font-size: 11px;
  color: #bbb;
  padding: 0 2px;
}

/* ── Footer ── */
.support-footer {
  border-top: 1px solid #f0eeeb;
  padding: 12px 16px 16px;
  background: #fff;
}

.support-input-wrap {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  background: #f5f4f2;
  border: 1px solid #ebebeb;
  border-radius: 14px;
  padding: 10px 10px 10px 14px;
  transition: border-color .15s;
}
.support-input-wrap:focus-within {
  border-color: #1a73e8;
}
.support-input-wrap.disabled {
  opacity: .6;
}

.support-input {
  flex: 1;
  border: none;
  background: transparent;
  resize: none;
  font-size: 13px;
  color: #1a1a1a;
  line-height: 1.65;
  outline: none;
  padding: 0;
  font-family: inherit;
}
.support-input::placeholder { color: #bbb; }
.support-input:disabled { cursor: not-allowed; }

.support-send {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: #1a73e8;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: background .15s, transform .1s;
}
.support-send:hover { background: #1558b0; }
.support-send:active { transform: scale(.93); }
.support-send:disabled {
  background: #c5d8f7;
  cursor: not-allowed;
  transform: none;
}

.support-hint {
  margin-top: 8px;
  text-align: center;
  font-size: 11px;
  color: #bbb;
}

.support-closed-tag {
  display: inline-block;
  background: #fef3f2;
  color: #b42318;
  border: 1px solid #fecdca;
  border-radius: 20px;
  padding: 2px 10px;
  font-size: 11px;
}

/* ── Spinner ── */
@keyframes spin { to { transform: rotate(360deg); } }
.spin { animation: spin .8s linear infinite; }

/* ── Drawer transition ── */
.support-drawer-enter-active,
.support-drawer-leave-active {
  transition: opacity .22s ease;
}
.support-drawer-enter-active .support-drawer,
.support-drawer-leave-active .support-drawer {
  transition: transform .26s cubic-bezier(.32, .72, 0, 1);
}
.support-drawer-enter-from,
.support-drawer-leave-to {
  opacity: 0;
}
.support-drawer-enter-from .support-drawer,
.support-drawer-leave-to .support-drawer {
  transform: translateX(100%);
}

/* ── Scrollbar ── */
.support-body::-webkit-scrollbar { width: 4px; }
.support-body::-webkit-scrollbar-track { background: transparent; }
.support-body::-webkit-scrollbar-thumb { background: #ddd; border-radius: 4px; }
</style>