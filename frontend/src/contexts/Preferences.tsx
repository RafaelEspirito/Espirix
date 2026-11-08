import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import type { TipoOperacao } from '../types/api'

type Theme = 'dark' | 'light'
interface Prefs {
  theme: Theme
  toggleTheme: () => void
  privacidade: boolean
  togglePrivacidade: () => void
  tipo: TipoOperacao
  setTipo: (next: TipoOperacao) => void
}
const Context = createContext<Prefs | null>(null)
function stored<T extends string | boolean>(name: string, fallback: T): T {
  try {
    const value = localStorage.getItem(name)
    if (value === null) return fallback
    if (typeof fallback === 'boolean') return (value === 'true') as T
    return value as T
  } catch { return fallback }
}
export function PreferencesProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(() => stored('espirix-theme', 'dark'))
  const [privacidade, setPrivacidade] = useState<boolean>(() => stored<boolean>('espirix-privacy', false))
  const [tipo, setTipo] = useState<TipoOperacao>(() => stored('espirix-operation', 'RECEBER'))
  useEffect(() => { document.documentElement.dataset.theme = theme; localStorage.setItem('espirix-theme', theme) }, [theme])
  useEffect(() => { localStorage.setItem('espirix-privacy', String(privacidade)) }, [privacidade])
  useEffect(() => { localStorage.setItem('espirix-operation', tipo) }, [tipo])
  return <Context.Provider value={{ theme, toggleTheme: () => setTheme(v => v === 'dark' ? 'light' : 'dark'), privacidade, togglePrivacidade: () => setPrivacidade(v => !v), tipo, setTipo }}>{children}</Context.Provider>
}
export function usePreferences() {
  const prefs = useContext(Context)
  if (!prefs) throw new Error('PreferencesProvider ausente')
  return prefs
}
