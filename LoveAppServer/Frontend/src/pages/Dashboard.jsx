import { useEffect, useState } from 'react'
import { Title } from 'react-admin'
import {
  Box, Grid, Typography, CircularProgress, Skeleton, Avatar,
} from '@mui/material'
import {
  LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Legend, Cell,
} from 'recharts'
import PeopleIcon        from '@mui/icons-material/People'
import FavoriteIcon      from '@mui/icons-material/Favorite'
import DirectionsRunIcon from '@mui/icons-material/DirectionsRun'
import MoodIcon          from '@mui/icons-material/Mood'
import NotesIcon         from '@mui/icons-material/Notes'
import CardGiftcardIcon  from '@mui/icons-material/CardGiftcard'
import CategoryIcon      from '@mui/icons-material/Category'
import api from '../api/axios'

/* ── Stat Card ─────────────────────────────────────────────────────────── */
const StatCard = ({ label, value, icon: Icon, gradient, isLoading }) => (
  <Box
    className="stat-card"
    sx={{
      background: gradient,
      minHeight: 110,
      position: 'relative',
      overflow: 'hidden',
      '&::after': {
        content: '""',
        position: 'absolute',
        right: -20, top: -20,
        width: 100, height: 100,
        borderRadius: '50%',
        background: 'rgba(255,255,255,0.06)',
      },
    }}
  >
    <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', zIndex: 1 }}>
      <Box>
        <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.7)', fontWeight: 500, mb: 0.5 }}>
          {label}
        </Typography>
        {isLoading
          ? <Skeleton width={64} height={44} sx={{ bgcolor: 'rgba(255,255,255,0.1)' }} />
          : (
            <Typography variant="h4" sx={{ fontWeight: 800, color: '#fff', lineHeight: 1 }}>
              {typeof value === 'number' ? value.toLocaleString() : value ?? '—'}
            </Typography>
          )
        }
      </Box>
      <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.15)', width: 44, height: 44 }}>
        <Icon sx={{ color: '#fff', fontSize: 22 }} />
      </Avatar>
    </Box>
  </Box>
)

/* ── Chart tooltip ─────────────────────────────────────────────────────── */
const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null
  return (
    <Box sx={{ bgcolor: '#1a1a2e', border: '1px solid rgba(233,30,99,0.3)', borderRadius: 2, p: 1.5 }}>
      <Typography variant="caption" sx={{ color: '#94a3b8', display: 'block', mb: 0.5 }}>{label}</Typography>
      {payload.map((p, i) => (
        <Typography key={i} variant="body2" sx={{ color: p.color, fontWeight: 700 }}>
          {p.value}
        </Typography>
      ))}
    </Box>
  )
}

/* ── Section wrapper ───────────────────────────────────────────────────── */
const Section = ({ title, children }) => (
  <Box className="glass-card" sx={{ p: 3, height: '100%' }}>
    <Typography className="section-title">{title}</Typography>
    {children}
  </Box>
)

const BAR_COLORS = [
  '#e91e63','#7c4dff','#00bcd4','#ff9800','#4caf50',
  '#f44336','#9c27b0','#03a9f4','#8bc34a','#ff5722',
]

