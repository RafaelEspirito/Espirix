import { useEffect, useState } from 'react'
import { ArrowRight, Building2, RefreshCw, Search, Users } from 'lucide-react'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api, mensagemErro } from '../services/api'
import type { Cliente, ConsolidadoCliente, PageResult, PontoEvolucao } from '../types/api'
import { brl, compactBRL, dataBR, inteiro, mesBR } from '../lib/format'
import { usePreferences } from '../contexts/Preferences'
import { Button, Card, CardTitle, Empty, ErrorNotice, Loading, Modal, Pagination, PrivateText, PrivacyPlaceholder, SectionHeading } from '../components/UI'
import type { PageName } from '../components/Shell'

type Tab = 'consolidado' | 'cadastro'
export function Clientes({ navigate }: { navigate: (p: PageName, query?: string) => void }) {
  const { tipo, privacidade } = usePreferences()
  const [tab, setTab] = useState<Tab>('consolidado')
  const [search, setSearch] = useState('')
  const [applied, setApplied] = useState('')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [consolidado, setConsolidado] = useState<ConsolidadoCliente[]>([])
  const [clientes, setClientes] = useState<PageResult<Cliente> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [detailId, setDetailId] = useState<number | null>(null)
  useEffect(() => {
    let active = true
    setLoading(true); setError('')
    const promise = tab === 'consolidado' ? api.consolidado(tipo, applied) : api.clientes(applied, page, 12)
    promise.then(value => { if (!active) return; if (tab === 'consolidado') setConsolidado(value as ConsolidadoCliente[]); else setClientes(value as PageResult<Cliente>) })
      .catch(e => { if (active) setError(mensagemErro(e)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [tab, tipo, applied, page, reload])
  const totalSaldo = consolidado.reduce((sum, c) => sum + c.saldoDevedor, 0)
  return <div><SectionHeading title="Clientes e fornecedores" subtitle="Visão consolidada de saldos, histórico e contrapartes cadastradas." action={<Button variant="secondary" onClick={() => setReload(v => v + 1)}><RefreshCw size={16}/> Atualizar</Button>}/>
    <div className="flex gap-2 mb-5"><button className={`tab ${tab === 'consolidado' ? 'tab-active' : ''}`} onClick={() => { setTab('consolidado'); setPage(0) }}>Posição financeira</button><button className={`tab ${tab === 'cadastro' ? 'tab-active' : ''}`} onClick={() => { setTab('cadastro'); setPage(0) }}>Todos os cadastros</button></div>
    <Card className="mb-5"><form className="flex flex-col sm:flex-row gap-3" onSubmit={e => { e.preventDefault(); setPage(0); setApplied(search) }}><div className="input-icon flex-1"><Search size={17}/><input placeholder="Buscar por razão social ou nome fantasia" value={search} onChange={e => setSearch(e.target.value)}/></div><Button type="submit">Buscar</Button>{applied && <Button type="button" variant="secondary" onClick={() => { setSearch(''); setApplied(''); setPage(0) }}>Limpar</Button>}</form></Card>
    {error && <div className="mb-4"><ErrorNotice error={error} onRetry={() => setReload(v => v + 1)}/></div>}
    {tab === 'consolidado' && !loading && <div className="grid sm:grid-cols-3 gap-4 mb-5"><Card className="metric"><p className="muted text-sm">Contrapartes com títulos</p><div className="text-2xl font-bold mt-3"><PrivateText>{inteiro(consolidado.length)}</PrivateText></div></Card><Card className="metric"><p className="muted text-sm">Saldo consolidado</p><div className="text-2xl font-bold mt-3"><PrivateText>{brl(totalSaldo)}</PrivateText></div></Card><Card className="metric"><p className="muted text-sm">Títulos abertos</p><div className="text-2xl font-bold mt-3"><PrivateText>{inteiro(consolidado.reduce((s, c) => s + c.quantidadeNotasEmAberto, 0))}</PrivateText></div></Card></div>}
    <Card><CardTitle title={tab === 'consolidado' ? 'Saldos por contraparte' : 'Cadastros existentes'} subtitle={tab === 'consolidado' ? 'Inclui documentos quitados e abertos no percentual de liquidação.' : 'Consulta geral do banco, independente do tipo de operação.'}/>{loading ? <Loading/> : tab === 'consolidado' ? consolidado.length ? <div className="overflow-x-auto"><table className="data-table"><thead><tr><th>Razão social</th><th className="text-right">Original</th><th className="text-right">Pago</th><th className="text-right">Saldo</th><th>Quitação</th><th className="text-right">Ações</th></tr></thead><tbody>{consolidado.map(c => <tr key={c.clienteId}><td><button className="text-left font-semibold hover:text-cyan-500" onClick={() => setDetailId(c.clienteId)}><PrivateText>{c.nomeFantasia || c.razaoSocial}</PrivateText></button><div className="muted text-xs mt-1">{privacidade ? 'Dados ocultos' : `${c.razaoSocial} · ${c.quantidadeNotasEmAberto} em aberto`}</div></td><td className="text-right whitespace-nowrap"><PrivateText>{brl(c.valorOriginal)}</PrivateText></td><td className="text-right whitespace-nowrap"><PrivateText>{brl(c.valorPago)}</PrivateText></td><td className="text-right font-semibold whitespace-nowrap"><PrivateText>{brl(c.saldoDevedor)}</PrivateText></td><td className="min-w-36"><div className="flex items-center gap-2"><div className="track flex-1"><div className="track-fill" style={{ width: `${Math.max(0, Math.min(100, c.percentualQuitado))}%`, background: '#06b6d4' }}/></div><span className="text-xs muted tabular-nums">{privacidade ? '•••' : `${c.percentualQuitado.toFixed(0)}%`}</span></div></td><td className="text-right"><Button variant="secondary" onClick={() => setDetailId(c.clienteId)}>Detalhes <ArrowRight size={15}/></Button></td></tr>)}</tbody></table></div> : <Empty title="Nenhum saldo encontrado" subtitle="Importe registros ou ajuste a busca." icon={<Building2 size={26}/>}/> : clientes?.content.length ? <div className="overflow-x-auto"><table className="data-table"><thead><tr><th>Razão social</th><th>Nome fantasia</th><th>Documento</th><th>Cadastro</th><th className="text-right">Ações</th></tr></thead><tbody>{clientes.content.map(c => <tr key={c.id}><td className="font-semibold"><PrivateText>{c.razaoSocial}</PrivateText></td><td><PrivateText>{c.nomeFantasia || '—'}</PrivateText></td><td><PrivateText>{c.cnpjCpf || '—'}</PrivateText></td><td>{dataBR(c.dataCadastro)}</td><td className="text-right"><Button variant="secondary" onClick={() => setDetailId(c.id)}>Detalhes <ArrowRight size={15}/></Button></td></tr>)}</tbody></table></div> : <Empty title="Nenhum cadastro encontrado" icon={<Users size={26}/>}/>}
      {tab === 'cadastro' && clientes && <Pagination page={clientes.number} totalPages={clientes.totalPages} totalElements={clientes.totalElements} onChange={setPage}/>}
    </Card>
    {detailId !== null && <ClienteModal key={detailId} id={detailId} onClose={() => setDetailId(null)} onTitulos={name => { setDetailId(null); navigate('titulos', name) }}/>}</div>
}
function ClienteModal({ id, onClose, onTitulos }: { id: number; onClose: () => void; onTitulos: (name: string) => void }) {
  const { tipo, privacidade } = usePreferences()
  const [cliente, setCliente] = useState<Cliente | null>(null)
  const [pontos, setPontos] = useState<PontoEvolucao[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => { let active = true; setLoading(true); Promise.all([api.cliente(id), api.evolucaoCliente(id, tipo)]).then(([client, evolution]) => { if (active) { setCliente(client); setPontos(evolution) } }).catch(e => { if (active) setError(mensagemErro(e)) }).finally(() => { if (active) setLoading(false) }); return () => { active = false } }, [id, tipo])
  return <Modal title="Detalhes da contraparte" subtitle={`Cadastro #${id} · ${tipo === 'RECEBER' ? 'Contas a receber' : 'Contas a pagar'}`} onClose={onClose} size="xl">{loading ? <Loading/> : error ? <ErrorNotice error={error}/> : cliente && <><div className="grid md:grid-cols-2 gap-4"><div className="mini-stat"><p className="muted text-xs mb-1">Razão social</p><p className="font-semibold"><PrivateText>{cliente.razaoSocial}</PrivateText></p></div><div className="mini-stat"><p className="muted text-xs mb-1">Nome fantasia</p><p className="font-semibold"><PrivateText>{cliente.nomeFantasia || '—'}</PrivateText></p></div><div className="mini-stat"><p className="muted text-xs mb-1">CNPJ/CPF</p><p className="font-semibold"><PrivateText>{cliente.cnpjCpf || 'Não informado'}</PrivateText></p></div><div className="mini-stat"><p className="muted text-xs mb-1">Data do cadastro</p><p className="font-semibold">{dataBR(cliente.dataCadastro)}</p></div></div><div className="mt-6"><h3 className="font-semibold mb-3">Evolução do saldo · 12 meses</h3>{privacidade ? <PrivacyPlaceholder/> : pontos.length ? <div className="h-56"><ResponsiveContainer width="100%" height="100%"><AreaChart data={pontos}><defs><linearGradient id="clientGrad" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#06b6d4" stopOpacity={0.3}/><stop offset="95%" stopColor="#06b6d4" stopOpacity={0}/></linearGradient></defs><CartesianGrid stroke="var(--chart-grid)" vertical={false}/><XAxis dataKey="periodo" tickFormatter={mesBR} tick={{ fill: 'var(--muted)', fontSize: 11 }}/><YAxis tickFormatter={compactBRL} tick={{ fill: 'var(--muted)', fontSize: 11 }}/><Tooltip formatter={value => brl(Number(value ?? 0))} contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--text)' }}/><Area dataKey="saldoAcumulado" name="Saldo acumulado" stroke="#06b6d4" strokeWidth={2} fill="url(#clientGrad)"/></AreaChart></ResponsiveContainer></div> : <Empty title="Sem dados de evolução"/>}</div><div className="mt-6 flex justify-end"><Button onClick={() => onTitulos(cliente.razaoSocial)}>Ver títulos relacionados <ArrowRight size={16}/></Button></div></>}</Modal>
}
