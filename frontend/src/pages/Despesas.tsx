import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { BarChart3, CheckCircle2, Plus, RotateCcw, Search } from 'lucide-react'
import { api, mensagemErro } from '../services/api'
import type { CategoriaDespesa, Cliente, NovaDespesa, PageResult, Pagamento, Titulo, TipoCategoria, ResumoCategoria } from '../types/api'
import { brl, dataBR, hojeISO, parseValor, statusTitulo } from '../lib/format'
import { Button, Card, CardTitle, Empty, ErrorNotice, Loading, Modal, Pagination, PrivateText, SectionHeading, StatusPill, SuccessNotice } from '../components/UI'
import { usePreferences } from '../contexts/Preferences'
import { useAuth } from '../contexts/Auth'

const NOVO = () => ({ descricao: '', valor: '', dataEmissao: hojeISO(), dataVencimento: hojeISO(), categoriaId: '', fornecedorId: '', observacao: '' })
export function Despesas() {
  const { user } = useAuth()
  const { privacidade } = usePreferences()
  const podeEditar = user?.perfil !== 'CONSULTA'
  const [categorias, setCategorias] = useState<CategoriaDespesa[]>([])
  const [resumo, setResumo] = useState<ResumoCategoria[]>([])
  const [lista, setLista] = useState<PageResult<Titulo> | null>(null)
  const [page, setPage] = useState(0)
  const [busca, setBusca] = useState('')
  const [filtro, setFiltro] = useState('')
  const [fornecedores, setFornecedores] = useState<Cliente[]>([])
  const [buscaFornecedor, setBuscaFornecedor] = useState('')
  const [mostrarFornecedores, setMostrarFornecedores] = useState(false)
  const [carregandoFornecedores, setCarregandoFornecedores] = useState(false)
  const [opcaoFornecedor, setOpcaoFornecedor] = useState(-1)
  const [form, setForm] = useState(NOVO)
  const [categoria, setCategoria] = useState({ nome: '', tipo: 'VARIAVEL' as TipoCategoria })
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [reload, setReload] = useState(0)
  const [detalhe, setDetalhe] = useState<Titulo | null>(null)
  const [historico, setHistorico] = useState<Pagamento[]>([])
  const [dataPagamento, setDataPagamento] = useState(hojeISO())

  useEffect(() => {
    let active = true
    setLoading(true)
    Promise.all([api.titulos('PAGAR', { busca: filtro }, page, 20), api.categoriasDespesas(), api.resumoCategorias()])
      .then(([l, c, r]) => { if (active) { setLista(l); setCategorias(c); setResumo(r) } })
      .catch(e => { if (active) setErro(mensagemErro(e)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [filtro,page,reload])
  useEffect(() => {
    let active = true
    setCarregandoFornecedores(true)
    const timer = window.setTimeout(() => {
      api.clientes(buscaFornecedor.trim(), 0, 30, 'fornecedor')
        .then(r => { if (active) { setFornecedores(r.content); setOpcaoFornecedor(-1) } })
        .catch(e => { if (active) { setFornecedores([]); setErro(mensagemErro(e)) } })
        .finally(() => { if (active) setCarregandoFornecedores(false) })
    }, 220)
    return () => { active = false; window.clearTimeout(timer) }
  }, [buscaFornecedor, reload])
  const escolherFornecedor = (fornecedor: Cliente) => {
    setForm(f => ({ ...f, fornecedorId: String(fornecedor.id) }))
    setBuscaFornecedor(fornecedor.nomeFantasia || fornecedor.razaoSocial)
    setMostrarFornecedores(false)
    setOpcaoFornecedor(-1)
  }
  const atualizarBuscaFornecedor = (texto: string) => {
    setBuscaFornecedor(texto)
    setFornecedores([])
    // Um nome digitado nao equivale a uma selecao: invalida o ID anterior.
    setForm(f => ({ ...f, fornecedorId: '' }))
    setMostrarFornecedores(true)
    setOpcaoFornecedor(-1)
  }
  useEffect(() => {
    if (detalhe) void api.pagamentos(detalhe.id).then(setHistorico).catch(e=>setErro(mensagemErro(e)))
  }, [detalhe])
  const salvarCategoria = async (event: FormEvent) => {
    event.preventDefault(); setBusy(true); setErro('')
    try { await api.criarCategoriaDespesa(categoria.nome,categoria.tipo);setCategoria({nome:'',tipo:'VARIAVEL'});setSucesso('Categoria cadastrada.');setReload(v=>v+1) }
    catch(e){setErro(mensagemErro(e))}finally{setBusy(false)}
  }
  const salvarDespesa = async (event: FormEvent) => {
    event.preventDefault()
    const valor=parseValor(form.valor)
    if (!valor) { setErro('Informe um valor positivo com ate duas casas decimais.'); return }
    if (!form.categoriaId || !form.fornecedorId) {setErro('Selecione uma categoria e escolha um fornecedor nas sugestoes.');return}
    if (form.dataVencimento < form.dataEmissao) {setErro('Vencimento anterior a emissao.');return}
    const payload: NovaDespesa = { fornecedorId:Number(form.fornecedorId), categoriaId:Number(form.categoriaId),
      descricao:form.descricao,valor,dataEmissao:form.dataEmissao,dataVencimento:form.dataVencimento,observacao:form.observacao }
    setBusy(true);setErro('')
    try {await api.criarDespesa(payload);setForm(NOVO());setBuscaFornecedor('');setMostrarFornecedores(false);setSucesso('Despesa pendente cadastrada e incluida no dashboard A Pagar.');setPage(0);setReload(v=>v+1)}
    catch(e){setErro(mensagemErro(e))}finally{setBusy(false)}
  }
  async function quitar(t: Titulo) {
    if (busy || t.saldoDevedor <= 0) return
    if (!window.confirm(`Registrar pagamento de ${brl(t.saldoDevedor)} com data ${dataBR(dataPagamento)}?`)) return
    setBusy(true);setErro('')
    try {await api.pagar(t.id,{valor:t.saldoDevedor,dataPagamento,observacao:'Quitação manual de despesa'});
      setDetalhe(null);setSucesso('Pagamento registrado.');setReload(v=>v+1)}
    catch(e){setErro(mensagemErro(e))}finally{setBusy(false)}
  }
  async function estornar(t:Titulo,p:Pagamento) {
    const motivo=window.prompt('Motivo do estorno (obrigatório):')
    if(!motivo?.trim())return
    if(!window.confirm(`Estornar o pagamento de ${brl(p.valor)} de ${dataBR(p.dataPagamento)}?`))return
    setBusy(true);setErro('')
    try {await api.estornar(t.id,p.id,motivo.trim());setDetalhe(null);setSucesso('Estorno registrado com auditoria.');setReload(v=>v+1)}
    catch(e){setErro(mensagemErro(e))}finally{setBusy(false)}
  }
  const repetirProximoMes = (t:Titulo) => {
    if(t.origemArquivo!=='MANUAL'||!t.categoriaId)return
    const [ano,mes,dia]=t.dataVencimento.split('-').map(Number)
    const primeiro=new Date(Date.UTC(ano,mes,1))
    const ultimo=new Date(Date.UTC(ano,mes+1,0)).getUTCDate()
    const vencimento=`${primeiro.getUTCFullYear()}-${String(primeiro.getUTCMonth()+1).padStart(2,'0')}-${String(Math.min(dia,ultimo)).padStart(2,'0')}`
    setForm({descricao:t.descricao||'',valor:String(t.valorOriginal).replace('.',','),categoriaId:String(t.categoriaId),fornecedorId:String(t.cliente.id),dataEmissao:hojeISO(),dataVencimento:vencimento,observacao:t.observacao||''})
    setBuscaFornecedor(t.cliente.nomeFantasia || t.cliente.razaoSocial)
    setDetalhe(null)
    setSucesso('Formulário preenchido com o próximo mês. Revise e clique em Cadastrar; nada foi lançado automaticamente.')
    window.scrollTo({top:0,behavior:'smooth'})
  }
  const max = Math.max(1,...resumo.map(item=>item.totalPagoMes))
  return <div className="space-y-5">
    <SectionHeading title="Despesas manuais" subtitle="Categorias, lançamentos pendentes, vencimentos e pagamentos com histórico." />
    {erro && <ErrorNotice error={erro} onRetry={()=>setReload(v=>v+1)}/>}
    {sucesso && <SuccessNotice message={sucesso} onClose={()=>setSucesso('')}/>}
    {podeEditar && <div className="grid lg:grid-cols-[1.5fr_1fr] gap-5">
      <Card><CardTitle title="Novo lançamento a pagar" subtitle="O título nasce pendente e aparece no dashboard de despesas."/>
        <form onSubmit={salvarDespesa} className="space-y-3">
          <label className="field"><span>Descrição</span><input className="input" maxLength={200} required value={form.descricao} onChange={e=>setForm(f=>({...f,descricao:e.target.value}))} placeholder="Ex.: Energia elétrica novembro"/></label>
          <div className="field relative">
            <label htmlFor="despesa-fornecedor" className="text-xs font-semibold muted">Fornecedor</label>
            <input id="despesa-fornecedor" className="input" role="combobox" aria-autocomplete="list"
              aria-expanded={mostrarFornecedores} aria-controls="despesa-fornecedor-opcoes"
              aria-activedescendant={mostrarFornecedores && opcaoFornecedor >= 0 ? `despesa-fornecedor-opcao-${opcaoFornecedor}` : undefined}
              autoComplete="off" required value={buscaFornecedor}
              onFocus={() => setMostrarFornecedores(true)}
              onBlur={e => {
                // Clicar numa sugestao nao deve fechar a lista antes do onClick.
                if (!e.currentTarget.parentElement?.contains(e.relatedTarget as Node | null))
                  setMostrarFornecedores(false)
              }}
              onChange={e => atualizarBuscaFornecedor(e.target.value)}
              onKeyDown={e => {
                if (e.key === 'Escape') { setMostrarFornecedores(false); return }
                if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
                  e.preventDefault()
                  if (!mostrarFornecedores) { setMostrarFornecedores(true); setOpcaoFornecedor(0); return }
                  if (fornecedores.length) setOpcaoFornecedor(i => e.key === 'ArrowDown'
                    ? (i + 1) % fornecedores.length : (i <= 0 ? fornecedores.length - 1 : i - 1))
                }
                if (e.key === 'Enter' && mostrarFornecedores) {
                  e.preventDefault()
                  const indice = opcaoFornecedor >= 0 ? opcaoFornecedor : fornecedores.length === 1 ? 0 : -1
                  if (indice >= 0 && indice < fornecedores.length) escolherFornecedor(fornecedores[indice])
                }
              }}
              placeholder="Digite o nome ou a razão social; depois escolha uma sugestão"/>
            {form.fornecedorId ? <p className="text-xs text-emerald-400">Fornecedor selecionado ✓</p>
              : <p className="muted text-xs">Digite e selecione um fornecedor cadastrado.</p>}
            {mostrarFornecedores && <div id="despesa-fornecedor-opcoes" role="listbox"
              className="absolute left-0 right-0 top-full mt-1 z-50 max-h-56 overflow-y-auto rounded-xl border shadow-xl"
              style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
              {carregandoFornecedores ? <p className="muted p-3 text-xs">Procurando fornecedores...</p>
                : fornecedores.length ? fornecedores.map((f, i) => <button type="button" key={f.id}
                  id={`despesa-fornecedor-opcao-${i}`} role="option" aria-selected={opcaoFornecedor === i}
                  className="block w-full px-3 py-3 text-left text-xs border-b last:border-b-0"
                  style={{ borderColor: 'var(--border)', background: opcaoFornecedor === i ? 'var(--hover)' : 'transparent' }}
                  onMouseDown={e => e.preventDefault()} onMouseEnter={() => setOpcaoFornecedor(i)}
                  onClick={() => escolherFornecedor(f)}>
                  <span className="block font-semibold">{f.nomeFantasia || f.razaoSocial}</span>
                  {f.nomeFantasia && f.nomeFantasia !== f.razaoSocial && <span className="muted block mt-1">{f.razaoSocial}</span>}
                </button>) : <p className="muted p-3 text-xs">Nenhum fornecedor encontrado. Cadastre em Clientes e fornecedores.</p>}
            </div>}
          </div>
          <div className="grid sm:grid-cols-2 gap-3"><label className="field"><span>Categoria</span><select className="input" required value={form.categoriaId} onChange={e=>setForm(f=>({...f,categoriaId:e.target.value}))}><option value="">Selecione</option>{categorias.filter(c=>c.ativa).map(c=><option key={c.id} value={c.id}>{c.nome} ({c.tipo==='FIXA'?'Fixa':'Variável'})</option>)}</select></label><label className="field"><span>Valor (R$)</span><input className="input" inputMode="decimal" required value={form.valor} onChange={e=>setForm(f=>({...f,valor:e.target.value}))} placeholder="0,00"/></label></div>
          <div className="grid sm:grid-cols-2 gap-3"><label className="field"><span>Data de emissão</span><input className="input" type="date" required value={form.dataEmissao} max={hojeISO()} onChange={e=>setForm(f=>({...f,dataEmissao:e.target.value}))}/></label><label className="field"><span>Vencimento</span><input className="input" type="date" required value={form.dataVencimento} onChange={e=>setForm(f=>({...f,dataVencimento:e.target.value}))}/></label></div>
          <label className="field"><span>Observações (opcional)</span><input className="input" maxLength={300} value={form.observacao} onChange={e=>setForm(f=>({...f,observacao:e.target.value}))}/></label>
          <Button type="submit" disabled={busy}><Plus size={16}/> {busy?'Salvando...':'Cadastrar despesa pendente'}</Button>
        </form>
      </Card>
      <Card><CardTitle title="Categorias de despesas" subtitle="Separação por custos fixos e variáveis."/>
        <form className="space-y-3 mb-4" onSubmit={salvarCategoria}><label className="field"><span>Categoria</span><input className="input" maxLength={100} required placeholder="Ex.: Energia, Transporte, Salários" value={categoria.nome} onChange={e=>setCategoria(v=>({...v,nome:e.target.value}))}/></label><label className="field"><span>Tipo</span><select className="input" value={categoria.tipo} onChange={e=>setCategoria(v=>({...v,tipo:e.target.value as TipoCategoria}))}><option value="FIXA">Custo fixo</option><option value="VARIAVEL">Custo variável</option></select></label><Button type="submit" variant="secondary" disabled={busy}><Plus size={16}/> Criar categoria</Button></form>
        <div className="space-y-2">{categorias.map(c=><div key={c.id} className="flex justify-between items-center gap-2 text-sm py-2 border-t subtle-border"><span>{c.nome}</span><span className="muted text-xs">{c.tipo==='FIXA'?'Fixa':'Variável'}</span></div>)}</div>
      </Card>
    </div>}
    <div className="grid lg:grid-cols-[1.1fr_1fr] gap-5">
      <Card><CardTitle title="Pagamentos do mês por categoria" subtitle="Somente quitações manuais efetivas neste mês, sem estornos." action={<BarChart3 size={18} className="muted"/>}/>{privacidade ? <Empty title="Dados ocultos pelo modo privacidade"/> : resumo.length ? <div className="space-y-4">{resumo.map(c=><div key={c.nome}><div className="flex items-center justify-between gap-2 text-sm mb-2"><span>{c.nome}</span><span className="font-semibold">{brl(c.totalPagoMes)}</span></div><div className="track"><div className="track-fill" style={{width:`${100*c.totalPagoMes/max}%`}}/></div></div>)}</div> : <Empty title="Sem despesas pagas no mês"/>}</Card>
      <Card><CardTitle title="Fluxo de pagamento" subtitle="Uma conta pendente é quitada com a data real. É possível estornar pagamentos manuais."/><div className="space-y-3 text-sm"><div className="mini-stat">1. Cadastre ou selecione um fornecedor</div><div className="mini-stat">2. Crie a categoria e o título com vencimento</div><div className="mini-stat">3. Registre a quitação, inclusive retroativa</div><div className="mini-stat">4. Para corrigir uma baixa, estorne com justificativa</div></div></Card>
    </div>
    <Card><CardTitle title="Títulos a pagar" subtitle="Todas as contas a pagar da empresa, inclusive lançamentos manuais." action={<form onSubmit={e=>{e.preventDefault();setPage(0);setFiltro(busca)}} className="flex gap-2"><input className="input" placeholder="Buscar fornecedor ou documento" value={busca} onChange={e=>setBusca(e.target.value)}/><Button type="submit" variant="secondary"><Search size={16}/></Button></form>}/>
      {loading ? <Loading/> : lista?.content.length ? <div className="overflow-x-auto"><table className="data-table"><thead><tr><th>Descrição / documento</th><th>Fornecedor</th><th>Categoria</th><th>Vencimento</th><th>Valor</th><th>Saldo</th><th>Situação</th><th>Ação</th></tr></thead><tbody>{lista.content.map(t=><tr key={t.id}><td><div className="font-medium"><PrivateText>{t.descricao||t.numeroNota}</PrivateText></div><div className="text-xs muted">{t.origemArquivo==='MANUAL'?'Manual':'Importado'} · {t.numeroNota}</div></td><td><PrivateText>{t.cliente.nomeFantasia||t.cliente.razaoSocial}</PrivateText></td><td>{t.categoriaNome||'—'}</td><td>{dataBR(t.dataVencimento)}</td><td><PrivateText>{brl(t.valorOriginal)}</PrivateText></td><td><PrivateText>{brl(t.saldoDevedor)}</PrivateText></td><td><StatusPill {...(() => {const s=statusTitulo(t);return {label:s.texto,tone:s.cor}})()}/></td><td><Button variant="secondary" onClick={()=>{setDetalhe(t);setDataPagamento(hojeISO())}}>Detalhes</Button></td></tr>)}</tbody></table></div> : <Empty title="Nenhuma despesa encontrada" subtitle="Cadastre seu primeiro lançamento ou ajuste a busca."/>}
      {lista && <Pagination page={lista.number} totalPages={lista.totalPages} totalElements={lista.totalElements} onChange={setPage}/>}
    </Card>
    {detalhe && <Modal title="Detalhes e pagamentos" subtitle={detalhe.descricao||detalhe.numeroNota} onClose={()=>setDetalhe(null)}>
      <div className="grid sm:grid-cols-2 gap-3 mb-5"><div className="mini-stat">Vencimento: {dataBR(detalhe.dataVencimento)}</div><div className="mini-stat">Saldo: <PrivateText>{brl(detalhe.saldoDevedor)}</PrivateText></div></div>
      {podeEditar && detalhe.saldoDevedor>0 && <div className="flex flex-wrap items-end gap-3 mb-5"><label className="field"><span>Data real do pagamento</span><input type="date" className="input" value={dataPagamento} min={detalhe.dataEmissao} max={hojeISO()} onChange={e=>setDataPagamento(e.target.value)}/></label><Button disabled={busy} onClick={()=>void quitar(detalhe)}><CheckCircle2 size={16}/> Quitar saldo</Button></div>}
      {podeEditar && detalhe.origemArquivo==='MANUAL' && detalhe.categoriaId && <div className="mb-5"><Button variant="secondary" onClick={()=>repetirProximoMes(detalhe)}>Preparar repetição no próximo mês</Button><p className="text-xs muted mt-2">Não cria lançamentos automaticamente.</p></div>}
      <h3 className="font-semibold mb-3">Histórico de pagamentos</h3>
      {historico.length ? <div className="space-y-2">{historico.map(p=><div className="mini-stat flex flex-wrap justify-between items-center gap-3" key={p.id}><div><p className="font-semibold"><PrivateText>{brl(p.valor)}</PrivateText> · {dataBR(p.dataPagamento)}</p><p className="muted text-xs">{p.estornadoEm ? `Estornado: ${p.motivoEstorno}` : p.origem==='MANUAL'?'Pagamento manual':'Importado'}</p></div>{podeEditar && !p.estornadoEm && p.origem==='MANUAL' && <Button variant="secondary" disabled={busy} onClick={()=>void estornar(detalhe,p)}><RotateCcw size={15}/> Estornar</Button>}</div>)}</div> : <Empty title="Nenhum pagamento registrado"/>}
    </Modal>}
  </div>
}
