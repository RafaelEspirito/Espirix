import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { CheckCheck, Download, Eye, FileText, FilterX, ReceiptText, Search, Send, Wallet } from 'lucide-react'
import { api, mensagemErro } from '../services/api'
import type { FiltrosTitulos, PageResult, Pagamento, StatusTitulo, Titulo, BaixaItem } from '../types/api'
import { brl, dataBR, hojeISO, inteiro, parseValor, statusTitulo, STATUS_OPCOES } from '../lib/format'
import { usePreferences } from '../contexts/Preferences'
import { Button, Card, Empty, ErrorNotice, Loading, Modal, Pagination, PrivateText, SectionHeading, StatusPill, SuccessNotice } from '../components/UI'

const INITIAL: FiltrosTitulos = { busca: '', status: '', emissaoDe: '', emissaoAte: '', vencimentoDe: '', vencimentoAte: '', somenteAbertos: false }
const PAGE_SIZE = 12
function downloadCSV(rows: Titulo[]) {
  const quote = (input: unknown) => {
    let value = String(input ?? '')
    if (/^[=+@\-]/.test(value)) value = `'${value}` // Protege contra fórmula em planilhas.
    return `"${value.replace(/"/g, '""')}"`
  }
  const header = ['Documento', 'Parcela', 'Cliente', 'Emissão', 'Vencimento', 'Original', 'Pago', 'Saldo', 'Status']
  const body = rows.map(t => [t.numeroNota, t.numeroParcela, t.cliente.razaoSocial, t.dataEmissao, t.dataVencimento, t.valorOriginal, t.valorPago, t.saldoDevedor, t.status])
  const csv = '\uFEFF' + [header, ...body].map(row => row.map(quote).join(';')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a'); a.href = url; a.download = `espirix-titulos-pagina-${Date.now()}.csv`; a.click()
  URL.revokeObjectURL(url)
}
export function Titulos({ initialSearch = '' }: { initialSearch?: string }) {
  const { tipo, privacidade } = usePreferences()
  const [draft, setDraft] = useState<FiltrosTitulos>({ ...INITIAL, busca: initialSearch })
  const [filtros, setFiltros] = useState<FiltrosTitulos>({ ...INITIAL, busca: initialSearch })
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<PageResult<Titulo> | null>(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(true)
  const [reload, setReload] = useState(0)
  const [selected, setSelected] = useState<number[]>([])
  const [detail, setDetail] = useState<Titulo | null>(null)
  const [batchOpen, setBatchOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  useEffect(() => { setPage(0); setSelected([]); setDraft({ ...INITIAL, busca: initialSearch }); setFiltros({ ...INITIAL, busca: initialSearch }) }, [initialSearch])
  useEffect(() => {
    let active = true
    setLoading(true); setError('')
    api.titulos(tipo, filtros, page, PAGE_SIZE).then(data => { if (active) { setResult(data); setSelected([]) } }).catch(e => { if (active) setError(mensagemErro(e)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [tipo, filtros, page, reload])
  const rows = result?.content || []
  const selectable = rows.filter(t => t.saldoDevedor > 0)
  const currentSelected = selectable.filter(t => selected.includes(t.id))
  const allChecked = selectable.length > 0 && selectable.every(t => selected.includes(t.id))
  function refresh(message?: string) { if (message) setSuccess(message); setSelected([]); setDetail(null); setBatchOpen(false); setReload(v => v + 1) }
  async function quitarSelecionados() {
    if (!currentSelected.length || busy) return
    if (!window.confirm(`Deseja quitar integralmente ${currentSelected.length} título(s) em ${dataBR(hojeISO())}? Esta ação registra pagamentos no banco.`)) return
    setBusy(true); setError('')
    try { await api.quitarLote(currentSelected.map(t => t.id), hojeISO(), 'Quitação em lote pelo Espirix'); refresh(`${currentSelected.length} título(s) quitado(s) com sucesso.`) }
    catch (e) { setError(mensagemErro(e)) }
    finally { setBusy(false) }
  }
  const submit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (draft.emissaoDe && draft.emissaoAte && draft.emissaoDe > draft.emissaoAte) { setError('Intervalo de emissão inválido.'); return }
    if (draft.vencimentoDe && draft.vencimentoAte && draft.vencimentoDe > draft.vencimentoAte) { setError('Intervalo de vencimento inválido.'); return }
    setError(''); setPage(0); setFiltros({ ...draft })
  }
  return <div><SectionHeading title="Títulos financeiros" subtitle="Consulte, filtre, exporte e registre baixas de contas a pagar e receber." action={<div className="flex flex-wrap gap-2"><Button variant="secondary" disabled={!rows.length || privacidade} title={privacidade ? 'Desative a privacidade para exportar' : 'Exportar somente a página atual'} onClick={() => downloadCSV(rows)}><Download size={16}/> Exportar página</Button></div>}/>
    {error && <div className="mb-4"><ErrorNotice error={error} onRetry={() => setReload(v => v + 1)}/></div>}
    {success && <div className="mb-4"><SuccessNotice message={success} onClose={() => setSuccess('')}/></div>}
    <Card className="mb-5"><form onSubmit={submit} className="space-y-4"><div className="grid md:grid-cols-[1.5fr_1fr_auto] gap-3"><label className="field"><span>Busca</span><div className="input-icon"><Search size={17}/><input aria-label="Buscar por cliente ou documento" placeholder="Cliente, fornecedor ou documento" value={draft.busca || ''} onChange={e => setDraft(f => ({ ...f, busca: e.target.value }))}/></div></label><label className="field"><span>Situação</span><select className="input" value={draft.status || ''} onChange={e => setDraft(f => ({ ...f, status: e.target.value as StatusTitulo | '' }))}>{STATUS_OPCOES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}</select></label><div className="flex gap-2 items-end"><Button type="submit" className="h-[43px]">Aplicar filtros</Button><Button title="Limpar filtros" variant="secondary" className="h-[43px]" type="button" onClick={() => { setDraft(INITIAL); setFiltros(INITIAL); setPage(0) }}><FilterX size={17}/></Button></div></div><div className="grid grid-cols-2 md:grid-cols-4 gap-3"><label className="field"><span>Emissão inicial</span><input className="input" type="date" value={draft.emissaoDe || ''} onChange={e => setDraft(f => ({ ...f, emissaoDe: e.target.value }))}/></label><label className="field"><span>Emissão final</span><input className="input" type="date" value={draft.emissaoAte || ''} onChange={e => setDraft(f => ({ ...f, emissaoAte: e.target.value }))}/></label><label className="field"><span>Vencimento inicial</span><input className="input" type="date" value={draft.vencimentoDe || ''} onChange={e => setDraft(f => ({ ...f, vencimentoDe: e.target.value }))}/></label><label className="field"><span>Vencimento final</span><input className="input" type="date" value={draft.vencimentoAte || ''} onChange={e => setDraft(f => ({ ...f, vencimentoAte: e.target.value }))}/></label></div><label className="flex items-center gap-2 text-xs muted cursor-pointer"><input type="checkbox" checked={!!draft.somenteAbertos} onChange={e => setDraft(f => ({ ...f, somenteAbertos: e.target.checked }))}/> Mostrar somente títulos com saldo em aberto</label></form></Card>
    <Card><div className="flex flex-wrap gap-4 items-center justify-between mb-5"><div><h2 className="font-semibold">Lista de títulos</h2><p className="text-xs muted mt-1">{privacidade ? 'Quantidade oculta' : `${inteiro(result?.totalElements || 0)} resultado(s)`} · Ordenado por vencimento</p></div><div className="flex flex-wrap items-center gap-2">{currentSelected.length > 0 && <><span className="text-xs muted">{currentSelected.length} selecionado(s)</span><Button variant="secondary" disabled={busy} onClick={() => setBatchOpen(true)}><Wallet size={15}/> Baixa em lote</Button><Button disabled={busy} onClick={quitarSelecionados}><CheckCheck size={15}/> Quitar selecionados</Button></>}</div></div>
      {loading ? <Loading/> : rows.length ? <div className="overflow-x-auto"><table className="data-table"><thead><tr><th><input aria-label="Selecionar títulos em aberto da página" type="checkbox" checked={allChecked} onChange={e => setSelected(e.target.checked ? selectable.map(t => t.id) : [])}/></th><th>Documento</th><th>Cliente / fornecedor</th><th>Vencimento</th><th>Original</th><th>Saldo</th><th>Situação</th><th className="text-right">Ações</th></tr></thead><tbody>{rows.map(t => { const status = statusTitulo(t); return <tr key={t.id}><td><input aria-label={`Selecionar título ${t.id}`} type="checkbox" disabled={t.saldoDevedor <= 0} checked={selected.includes(t.id)} onChange={e => setSelected(v => e.target.checked ? [...v, t.id] : v.filter(id => id !== t.id))}/></td><td className="font-semibold whitespace-nowrap"><PrivateText>{t.numeroNota}</PrivateText><div className="muted text-xs mt-1">Parcela {t.numeroParcela}</div></td><td className="min-w-40"><PrivateText>{t.cliente.nomeFantasia || t.cliente.razaoSocial}</PrivateText><div className="muted text-xs mt-1">#{t.cliente.id}</div></td><td className="whitespace-nowrap">{dataBR(t.dataVencimento)}</td><td className="whitespace-nowrap"><PrivateText>{brl(t.valorOriginal)}</PrivateText></td><td className="font-semibold whitespace-nowrap"><PrivateText>{brl(t.saldoDevedor)}</PrivateText></td><td><StatusPill label={status.texto} tone={status.cor}/></td><td className="text-right"><Button variant="secondary" onClick={() => setDetail(t)}><Eye size={15}/> Detalhes</Button></td></tr> })}</tbody></table></div> : <Empty title="Nenhum título encontrado" subtitle="Altere os filtros ou importe um relatório CSV/PDF." icon={<FileText size={27}/>}/>}
      {result && <Pagination page={result.number} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage}/>}
    </Card>
    {detail && <TituloModal key={detail.id} titulo={detail} onClose={() => setDetail(null)} onSaved={() => refresh('Pagamento registrado com sucesso.')}/>}
    {batchOpen && <BatchModal titulos={currentSelected} onClose={() => setBatchOpen(false)} onSaved={() => refresh('Baixas em lote registradas com sucesso.')}/>}
  </div>
}

function TituloModal({ titulo, onClose, onSaved }: { titulo: Titulo; onClose: () => void; onSaved: () => void }) {
  const { privacidade } = usePreferences()
  const [history, setHistory] = useState<Pagamento[]>([])
  const [loading, setLoading] = useState(true)
  const [valor, setValor] = useState('')
  const [data, setData] = useState(hojeISO())
  const [observacao, setObservacao] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => { let active = true; api.pagamentos(titulo.id).then(r => { if (active) setHistory(r) }).catch(e => { if (active) setError(mensagemErro(e)) }).finally(() => { if (active) setLoading(false) }); return () => { active = false } }, [titulo.id])
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError('')
    const numero = parseValor(valor)
    if (numero === null) { setError('Informe um valor positivo, com no máximo duas casas decimais.'); return }
    if (numero > titulo.saldoDevedor + 0.001) { setError('O valor excede o saldo devedor.'); return }
    if (data < titulo.dataEmissao || data > hojeISO()) { setError('A data não pode ser anterior à emissão nem posterior a hoje.'); return }
    setBusy(true)
    try { await api.pagar(titulo.id, { valor: numero, dataPagamento: data, observacao }); onSaved() }
    catch (e) { setError(mensagemErro(e)) } finally { setBusy(false) }
  }
  const status = statusTitulo(titulo)
  return <Modal title={`Título #${titulo.id}`} subtitle={`Documento ${titulo.numeroNota} · Parcela ${titulo.numeroParcela}`} onClose={onClose} size="xl">
    <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-3 mb-6">{[['Valor original', brl(titulo.valorOriginal)], ['Já pago', brl(titulo.valorPago)], ['Saldo em aberto', brl(titulo.saldoDevedor)], ['Vencimento', dataBR(titulo.dataVencimento)]].map(([label, value]) => <div key={label} className="mini-stat"><p className="text-xs muted">{label}</p><p className="font-bold mt-1"><PrivateText>{value}</PrivateText></p></div>)}</div>
    <div className="grid lg:grid-cols-2 gap-6"><div><div className="flex justify-between items-center mb-3"><h3 className="font-semibold">Detalhes</h3><StatusPill label={status.texto} tone={status.cor}/></div><dl className="detail-list"><dt>Razão social</dt><dd><PrivateText>{titulo.cliente.razaoSocial}</PrivateText></dd><dt>Emissão</dt><dd>{dataBR(titulo.dataEmissao)}</dd><dt>Origem</dt><dd>{titulo.origemArquivo}</dd><dt>Tipo</dt><dd>{titulo.tipoOperacao === 'RECEBER' ? 'Conta a receber' : 'Conta a pagar'}</dd></dl><h3 className="font-semibold mt-6 mb-3">Histórico de pagamentos</h3>{loading ? <Loading/> : history.length ? <div className="space-y-2 max-h-56 overflow-y-auto">{history.map(p => <div key={p.id} className="mini-stat flex justify-between gap-3"><div><div className="font-semibold text-sm"><PrivateText>{brl(p.valor)}</PrivateText></div><p className="text-xs muted mt-1">{dataBR(p.dataPagamento)} · {p.origem === 'MANUAL' ? 'Manual' : 'Importação'}</p>{p.observacao && <p className="text-xs muted mt-1"><PrivateText>{p.observacao}</PrivateText></p>}</div><ReceiptText size={17} className="shrink-0 text-cyan-500"/></div>)}</div> : <Empty title="Sem pagamentos registrados" subtitle="Os registros de baixas aparecerão aqui."/>}</div>
      <div><h3 className="font-semibold mb-3">Registrar baixa</h3>{titulo.saldoDevedor <= 0 ? <div className="success-notice"><CheckCheck size={18}/> Este título já está quitado.</div> : <form onSubmit={submit} className="space-y-4"><label className="field"><span>Valor (R$)</span><input className="input" inputMode="decimal" placeholder="Ex.: 250,00" value={valor} onChange={e => setValor(e.target.value)} required/></label><div className="flex flex-wrap items-center gap-2"><Button type="button" variant="secondary" onClick={() => setValor(titulo.saldoDevedor.toFixed(2).replace('.', ','))}>Usar saldo integral</Button><span className="text-xs muted">Até <PrivateText>{brl(titulo.saldoDevedor)}</PrivateText></span></div><label className="field"><span>Data do pagamento</span><input className="input" type="date" value={data} min={titulo.dataEmissao} max={hojeISO()} onChange={e => setData(e.target.value)} required/></label><label className="field"><span>Observação (opcional)</span><textarea className="input min-h-20" maxLength={300} value={observacao} onChange={e => setObservacao(e.target.value)} placeholder="Ex.: Pix confirmado"/></label>{error && <ErrorNotice error={error}/>}<Button disabled={busy || privacidade} className="w-full" type="submit"><Send size={16}/>{busy ? 'Registrando...' : 'Confirmar pagamento'}</Button>{privacidade && <p className="text-xs muted">Desative a privacidade para registrar uma baixa.</p>}<p className="text-[0.69rem] muted">A baixa será gravada no H2 pelo backend. Não é possível desfazer por esta interface.</p></form>}</div></div>
  </Modal>
}

function BatchModal({ titulos, onClose, onSaved }: { titulos: Titulo[]; onClose: () => void; onSaved: () => void }) {
  const { privacidade } = usePreferences()
  const [amounts, setAmounts] = useState<Record<number, string>>(() => Object.fromEntries(titulos.map(t => [t.id, t.saldoDevedor.toFixed(2).replace('.', ',')])))
  const [date, setDate] = useState(hojeISO())
  const [obs, setObs] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); setError('')
    const items: BaixaItem[] = []
    for (const t of titulos) {
      const amount = parseValor(amounts[t.id] || '')
      if (amount === null || amount > t.saldoDevedor + 0.001) { setError(`Valor inválido para o documento ${t.numeroNota}.`); return }
      if (date < t.dataEmissao || date > hojeISO()) { setError(`Data inválida para o documento ${t.numeroNota}.`); return }
      items.push({ tituloId: t.id, valor: amount, dataPagamento: date })
    }
    if (!window.confirm(`Registrar ${items.length} pagamento(s) em lote?`)) return
    setBusy(true)
    try { await api.pagarLote(items, obs); onSaved() } catch (e) { setError(mensagemErro(e)) } finally { setBusy(false) }
  }
  return <Modal title="Baixa em lote" subtitle="Defina o valor individual de cada título selecionado." onClose={onClose}><form className="space-y-4" onSubmit={submit}><div className="max-h-64 overflow-auto space-y-3">{titulos.map(t => <div className="mini-stat flex flex-wrap items-center justify-between gap-3" key={t.id}><div className="min-w-0"><div className="font-semibold text-sm"><PrivateText>{t.numeroNota}</PrivateText></div><p className="muted text-xs truncate max-w-56"><PrivateText>{t.cliente.razaoSocial}</PrivateText></p><p className="muted text-xs mt-1">Saldo: <PrivateText>{brl(t.saldoDevedor)}</PrivateText></p></div><input className="input !w-36" aria-label={`Pagamento do título ${t.numeroNota}`} inputMode="decimal" value={amounts[t.id] || ''} onChange={e => setAmounts(a => ({ ...a, [t.id]: e.target.value }))}/></div>)}</div><div className="grid sm:grid-cols-2 gap-3"><label className="field"><span>Data da baixa</span><input className="input" type="date" max={hojeISO()} value={date} onChange={e => setDate(e.target.value)}/></label><label className="field"><span>Observação (opcional)</span><input className="input" value={obs} maxLength={300} onChange={e => setObs(e.target.value)}/></label></div>{error && <ErrorNotice error={error}/>}<div className="flex justify-end gap-2"><Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button><Button type="submit" disabled={busy || !titulos.length || privacidade}><CheckCheck size={16}/>{busy ? 'Registrando...' : `Confirmar ${titulos.length} baixa(s)`}</Button></div></form></Modal>
}
