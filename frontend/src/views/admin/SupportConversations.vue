<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { createSupportSocket } from '../../utils/supportSocket'

const route = useRoute()
const conversations = ref([])
const loading = ref(false)
const messagesLoading = ref(false)
const sending = ref(false)
const statusFilter = ref('OPEN')
const keyword = ref('')
const selectedConversation = ref(null)
const messages = ref([])
const draft = ref('')
const connectionState = ref('idle')
const chatBodyRef = ref(null)
let supportSocket = null

const statusText = (status) => status === 'CLOSED' ? '已关闭' : '进行中'
const formatTime = (value) => value ? String(value).replace('T', ' ').slice(11, 16) : ''
const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 10) : ''
const formatTimeShort = (value) => {
  if (!value) return ''
  const str = String(value).replace('T', ' ')
  const today = new Date().toISOString().slice(0, 10)
  const date = str.slice(0, 10)
  return date === today ? str.slice(11, 16) : str.slice(5, 10)
}
const displayName = (item) => item?.userRealName || item?.userName || '用户'
const senderName = (item) => item?.senderRealName || item?.senderName || (item?.senderRole === 'ADMIN' ? '客服' : '用户')
const userInitial = (item) => displayName(item).slice(0, 1)

const emitSupportPrompt = (conversation) => {
  if (!conversation?.id) return
  window.dispatchEvent(new CustomEvent('admin-support-notice', {
    detail: {
      conversationId: conversation.id,
      title: '新的客服消息',
      content: `${displayName(conversation)} 发来新的客服消息，请及时处理`,
    },
  }))
}

const emitSupportRead = (conversationId) => {
  if (!conversationId) return
  window.dispatchEvent(new CustomEvent('admin-support-notice', {
    detail: {
      action: 'read',
      conversationId,
    },
  }))
}

const scrollToBottom = async () => {
  await nextTick()
  if (!chatBodyRef.value) return
  chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
}

const upsertConversation = (conversation) => {
  if (!conversation) return
  const index = conversations.value.findIndex(item => item.id === conversation.id)
  if (index === -1) { conversations.value.unshift(conversation); return }
  conversations.value.splice(index, 1)
  conversations.value.unshift(conversation)
}

const loadConversations = async (preferredId) => {
  loading.value = true
  try {
    const routePreferredId = Number(route.query.conversationId) || undefined
    const page = await request.get('/admin/support/conversations', {
      params: {
        pageNum: 1,
        pageSize: 20,
        status: statusFilter.value || undefined,
        keyword: keyword.value.trim() || undefined,
      },
    })
    conversations.value = page.records || []
    const targetId = preferredId || routePreferredId || selectedConversation.value?.id || conversations.value[0]?.id
    if (targetId) {
      const target = conversations.value.find(item => item.id === targetId)
      if (target) await selectConversation(target)
    } else {
      selectedConversation.value = null
      messages.value = []
    }
  } finally {
    loading.value = false
  }
}

const markConversationRead = async (id = selectedConversation.value?.id) => {
  if (!id) return false
  const hasUnreadUserMessages = messages.value.some(item => item.senderRole === 'USER' && item.readStatus === 0)
  const conversation = await request.put(`/admin/support/conversations/${id}/read`)
  selectedConversation.value = conversation
  upsertConversation(conversation)
  if (hasUnreadUserMessages) {
    emitSupportRead(id)
  }
  return hasUnreadUserMessages
}

const loadMessages = async (conversationId) => {
  if (!conversationId) return
  messagesLoading.value = true
  try {
    messages.value = await request.get(`/admin/support/conversations/${conversationId}/messages`)
    await scrollToBottom()
  } finally {
    messagesLoading.value = false
  }
}

const selectConversation = async (conversation) => {
  selectedConversation.value = conversation
  await loadMessages(conversation.id)
  await markConversationRead(conversation.id)
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
          const isCurrentConversation = message?.conversationId === selectedConversation.value?.id
          if (isCurrentConversation && !messages.value.some(item => item.id === message.id)) {
            messages.value.push(message)
            await scrollToBottom()
            if (message.senderRole === 'USER') await markConversationRead(message.conversationId)
          }
          if (!isCurrentConversation && message?.senderRole === 'USER') {
            const targetConversation = conversations.value.find(item => item.id === message.conversationId)
            emitSupportPrompt(targetConversation || { id: message.conversationId, userRealName: message.senderRealName, userName: message.senderName })
          }
        }
        if (payload.type === 'CONVERSATION_UPDATED') {
          const conversation = payload.payload
          upsertConversation(conversation)
          if (conversation?.id === selectedConversation.value?.id) selectedConversation.value = conversation
        }
      },
    })
  }
  if (!supportSocket.isConnected()) connectionState.value = 'connecting'
  return supportSocket.connect()
}

