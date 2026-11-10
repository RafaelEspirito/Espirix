import { useEffect, useState } from 'react'
import { Activity, CheckCircle2, ExternalLink, Eye, EyeOff, GitBranch, Monitor, Moon, RefreshCw, Server, ShieldAlert, Sun } from 'lucide-react'
import { useAuth } from '../contexts/Auth'
import type { LimpezaPreview, Role, Usuario } from '../services/api'
import { api, mensagemErro } from '../services/api'
import { usePreferences } from '../contexts/Preferences'
import { Button, Card, CardTitle, ErrorNotice, SectionHeading, StatusPill } from '../components/UI'

export function Configuracoes() {
  const { theme, toggleTheme, privacidade, togglePrivacidade, tipo } = usePreferences()
  const { user } = useAuth()
  const isAdmin = user?.perfil === 'ADMIN' || user?.perfil === 'ADMIN_MASTER'
  const [preview,setPreview]=useState<LimpezaPreview|null>(null)
  const [confirmacao,setConfirmacao]=useState('')
  const [clearing,setClearing]=useState(false)
  const [clearMessage,setClearMessage]=useState('')
  const [adminError,setAdminError]=useState('')
  const [senhaAtual,setSenhaAtual]=useState('')
  const [novaSenha,setNovaSenha]=useState('')
  const [senhaAviso,setSenhaAviso]=useState('')
  const [alterandoSenha,setAlterandoSenha]=useState(false)
  const [usuarioNome,setUsuarioNome]=useState('')
  const [usuarioEmail,setUsuarioEmail]=useState('')
  const [usuarioSenha,setUsuarioSenha]=useState('')
  const [usuarioPerfil,setUsuarioPerfil]=useState<Exclude<Role,'ADMIN_MASTER'>>('FINANCEIRO')
  const [usuarios,setUsuarios]=useState<Usuario[]>([])
  useEffect(()=>{if(isAdmin){void api.limpezaPreview().then(setPreview).catch(e=>setAdminError(mensagemErro(e)));void api.usuarios().then(setUsuarios).catch(()=>{})}},[isAdmin,user?.empresa.id])
  async function cadastrar(event:React.FormEvent){event.preventDefault();setAdminError('');try{
    await api.criarUsuario({nome:usuarioNome,email:usuarioEmail,senha:usuarioSenha,perfil:usuarioPerfil});
    setUsuarioNome('');setUsuarioEmail('');setUsuarioSenha('');setUsuarios(await api.usuarios());setClearMessage('Usuário criado com sucesso.')
  }catch(e){setAdminError(mensagemErro(e))}}
  async function limpar(){if(!preview || confirmacao!==preview.confirmacao || clearing)return;
    if(!window.confirm(`Última confirmação: excluir todos os dados financeiros de ${preview.empresa}?`))return;
    setClearing(true);setAdminError('');try{await api.limparWorkspace(confirmacao);setPreview(await api.limpezaPreview());setConfirmacao('');setClearMessage('Dados financeiros deste ambiente excluídos. Atualize o dashboard.')}catch(e){setAdminError(mensagemErro(e))}finally{setClearing(false)}}

  async function alterarSenha(event:React.FormEvent) {
    event.preventDefault()
    setSenhaAviso('');setAlterandoSenha(true)
    try {
      await api.alterarMinhaSenha(senhaAtual,novaSenha)
      setSenhaAtual('');setNovaSenha('')
      setSenhaAviso('Senha atualizada. Encerre a sessão e entre com a nova senha.')
    } catch(e) {setSenhaAviso(mensagemErro(e))}
    finally {setAlterandoSenha(false)}
  }
  const [checking, setChecking] = useState(false)
  const [connection, setConnection] = useState<'unknown' | 'ok' | 'error'>('unknown')
  const [error, setError] = useState('')
  async function check() { setChecking(true); setError(''); try { await api.resumo(tipo); setConnection('ok') } catch (e) { setConnection('error'); setError(mensagemErro(e)) } finally { setChecking(false) } }
  return <div><SectionHeading title="Preferências" subtitle="Personalize a interface e verifique a conectividade com o backend."/><div className="grid lg:grid-cols-2 gap-5"><Card><CardTitle title="Aparência" subtitle="As preferências ficam salvas somente neste navegador."/><div className="settings-row"><div className="flex gap-3 items-center"><div className="settings-icon">{theme === 'dark' ? <Moon size={19}/> : <Sun size={19}/>}</div><div><p className="font-semibold text-sm">Tema {theme === 'dark' ? 'escuro' : 'claro'}</p><p className="muted text-xs mt-1">Alterne entre os temas do Espirix.</p></div></div><Button variant="secondary" onClick={toggleTheme}>{theme === 'dark' ? 'Tema claro' : 'Tema escuro'}</Button></div><div className="settings-row"><div className="flex gap-3 items-center"><div className="settings-icon">{privacidade ? <EyeOff size={19}/> : <Eye size={19}/>}</div><div><p className="font-semibold text-sm">Modo privacidade</p><p className="muted text-xs mt-1">Oculta valores e nomes sensíveis na interface.</p></div></div><Button variant="secondary" onClick={togglePrivacidade}>{privacidade ? 'Desativar' : 'Ativar'}</Button></div><div className="info-note mt-5"><ShieldAlert size={19} className="shrink-0 text-amber-500"/><p>Este modo é apenas visual: não criptografa, não anonimiza e não restringe respostas da API ou dados no navegador.</p></div></Card>
    <Card><CardTitle title="Integração com a API" subtitle="Conectividade entre a interface e a API protegida."/><div className="settings-row"><div className="flex items-center gap-3"><div className="settings-icon"><Server size={19}/></div><div><p className="font-semibold text-sm">Spring Boot</p><p className="muted text-xs mt-1">API via /api · mesma origem</p></div></div><StatusPill tone={connection === 'ok' ? 'green' : connection === 'error' ? 'red' : 'neutral'} label={connection === 'ok' ? 'Conectado' : connection === 'error' ? 'Indisponível' : 'Não verificado'}/></div><Button className="mt-5" disabled={checking} onClick={check}><RefreshCw size={16} className={checking ? 'animate-spin' : ''}/>{checking ? 'Verificando...' : 'Testar conexão'}</Button>{error && <div className="mt-4"><ErrorNotice error={error}/></div>}<p className="text-xs muted mt-5">Teste realizado com <code>GET /api/dashboard/resumo</code>; nenhum dado é modificado.</p></Card>
    <Card><CardTitle title="Tecnologias e ambiente"/><div className="space-y-4"><div className="settings-row !py-0"><div className="flex items-center gap-3"><div className="settings-icon"><Monitor size={18}/></div><div><p className="text-sm font-semibold">Frontend</p><p className="muted text-xs">React · TypeScript · Vite · Tailwind</p></div></div><StatusPill label="Web" tone="blue"/></div><div className="settings-row"><div className="flex items-center gap-3"><div className="settings-icon"><Activity size={18}/></div><div><p className="text-sm font-semibold">Backend</p><p className="muted text-xs">Java 21 · Spring Boot · H2/PostgreSQL</p></div></div></div></div></Card>
    <Card><CardTitle title="Sobre o projeto"/><div className="rounded-xl bg-[var(--surface-2)] p-5"><div className="text-xl font-extrabold">espirix<span className="text-cyan-500">.</span></div><div className="muted text-xs uppercase tracking-widest mt-1">by Espirito Labs</div><p className="text-sm leading-relaxed mt-4 muted">Plataforma modular de gestão financeira, inicialmente focada em títulos, recebimentos, pagamentos e importações de relatórios.</p><div className="flex gap-2 items-center text-xs mt-4"><CheckCircle2 size={16} className="text-emerald-500"/> Frontend conectado aos contratos do backend inicial.</div></div><a href="https://github.com/RafaelEspirito/Espirix" target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-2 mt-5 text-sm font-semibold text-cyan-500 hover:underline"><GitBranch size={16}/> Ver código-fonte <ExternalLink size={14}/></a></Card></div>
    <div className="mt-6 max-w-xl"><Card><CardTitle title="Segurança da conta" subtitle="Altere sua senha de acesso. Não compartilhe credenciais entre usuários."/>
      <form className="space-y-3" onSubmit={alterarSenha}>
        <label className="field"><span>Senha atual</span><input type="password" autoComplete="current-password" className="input" required value={senhaAtual} onChange={e=>setSenhaAtual(e.target.value)}/></label>
        <label className="field"><span>Nova senha (mín. 12 caracteres)</span><input type="password" autoComplete="new-password" minLength={12} maxLength={128} className="input" required value={novaSenha} onChange={e=>setNovaSenha(e.target.value)}/></label>
        <Button type="submit" disabled={alterandoSenha}>{alterandoSenha?'Alterando...':'Atualizar minha senha'}</Button>
      </form>{senhaAviso && <p role="status" className="text-sm mt-3 muted">{senhaAviso}</p>}
    </Card></div>
    {isAdmin && <div className="grid lg:grid-cols-2 gap-5 mt-6">
      <Card><CardTitle title="Gerenciar equipe" subtitle={`Usuários de ${user?.empresa.nome}. Somente este ambiente.`}/>
        <form className="space-y-3" onSubmit={cadastrar}>
          <label className="field"><span>Nome do usuário</span><input className="input" required value={usuarioNome} onChange={e=>setUsuarioNome(e.target.value)}/></label>
          <label className="field"><span>E-mail</span><input className="input" type="email" required value={usuarioEmail} onChange={e=>setUsuarioEmail(e.target.value)}/></label>
          <label className="field"><span>Senha inicial (mín. 12 caracteres)</span><input className="input" type="password" minLength={12} required value={usuarioSenha} onChange={e=>setUsuarioSenha(e.target.value)}/></label>
          <label className="field"><span>Permissão</span><select className="input" value={usuarioPerfil} onChange={e=>setUsuarioPerfil(e.target.value as Exclude<Role,'ADMIN_MASTER'>)}>
            <option value="FINANCEIRO">Financeiro</option><option value="CONSULTA">Somente consulta</option>{user?.perfil==='ADMIN_MASTER' && <option value="ADMIN">Administrador da empresa</option>}
          </select></label>
          <Button type="submit">Criar acesso</Button>
        </form><p className="text-xs muted mt-4">Usuários cadastrados nesta empresa: {usuarios.length}.</p>
      </Card>
      <Card><CardTitle title="Zona de perigo — limpeza do ambiente" subtitle="Limpa apenas o ambiente ativo. Também remove pagamentos e clientes vinculados."/>
        {preview && <><p className="text-sm mt-2 font-semibold">Ambiente: {preview.empresa}</p>
          <div className="text-sm muted mt-3 space-y-1"><p>{preview.importacoes} importações</p><p>{preview.titulos} títulos</p><p>{preview.pagamentos} pagamentos</p><p>{preview.clientes} clientes</p></div>
          {preview.permitido ? <><p className="text-xs mt-4">Para confirmar, digite exatamente <strong>{preview.confirmacao}</strong></p>
          <input className="input mt-2" value={confirmacao} onChange={e=>setConfirmacao(e.target.value)} autoComplete="off" aria-label="Frase de confirmação"/>
          <button className="mt-4 px-4 py-2 rounded-xl bg-red-700 text-white font-semibold disabled:opacity-40" disabled={confirmacao!==preview.confirmacao || clearing} onClick={()=>void limpar()}>{clearing?'Limpando...':'Excluir TODOS os dados financeiros deste ambiente'}</button></>
          : <p className="text-sm text-amber-500 mt-4">Limpeza bloqueada pelo servidor neste ambiente. Exige habilitação administrativa explícita.</p>}</>}
        {adminError && <p role="alert" className="text-sm text-red-400 mt-3">{adminError}</p>}
        {clearMessage && <p role="status" className="text-sm text-emerald-500 mt-3">{clearMessage}</p>}
      </Card>
    </div>}
    <div className="info-note mt-6"><ShieldAlert className="shrink-0 text-amber-500" size={19}/><p><strong>Segurança de produção.</strong> A publicação externa requer autorização da empresa, backups com restauração testada, MFA e homologação da infraestrutura.</p></div>
  </div>
}
