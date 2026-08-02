import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/authStore.js'

export function useWebSocket() {
  const bagli = ref(false)
  const sonBildirim = ref(null)
  let stompClient = null
  let subscription = null

  const baglan = () => {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn || !authStore.kullanici?.sirketId) return

    const token = authStore.token || ''
    const SOCKET_URL = import.meta.env.VITE_WS_URL ||
      (window.location.protocol === 'https:' ? 'https://' : 'http://') + window.location.hostname + ':8081/ws' + (token ? '?token=' + encodeURIComponent(token) : '')

    import('sockjs-client').then(SockJS => {
      import('@stomp/stompjs').then(({ Client }) => {
        const socket = new SockJS.default(SOCKET_URL)
        stompClient = new Client({
          webSocketFactory: () => socket,
          reconnectDelay: 5000,
          onConnect: () => {
            bagli.value = true
            const sirketId = authStore.kullanici.sirketId
            subscription = stompClient.subscribe(`/topic/bildirimler/${sirketId}`, (msg) => {
              try {
                sonBildirim.value = JSON.parse(msg.body)
              } catch {}
            })
          },
          onDisconnect: () => { bagli.value = false }
        })
        stompClient.activate()
      })
    }).catch(() => {})
  }

  const baglantiKes = () => {
    if (subscription) subscription.unsubscribe()
    if (stompClient) stompClient.deactivate()
    bagli.value = false
  }

  onMounted(baglan)
  onUnmounted(baglantiKes)

  return { bagli, sonBildirim, baglan, baglantiKes }
}
