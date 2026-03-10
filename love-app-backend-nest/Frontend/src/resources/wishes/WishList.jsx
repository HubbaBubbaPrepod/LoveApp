import {
  List, Datagrid, TextField, DateField,
  SearchInput, FunctionField, DeleteButton, useRecordContext,
} from 'react-admin'
import { Chip } from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'

const wishFilters = [
  <SearchInput source="q" placeholder="Название или категория..." alwaysOn />,
]

const PRIORITY_COLOR = { low: '#4caf50', medium: '#ff9800', high: '#f44336' }
const PRIORITY_LABEL = { low: 'Низкий', medium: 'Средний', high: 'Высокий' }

const PriorityChip = () => {
  const record = useRecordContext()
  if (!record) return null
  const p = record.priority || 'low'
  const color = PRIORITY_COLOR[p] || '#64748b'
  return (
    <Chip
      label={PRIORITY_LABEL[p] || p}
      size="small"
      sx={{ bgcolor: `${color}1a`, color, border: `1px solid ${color}40`, fontWeight: 600 }}
    />
  )
}

const CompletedField = () => {
  const record = useRecordContext()
  if (!record) return null
  return record.is_completed
    ? <CheckCircleIcon sx={{ color: '#4caf50', fontSize: 18 }} />
    : null
}

export const WishList = () => (
  <List
    title="Желания"
    filters={wishFilters}
    perPage={25}
    sort={{ field: 'created_at', order: 'DESC' }}
  >
    <Datagrid bulkActionButtons={false}>
      <FunctionField source="title" label="Название" sortable={false} render={r => (
        <span style={{ display: 'block', maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontWeight: 600 }}>
          {r.title || '—'}
        </span>
      )} />
      <FunctionField label="Пользователь" sortable={false} render={r => (
        <div>
          <div style={{ fontWeight: 600, fontSize: 13 }}>{r.display_name || '—'}</div>
          <div style={{ color: '#64748b', fontSize: 11 }}>@{r.username}</div>
        </div>
      )} />
      <TextField     source="category"  label="Категория" sortable={false} />
      <FunctionField label="Приоритет"  render={() => <PriorityChip />} />
      <FunctionField label="✔"          render={() => <CompletedField />} />
      <DateField     source="created_at" label="Дата" options={{ dateStyle: 'medium' }} />
      <DeleteButton  label="" mutationMode="pessimistic" />
    </Datagrid>
  </List>
)

export default WishList
