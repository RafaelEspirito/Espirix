import { useState, type FormEvent } from 'react'
import { BarChart3, LockKeyhole } from 'lucide-react'
import { mensagemErro } from '../services/api'
import { useAuth } from '../contexts/Auth'
export function Login() {
  const { login } = useAuth()
  const [email,setEmail] = useState('')
  const [senha,setSenha] = useState('')
  const [busy,setBusy] = useState(false)
  const [error,setError] = useState('')
  const submit=async(e:FormEvent)=>{e.preventDefault();setBusy(true);setError('');try{await login(email,senha)}catch(err){setError(mensagemErro(err))}finally{setBusy(false)}}
  return <div className="min-h-screen flex items-center justify-center p-6 bg-[var(--background)] text-[var(--text)]">
    <div className="w-full max-w-md rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-8 shadow-xl">
      <div className="flex gap-3 items-center mb-7"><div className="brand-icon"><BarChart3/></div><div><h1 className="font-extrabold text-2xl">espirix<span className="text-cyan-400">.</span></h1><p className="text-sm muted">Acesso à plataforma • Espirito Labs</p></div></div>
      <h2 className="font-semibold text-xl mb-2">Entrar na sua conta</h2><p className="muted text-sm mb-6">Os dados são separados por empresa e ambiente.</p>
      <form onSubmit={submit} className="space-y-4"><label className="field"><span>E-mail</span><input className="input" type="email" autoComplete="username" required value={email} onChange={e=>setEmail(e.target.value)} /></label>
      <label className="field"><span>Senha</span><input className="input" type="password" autoComplete="current-password" required value={senha} onChange={e=>setSenha(e.target.value)}/></label>
      {error && <p role="alert" className="text-red-400 text-sm">{error}</p>}
      <button className="w-full inline-flex gap-2 items-center justify-center rounded-xl p-3 bg-cyan-600 text-white font-semibold disabled:opacity-50" disabled={busy}><LockKeyhole size={17}/>{busy?'Entrando...':'Entrar'}</button></form>
      <p className="muted text-xs mt-6">Não há cadastro público. Solicite seu acesso ao administrador.</p>
    </div></div>
}
