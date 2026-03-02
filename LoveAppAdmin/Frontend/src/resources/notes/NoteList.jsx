import {
  List, Datagrid, TextField, DateField, BooleanField,
  SearchInput, FunctionField, DeleteButton, useRecordContext,
} from 'react-admin'
import { Chip, Box } from '@mui/material'
import PushPinIcon from '@mui/icons-material/PushPin'

const noteFilters = [
  <SearchInput source="q" placeholder="Заголовок или текст..." alwaysOn />,
]

const ColorDot = () => {
  const record = useRecordContext()
  if (!record) return null
  return (
    <Box
      sx={{
        width: 16, height: 16, borderRadius: '50%',
        bgcolor: record.color || '#607d8b',
        border: '2px solid rgba(255,255,255,0.15)',
        display: 'inline-block',
      }}
    />
  )
}

export const NoteList = () => (
  <List
    title="Заметки"
    filters={noteFilters}
    perPage={25}
    sort={{ field: 'created_at', order: 'DESC' }}
  >
    <Datagrid bulkActionButtons={false}>
      <FunctionField label="Цвет"          render={() => <ColorDot />} />
      <TextField     source="title"        label="Заголовок"    sortable={false} />
      <TextField     source="display_name" label="Пользователь" sortable={false} />
      <TextField     source="username"     label="Username"     sortable={false} />
      <FunctionField label="Закреплена" render={r => r?.is_pinned ? (
        <PushPinIcon sx={{ color: '#e91e63', fontSize: 16 }} />
      ) : null} />
      <DateField     source="created_at"   label="Дата"         options={{ dateStyle: 'medium' }} />
      <DeleteButton  label="" mutationMode="pessimistic" />
    </Datagrid>
  </List>
)

export default NoteList
