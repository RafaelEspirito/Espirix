import { useEffect, useState } from 'react'
import { Activity, CalendarClock, Clock3, FileCheck2, Files, RefreshCw, TrendingUp, Wallet } from 'lucide-react'
import { Area, AreaChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api, mensagemErro } from '../services/api'
import type { Devedor, FaixaPrevisao, PageResult, PontoEvolucao, ResumoDashboard, Titulo } from '../types/api'
import { brl, compactBRL, dataBR, inteiro, mesBR, statusTitulo } from '../lib/format'
import { usePreferences } from '../contexts/Preferences'
import { Button, Card, CardTitle, Empty, ErrorNotice, InlineLink, Loading, PrivateText, PrivacyPlaceholder, SectionHeading, StatusPill } from '../components/UI'
import type { PageName } from '../components/Shell'

const COLORS = ['#06b6d4', '#8b5cf6', '#f59e0b', '#22c55e']
function Stat({ label, value, note, icon: Icon, accent = 'cyan' }: { label: string; value: number; note: string; icon: typeof Wallet; accent?: 'cyan' | 'purple' | 'amber' | 'green' }) {
  return <Card className="metric"><div className="flex items-start justify-between gap-2"><span className="text-sm muted">{label}</span><div className={`metric-icon metric-${accent}`}><Icon size={19}/></div></div><div className="mt-4 text-2xl xl:text-[1.8rem] font-extrabold tracking-tight"><PrivateText>{brl(value)}</PrivateText></div><div className="mt-2 text-xs muted">{note}</div></Card>
}
interface DashboardData {
  resumo: ResumoDashboard | null; evolucao: PontoEvolucao[]; previsao: FaixaPrevisao[];
  top: Devedor[]; recentes: Titulo[]; error: string | null
}
const INITIAL: DashboardData = { resumo: null, evolucao: [], previsao: [], top: [], recentes: [], error: null }
export function Dashboard({ navigate }: { navigate: (p: PageName, query?: string) => void }) {
  const { tipo, privacidade } = usePreferences()
  const [meses, setMeses] = useState(12)
  const [reload, setReload] = useState(0)
  const [data, setData] = useState<DashboardData>(INITIAL)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setLoading(true)
    setData(INITIAL)
    Promise.allSettled([
      api.resumo(tipo), api.evolucao(tipo, meses), api.previsao(tipo),
      api.topDevedores(tipo, 5), api.titulos(tipo, { somenteAbertos: true }, 0, 5),
    ]).then(result => {
      if (!active) return
      const [r, e, p, t, titles] = result
      const failed = result.find(s => s.status === 'rejected')
      setData({
        resumo: r.status === 'fulfilled' ? r.value : null,
        evolucao: e.status === 'fulfilled' ? e.value : [],
        previsao: p.status === 'fulfilled' ? p.value : [],
        top: t.status === 'fulfilled' ? t.value : [],
        recentes: titles.status === 'fulfilled' ? (titles.value as PageResult<Titulo>).content : [],
        error: failed?.status === 'rejected' ? mensagemErro(failed.reason) : null,
      })
      setLoading(false)
    })
    return () => { active = false }
  }, [tipo, meses, reload])

  return <div className="space-y-6"><SectionHeading title="Visão geral" subtitle={`Acompanhe ${tipo === 'RECEBER' ? 'seus recebíveis' : 'suas obrigações'} e indicadores financeiros em tempo real.`} action={<div className="flex gap-2 items-center"><select aria-label="Período do gráfico" className="input !w-auto" value={meses} onChange={e => setMeses(Number(e.target.value))}><option value={6}>6 meses</option><option value={12}>12 meses</option><option value={24}>24 meses</option></select><Button variant="secondary" title="Atualizar dados" onClick={() => setReload(v => v + 1)}><RefreshCw size={16}/> <span className="hidden sm:inline">Atualizar</span></Button></div>}/>
    {data.error && <ErrorNotice error={data.error} onRetry={() => setReload(v => v + 1)}/>}
    {loading && !data.resumo ? <Loading label="Consultando indicadores do backend..."/> : data.resumo ? <>
      <div className="grid sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <Stat label={tipo === 'RECEBER' ? 'Saldo a receber' : 'Saldo a pagar'} value={data.resumo.dividaAtiva} note={privacidade ? "Títulos em aberto: ocultos" : `${inteiro(data.resumo.quantidadeTitulosAbertos)} títulos em aberto`} icon={Wallet}/>
        <Stat label="Valores vencidos" value={data.resumo.totalVencido} note="Saldo vencido ainda não liquidado" icon={Clock3} accent="amber"/>
        <Stat label="A vencer em 30 dias" value={data.resumo.aVencer30Dias} note="Faixa cumulativa: hoje até D+30" icon={CalendarClock} accent="purple"/>
        <Stat label={tipo === 'RECEBER' ? 'Recebimentos no mês' : 'Pagamentos no mês'} value={data.resumo.totalPagamentosMes} note="Inclui liquidações parciais" icon={TrendingUp} accent="green"/>
      </div>
      <div className="grid lg:grid-cols-[1.6fr_1fr] gap-5">
        <Card className="min-w-0"><CardTitle title="Evolução financeira" subtitle="Novos títulos, pagamentos e saldo acumulado por mês" action={<div className="text-xs muted flex items-center gap-1"><Activity size={14}/> {meses} meses</div>}/>{privacidade ? <PrivacyPlaceholder/> : data.evolucao.length ? <div className="h-[285px] min-w-0"><ResponsiveContainer width="100%" height="100%"><AreaChart data={data.evolucao} margin={{ top: 12, right: 8, left: -12, bottom: 0 }}><defs><linearGradient id="saldoGrad" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#06b6d4" stopOpacity={0.27}/><stop offset="95%" stopColor="#06b6d4" stopOpacity={0}/></linearGradient></defs><CartesianGrid stroke="var(--chart-grid)" vertical={false} strokeDasharray="3 4"/><XAxis tickLine={false} axisLine={false} tick={{ fill: 'var(--muted)', fontSize: 11 }} dataKey="periodo" tickFormatter={mesBR}/><YAxis tickLine={false} axisLine={false} tick={{ fill: 'var(--muted)', fontSize: 11 }} tickFormatter={compactBRL}/><Tooltip contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 12, color: 'var(--text)' }} labelFormatter={label => mesBR(String(label))} formatter={value => brl(Number(value ?? 0))}/><Legend verticalAlign="top" height={30} iconType="circle" wrapperStyle={{ fontSize: 11 }}/><Area name="Saldo acumulado" type="monotone" dataKey="saldoAcumulado" stroke="#06b6d4" fill="url(#saldoGrad)" strokeWidth={2.6}/><Area name="Novos títulos" type="monotone" dataKey="novasNotas" stroke="#8b5cf6" fill="transparent" strokeWidth={1.8}/><Area name="Pagamentos" type="monotone" dataKey="pagamentos" stroke="#10b981" fill="transparent" strokeWidth={1.8}/></AreaChart></ResponsiveContainer></div> : <Empty title="Sem evolução disponível"/>}</Card>
        <Card className="min-w-0"><CardTitle title="Previsão de vencimentos" subtitle="Faixas exclusivas de valores a vencer"/>{privacidade ? <PrivacyPlaceholder/> : data.previsao.length && data.previsao.some(f => f.valor > 0) ? <div className="h-[285px] flex flex-col sm:flex-row lg:flex-col xl:flex-row items-center gap-3"><div className="w-full min-w-0 h-[210px]"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={data.previsao} dataKey="valor" nameKey="faixa" innerRadius="60%" outerRadius="84%" paddingAngle={4} stroke="none">{data.previsao.map((item, i) => <Cell key={item.faixa} fill={COLORS[i % COLORS.length]}/>)}</Pie><Tooltip formatter={value => brl(Number(value ?? 0))} contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 12, color: 'var(--text)' }}/></PieChart></ResponsiveContainer></div><div className="w-full space-y-3">{data.previsao.map((f, i) => <div className="flex justify-between gap-2 text-xs" key={f.faixa}><div className="flex items-center gap-2"><span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: COLORS[i] }}/><span className="muted whitespace-nowrap">{f.faixa}</span></div><span className="font-semibold whitespace-nowrap">{brl(f.valor)}</span></div>)}</div></div> : <Empty title="Sem vencimentos futuros" subtitle="Não há valores futuros em aberto nesta operação."/>}</Card>
      </div>
      <div className="grid lg:grid-cols-2 gap-5">
        <Card><CardTitle title={tipo === 'RECEBER' ? 'Maiores saldos por cliente' : 'Maiores obrigações por fornecedor'} subtitle="Ranking de contrapartes com valores em aberto" action={<InlineLink onClick={() => navigate('clientes')}>Ver todos</InlineLink>}/>{privacidade ? <PrivacyPlaceholder/> : data.top.length ? <div className="space-y-5">{data.top.map((c, idx) => { const max = data.top[0]?.saldoDevedor || 1; return <div key={c.clienteId}><div className="flex justify-between items-center gap-2 text-sm mb-2"><span className="truncate font-medium">{c.nomeFantasia || c.razaoSocial}</span><span className="tabular-nums font-semibold whitespace-nowrap">{brl(c.saldoDevedor)}</span></div><div className="track"><div className="track-fill" style={{ width: `${Math.max(2, c.saldoDevedor / max * 100)}%`, background: COLORS[idx % COLORS.length] }}/></div></div> })}</div> : <Empty title="Sem saldos pendentes"/>}</Card>
        <Card><CardTitle title="Próximos vencimentos" subtitle="Títulos abertos ordenados pela data de vencimento" action={<InlineLink onClick={() => navigate('titulos')}>Ver títulos</InlineLink>}/>{data.recentes.length ? <div className="overflow-x-auto"><table className="data-table"><thead><tr><th>Cliente / fornecedor</th><th>Vencimento</th><th className="text-right">Saldo</th></tr></thead><tbody>{data.recentes.map(t => <tr key={t.id}><td className="font-medium"><PrivateText>{t.cliente.nomeFantasia || t.cliente.razaoSocial}</PrivateText><div className="text-xs muted mt-1"><PrivateText>NF {t.numeroNota}</PrivateText></div></td><td><span className="block text-sm">{dataBR(t.dataVencimento)}</span><div className="mt-1"><StatusPill {...(() => { const s = statusTitulo(t); return { label: s.texto, tone: s.cor } })()}/></div></td><td className="text-right whitespace-nowrap font-semibold"><PrivateText>{brl(t.saldoDevedor)}</PrivateText></td></tr>)}</tbody></table></div> : <Empty title="Nenhum título aberto" subtitle="Faça uma importação para começar."/>}</Card>
      </div>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3"><MiniStat icon={FileCheck2} label="Quitados no mês" value={data.resumo.totalQuitadoMes}/><MiniStat icon={CalendarClock} label="Próximos 7 dias" value={data.resumo.aVencer7Dias}/><MiniStat icon={Clock3} label="Próximos 15 dias" value={data.resumo.aVencer15Dias}/><MiniStat icon={Files} label="Títulos abertos" value={data.resumo.quantidadeTitulosAbertos} count/></div>
    </> : !data.error && <Empty title="O resumo ainda não está disponível"/>}
  </div>
}
function MiniStat({ icon: Icon, label, value, count = false }: { icon: typeof Activity; label: string; value: number; count?: boolean }) {
  return <div className="mini-stat"><div className="flex items-center gap-2 muted text-xs"><Icon size={15}/>{label}</div><div className="font-semibold text-lg mt-2"><PrivateText>{count ? inteiro(value) : brl(value)}</PrivateText></div></div>
}
