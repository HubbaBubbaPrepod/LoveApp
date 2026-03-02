import { Menu } from 'react-admin'
import DashboardIcon      from '@mui/icons-material/Dashboard'
import PeopleIcon         from '@mui/icons-material/People'
import DirectionsRunIcon  from '@mui/icons-material/DirectionsRun'
import MoodIcon           from '@mui/icons-material/Mood'
import NotesIcon          from '@mui/icons-material/Notes'
import CardGiftcardIcon   from '@mui/icons-material/CardGiftcard'
import { Box, Divider, Typography } from '@mui/material'

const AdminMenu = () => (
  <Menu
    sx={{
      backgroundColor: '#16213e',
      borderRight: '1px solid rgba(255,255,255,0.06)',
      height: '100%',
      pt: 2,
      '& .RaMenu-item': {
        borderRadius: 2,
        mx: 1,
        mb: 0.5,
        '&.RaMenuItemLink-active': {
          background: 'linear-gradient(90deg, rgba(233,30,99,0.2), rgba(124,77,255,0.1))',
          borderLeft: '3px solid #e91e63',
          '& .MuiSvgIcon-root': { color: '#e91e63' },
          '& .MuiTypography-root': { color: '#fff', fontWeight: 700 },
        },
        '&:hover': {
          background: 'rgba(255,255,255,0.05)',
        },
      },
    }}
  >
    <Box sx={{ px: 2, mb: 1 }}>
      <Typography variant="caption" sx={{ color: '#475569', fontWeight: 700, letterSpacing: 1.2 }}>
        ОБЗОР
      </Typography>
    </Box>
    <Menu.DashboardItem primaryText="Дашборд" leftIcon={<DashboardIcon />} />

    <Box sx={{ px: 2, mt: 2, mb: 1 }}>
      <Typography variant="caption" sx={{ color: '#475569', fontWeight: 700, letterSpacing: 1.2 }}>
        ДАННЫЕ
      </Typography>
    </Box>
    <Menu.Item to="/users"      primaryText="Пользователи"  leftIcon={<PeopleIcon />} />
    <Menu.Item to="/activities" primaryText="Активности"    leftIcon={<DirectionsRunIcon />} />
    <Menu.Item to="/moods"      primaryText="Настроения"    leftIcon={<MoodIcon />} />
    <Menu.Item to="/notes"      primaryText="Заметки"       leftIcon={<NotesIcon />} />
    <Menu.Item to="/wishes"     primaryText="Желания"       leftIcon={<CardGiftcardIcon />} />
  </Menu>
)

export default AdminMenu
