import { Layout } from 'react-admin'
import AdminAppBar from './AdminAppBar'
import AdminMenu   from './AdminMenu'

const AdminLayout = props => (
  <Layout
    {...props}
    appBar={AdminAppBar}
    menu={AdminMenu}
    sx={{
      '& .RaLayout-content': {
        backgroundColor: '#0f1117',
        minHeight: '100vh',
      },
      '& .RaLayout-appFrame': {
        backgroundColor: '#0f1117',
      },
    }}
  />
)

export default AdminLayout
