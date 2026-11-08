import type { StatusTitulo, Titulo } from '../types/api'

export const brl = (value: number | null | undefined) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(value ?? 0))

export const compactBRL = (value: number) =>
  new Intl.NumberFormat('pt-BR', { notation: 'compact', maximumFractionDigits: 1 }).format(value)

export const inteiro = (value: number) => new Intl.NumberFormat('pt-BR').format(value)

/** Datas ISO sem deslocamento de fuso (evita exibir o dia anterior no Brasil). */
export function dataBR(value?: string | null): string {
  if (!value) return '—'
  const parts = value.slice(0, 10).split('-')
  return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : value
}
export function dataHoraBR(value?: string | null): string {
  if (!value) return '—'
  return `${dataBR(value)}${value.length >= 16 ? ` às ${value.slice(11, 16)}` : ''}`
}
export function mesBR(value: string): string {
  if (!/^\d{4}-\d{2}$/.test(value)) return value
  const [ano, mes] = value.split('-')
  return `${mes}/${ano.slice(2)}`
}
export function hojeISO() {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

/** Aceita 1.234,56 ou 1234.56; não arredonda uma entrada com mais de duas casas. */
export function parseValor(raw: string): number | null {
  let s = raw.trim().replace(/\s/g, '').replace(/^R\$/, '')
  if (!s) return null
  if (s.includes(',') && s.includes('.')) s = s.replace(/\./g, '').replace(',', '.')
  else if (s.includes(',')) s = s.replace(',', '.')
  if (!/^\d+(?:\.\d{1,2})?$/.test(s)) return null
  const n = Number(s)
  return Number.isFinite(n) && n > 0 ? n : null
}

export function statusTitulo(titulo: Pick<Titulo, 'status' | 'emAtraso'>) {
  if (titulo.status === 'PAGA') return { texto: 'Quitado', cor: 'green' as const }
  if (titulo.status === 'PAGA_PARCIAL') return titulo.emAtraso
    ? { texto: 'Parcial · vencido', cor: 'red' as const }
    : { texto: 'Parcial', cor: 'amber' as const }
  if (titulo.emAtraso || titulo.status === 'VENCIDA') return { texto: 'Vencido', cor: 'red' as const }
  return { texto: 'A vencer', cor: 'blue' as const }
}
export const STATUS_OPCOES: { value: StatusTitulo | ''; label: string }[] = [
  { value: '', label: 'Todos os status' }, { value: 'A_VENCER', label: 'A vencer' },
  { value: 'VENCIDA', label: 'Vencidos (inclui parcial)' },
  { value: 'PAGA_PARCIAL', label: 'Pagos parcialmente' }, { value: 'PAGA', label: 'Quitados' },
]
