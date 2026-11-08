import { useEffect, useState } from 'react'
import { AuthProvider, useAuth } from './contexts/Auth'
import { Login } from './pages/Login'
import { PreferencesProvider } from './contexts/Preferences'
import { Shell } from './components/Shell'
import type { PageName } from './components/Shell'
import { Dashboard } from './pages/Dashboard'
import { Titulos } from './pages/Titulos'
import { Clientes } from './pages/Clientes'
import { Importacoes } from './pages/Importacoes'
import { Configuracoes } from './pages/Configuracoes'

const pages: PageName[] = ['dashboard', 'titulos', 'clientes', 'importacoes', 'configuracoes']
function currentLocation(): { page: PageName; search: string } {
  const raw = window.location.hash.replace(/^#\/?/, '')
  const [route = '', query = ''] = raw.split('?')
  const page = pages.includes(route as PageName) ? route as PageName : 'dashboard'
  return { page, search: new URLSearchParams(query).get('busca') || '' }
}

function AppContent() {
  const { user, loading } = useAuth()
  const [location, setLocation] = useState(currentLocation)
  useEffect(() => {
    const update = () => setLocation(currentLocation())
    window.addEventListener('hashchange', update)
    return () => window.removeEventListener('hashchange', update)
  }, [])
  function navigate(page: PageName, query?: string) {
    const qs = query ? `?busca=${encodeURIComponent(query)}` : ''
    const hash = `#/${page}${qs}`
    if (window.location.hash === hash) setLocation(currentLocation())
    else window.location.hash = hash
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  if (loading) return <div className="min-h-screen flex items-center justify-center">Carregando Espirix...</div>
  if (!user) return <Login/>
  return <Shell key={user.empresa.id} page={location.page} onPage={navigate}>
    {location.page === 'dashboard' && <Dashboard navigate={navigate}/>}
    {location.page === 'titulos' && <Titulos initialSearch={location.search}/>}
    {location.page === 'clientes' && <Clientes navigate={navigate}/>}
    {location.page === 'importacoes' && <Importacoes/>}
    {location.page === 'configuracoes' && <Configuracoes/>}
  </Shell>
}
export default function App() { return <PreferencesProvider><AuthProvider><AppContent/></AuthProvider></PreferencesProvider> }
