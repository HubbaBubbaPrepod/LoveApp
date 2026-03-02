import {
  List, Datagrid, TextField, DateField, NumberField,
  SearchInput, FunctionField, ShowButton, DeleteButton,
  useRecordContext, BooleanField, ChipField,
} from 'react-admin'
import { Chip, Avatar, Box, Typography } from '@mui/material'
import PersonIcon from '@mui/icons-material/Person'
import FavoriteIcon from '@mui/icons-material/Favorite'

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
      label="В паре"
      size="small"
      sx={{ bgcolor: 'rgba(233,30,99,0.15)', color: '#f06292', border: '1px solid rgba(233,30,99,0.3)', fontWeight: 600 }}
    />
  ) : (
    <Chip label="Одиночка" size="small" sx={{ bgcolor: 'rgba(255,255,255,0.06)', color: '#64748b' }} />
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
      <TextField    source="display_name" label="Имя"      sortable={false} />
      <TextField    source="username"     label="Username" sortable={false} />
      <TextField    source="email"        label="Email"    sortable={false} />
      <TextField    source="gender"       label="Пол"      sortable={false} />
      <FunctionField label="Пара"    render={() => <PartnerBadge />} />
      <NumberField  source="activity_count" label="Актив." sortable={false} />
      <NumberField  source="mood_count"     label="Настр."  sortable={false} />
      <DateField    source="created_at"  label="Дата" options={{ day: '2-digit', month: 'short', year: 'numeric' }} />
      <ShowButton   label="" />
      <DeleteButton label="" mutationMode="pessimistic" />
    </Datagrid>
  </List>
)

export default UserList
