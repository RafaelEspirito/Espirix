import { useEffect } from 'react'
import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { AlertCircle, ArrowLeft, ArrowRight, CheckCircle2, ChevronLeft, ChevronRight, EyeOff, LoaderCircle, X } from 'lucide-react'
import { usePreferences } from '../contexts/Preferences'
import { inteiro } from '../lib/format'

export function Button({ children, variant = 'primary', className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'danger' | 'ghost' }) {
  return <button className={`btn btn-${variant} ${className}`} {...props}>{children}</button>
}
export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`surface ${className}`}>{children}</section>
}
export function CardTitle({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return <div className="flex flex-wrap items-start justify-between gap-4 mb-5"><div><h2 className="text-base font-semibold tracking-tight">{title}</h2>{subtitle && <p className="text-xs muted mt-1">{subtitle}</p>}</div>{action}</div>
}
export function SectionHeading({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4 mb-7"><div><h1 className="text-[1.85rem] leading-tight font-bold tracking-tight">{title}</h1>{subtitle && <p className="muted text-sm mt-2">{subtitle}</p>}</div>{action}</div>
}
export function PrivateText({ children, className = '' }: { children: ReactNode; className?: string }) {
  const { privacidade } = usePreferences()
  return <span className={className} title={privacidade ? 'Valor oculto pelo modo privacidade' : undefined}>{privacidade ? <span className="tracking-widest" aria-label="Informação oculta">••••••</span> : children}</span>
}
export function PrivacyPlaceholder() {
  return <div className="empty-state py-12"><EyeOff size={26} className="text-indigo-400"/><h3>Visualização protegida</h3><p>Desative o modo privacidade para visualizar gráficos e informações financeiras.</p></div>
}
export function StatusPill({ label, tone = 'blue' }: { label: string; tone?: 'blue' | 'green' | 'red' | 'amber' | 'neutral' }) {
  return <span className={`status status-${tone}`}>{label}</span>
}
export function Loading({ label = 'Carregando dados...' }: { label?: string }) {
  return <div className="empty-state min-h-48"><LoaderCircle className="animate-spin text-cyan-500" size={26}/><p>{label}</p></div>
}
export function Empty({ title = 'Nenhum registro encontrado', subtitle = 'Tente alterar os filtros ou importe novos dados.', icon }: { title?: string; subtitle?: string; icon?: ReactNode }) {
  return <div className="empty-state min-h-44">{icon || <AlertCircle size={27}/>}<h3>{title}</h3><p>{subtitle}</p></div>
}
export function ErrorNotice({ error, onRetry }: { error: string; onRetry?: () => void }) {
  return <div className="error-notice" role="alert"><AlertCircle size={18} className="shrink-0"/><div className="flex-1"><p className="font-semibold text-sm">Não foi possível carregar os dados</p><p className="text-xs mt-1">{error}</p></div>{onRetry && <Button variant="secondary" onClick={onRetry}>Tentar novamente</Button>}</div>
}
export function SuccessNotice({ message, onClose }: { message: string; onClose?: () => void }) {
  return <div role="status" className="success-notice"><CheckCircle2 size={18}/><span>{message}</span>{onClose && <button type="button" onClick={onClose} className="ml-auto" aria-label="Fechar mensagem"><X size={15}/></button>}</div>
}
export function Pagination({ page, totalPages, totalElements, onChange }: { page: number; totalPages: number; totalElements: number; onChange: (p: number) => void }) {
  const { privacidade } = usePreferences()
  return <div className="flex items-center justify-between gap-3 flex-wrap pt-5 border-t subtle-border mt-5"><p className="text-xs muted">{privacidade ? 'Registros ocultos' : `${inteiro(totalElements)} registro(s)`} · Página {page + 1} de {Math.max(1, totalPages)}</p><div className="flex gap-2"><Button variant="secondary" disabled={page <= 0} onClick={() => onChange(page - 1)}><ChevronLeft size={16}/> Anterior</Button><Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Próxima <ChevronRight size={16}/></Button></div></div>
}
export function Modal({ title, subtitle, children, onClose, size = 'lg' }: { title: string; subtitle?: string; children: ReactNode; onClose: () => void; size?: 'lg' | 'xl' }) {
  useEffect(() => {
    const key = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', key)
    const previous = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.removeEventListener('keydown', key); document.body.style.overflow = previous }
  }, [onClose])
  return <div className="modal-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) onClose() }}><section className={`modal-panel ${size === 'xl' ? 'max-w-4xl' : 'max-w-2xl'}`} role="dialog" aria-modal="true" aria-label={title}><div className="flex justify-between gap-4 items-start pb-5 border-b subtle-border"><div><h2 className="text-xl font-bold">{title}</h2>{subtitle && <p className="muted text-sm mt-1">{subtitle}</p>}</div><button aria-label="Fechar" className="icon-button" onClick={onClose}><X size={19}/></button></div><div className="pt-5">{children}</div></section></div>
}
export function BackLink({ onClick, children }: { onClick: () => void; children: ReactNode }) {
  return <button className="inline-flex items-center gap-2 text-sm text-cyan-500 hover:text-cyan-400" onClick={onClick}><ArrowLeft size={15}/>{children}</button>
}
export function InlineLink({ onClick, children }: { onClick: () => void; children: ReactNode }) {
  return <button className="inline-flex items-center gap-1 text-sm font-semibold text-cyan-500 hover:underline" onClick={onClick}>{children}<ArrowRight size={14}/></button>
}
