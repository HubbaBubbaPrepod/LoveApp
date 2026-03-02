import { AppBar, TitlePortal, UserMenu, Logout, useUserMenu } from 'react-admin'
import { MenuItem, ListItemIcon, ListItemText, Typography, Box, Chip } from '@mui/material'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import { useSelector } from 'react-redux'
import { selectAdminUsername } from '../store/authSlice'

const AdminAppBar = () => {
  const username = useSelector(selectAdminUsername)

  return (
    <AppBar
      sx={{
        background: 'linear-gradient(90deg, #0f3460 0%, #16213e 50%, #1a1a2e 100%)',
        borderBottom: '1px solid rgba(233,30,99,0.3)',
        boxShadow: '0 2px 20px rgba(0,0,0,0.4)',
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Box
          sx={{
            width: 32, height: 32, borderRadius: '50%',
            background: 'linear-gradient(135deg, #e91e63, #7c4dff)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 16,
          }}
        >
          💖
        </Box>
        <Typography variant="h6" sx={{ fontWeight: 800, fontSize: 18 }}>
          LoveApp
          <Typography component="span" sx={{ color: '#e91e63', fontWeight: 800 }}>
            {' '}Admin
          </Typography>
        </Typography>
      </Box>

      <TitlePortal />

      <Chip
        label={`@${username || 'admin'}`}
        size="small"
        icon={<AccountCircleIcon sx={{ fontSize: 14 }} />}
        sx={{
          backgroundColor: 'rgba(233,30,99,0.15)',
          color: '#f06292',
          border: '1px solid rgba(233,30,99,0.3)',
          mr: 1,
          fontWeight: 600,
        }}
      />

      <UserMenu>
        <Logout />
      </UserMenu>
    </AppBar>
  )
}

export default AdminAppBar
