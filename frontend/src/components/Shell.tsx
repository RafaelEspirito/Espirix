import { useState } from 'react'
import type { ReactNode } from 'react'
import { Activity, ArrowDownLeft, ArrowUpRight, BarChart3, Building2, ChevronDown, Code2, Eye, EyeOff, FileSpreadsheet, GitBranch, LayoutDashboard, Menu, Moon, Settings2, ShieldAlert, Sun, WalletCards, ReceiptText, X } from 'lucide-react'
import { useAuth } from '../contexts/Auth'
import { api, type EmpresaInfo } from '../services/api'
import { useEffect } from 'react'
import { LogOut } from 'lucide-react'
import { usePreferences } from '../contexts/Preferences'
import type { TipoOperacao } from '../types/api'

export type PageName = 'dashboard' | 'titulos' | 'despesas' | 'clientes' | 'importacoes' | 'configuracoes'
export const pageNames: Record<PageName, string> = {
  dashboard: 'Visão geral', titulos: 'Títulos financeiros', despesas: 'Despesas manuais', clientes: 'Clientes e fornecedores',
  importacoes: 'Importações', configuracoes: 'Preferências',
}
const links = [
  { id: 'dashboard', label: 'Visão geral', icon: LayoutDashboard },
  { id: 'titulos', label: 'Títulos financeiros', icon: WalletCards },
  { id: 'despesas', label: 'Despesas manuais', icon: ReceiptText },
  { id: 'clientes', label: 'Clientes e fornecedores', icon: Building2 },
  { id: 'importacoes', label: 'Importações', icon: FileSpreadsheet },
  { id: 'configuracoes', label: 'Preferências', icon: Settings2 },
] as const

