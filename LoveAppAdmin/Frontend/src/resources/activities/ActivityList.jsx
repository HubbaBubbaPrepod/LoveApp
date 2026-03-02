import {
  List, Datagrid, TextField, DateField, NumberField,
  SearchInput, FunctionField, DeleteButton, useRecordContext,
} from 'react-admin'
import { Chip, Box, Typography } from '@mui/material'

const activityFilters = [
  <SearchInput source="q" placeholder="Тип или название..." alwaysOn />,
]

const TYPE_COLORS = {
  work:     '#2196f3', computer: '#9c27b0', sport:   '#4caf50',
  food:     '#ff9800', walk:     '#00bcd4', sleep:   '#607d8b',
  reading:  '#f44336', social:   '#e91e63', relax:   '#8bc34a',
}

const TypeChip = () => {
  const record = useRecordContext()
  if (!record) return null
  const type = record.activity_type || ''
  const isCustom = type.startsWith('c_')
  const color = isCustom ? '#7c4dff' : (TYPE_COLORS[type] || '#64748b')
  const label = isCustom ? record.title || type : type
  return (
    <Chip
      label={label}
      size="small"
      sx={{
        bgcolor: `${color}1a`,
        color,
        border: `1px solid ${color}40`,
        fontWeight: 600,
        fontSize: 11,
        maxWidth: 140,
      }}
    />
  )
}

export const ActivityList = () => (
  <List
    title="Активности"
    filters={activityFilters}
    perPage={25}
    sort={{ field: 'created_at', order: 'DESC' }}
  >
    <Datagrid bulkActionButtons={false}>
      <FunctionField label="Тип"          render={() => <TypeChip />} />
      <TextField     source="display_name" label="Пользователь" sortable={false} />
      <TextField     source="username"     label="Username"     sortable={false} />
      <NumberField   source="duration_minutes" label="Мин."    sortable={false} />
      <TextField     source="start_time"   label="Начало"       sortable={false} />
      <TextField     source="note"         label="Заметка"      sortable={false} />
      <DateField     source="created_at"   label="Дата"         options={{ dateStyle: 'medium' }} />
      <DeleteButton  label="" mutationMode="pessimistic" />
    </Datagrid>
  </List>
)

export default ActivityList
