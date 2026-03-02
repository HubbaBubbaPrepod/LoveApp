import { createSlice } from '@reduxjs/toolkit'

const uiSlice = createSlice({
  name: 'ui',
  initialState: {
    sidebarOpen: true,
    notifications: [],
  },
  reducers: {
    toggleSidebar (state) { state.sidebarOpen = !state.sidebarOpen },
    setSidebar    (state, { payload }) { state.sidebarOpen = payload },
    addNotification (state, { payload }) {
      state.notifications.push({ id: Date.now(), ...payload })
    },
    removeNotification (state, { payload: id }) {
      state.notifications = state.notifications.filter(n => n.id !== id)
    },
  },
})

export const { toggleSidebar, setSidebar, addNotification, removeNotification } = uiSlice.actions
export default uiSlice.reducer
