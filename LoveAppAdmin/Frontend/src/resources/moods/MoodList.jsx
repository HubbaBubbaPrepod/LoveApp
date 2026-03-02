import {
  List, Datagrid, TextField, DateField,
  SearchInput, FunctionField, DeleteButton, useRecordContext,
} from 'react-admin'
import { Chip } from '@mui/material'

const moodFilters = [
  <SearchInput source="q" placeholder="Тип настроения..." alwaysOn />,
]

const MOOD_EMOJI = {
  great: '😄', отлично: '😄',
  good: '🙂',  хорошо: '🙂',
  okay: '😐',  нормально: '😐',
  bad: '😔',   плохо: '😔',
  terrible: '😢', ужасно: '😢',
}
const MOOD_COLOR = {
  great: '#4caf50', good: '#8bc34a', okay: '#ff9800', bad: '#f44336', terrible: '#9c27b0',
  отлично: '#4caf50', хорошо: '#8bc34a', нормально: '#ff9800', плохо: '#f44336', ужасно: '#9c27b0',
}

const MoodChip = () => {
  const record = useRecordContext()
  if (!record) return null
  const type  = (record.mood_type || '').toLowerCase()
  const emoji = MOOD_EMOJI[type] || '💬'
  const color = MOOD_COLOR[type] || '#64748b'
  return (
    <Chip
      label={`${emoji} ${record.mood_type}`}
      size="small"
      sx={{
        bgcolor: `${color}1a`, color,
        border: `1px solid ${color}40`,
        fontWeight: 600,
      }}
    />
  )
}

export const MoodList = () => (
  <List
    title="Настроения"
    filters={moodFilters}
    perPage={25}
    sort={{ field: 'created_at', order: 'DESC' }}
  >
    <Datagrid bulkActionButtons={false}>
      <FunctionField label="Настроение"  render={() => <MoodChip />} />
      <TextField     source="display_name" label="Пользователь" sortable={false} />
      <TextField     source="username"     label="Username"     sortable={false} />
      <TextField     source="note"         label="Заметка"      sortable={false} />
      <DateField     source="created_at"   label="Дата"         options={{ dateStyle: 'medium', timeStyle: 'short' }} />
      <DeleteButton  label="" mutationMode="pessimistic" />
    </Datagrid>
  </List>
)

export default MoodList
