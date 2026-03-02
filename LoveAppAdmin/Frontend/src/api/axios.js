import axios from 'axios'

const BASE_URL = '/api'
const TOKEN_KEY = 'loveapp_admin_token'

const api = axios.create({ baseURL: BASE_URL })

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      localStorage.removeItem(TOKEN_KEY)
      window.location.href = '/#/login'
    }
    return Promise.reject(err)
  }
)

export default api
