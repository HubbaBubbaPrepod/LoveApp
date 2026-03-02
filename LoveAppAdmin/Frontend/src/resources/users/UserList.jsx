import {
  List, Datagrid, TextField, DateField,
  SearchInput, FunctionField, ShowButton, DeleteButton,
  useRecordContext, EditButton,
} from 'react-admin'
import { Chip, Avatar, Box } from '@mui/material'
import PersonIcon from '@mui/icons-material/Person'
import FavoriteIcon from '@mui/icons-material/Favorite'
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'

const userFilters = [
  <SearchInput source="q" placeholder="Поиск по имени, email..." alwaysOn />,
]

const AvatarField = () => {
  const record = useRecordContext()
  if (!record) return null
  const initials = (record.display_name || record.username || '?')[0].toUpperCase()
  return (
    <Avatar sx={{ width: 32, height: 32, fontSize: 13, bgcolor: '#e91e63', fontWeight: 700 }}>
      {initials}
    </Avatar>
  )
}

const PartnerBadge = () => {
  const record = useRecordContext()
  if (!record) return null
  return record.partner_user_id ? (
    <Chip
      icon={<FavoriteIcon sx={{ fontSize: 12 }} />}
      label={record.partner_username ? `@${record.partner_username}` : 'В паре'}
      size="small"
      sx={{ bgcolor: 'rgba(233,30,99,0.15)', color: '#f06292', border: '1px solid rgba(233,30,99,0.3)', fontWeight: 600 }}
    />
  ) : (
    <Chip label="Одиночка" size="small" sx={{ bgcolor: 'rgba(255,255,255,0.06)', color: '#64748b' }} />
  )
}

const RoleBadge = () => {
  const record = useRecordContext()
  if (!record) return null
  const isAdmin = record.role === 'admin' || record.role === 'superadmin'
  return (
    <Chip
      icon={isAdmin ? <AdminPanelSettingsIcon sx={{ fontSize: 12 }} /> : undefined}
      label={isAdmin ? 'Админ' : 'Пользователь'}
      size="small"
      sx={isAdmin
        ? { bgcolor: 'rgba(124,77,255,0.15)', color: '#b47cff', border: '1px solid rgba(124,77,255,0.3)', fontWeight: 600 }
        : { bgcolor: 'rgba(255,255,255,0.06)', color: '#64748b' }
      }
    />
  )
}

export const UserList = () => (
  <List
    title="Пользователи"
    filters={userFilters}
    perPage={25}
    sort={{ field: 'created_at', order: 'DESC' }}
    sx={{ '& .RaList-main': { boxShadow: 'none' } }}
  >
    <Datagrid
      bulkActionButtons={false}
      sx={{
        '& .RaDatagrid-headerCell': { fontWeight: 700, fontSize: 12, letterSpacing: 0.5 },
        '& .RaDatagrid-row': { cursor: 'pointer' },
      }}
    >
      <FunctionField label="" render={() => <AvatarField />} />
      <FunctionField label="Имя / Username" sortable={false} render={r => (
        <div>
          <div style={{ fontWeight: 600, fontSize: 13 }}>{r.display_name || '—'}</div>
          <div style={{ color: '#64748b', fontSize: 11 }}>@{r.username}</div>
        </div>
      )} />
      <TextField    source="email"  label="Email"  sortable={false} />
      <FunctionField label="Роль"   render={() => <RoleBadge />} />
      <FunctionField label="Пара"   render={() => <PartnerBadge />} />
      <DateField    source="created_at" label="Дата" options={{ day: '2-digit', month: 'short', year: 'numeric' }} />
      <ShowButton   label="" />
      <EditButton   label="" />
      <DeleteButton label="" mutationMode="pessimistic" />
    </Datagrid>
  </List>
)

export default UserList
