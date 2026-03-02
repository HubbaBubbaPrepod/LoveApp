import axios from 'axios'
import store from '../store'
import { loginStart, loginSuccess, loginFailure, logout } from '../store/authSlice'

const TOKEN_KEY = 'loveapp_admin_token'

const authProvider = {
  // Called when user submits login form
  login: async ({ username, password }) => {
    store.dispatch(loginStart())
    try {
      const res = await axios.post('/api/admin/login', { username, password })
      const { token, username: user } = res.data.data
      store.dispatch(loginSuccess({ token, username: user }))
      return Promise.resolve()
    } catch (err) {
      const msg = err.response?.data?.message || 'Invalid credentials'
      store.dispatch(loginFailure(msg))
      return Promise.reject(new Error(msg))
    }
  },

  logout: () => {
    store.dispatch(logout())
    return Promise.resolve()
  },

  checkAuth: () => {
    const token = localStorage.getItem(TOKEN_KEY)
    return token ? Promise.resolve() : Promise.reject()
  },

  checkError: ({ status }) => {
    if (status === 401 || status === 403) {
      store.dispatch(logout())
      return Promise.reject()
    }
    return Promise.resolve()
  },

  getPermissions: () => Promise.resolve('admin'),

  getIdentity: () => {
    const username = localStorage.getItem('loveapp_admin_user') || 'Admin'
    return Promise.resolve({ id: 'admin', fullName: username, avatar: null })
  },
}

export default authProvider
