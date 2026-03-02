import { createTheme } from '@mui/material/styles'

const adminTheme = createTheme({
  palette: {
    mode: 'dark',
    primary:   { main: '#e91e63', light: '#f06292', dark: '#c2185b' },
    secondary: { main: '#7c4dff', light: '#b47cff', dark: '#5600e8' },
    background: {
      default: '#0f1117',
      paper:   '#1a1a2e',
    },
    surface:   { main: '#16213e' },
    success:   { main: '#4caf50' },
    warning:   { main: '#ff9800' },
    error:     { main: '#f44336' },
    info:      { main: '#2196f3' },
    text: {
      primary:   '#e2e8f0',
      secondary: '#94a3b8',
      disabled:  '#475569',
    },
    divider: 'rgba(255,255,255,0.08)',
  },
  typography: {
    fontFamily: "'Inter', system-ui, sans-serif",
    h4: { fontWeight: 700 },
    h5: { fontWeight: 600 },
    h6: { fontWeight: 600 },
    subtitle1: { fontWeight: 500 },
    button:    { textTransform: 'none', fontWeight: 600 },
  },
  shape: { borderRadius: 10 },
  components: {
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          backgroundColor: '#1a1a2e',
          border: '1px solid rgba(255,255,255,0.05)',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: '#1a1a2e',
          backgroundImage: 'none',
          border: '1px solid rgba(255,255,255,0.05)',
        },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-head': {
            backgroundColor: '#0f3460',
            color: '#e2e8f0',
            fontWeight: 700,
          },
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          '&:hover': { backgroundColor: 'rgba(233,30,99,0.06)' },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: { borderColor: 'rgba(255,255,255,0.05)' },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600 },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: { borderRadius: 8, textTransform: 'none', fontWeight: 600 },
        containedPrimary: {
          background: 'linear-gradient(135deg, #e91e63 0%, #c2185b 100%)',
          '&:hover': { background: 'linear-gradient(135deg, #f06292 0%, #e91e63 100%)' },
        },
      },
    },
    MuiTextField: {
      defaultProps: { size: 'small', variant: 'outlined' },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          '& fieldset': { borderColor: 'rgba(255,255,255,0.15)' },
          '&:hover fieldset': { borderColor: 'rgba(255,255,255,0.3)' },
          '&.Mui-focused fieldset': { borderColor: '#e91e63' },
        },
      },
    },
  },
})

export default adminTheme
