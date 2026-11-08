/** Tipos espelhados dos records do backend Java (ApiDtos.java). */
export type TipoOperacao = 'RECEBER' | 'PAGAR'
export type StatusTitulo = 'A_VENCER' | 'VENCIDA' | 'PAGA' | 'PAGA_PARCIAL'
export type StatusImportacao = 'SUCESSO' | 'FALHA'
export type OrigemArquivo = 'CSV' | 'PDF'
export type OrigemPagamento = 'MANUAL' | 'IMPORTACAO'

export interface Cliente {
  id: number
  razaoSocial: string
  nomeFantasia: string | null
  cnpjCpf: string | null
  dataCadastro: string
}

export interface Titulo {
  id: number
  numeroNota: string
  numeroParcela: string
  cliente: Cliente
  tipoOperacao: TipoOperacao
  dataEmissao: string
  dataVencimento: string
  dataPagamento: string | null
  valorOriginal: number
  valorPago: number
  saldoDevedor: number
  status: StatusTitulo
  emAtraso: boolean
  origemArquivo: OrigemArquivo
}

export interface Pagamento {
  id: number
  tituloId: number
  valor: number
  dataPagamento: string
  origem: OrigemPagamento
  observacao: string | null
  registradoEm: string
}

export interface Importacao {
  id: number
  nomeArquivo: string
  tipoArquivo: OrigemArquivo
  dataImportacao: string
  quantidadeRegistrosNovos: number
  quantidadeAtualizados: number
  quantidadeIgnorados: number
  status: StatusImportacao
  mensagem: string | null
}

export interface ResumoDashboard {
  tipoOperacao: TipoOperacao
  dataReferencia: string
  dividaAtiva: number
  totalVencido: number
  aVencer7Dias: number
  aVencer15Dias: number
  aVencer30Dias: number
  totalQuitadoMes: number
  totalPagamentosMes: number
  quantidadeTitulosAbertos: number
}

export interface PontoEvolucao {
  periodo: string
  novasNotas: number
  pagamentos: number
  saldoAcumulado: number
}

export interface FaixaPrevisao { faixa: string; valor: number; quantidade: number }
export interface Devedor {
  clienteId: number
  nomeFantasia: string | null
  razaoSocial: string
  saldoDevedor: number
  quantidadeNotasEmAberto: number
}
export interface ConsolidadoCliente {
  clienteId: number
  razaoSocial: string
  nomeFantasia: string | null
  quantidadeNotasEmAberto: number
  valorOriginal: number
  valorPago: number
  saldoDevedor: number
  percentualQuitado: number
}

export interface PageResult<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface FiltrosTitulos {
  busca?: string
  emissaoDe?: string
  emissaoAte?: string
  vencimentoDe?: string
  vencimentoAte?: string
  status?: StatusTitulo | ''
  somenteAbertos?: boolean
}

export interface BaixaItem { tituloId: number; valor: number; dataPagamento: string }
export interface ErrorResponse {
  status?: number
  erro?: string
  mensagem?: string
  detalhes?: string[]
}
