import { useAuthStore } from '../stores/authStore.js'

export default {
  mounted(el, binding) {
    const authStore = useAuthStore()
    const { value } = binding

    if (value && typeof value === 'string') {
      const hasPermission = authStore.hasPermission(value)

      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  },
  updated(el, binding) {
    const authStore = useAuthStore()
    const { value } = binding
    if (value && typeof value === 'string') {
      const hasPermission = authStore.hasPermission(value)
      if (!hasPermission && el.parentNode) {
        el.parentNode.removeChild(el)
      }
    }
  }
}
