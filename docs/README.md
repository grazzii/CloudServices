## Serviços em Nuvem - 05J

# Projeto Integrador – Cloud Developing 2025/1

**URL do backend (EC2/EIP):** [http://54.197.119.102:8080](http://54.197.119.102:8080)

CRUD simples + API Gateway + Lambda `/report` + RDS

---

## Grupo

* 10425431 – **Graziely de Oliveira Severo** – Spring Boot / RDS / API Gateway / Lambda / Documentação / Vídeo 2
* 10428577 – **Marcos Minhano** – EC2 / VPC / Spring Boot / Documentação / Vídeo 1

---

## 1) Visão Geral

O **Portal de Doações Ajudaê** é uma aplicação simples que reúne campanhas solidárias e permite visualizar informações, metas e doações realizadas.

* **Backend:** Spring Boot (Java), containerizado
* **Frontend:** React (JavaScript)
* **Infra AWS:** EC2, RDS MySQL, API Gateway e Lambda (rota `/report`)

> A instância EC2 utiliza **Elastic IP**, garantindo que o endereço público do backend permaneça fixo e acessível em [http://54.197.119.102:8080](http://54.197.119.102:8080)

---

## 2) Arquitetura



| Camada       | Serviço                      | Descrição                                                                              |
| ------------ | ---------------------------- | -------------------------------------------------------------------------------------- |
| **Frontend** | React (JavaScript)           | Para deixar o design bonito e interativo                                               |
| **Backend**  | EC2 (Docker + Spring Boot)   | API REST responsável pelo CRUD de campanhas e doações                                  |
| **Banco**    | Amazon RDS (MySQL – privada) | Armazena os dados de campanhas e doações; acesso restrito ao backend                   |
| **Gateway**  | Amazon API Gateway           | Encaminha as rotas `/campanhas` para a EC2 e `/report` para a Lambda                   |
| **Função**   | AWS Lambda (`/report`)       | Gera um relatório com totais doaçoes e campanhas, top arrecadações e top causas        |

---

## 3) Endpoints REST

| Método | Endpoint               | Descrição                                                                                               | 
| ------ | ---------------------- | ------------------------------------------------------------------------------------------------------  |
| GET    | `/campanhas`           | Retorna todas as campanhas e suas doações                                                               |
| GET    | `/campanhas/{id}`      | Detalha uma campanha específica                                                                         |
| POST   | `/campanhas`           | Cria uma nova campanha                                                                                  |
| PUT    | `/campanhas/{id}`      | Atualiza uma campanha existente                                                                         |
| PUT    | `/campanhas/{id}/doar` | Registra uma nova doação pra campanha específica                                                        |
| DELETE | `/campanhas/{id}`      | Exclui uma campanha                                                                                     |
| GET    | `/report` (Lambda)     | Gera relatório consolidado com totais e ranking das campanhas com maior arrecadamento e causas similares|

---

## 4) Banco de Dados (Amazon RDS – MySQL)

O banco foi criado dentro da **VPC**, em uma **sub-rede privada**, sem acesso direto da internet.
Apenas a EC2 pode se conectar a ele, pela porta **3306**, via Security Group configurado.

O RDS armazena todas as campanhas e doações.
**Endpoint:** `lab.cn00y8o8m69e.us-east-1.rds.amazonaws.com`

---

## 5) Função Lambda `/report`

A função acessa o endpoint do backend (`/api/campanhas`) e retorna um JSON com:

* totais de campanhas e doações,
* top 3 campanhas com maior arrecadação,
* top 5 causas mais frequentes.

**Variáveis de ambiente:**
`BACKEND_BASE_URL = http://54.197.119.102:8080`

**Código principal:**

```js
export const handler = async () => {
  const base = process.env.BACKEND_BASE_URL || "http://52.90.62.231:8080";
  const url = `${base}/api/campanhas`;
  const N = (v) => Number(v ?? 0);
  const pct = (num, den) => (den > 0 ? ((num / den) * 100) : 0);
  const brl = (v) => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(N(v));

  try {
    const r = await fetch(url);
    const data = await r.json();

    const campanhas = data.map(c => ({
      id: c.id,
      nome: c.nome,
      causa: c.causa || "Indefinida",
      meta: N(c.meta),
      arrec: N(c.valorArrecadado),
      progresso: pct(N(c.valorArrecadado), N(c.meta))
    }));

    const totalCampanhas = campanhas.length;
    const totalArrecadado = campanhas.reduce((acc, c) => acc + c.arrec, 0);
    const totalDoacoes = data.reduce((acc, c) => acc + (c.doacoes?.length || c.quantidadeDoacoes || 0), 0);

    const topArrecadados = campanhas
      .sort((a, b) => b.arrec - a.arrec)
      .slice(0, 3)
      .map(c => ({ nome: c.nome, causa: c.causa, arrecadado: brl(c.arrec), meta: brl(c.meta) }));

    const causas = {};
    campanhas.forEach(c => {
      if (!causas[c.causa]) causas[c.causa] = { qtd: 0, arrec: 0, meta: 0 };
      causas[c.causa].qtd++;
      causas[c.causa].arrec += c.arrec;
      causas[c.causa].meta += c.meta;
    });

    const topCausas = Object.entries(causas)
      .map(([causa, v]) => ({ causa, campanhas: v.qtd, arrecadado: brl(v.arrec) }))
      .sort((a, b) => b.campanhas - a.campanhas)
      .slice(0, 5);

    return {
      statusCode: 200,
      body: JSON.stringify({
        totais: { campanhas: totalCampanhas, doacoes: totalDoacoes, arrecadado: brl(totalArrecadado) },
        topArrecadados,
        topCausas
      })
    };
  } catch (err) {
    return { statusCode: 500, body: JSON.stringify({ erro: "Falha ao gerar relatório", detalhe: err.message }) };
  }
};
```

---

## 6) Comandos na EC2

Após acessar a instância, escreva na EC2:

```bash
cd ~/CloudServices
git pull
docker compose up --build
```

Esses comandos atualizam o código e recompilam os containers do **backend** e **frontend**.

---

## 7) Controle de Acesso via tela

* **Administrador:** pode criar, editar e excluir campanhas (CRUD completo)
* **Usuário comum:** pode visualizar campanhas e realizar doações
* As rotas sensíveis são protegidas no backend

---

## 8) Deploy e Integração com o API Gateway

* Rotas CRUD (`/campanhas`, `/campanhas/{id}` etc.) → **HTTP Integration** com o backend (EC2:8080)
* Rota `/report` → **Lambda Integration**
* **CORS habilitado** (GET, POST, PUT, DELETE, OPTIONS)

---

## 9) Rodando Localmente

```bash
# 1. Clonar o repo
git clone https://github.com/grazzii/CloudServices.git
cd CloudServices

# 2. Configurar variáveis
cp .env.example .env   # editar credenciais do banco

# 3. Subir os containers
docker compose up --build
```

**Endpoints locais:**
[http://localhost:8080](http://localhost:8080)
