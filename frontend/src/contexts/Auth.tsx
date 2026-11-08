import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { api, type Me } from '../services/api'
type AuthType = { user: Me | null; loading: boolean; login: (email: string, senha: string) => Promise<void>; logout: () => Promise<void>; refresh: () => Promise<void>; }
const Ctx = createContext<AuthType | null>(null)
export function AuthProvider({ children }: {children: ReactNode}) {
  const [user, setUser] = useState<Me|null>(null)
  const [loading, setLoading] = useState(true)
  const refresh = async () => { try { setUser(await api.me()) } catch { setUser(null) } }
  useEffect(() => { void refresh().finally(() => setLoading(false)) }, [])
  const login = async (email: string, senha: string) => { setUser(await api.login(email, senha)) }
  const logout = async () => { try { await api.logout() } finally { setUser(null) } }
  return <Ctx.Provider value={{ user, loading, login, logout, refresh }}>{children}</Ctx.Provider>
}
export function useAuth() { const value = useContext(Ctx); if (!value) throw new Error('AuthProvider ausente'); return value }
