import axios from 'axios'
import type {
  BaixaItem, Cliente, ConsolidadoCliente, Devedor, FaixaPrevisao, FiltrosTitulos,
  Importacao, PageResult, Pagamento, PontoEvolucao, ResumoDashboard, TipoOperacao, Titulo,
  ErrorResponse,
} from '../types/api'

/** O Vite redireciona /api para localhost:8080; não expõe o backend na internet. */
const http = axios.create({ baseURL: '/api', timeout: 20000, withCredentials: true })
let csrfToken: string | null = null
async function getCsrf() {
  if (!csrfToken) {
    const response = await axios.get<{ token: string }>('/api/auth/csrf', { withCredentials: true })
    csrfToken = response.data.token
  }
  return csrfToken
}
http.interceptors.request.use(async config => {
  if (config.method && !['get', 'head', 'options'].includes(config.method.toLowerCase())) {
    config.headers.set('X-CSRF-TOKEN', await getCsrf())
  }
  return config
})
export type Role = 'ADMIN_MASTER' | 'ADMIN' | 'FINANCEIRO' | 'CONSULTA'
export interface EmpresaInfo { id: number; slug: string; nome: string }
export interface Me { id: number; nome: string; email: string; perfil: Role; empresa: EmpresaInfo }
export interface Usuario { id: number; email: string; nome: string; perfil: Role; ativo: boolean }
export interface LimpezaPreview { empresa: string; slug: string; clientes: number; titulos: number; pagamentos: number; importacoes: number; permitido: boolean; confirmacao: string }


export function mensagemErro(error: unknown): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    if (!error.response) return 'Não foi possível conectar à API do Espirix.'
    const payload = error.response.data
    return payload?.mensagem || payload?.detalhes?.join('; ') || `Erro HTTP ${error.response.status}.`
  }
  return error instanceof Error ? error.message : 'Ocorreu um erro inesperado.'
}

export const api = {
  async me() { return (await http.get<Me>('/auth/me')).data },
  async login(email: string, senha: string) {
    const params = new URLSearchParams({ username: email, password: senha })
    await http.post('/auth/login', params, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
    csrfToken = null  // Spring invalida CSRF apos autenticar
    return (await http.get<Me>('/auth/me')).data
  },
  async logout() { await http.post('/auth/logout'); csrfToken = null },
  async empresas() { return (await http.get<EmpresaInfo[]>('/auth/empresas')).data },
  async workspace(empresaId: number) { return (await http.post<Me>('/auth/workspace', { empresaId })).data },
  async usuarios() { return (await http.get<Usuario[]>('/auth/usuarios')).data },
  async criarUsuario(u: { email: string; nome: string; senha: string; perfil: Exclude<Role,'ADMIN_MASTER'> }) {
    return (await http.post<Usuario>('/auth/usuarios', u)).data
  },
  async limpezaPreview() { return (await http.get<LimpezaPreview>('/admin/limpeza/preview')).data },
  async limparWorkspace(confirmacao: string) {
    return (await http.post<LimpezaPreview>('/admin/limpeza/executar', { confirmacao })).data
  },

  async resumo(tipoOperacao: TipoOperacao) {
    return (await http.get<ResumoDashboard>('/dashboard/resumo', { params: { tipoOperacao } })).data
  },
  async evolucao(tipoOperacao: TipoOperacao, meses = 12) {
    return (await http.get<PontoEvolucao[]>('/dashboard/evolucao', { params: { tipoOperacao, meses } })).data
  },
  async previsao(tipoOperacao: TipoOperacao) {
    return (await http.get<FaixaPrevisao[]>('/dashboard/previsao', { params: { tipoOperacao } })).data
  },
  async topDevedores(tipoOperacao: TipoOperacao, limite = 5) {
    return (await http.get<Devedor[]>('/dashboard/top-devedores', { params: { tipoOperacao, limite } })).data
  },
  async titulos(tipoOperacao: TipoOperacao, filtros: FiltrosTitulos, page = 0, size = 10) {
    const cleaned = Object.fromEntries(Object.entries(filtros).filter(([, value]) => value !== '' && value !== undefined))
    const params = { ...cleaned, tipoOperacao, page, size }
    return (await http.get<PageResult<Titulo>>('/titulos', { params })).data
  },
  async titulo(id: number) { return (await http.get<Titulo>(`/titulos/${id}`)).data },
  async pagamentos(id: number) { return (await http.get<Pagamento[]>(`/titulos/${id}/pagamentos`)).data },
  async pagar(id: number, payload: { valor: number; dataPagamento: string; observacao: string }) {
    return (await http.post<Titulo>(`/titulos/${id}/pagamentos`, payload)).data
  },
  async pagarLote(pagamentos: BaixaItem[], observacao: string) {
    return (await http.post<Titulo[]>('/titulos/pagamentos/lote', { pagamentos, observacao })).data
  },
  async quitarLote(titulosIds: number[], dataPagamento: string, observacao: string) {
    return (await http.post<Titulo[]>('/titulos/quitar-lote', { titulosIds, dataPagamento, observacao })).data
  },
  async clientes(busca = '', page = 0, size = 15) {
    return (await http.get<PageResult<Cliente>>('/clientes', { params: { busca, page, size } })).data
  },
  async consolidado(tipoOperacao: TipoOperacao, busca = '') {
    return (await http.get<ConsolidadoCliente[]>('/clientes/consolidado', { params: { tipoOperacao, busca } })).data
  },
  async cliente(id: number) { return (await http.get<Cliente>(`/clientes/${id}`)).data },
  async evolucaoCliente(id: number, tipoOperacao: TipoOperacao, meses = 12) {
    return (await http.get<PontoEvolucao[]>(`/clientes/${id}/evolucao`, { params: { tipoOperacao, meses } })).data
  },
  async importacoes(page = 0, size = 10) {
    return (await http.get<PageResult<Importacao>>('/importacoes', { params: { page, size } })).data
  },
  async importar(arquivo: File, tipoOperacao?: TipoOperacao) {
    const data = new FormData()
    data.append('arquivo', arquivo)
    return (await http.post<Importacao>('/importacoes', data, {
      params: tipoOperacao ? { tipoOperacao } : undefined,
      timeout: 120000,
      // Axios define o boundary multipart automaticamente.
    })).data
  },
}
