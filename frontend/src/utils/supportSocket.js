import { getAuth } from './auth'

function buildSocketUrl(token) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/support-chat?token=${encodeURIComponent(token)}`
}

export function createSupportSocket({ onOpen, onClose, onMessage, onError } = {}) {
  let socket = null
  let reconnectTimer = null
  let manualClose = false
  let pendingConnects = []

  const clearReconnectTimer = () => {
    if (!reconnectTimer) return
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  const settlePendingConnects = (error = null) => {
    const waiters = pendingConnects
    pendingConnects = []
    waiters.forEach(({ resolve, reject }) => {
      if (error) {
        reject(error)
        return
      }
      resolve(true)
    })
  }

  const createConnectPromise = () => new Promise((resolve, reject) => {
    pendingConnects.push({ resolve, reject })
  })

  const scheduleReconnect = () => {
    clearReconnectTimer()
    reconnectTimer = setTimeout(() => {
      connect().catch(() => {})
    }, 3000)
  }

  const connect = () => {
    const auth = getAuth()
    if (!auth?.token) {
      return Promise.reject(new Error('未登录'))
    }
    if (socket?.readyState === WebSocket.OPEN) {
      return Promise.resolve(true)
    }
    if (socket?.readyState === WebSocket.CONNECTING) {
      return createConnectPromise()
    }

    manualClose = false
    const connectPromise = createConnectPromise()
    const currentSocket = new WebSocket(buildSocketUrl(auth.token))
    socket = currentSocket

    currentSocket.onopen = () => {
      if (socket !== currentSocket) return
      clearReconnectTimer()
      onOpen?.()
      settlePendingConnects()
    }

    currentSocket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data)
        onMessage?.(payload)
      } catch (error) {
        onError?.(error)
      }
    }

    currentSocket.onerror = (event) => {
      if (socket !== currentSocket) return
      onError?.(event)
      settlePendingConnects(new Error('客服连接失败'))
    }

    currentSocket.onclose = () => {
      const isCurrentSocket = socket === currentSocket
      if (isCurrentSocket) {
        socket = null
      }
      onClose?.()
      settlePendingConnects(new Error('客服连接已关闭'))
      if (isCurrentSocket && !manualClose) {
        scheduleReconnect()
      }
    }

    return connectPromise
  }

  const disconnect = () => {
    manualClose = true
    clearReconnectTimer()
    if (!socket) return
    socket.close()
    socket = null
  }

  const send = (payload) => {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      return false
    }
    socket.send(JSON.stringify(payload))
    return true
  }

  const isConnected = () => socket?.readyState === WebSocket.OPEN

  return {
    connect,
    disconnect,
    send,
    isConnected,
  }
}
