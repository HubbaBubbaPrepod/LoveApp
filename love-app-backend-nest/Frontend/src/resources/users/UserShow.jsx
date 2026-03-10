import {
  Show, SimpleShowLayout, TextField, DateField, NumberField,
  FunctionField, useRecordContext, TopToolbar, ListButton, DeleteButton,
} from 'react-admin'
import { Box, Typography, Chip, Avatar, Grid, Divider } from '@mui/material'
import FavoriteIcon from '@mui/icons-material/Favorite'

const UserTitle = () => {
  const record = useRecordContext()
  return <span>{record ? `👤 ${record.display_name || record.username}` : 'Пользователь'}</span>
}

const InfoRow = ({ label, children }) => (
  <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2, py: 1.5 }}>
    <Typography variant="caption" sx={{ color: '#64748b', width: 130, flexShrink: 0, fontWeight: 600, pt: 0.2 }}>
      {label}
    </Typography>
    <Box sx={{ flex: 1 }}>{children}</Box>
  </Box>
)

const UserActions = () => (
  <TopToolbar>
    <ListButton />
    <DeleteButton mutationMode="pessimistic" />
  </TopToolbar>
)

export const UserShow = () => (
  <Show title={<UserTitle />} actions={<UserActions />}>
    <Box sx={{ p: 3 }}>
      <FunctionField render={record => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
          <Avatar sx={{ width: 64, height: 64, fontSize: 28, bgcolor: '#e91e63', fontWeight: 800 }}>
            {(record?.display_name || record?.username || '?')[0].toUpperCase()}
          </Avatar>
          <Box>
            <Typography variant="h5" sx={{ fontWeight: 700, color: '#fff' }}>
              {record?.display_name}
            </Typography>
            <Typography variant="body2" sx={{ color: '#64748b' }}>
              @{record?.username} · {record?.email}
            </Typography>
          </Box>
          {record?.partner_user_id && (
            <Chip
              icon={<FavoriteIcon sx={{ fontSize: 14 }} />}
              label={`В паре: @${record.partner_username || record.partner_user_id}`}
              sx={{ bgcolor: 'rgba(233,30,99,0.15)', color: '#f06292', border: '1px solid rgba(233,30,99,0.3)', ml: 'auto', fontWeight: 600 }}
            />
          )}
        </Box>
      )} />

      <Divider sx={{ borderColor: 'rgba(255,255,255,0.06)', mb: 2 }} />

      <Grid container spacing={4}>
        <Grid item xs={12} md={6}>
          <Typography variant="subtitle2" sx={{ color: '#e91e63', fontWeight: 700, mb: 1.5, textTransform: 'uppercase', letterSpacing: 1 }}>
            Профиль
          </Typography>
          <InfoRow label="ID">
            <FunctionField render={r => (
              <Typography variant="body2" sx={{ fontFamily: 'monospace', color: '#94a3b8' }}>{r?.id}</Typography>
            )} />
          </InfoRow>
          <InfoRow label="Пол">
            <TextField source="gender" sx={{ color: '#e2e8f0' }} />
          </InfoRow>
          <InfoRow label="Дата регистрации">
            <DateField source="created_at" options={{ dateStyle: 'long' }} sx={{ color: '#e2e8f0' }} />
          </InfoRow>
        </Grid>

        <Grid item xs={12} md={6}>
          <Typography variant="subtitle2" sx={{ color: '#e91e63', fontWeight: 700, mb: 1.5, textTransform: 'uppercase', letterSpacing: 1 }}>
            Статистика
          </Typography>
          <InfoRow label="Активности">
            <NumberField source="activity_count" sx={{ color: '#00bcd4', fontWeight: 700, fontSize: 18 }} />
          </InfoRow>
          <InfoRow label="Настроения">
            <NumberField source="mood_count" sx={{ color: '#ff9800', fontWeight: 700, fontSize: 18 }} />
          </InfoRow>
          <InfoRow label="Заметки">
            <NumberField source="note_count" sx={{ color: '#4caf50', fontWeight: 700, fontSize: 18 }} />
          </InfoRow>
          <InfoRow label="Желания">
            <NumberField source="wish_count" sx={{ color: '#7c4dff', fontWeight: 700, fontSize: 18 }} />
          </InfoRow>
        </Grid>
      </Grid>
    </Box>
  </Show>
)

export default UserShow