/* ── Dashboard ─────────────────────────────────────────────────────────── */
export default function Dashboard () {
  const [stats,    setStats]    = useState(null)
  const [timeline, setTimeline] = useState([])
  const [byType,   setByType]   = useState([])
  const [loading,  setLoading]  = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const [sRes, tRes, bRes] = await Promise.all([
          api.get('/admin/stats'),
          api.get('/admin/stats/timeline'),
          api.get('/admin/stats/activities-by-type'),
        ])
        setStats(sRes.data.data)
        setTimeline((tRes.data.data || []).map(r => ({
          date: new Date(r.day).toLocaleDateString('ru', { day: 'numeric', month: 'short' }),
          Пользователи: r.count,
        })))
        setByType((bRes.data.data || []).map(r => ({
          name: r.activity_type,
          count: r.count,
        })))
      } catch (e) {
        console.error('Dashboard load error:', e)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  const cards = [
    { label: 'Пользователи',     key: 'users',        icon: PeopleIcon,        gradient: 'linear-gradient(135deg,#e91e63,#c2185b)' },
    { label: 'Пары',             key: 'couples',       icon: FavoriteIcon,      gradient: 'linear-gradient(135deg,#7c4dff,#5600e8)' },
    { label: 'Активности',       key: 'activities',   icon: DirectionsRunIcon, gradient: 'linear-gradient(135deg,#00bcd4,#0097a7)' },
    { label: 'Настроения',       key: 'moods',         icon: MoodIcon,          gradient: 'linear-gradient(135deg,#ff9800,#e65100)' },
    { label: 'Заметки',          key: 'notes',         icon: NotesIcon,         gradient: 'linear-gradient(135deg,#4caf50,#2e7d32)' },
    { label: 'Желания',          key: 'wishes',        icon: CardGiftcardIcon,  gradient: 'linear-gradient(135deg,#f44336,#b71c1c)' },
    { label: 'Кастомные типы',   key: 'custom_types',  icon: CategoryIcon,      gradient: 'linear-gradient(135deg,#9c27b0,#6a1b9a)' },
  ]

  return (
    <Box sx={{ p: { xs: 2, md: 3 }, maxWidth: 1400 }}>
      <Title title="Дашборд" />

      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 800, color: '#fff' }}>
          Добро пожаловать в{' '}
          <Typography component="span" sx={{ color: '#e91e63', fontWeight: 800 }}>
            LoveApp Admin
          </Typography>{' '}
          💖
        </Typography>
        <Typography variant="body2" sx={{ color: '#64748b', mt: 0.5 }}>
          Общая статистика приложения в реальном времени
        </Typography>
      </Box>

      {/* Stat cards */}
      <Grid container spacing={2.5} sx={{ mb: 4 }}>
        {cards.map(c => (
          <Grid key={c.key} item xs={12} sm={6} md={4} lg={3}>
            <StatCard
              label={c.label} value={stats?.[c.key]}
              icon={c.icon} gradient={c.gradient} isLoading={loading}
            />
          </Grid>
        ))}
      </Grid>

      {/* Charts */}
      <Grid container spacing={3}>
        {/* Registrations timeline */}
        <Grid item xs={12} lg={7}>
          <Section title="📈 Регистрации за 30 дней">
            {loading ? (
              <Skeleton variant="rectangular" height={260} sx={{ borderRadius: 2, bgcolor: 'rgba(255,255,255,0.05)' }} />
            ) : timeline.length === 0 ? (
              <Box sx={{ height: 260, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Typography sx={{ color: '#475569' }}>Нет данных</Typography>
              </Box>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={timeline}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                  <XAxis dataKey="date" tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} allowDecimals={false} />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend wrapperStyle={{ color: '#94a3b8', fontSize: 12 }} />
                  <Line
                    type="monotone" dataKey="Пользователи"
                    stroke="#e91e63" strokeWidth={2.5}
                    dot={{ fill: '#e91e63', r: 3 }}
                    activeDot={{ r: 6, fill: '#e91e63' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
          </Section>
        </Grid>

        {/* Activities by type */}
        <Grid item xs={12} lg={5}>
          <Section title="🏃 Активности по типам">
            {loading ? (
              <Skeleton variant="rectangular" height={260} sx={{ borderRadius: 2, bgcolor: 'rgba(255,255,255,0.05)' }} />
            ) : byType.length === 0 ? (
              <Box sx={{ height: 260, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Typography sx={{ color: '#475569' }}>Нет данных</Typography>
              </Box>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={byType} layout="vertical" margin={{ left: 40 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" horizontal={false} />
                  <XAxis type="number" tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} allowDecimals={false} />
                  <YAxis type="category" dataKey="name" tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} width={90} />
                  <Tooltip content={<CustomTooltip />} />
                  <Bar dataKey="count" radius={[0, 4, 4, 0]}>
                    {byType.map((_, i) => <Cell key={i} fill={BAR_COLORS[i % BAR_COLORS.length]} />)}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )}
          </Section>
        </Grid>
      </Grid>
    </Box>
  )
}
