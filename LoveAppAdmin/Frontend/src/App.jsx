import { Admin, Resource } from 'react-admin'

import dataProvider  from './api/dataProvider'
import authProvider  from './api/authProvider'
import adminTheme    from './theme/muiTheme'
import AdminLayout   from './layout/AdminLayout'

import Dashboard     from './pages/Dashboard'

import UserList      from './resources/users/UserList'
import UserShow      from './resources/users/UserShow'
import ActivityList  from './resources/activities/ActivityList'
import MoodList      from './resources/moods/MoodList'
import NoteList      from './resources/notes/NoteList'
import WishList      from './resources/wishes/WishList'

import PeopleIcon        from '@mui/icons-material/People'
import DirectionsRunIcon from '@mui/icons-material/DirectionsRun'
import MoodIcon          from '@mui/icons-material/Mood'
import NotesIcon         from '@mui/icons-material/Notes'
import CardGiftcardIcon  from '@mui/icons-material/CardGiftcard'

export default function App () {
  return (
    <Admin
      dataProvider={dataProvider}
      authProvider={authProvider}
      layout={AdminLayout}
      dashboard={Dashboard}
      theme={adminTheme}
      title="LoveApp Admin"
      requireAuth
      disableTelemetry
    >
      <Resource
        name="users"
        options={{ label: 'Пользователи' }}
        icon={PeopleIcon}
        list={UserList}
        show={UserShow}
      />
      <Resource
        name="activities"
        options={{ label: 'Активности' }}
        icon={DirectionsRunIcon}
        list={ActivityList}
      />
      <Resource
        name="moods"
        options={{ label: 'Настроения' }}
        icon={MoodIcon}
        list={MoodList}
      />
      <Resource
        name="notes"
        options={{ label: 'Заметки' }}
        icon={NotesIcon}
        list={NoteList}
      />
      <Resource
        name="wishes"
        options={{ label: 'Желания' }}
        icon={CardGiftcardIcon}
        list={WishList}
      />
    </Admin>
  )
}

