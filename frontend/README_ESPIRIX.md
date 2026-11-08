# Espirix Frontend

Frontend completo da plataforma Espirix by Espirito Labs.

## Stack

React + TypeScript + Vite + Tailwind CSS v4 + Axios + Lucide React + Recharts.

## API e execução

O frontend usa URLs relativas `/api`, configuradas no `vite.config.ts` para encaminhar a API Spring Boot local (`http://localhost:8080`). O backend Java deve ser iniciado separadamente.

```powershell
npm.cmd install axios lucide-react recharts tailwindcss @tailwindcss/vite
npm.cmd run build
npm.cmd run dev
```

## Telas

- `#/dashboard`: visão geral, evolução, vencimentos e rankings.
- `#/titulos`: filtros, detalhes, pagamentos e baixas em lote.
- `#/clientes`: saldos consolidados, cadastros, evolução por cliente.
- `#/importacoes`: upload e histórico CSV/PDF.
- `#/configuracoes`: tema, privacidade visual, conectividade.

O frontend foi estruturado de acordo com os contratos `backend/src/main/java/br/com/espiritolabs/espirix/dto/ApiDtos.java` do primeiro backend, sem endpoints simulados.

**Atenção:** interface para desenvolvimento local. Sem autenticação ou autorização no backend; não utilizar com dados reais em servidor público.
