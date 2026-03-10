import { createSlice } from '@reduxjs/toolkit'

const TOKEN_KEY = 'loveapp_admin_token'
const USER_KEY  = 'loveapp_admin_user'

const stored = () => ({
  token:    localStorage.getItem(TOKEN_KEY) || null,
  username: localStorage.getItem(USER_KEY)  || null,
})

const authSlice = createSlice({
  name: 'auth',
  initialState: { ...stored(), loading: false, error: null },
  reducers: {
    loginStart (state) { state.loading = true; state.error = null },
    loginSuccess (state, { payload: { token, username } }) {
      state.loading  = false
      state.token    = token
      state.username = username
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY,  username)
    },
    loginFailure (state, { payload }) {
      state.loading = false
      state.error   = payload
    },
    logout (state) {
      state.token    = null
      state.username = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})

export const { loginStart, loginSuccess, loginFailure, logout } = authSlice.actions
export default authSlice.reducer
export const selectAdminToken    = s => s.auth.token
export const selectAdminUsername = s => s.auth.username
export const selectIsLoggedIn    = s => !!s.auth.token
