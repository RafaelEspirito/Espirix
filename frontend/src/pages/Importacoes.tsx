import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { CheckCircle2, CloudUpload, FileClock, FileSpreadsheet, FileText, RefreshCw, UploadCloud, X } from 'lucide-react'
import { api, mensagemErro } from '../services/api'
import type { Importacao, PageResult, TipoOperacao } from '../types/api'
import { dataHoraBR, inteiro } from '../lib/format'
import { usePreferences } from '../contexts/Preferences'
import { Button, Card, CardTitle, Empty, ErrorNotice, Loading, Pagination, PrivateText, SectionHeading, StatusPill, SuccessNotice } from '../components/UI'

const MAX_FILE = 10 * 1024 * 1024
export function Importacoes() {
  const { tipo } = usePreferences()
  const [file, setFile] = useState<File | null>(null)
  const [uploadTipo, setUploadTipo] = useState<TipoOperacao | 'AUTO'>(tipo)
  const [dragging, setDragging] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [history, setHistory] = useState<PageResult<Importacao> | null>(null)
  const [loading, setLoading] = useState(true)
  const fileInput = useRef<HTMLInputElement>(null)
  useEffect(() => { setUploadTipo(tipo) }, [tipo])
  useEffect(() => { let active = true; setLoading(true); api.importacoes(page, 10).then(r => { if (active) setHistory(r) }).catch(e => { if (active) setError(mensagemErro(e)) }).finally(() => { if (active) setLoading(false) }); return () => { active = false } }, [page, reload])
  function selectFile(next?: File) {
    if (!next) return
    setSuccess(''); setError('')
    if (!/\.(csv|pdf)$/i.test(next.name)) { setError('Formato não aceito: selecione um arquivo CSV ou PDF.'); return }
    if (!next.size || next.size > MAX_FILE) { setError('O arquivo deve ter conteúdo e até 10 MB.'); return }
    setFile(next)
  }
  async function upload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setSuccess('')
    if (!file) { setError('Selecione um arquivo para importar.'); return }
    setBusy(true)
    try {
      const result = await api.importar(file, uploadTipo === 'AUTO' ? undefined : uploadTipo)
      setSuccess(`Importação ${result.status === 'SUCESSO' ? 'concluída' : 'registrada'}: ${result.quantidadeRegistrosNovos} novo(s), ${result.quantidadeAtualizados} atualizado(s) e ${result.quantidadeIgnorados} ignorado(s).`)
      setFile(null); if (fileInput.current) fileInput.current.value = ''; setPage(0); setReload(v => v + 1)
    } catch (e) { setError(mensagemErro(e)); setReload(v => v + 1) }
    finally { setBusy(false) }
  }
  return <div><SectionHeading title="Importações" subtitle="Envie relatórios CSV ou PDF e acompanhe o processamento pelo backend." action={<Button variant="secondary" onClick={() => setReload(v => v + 1)}><RefreshCw size={16}/> Atualizar histórico</Button>}/>
    {error && <div className="mb-5"><ErrorNotice error={error}/></div>}
    {success && <div className="mb-5"><SuccessNotice message={success} onClose={() => setSuccess('')}/></div>}
    <div className="grid lg:grid-cols-[1.15fr_.85fr] gap-5 mb-6"><Card><CardTitle title="Nova importação" subtitle="O arquivo é validado pelo Java e processado em uma transação."/><form onSubmit={upload} className="space-y-5"><input aria-label="Selecionar arquivo" ref={fileInput} accept=".csv,.pdf,text/csv,application/pdf" type="file" className="hidden" onChange={e => selectFile(e.currentTarget.files?.[0])}/><div className={`dropzone ${dragging ? 'dropzone-active' : ''}`} onDragEnter={e => { e.preventDefault(); setDragging(true) }} onDragOver={e => e.preventDefault()} onDragLeave={e => { e.preventDefault(); setDragging(false) }} onDrop={e => { e.preventDefault(); setDragging(false); selectFile(e.dataTransfer.files?.[0]) }}><div className="upload-icon"><CloudUpload size={28}/></div><p className="font-semibold mt-4">Arraste seu arquivo aqui</p><p className="text-xs muted mt-1">CSV ou PDF com texto selecionável · Até 10 MB</p><Button type="button" variant="secondary" className="mt-4" onClick={() => fileInput.current?.click()}><FileSpreadsheet size={16}/> Escolher arquivo</Button></div>{file && <div className="mini-stat flex items-center gap-3"><FileText size={22} className="text-cyan-500"/><div className="min-w-0 flex-1"><p className="font-semibold text-sm truncate">{file.name}</p><p className="muted text-xs">{(file.size / 1024).toFixed(1)} KB</p></div><button type="button" className="icon-button" aria-label="Remover arquivo" onClick={() => { setFile(null); if (fileInput.current) fileInput.current.value = '' }}><X size={18}/></button></div>}<label className="field"><span>Tipo de operação</span><select className="input" value={uploadTipo} onChange={e => setUploadTipo(e.target.value as TipoOperacao | 'AUTO')}><option value="RECEBER">Contas a receber</option><option value="PAGAR">Contas a pagar</option><option value="AUTO">Ler do arquivo (padrão RECEBER se ausente)</option></select></label><Button type="submit" disabled={!file || busy} className="w-full justify-center"><UploadCloud size={17}/>{busy ? 'Enviando e validando...' : 'Importar relatório'}</Button></form></Card>
      <Card><CardTitle title="Como funciona" subtitle="Importação segura para o ambiente local."/><div className="space-y-5"><Instruction n="01" title="Escolha o arquivo" desc="Relatórios CSV com cabeçalhos compatíveis ou PDF com texto selecionável."/><Instruction n="02" title="Defina a operação" desc="O tipo escolhido pode definir contas a pagar ou receber. Evite misturar categorias."/><Instruction n="03" title="Revise o resultado" desc="O backend informa registros novos, atualizados e ignorados, impedindo duplicidades."/></div><div className="info-note mt-6"><CheckCircle2 size={18} className="shrink-0 text-cyan-500"/><p>Falhas de validação impedem alterações parciais. PDFs escaneados não possuem OCR. Utilize apenas dados fictícios ou anonimizados.</p></div></Card></div>
    <Card><CardTitle title="Histórico de importações" subtitle="Arquivos enviados, resultados e mensagens do processamento."/>{loading ? <Loading/> : history?.content.length ? <div className="overflow-x-auto"><table className="data-table"><thead><tr><th>Arquivo</th><th>Data</th><th>Situação</th><th>Novos</th><th>Atualizados</th><th>Ignorados</th><th>Mensagem</th></tr></thead><tbody>{history.content.map(item => <tr key={item.id}><td className="font-semibold"><div className="flex items-center gap-2"><FileSpreadsheet size={15} className="text-cyan-500 shrink-0"/><PrivateText>{item.nomeArquivo}</PrivateText></div><div className="text-xs muted mt-1">{item.tipoArquivo}</div></td><td className="whitespace-nowrap">{dataHoraBR(item.dataImportacao)}</td><td><StatusPill tone={item.status === 'SUCESSO' ? 'green' : 'red'} label={item.status === 'SUCESSO' ? 'Sucesso' : 'Falha'}/></td><td><PrivateText>{inteiro(item.quantidadeRegistrosNovos)}</PrivateText></td><td><PrivateText>{inteiro(item.quantidadeAtualizados)}</PrivateText></td><td><PrivateText>{inteiro(item.quantidadeIgnorados)}</PrivateText></td><td className="text-xs muted min-w-36"><PrivateText>{item.mensagem || '—'}</PrivateText></td></tr>)}</tbody></table></div> : <Empty icon={<FileClock size={27}/>} title="Nenhuma importação registrada" subtitle="O histórico aparecerá após o primeiro envio."/>}{history && <Pagination page={history.number} totalPages={history.totalPages} totalElements={history.totalElements} onChange={setPage}/>}</Card>
  </div>
}
function Instruction({ n, title, desc }: { n: string; title: string; desc: string }) { return <div className="flex gap-3 items-start"><div className="step-number">{n}</div><div><p className="text-sm font-semibold">{title}</p><p className="text-xs muted mt-1 leading-relaxed">{desc}</p></div></div> }
