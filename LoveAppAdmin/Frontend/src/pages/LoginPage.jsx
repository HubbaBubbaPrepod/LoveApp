import { useState } from 'react'
import { useLogin, useNotify } from 'react-admin'
import {
  Box, Button, TextField, Typography, CircularProgress, InputAdornment, IconButton,
} from '@mui/material'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import LockIcon          from '@mui/icons-material/Lock'
import VisibilityIcon    from '@mui/icons-material/Visibility'
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff'

export default function LoginPage () {
  const login  = useLogin()
  const notify = useNotify()

  const [username,    setUsername]    = useState('')
  const [password,    setPassword]    = useState('')
  const [showPass,    setShowPass]    = useState(false)
  const [loading,     setLoading]     = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      await login({ username, password })
    } catch (err) {
      notify(err?.message || 'Invalid credentials', { type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        width: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #0f1117 0%, #0f3460 50%, #16213e 100%)',
      }}
    >
      {/* Decorative blobs */}
      <Box sx={{
        position: 'fixed', top: '-10%', left: '-10%',
        width: 400, height: 400, borderRadius: '50%',
        background: 'radial-gradient(circle, rgba(233,30,99,0.15) 0%, transparent 70%)',
        pointerEvents: 'none',
      }} />
      <Box sx={{
        position: 'fixed', bottom: '-10%', right: '-10%',
        width: 500, height: 500, borderRadius: '50%',
        background: 'radial-gradient(circle, rgba(124,77,255,0.12) 0%, transparent 70%)',
        pointerEvents: 'none',
      }} />

      <Box
        component="form"
        onSubmit={handleSubmit}
        sx={{
          width: 380,
          backgroundColor: 'rgba(26,26,46,0.9)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(233,30,99,0.2)',
          borderRadius: 4,
          p: 5,
          boxShadow: '0 24px 64px rgba(0,0,0,0.5)',
          display: 'flex',
          flexDirection: 'column',
          gap: 2.5,
        }}
      >
        {/* Logo */}
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 1 }}>
          <Box
            sx={{
              width: 56, height: 56, borderRadius: '50%',
              background: 'linear-gradient(135deg, #e91e63, #7c4dff)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 28, mb: 1.5,
              boxShadow: '0 8px 24px rgba(233,30,99,0.4)',
            }}
          >
            💖
          </Box>
          <Typography variant="h5" sx={{ fontWeight: 800, color: '#e2e8f0' }}>
            LoveApp <Typography component="span" sx={{ color: '#e91e63', fontWeight: 800 }}>Admin</Typography>
          </Typography>
          <Typography variant="body2" sx={{ color: '#64748b', mt: 0.5 }}>
            Войдите в панель управления
          </Typography>
        </Box>

        {/* Username or email */}
        <TextField
          label="Логин или Email"
          value={username}
          onChange={e => setUsername(e.target.value)}
          required
          autoFocus
          autoComplete="username"
          fullWidth
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <AccountCircleIcon sx={{ color: '#64748b', fontSize: 20 }} />
              </InputAdornment>
            ),
          }}
          sx={{ '& .MuiInputBase-root': { width: '100%' } }}
        />

        {/* Password */}
        <TextField
          label="Пароль"
          type={showPass ? 'text' : 'password'}
          value={password}
          onChange={e => setPassword(e.target.value)}
          required
          autoComplete="current-password"
          fullWidth
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <LockIcon sx={{ color: '#64748b', fontSize: 20 }} />
              </InputAdornment>
            ),
            endAdornment: (
              <InputAdornment position="end">
                <IconButton onClick={() => setShowPass(v => !v)} edge="end" size="small" tabIndex={-1}>
                  {showPass
                    ? <VisibilityOffIcon sx={{ color: '#64748b', fontSize: 20 }} />
                    : <VisibilityIcon    sx={{ color: '#64748b', fontSize: 20 }} />
                  }
                </IconButton>
              </InputAdornment>
            ),
          }}
          sx={{ '& .MuiInputBase-root': { width: '100%' } }}
        />

        {/* Submit */}
        <Button
          type="submit"
          variant="contained"
          fullWidth
          disabled={loading}
          sx={{
            mt: 1, py: 1.5, fontSize: 15, fontWeight: 700,
            background: 'linear-gradient(135deg, #e91e63 0%, #c2185b 100%)',
            '&:hover': { background: 'linear-gradient(135deg, #f06292 0%, #e91e63 100%)' },
            boxShadow: '0 4px 16px rgba(233,30,99,0.4)',
          }}
        >
          {loading ? <CircularProgress size={22} sx={{ color: '#fff' }} /> : 'Войти'}
        </Button>
      </Box>
    </Box>
  )
}