const sendMessage = async () => {
  if (!selectedConversation.value?.id) return
  if (!draft.value.trim()) { ElMessage.warning('请输入消息内容'); return }
  sending.value = true
  try {
    try { await ensureSocket() } catch {
      ElMessage.error('客服连接失败，请稍后重试'); return
    }
    const sent = supportSocket.send({
      type: 'SEND_MESSAGE',
      conversationId: selectedConversation.value.id,
      content: draft.value.trim(),
    })
    if (!sent) { ElMessage.warning('客服暂未连接成功，请稍后重试'); return }
    draft.value = ''
  } finally {
    sending.value = false
  }
}

const closeConversation = async () => {
  if (!selectedConversation.value?.id) return
  const conversation = await request.put(`/admin/support/conversations/${selectedConversation.value.id}/close`)
  selectedConversation.value = conversation
  upsertConversation(conversation)
  ElMessage.success('会话已关闭')
}

onMounted(() => {
  ensureSocket()
  loadConversations()
})

onBeforeUnmount(() => {
  supportSocket?.disconnect()
  supportSocket = null
})
</script>

<template>
  <div class="support-page">

    <!-- Toolbar -->
    <div class="support-toolbar">
      <div class="toolbar-left">
        <div class="support-title">客服工作台</div>
        <div class="support-sub">实时处理用户咨询消息</div>
      </div>
      <div class="toolbar-right">
        <div class="conn-status">
          <span class="conn-dot" :class="connectionState"></span>
          <span>{{ connectionState === 'connected' ? '已连接' : connectionState === 'connecting' ? '连接中…' : '已断开' }}</span>
        </div>
        <select v-model="statusFilter" class="toolbar-select" @change="loadConversations()">
          <option value="OPEN">进行中</option>
          <option value="CLOSED">已关闭</option>
          <option value="">全部</option>
        </select>
        <div class="toolbar-search">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input v-model="keyword" class="toolbar-input" placeholder="搜索用户或消息" @keyup.enter="loadConversations()" />
        </div>
        <button class="toolbar-btn" @click="loadConversations()">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-.08-5"/>
          </svg>
          刷新
        </button>
      </div>
    </div>

    <!-- Board -->
    <div class="support-board">

      <!-- Conversation list -->
      <aside class="conversation-list" v-loading="loading">
        <div v-if="conversations.length === 0 && !loading" class="empty-hint">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" style="opacity:.35;margin-bottom:8px">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          <p>暂无会话</p>
        </div>
        <button
          v-for="item in conversations"
          :key="item.id"
          class="conv-item"
          :class="{ active: selectedConversation?.id === item.id }"
          @click="selectConversation(item)"
        >
          <div class="conv-avatar" :class="item.status === 'CLOSED' ? 'closed' : 'open'">
            {{ userInitial(item) }}
          </div>
          <div class="conv-body">
            <div class="conv-row">
              <span class="conv-name">{{ displayName(item) }}</span>
              <span class="conv-time">{{ formatTimeShort(item.lastMessageTime || item.updateTime) }}</span>
            </div>
            <div class="conv-preview">{{ item.lastMessagePreview || '暂无消息' }}</div>
            <div class="conv-row" style="margin-top:6px">
              <span class="conv-status" :class="item.status === 'CLOSED' ? 'closed' : 'open'">
                {{ statusText(item.status) }}
              </span>
              <span v-if="item.adminUnreadCount > 0" class="conv-badge">{{ item.adminUnreadCount }}</span>
            </div>
          </div>
        </button>
      </aside>

      <!-- Chat panel -->
      <section class="chat-panel">
        <template v-if="selectedConversation">

          <!-- Chat header -->
          <div class="chat-header">
            <div class="chat-header-avatar" :class="selectedConversation.status === 'CLOSED' ? 'closed' : 'open'">
              {{ userInitial(selectedConversation) }}
            </div>
            <div class="chat-header-info">
              <div class="chat-title">{{ displayName(selectedConversation) }}</div>
              <div class="chat-sub">
                <span class="chat-status-dot" :class="selectedConversation.status === 'CLOSED' ? 'closed' : 'open'"></span>
                {{ statusText(selectedConversation.status) }}
              </div>
            </div>
            <button
              class="close-btn"
              :disabled="selectedConversation.status === 'CLOSED'"
              @click="closeConversation"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
              关闭会话
            </button>
          </div>

          <!-- Messages -->
          <div ref="chatBodyRef" class="chat-body" v-loading="messagesLoading">
            <div v-if="messages.length === 0 && !messagesLoading" class="empty-hint">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" style="opacity:.35;margin-bottom:8px">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
              <p>暂无消息</p>
            </div>

            <template v-for="(item, index) in messages" :key="item.id">
              <!-- Date divider -->
              <div
                v-if="index === 0 || formatDate(item.createTime) !== formatDate(messages[index - 1].createTime)"
                class="date-divider"
              >
                <span>{{ formatDate(item.createTime) }}</span>
              </div>

              <!-- Message -->
              <div class="chat-message" :class="item.senderRole === 'ADMIN' ? 'self' : 'other'">
                <div v-if="item.senderRole !== 'ADMIN'" class="msg-avatar user">
                  {{ userInitial(selectedConversation) }}
                </div>
                <div class="msg-group">
                  <div class="msg-sender">{{ senderName(item) }}</div>
                  <div class="msg-bubble">{{ item.content }}</div>
                  <div class="msg-time">{{ formatTime(item.createTime) }}</div>
                </div>
                <!-- <div v-if="item.senderRole === 'ADMIN'" class="msg-avatar admin"> -->
                  <!-- <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 18v-6a9 9 0 0 1 18 0v6"/>
                    <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3z"/>
                    <path d="M3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/>
                  </svg> -->
                <!-- </div> -->
              </div>
            </template>
          </div>

          <!-- Input -->
          <div class="chat-footer">
            <div class="input-wrap" :class="{ disabled: selectedConversation.status === 'CLOSED' }">
              <textarea
                v-model="draft"
                class="chat-input"
                rows="3"
                :disabled="selectedConversation.status === 'CLOSED'"
                placeholder="输入回复内容…"
                @keyup.ctrl.enter="sendMessage"
              ></textarea>
              <button
                class="send-btn"
                :disabled="sending || selectedConversation.status === 'CLOSED'"
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
            <div class="footer-hint">
              <span v-if="selectedConversation.status === 'CLOSED'" class="closed-tag">会话已关闭</span>
              <span v-else>Ctrl + Enter 发送</span>
            </div>
          </div>
        </template>

        <!-- Empty state -->
        <div v-else class="empty-hint large">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round" style="opacity:.25;margin-bottom:12px">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          <p>请选择左侧会话开始处理</p>
        </div>
      </section>

    </div>
  </div>
