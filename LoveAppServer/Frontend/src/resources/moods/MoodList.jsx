import {
  List, Datagrid, TextField,
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
      <FunctionField label="Пользователь" sortable={false} render={r => (
        <div>
          <div style={{ fontWeight: 600, fontSize: 13 }}>{r.display_name || '—'}</div>
          <div style={{ color: '#64748b', fontSize: 11 }}>@{r.username}</div>
        </div>
      )} />
      <FunctionField source="note" label="Заметка" sortable={false} render={r => (
        <span style={{ display: 'block', maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {r.note || '—'}
        </span>
      )} />
      <FunctionField   source="created_at"   label="Дата" render={r => {
        if (!r?.created_at) return '—'
        return new Date(r.created_at).toLocaleString('ru', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
      }} />
      <DeleteButton  label="" mutationMode="pessimistic" />
    </Datagrid>
  </List>
)

export default MoodList
