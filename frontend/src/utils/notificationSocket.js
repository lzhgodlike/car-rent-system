import { getAuth } from './auth'

function buildSocketUrl(token) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/notifications?token=${encodeURIComponent(token)}`
}

export function createNotificationSocket({ onOpen, onClose, onMessage, onError } = {}) {
  let socket = null
  let reconnectTimer = null
  let pingTimer = null
  let manualClose = false
  let pendingConnects = []

  const clearReconnectTimer = () => {
    if (!reconnectTimer) return
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  const clearPingTimer = () => {
    if (!pingTimer) return
    clearInterval(pingTimer)
    pingTimer = null
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

  const sendPing = () => {
    if (!socket || socket.readyState !== WebSocket.OPEN) return
    socket.send(JSON.stringify({ type: 'PING' }))
  }

  const startPing = () => {
    clearPingTimer()
    pingTimer = setInterval(() => {
      sendPing()
    }, 25000)
  }

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
      startPing()
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
      settlePendingConnects(new Error('通知连接失败'))
    }

    currentSocket.onclose = () => {
      const isCurrentSocket = socket === currentSocket
      if (isCurrentSocket) {
        socket = null
      }
      clearPingTimer()
      onClose?.()
      settlePendingConnects(new Error('通知连接已关闭'))
      if (isCurrentSocket && !manualClose) {
        scheduleReconnect()
      }
    }

    return connectPromise
  }

  const disconnect = () => {
    manualClose = true
    clearReconnectTimer()
    clearPingTimer()
    if (!socket) return
    socket.close()
    socket = null
  }

  const isConnected = () => socket?.readyState === WebSocket.OPEN

  return {
    connect,
    disconnect,
    isConnected,
  }
}