</template>

<style scoped>
/* ── Page ── */
.support-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: calc(100vh - 128px);
}

/* ── Toolbar ── */
.support-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
}
.support-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}
.support-sub {
  margin-top: 3px;
  font-size: 13px;
  color: #999;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.conn-status {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #999;
  padding: 0 4px;
}
.conn-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #d0d0d0;
  flex-shrink: 0;
  transition: background .3s;
}
.conn-dot.connected { background: #2da44e; }
.conn-dot.connecting { background: #f0a500; }
.conn-dot.disconnected { background: #d93025; }

.toolbar-select {
  height: 36px;
  border-radius: 10px;
  border: 1px solid #ebebeb;
  background: #fff;
  padding: 0 10px;
  font-size: 13px;
  color: #1a1a1a;
  cursor: pointer;
}
.toolbar-search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid #ebebeb;
  background: #fff;
  padding: 0 12px;
  color: #bbb;
  transition: border-color .15s;
}
.toolbar-search:focus-within {
  border-color: #1a73e8;
  color: #1a73e8;
}
.toolbar-input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: #1a1a1a;
  width: 180px;
}
.toolbar-input::placeholder { color: #bbb; }
.toolbar-btn {
  height: 36px;
  border-radius: 10px;
  border: 1px solid #ebebeb;
  background: #fff;
  padding: 0 14px;
  font-size: 13px;
  color: #444;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background .15s;
}
.toolbar-btn:hover { background: #f5f5f5; }

/* ── Board ── */
.support-board {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 14px;
}

/* ── Conversation list ── */
.conversation-list {
  background: #fff;
  border: 1px solid #ebebeb;
  border-radius: 16px;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.conversation-list::-webkit-scrollbar { width: 4px; }
.conversation-list::-webkit-scrollbar-track { background: transparent; }
.conversation-list::-webkit-scrollbar-thumb { background: #e0e0e0; border-radius: 4px; }

.conv-item {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 11px 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: background .15s, border-color .15s;
}
.conv-item:hover { background: #fafafa; }
.conv-item.active {
  border-color: rgba(26, 115, 232, .3);
  background: #f0f6ff;
}

.conv-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
  margin-top: 1px;
}
.conv-avatar.open { background: #e8f0fe; color: #1a73e8; }
.conv-avatar.closed { background: #f5f5f5; color: #aaa; }

.conv-body { flex: 1; min-width: 0; }
.conv-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.conv-name {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.conv-time {
  font-size: 11px;
  color: #bbb;
  flex-shrink: 0;
}
.conv-preview {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.conv-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 20px;
}
.conv-status.open { background: #e6f4ea; color: #1e7e34; }
.conv-status.closed { background: #f5f5f5; color: #aaa; }
.conv-badge {
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: #d93025;
  color: #fff;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}

/* ── Chat panel ── */
.chat-panel {
  background: #fff;
  border: 1px solid #ebebeb;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

/* ── Chat header ── */
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.chat-header-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  flex-shrink: 0;
}
.chat-header-avatar.open { background: #e8f0fe; color: #1a73e8; }
.chat-header-avatar.closed { background: #f5f5f5; color: #aaa; }
.chat-header-info { flex: 1; min-width: 0; }
.chat-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.chat-sub {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 3px;
  font-size: 12px;
  color: #999;
}
.chat-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.chat-status-dot.open { background: #2da44e; }
.chat-status-dot.closed { background: #d0d0d0; }

.close-btn {
  height: 34px;
  border-radius: 10px;
  border: 1px solid #fecdca;
  background: #fef3f2;
  color: #b42318;
  padding: 0 14px;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  transition: background .15s;
}
.close-btn:hover:not(:disabled) { background: #fee4e2; }
.close-btn:disabled { opacity: .5; cursor: not-allowed; }

/* ── Chat body ── */
.chat-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 16px;
  background: #f7f6f4;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.chat-body::-webkit-scrollbar { width: 4px; }
.chat-body::-webkit-scrollbar-track { background: transparent; }
.chat-body::-webkit-scrollbar-thumb { background: #ddd; border-radius: 4px; }

/* ── Date divider ── */
.date-divider {
  display: flex;
  align-items: center;
  justify-content: center;
}
.date-divider span {
  font-size: 11px;
  color: #999;
  background: #ece9e5;
  padding: 3px 10px;
  border-radius: 20px;
}

/* ── Messages ── */
.chat-message {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 75%;
}
.chat-message.self {
  margin-left: auto;
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-bottom: 18px;
  font-size: 12px;
  font-weight: 600;
}
.msg-avatar.user { background: #e8f0fe; color: #1a73e8; }
.msg-avatar.admin { background: #e6f4ea; color: #1e7e34; }

.msg-group { display: flex; flex-direction: column; gap: 3px; }
.chat-message.self .msg-group { align-items: flex-end; }

.msg-sender {
  font-size: 11px;
  color: #bbb;
  padding: 0 2px;
}
.msg-bubble {
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
.chat-message.self .msg-bubble {
  background: #1a73e8;
  color: #fff;
  border: none;
  border-radius: 16px 16px 4px 16px;
}
.msg-time {
  font-size: 11px;
  color: #bbb;
  padding: 0 2px;
}

/* ── Footer ── */
.chat-footer {
  border-top: 1px solid #f0f0f0;
  padding: 12px 16px 16px;
  flex-shrink: 0;
  background: #fff;
}
.input-wrap {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  background: #f5f4f2;
  border: 1px solid #ebebeb;
  border-radius: 14px;
  padding: 10px 10px 10px 14px;
  transition: border-color .15s;
}
.input-wrap:focus-within { border-color: #1a73e8; }
.input-wrap.disabled { opacity: .6; }
.chat-input {
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
.chat-input::placeholder { color: #bbb; }
.chat-input:disabled { cursor: not-allowed; }
.send-btn {
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
.send-btn:hover { background: #1558b0; }
.send-btn:active { transform: scale(.93); }
.send-btn:disabled { background: #c5d8f7; cursor: not-allowed; transform: none; }

.footer-hint {
  margin-top: 8px;
  text-align: center;
  font-size: 11px;
  color: #bbb;
}
.closed-tag {
  display: inline-block;
  background: #fef3f2;
  color: #b42318;
  border: 1px solid #fecdca;
  border-radius: 20px;
  padding: 2px 10px;
  font-size: 11px;
}

/* ── Empty states ── */
.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 13px;
  padding: 32px 0;
  text-align: center;
}
.empty-hint.large {
  flex: 1;
  height: 100%;
}
.empty-hint p { margin: 0; }

/* ── Spinner ── */
@keyframes spin { to { transform: rotate(360deg); } }
.spin { animation: spin .8s linear infinite; }
</style>