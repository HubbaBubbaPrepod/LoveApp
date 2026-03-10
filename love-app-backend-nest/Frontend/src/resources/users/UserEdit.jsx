import {
  Edit, SimpleForm, TextInput, SelectInput,
  TopToolbar, ListButton, ShowButton, DeleteButton,
  useRecordContext,
} from 'react-admin'
import { Box, Typography, Avatar, Divider } from '@mui/material'

const UserEditTitle = () => {
  const record = useRecordContext()
  return <span>{record ? `✏️ ${record.display_name || record.username}` : 'Редактирование'}</span>
}

const UserEditActions = () => (
  <TopToolbar>
    <ShowButton />
    <ListButton />
    <DeleteButton mutationMode="pessimistic" />
  </TopToolbar>
)

const roleChoices = [
  { id: 'user',  name: '👤 Пользователь' },
  { id: 'admin', name: '🛡️ Администратор' },
]

export const UserEdit = () => (
  <Edit
    title={<UserEditTitle />}
    actions={<UserEditActions />}
    mutationMode="pessimistic"
  >
    <SimpleForm
      sx={{
        '& .RaSimpleForm-form': { gap: 2 },
        maxWidth: 600,
        p: 3,
      }}
    >
      {/* header */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2, width: '100%' }}>
        <FunctionFieldAvatar />
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 700, color: '#e2e8f0' }}>Редактирование пользователя</Typography>
          <Typography variant="body2" sx={{ color: '#64748b' }}>Изменение данных и роли</Typography>
        </Box>
      </Box>

      <Divider sx={{ borderColor: 'rgba(255,255,255,0.06)', width: '100%', mb: 1 }} />

      <Typography variant="subtitle2" sx={{ color: '#e91e63', fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1, width: '100%' }}>
        Основные данные
      </Typography>

      <TextInput source="display_name" label="Отображаемое имя" fullWidth />
      <TextInput source="email"        label="Email"            fullWidth />

      <Divider sx={{ borderColor: 'rgba(255,255,255,0.06)', width: '100%', mt: 1, mb: 1 }} />

      <Typography variant="subtitle2" sx={{ color: '#7c4dff', fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1, width: '100%' }}>
        Роль и права доступа
      </Typography>

      <SelectInput
        source="role"
        label="Роль"
        choices={roleChoices}
        fullWidth
        helperText="Администратор может войти в панель управления"
        sx={{
          '& .MuiSelect-select': { fontWeight: 600 },
        }}
      />
    </SimpleForm>
  </Edit>
)

/* little helper — shows avatar, can't use hooks at top level inside JSX */
function FunctionFieldAvatar () {
  const record = useRecordContext()
  if (!record) return null
  return (
    <Avatar sx={{ width: 48, height: 48, fontSize: 20, bgcolor: '#e91e63', fontWeight: 800 }}>
      {(record.display_name || record.username || '?')[0].toUpperCase()}
    </Avatar>
  )
}

export default UserEdit