export function Shell({ page, onPage, children }: { page: PageName; onPage: (p: PageName) => void; children: ReactNode }) {
  const { theme, toggleTheme, privacidade, togglePrivacidade, tipo, setTipo } = usePreferences()
  const { user, logout } = useAuth()
  const [companies,setCompanies]=useState<EmpresaInfo[]>([])
  useEffect(()=>{if(user?.perfil==='ADMIN_MASTER') void api.empresas().then(setCompanies).catch(()=>setCompanies([]))},[user?.perfil])
  const selectCompany=async(id:number)=>{if(!id || id===user?.empresa.id)return;await api.workspace(id);window.location.reload()}

  const [mobileOpen, setMobileOpen] = useState(false)
  const [sidebarCollapsed,setSidebarCollapsed]=useState(false)
  const navigate = (next: PageName) => { setMobileOpen(false); onPage(next) }
  return <div className={`layout ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
    {mobileOpen && <button className="mobile-shade" aria-label="Fechar menu" onClick={() => setMobileOpen(false)}/>}
    <aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}>
      <div className="flex items-center gap-3 px-6 pt-8 pb-10"><div className="brand-icon"><BarChart3 size={22}/></div><div className="leading-tight"><div className="font-extrabold text-[1.38rem] tracking-tight">espirix<span className="text-cyan-400">.</span></div><div className="text-[0.63rem] uppercase tracking-[.24em] muted mt-1">Espirito Labs</div></div><button className="ml-auto lg:hidden icon-button" aria-label="Fechar menu" onClick={() => setMobileOpen(false)}><X size={18}/></button></div>
      <p className="sidebar-heading">WORKSPACE</p>
      <nav aria-label="Navegação principal" className="px-3 space-y-1">{links.map(({ id, label, icon: Icon }) => <button type="button" key={id} onClick={() => navigate(id)} className={`nav-item ${page === id ? 'nav-active' : ''}`}><Icon size={18} strokeWidth={1.85}/><span>{label}</span>{page === id && <span className="nav-dot"/>}</button>)}</nav>
      <div className="mt-auto px-5 pt-8 pb-5 space-y-3"><div className="sidebar-info"><div className="flex items-center gap-2 text-sm font-semibold"><Activity size={16} className="text-cyan-400"/> Acesso por empresa</div><p className="text-xs muted mt-2">Acesso restrito aos dados do ambiente selecionado.</p></div><a href="https://github.com/RafaelEspirito/Espirix" target="_blank" rel="noopener noreferrer" className="nav-item !px-3"><GitBranch size={17}/> Código no GitHub <ArrowUpRight size={14} className="ml-auto"/></a><div className="flex items-center gap-2 text-[0.68rem] muted px-3"><Code2 size={13}/> v0.6 · Gestão financeira</div></div>
    </aside>
    <div className="main-area"><header className="topbar"><div className="flex items-center gap-3 min-w-0"><button className="icon-button lg:hidden" onClick={() => setMobileOpen(true)} aria-label="Abrir menu"><Menu size={20}/></button><button className="icon-button hidden lg:inline-flex" onClick={()=>setSidebarCollapsed(v=>!v)} aria-label={sidebarCollapsed?"Exibir barra lateral":"Ocultar barra lateral"} title={sidebarCollapsed?"Exibir barra lateral":"Ocultar barra lateral"}><Menu size={20}/></button><div className="hidden sm:flex items-center gap-2 muted text-xs"><span>Workspace</span><ChevronDown size={13} className="-rotate-90"/><span className="font-semibold text-[var(--text)]">{pageNames[page]}</span></div><div className="sm:hidden font-semibold">{pageNames[page]}</div></div><div className="flex items-center gap-2"><div className="hidden sm:flex items-center gap-2 mr-2 px-3 py-2 rounded-full status-local"><span className="w-2 h-2 rounded-full bg-emerald-500"/> {user?.empresa.nome}</div><button className="icon-button" onClick={togglePrivacidade} aria-label={privacidade ? 'Exibir dados' : 'Ocultar dados'} title={privacidade ? 'Mostrar valores' : 'Ocultar valores'}>{privacidade ? <EyeOff size={19}/> : <Eye size={19}/>}</button><button className="icon-button" onClick={toggleTheme} aria-label="Alternar tema" title="Alternar tema">{theme === 'dark' ? <Sun size={19}/> : <Moon size={19}/>}</button>{user?.perfil === 'ADMIN_MASTER' && <select className="input !w-auto text-xs hidden md:block" aria-label="Selecionar ambiente" value={user.empresa.id} onChange={e=>void selectCompany(Number(e.target.value))}>{companies.map(c=><option key={c.id} value={c.id}>{c.nome}</option>)}</select>}
        <span className="text-xs muted hidden lg:inline" title={user?.email}>{user?.nome}</span>
        <button className="icon-button" aria-label="Sair" title="Sair" onClick={()=>void logout()}><LogOut size={18}/></button>
        <div className="profile-avatar" title={user?.email}>{user?.nome.slice(0,2).toUpperCase()}</div></div></header>
      <main className="page-content"><div className="max-w-[1440px] mx-auto"><div className="flex flex-wrap items-center justify-between gap-3 mb-7"><div className="inline-flex items-center gap-2 muted text-xs"><ShieldAlert size={14}/><span>Acesso autenticado · {user?.empresa.nome} · {user?.perfil}</span></div>{page !== 'configuracoes' && page !== 'despesas' && <OperationSwitch value={tipo} onChange={setTipo}/>}</div>{children}<footer className="app-footer">Espirix <span className="mx-2">•</span> Espirito Labs</footer></div></main></div>
  </div>
}
export function OperationSwitch({ value, onChange }: { value: TipoOperacao; onChange: (v: TipoOperacao) => void }) {
  return <div className="operation-switch" aria-label="Tipo de operação"><button className={value === 'RECEBER' ? 'operation-active' : ''} onClick={() => onChange('RECEBER')}><ArrowDownLeft size={15}/> Receitas</button><button className={value === 'PAGAR' ? 'operation-active' : ''} onClick={() => onChange('PAGAR')}><ArrowUpRight size={15}/> Despesas</button></div>
}
