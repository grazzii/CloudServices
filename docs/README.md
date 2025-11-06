## Serviços em Nuvem - 05J

# Projeto Integrador – Cloud Developing 2025/1

**URL do backend (EC2/EIP):** [http://54.197.119.102:8080](http://54.197.119.102:8080)

CRUD simples + API Gateway + Lambda `/report` + RDS 

---

## Grupo

* 10425431 – Graziely de Oliveira Severo – SpringBoot/ RDS/ API GATEWAY/ Lambda/ Documentação / Vídeo2
* 10428577 – Marcos Minhano – EC2/ VPC/ SpringBoot/ Documentação/ Vídeo1

---

## 1) Visão Geral

O **Portal de Doações Ajudaê** é um site de campanhas variadas, no qual é possível realizar doações e conhecer mais sobre o projeto

* **Backend:** Spring Boot (Java), containerizado
* **Frontend:** React (JavaScript)
* **Infra AWS:** EC2, RDS MySQL, API Gateway e Lambda (rota `/report`)

> Está sendo utilizado **Elastic IP** na EC2. Assim, o endpoint público do backend permanece **estável** ([http://54.197.119.102:8080](http://54.197.119.102:8080))

---

## 2) Arquitetura

| Camada   | Serviço                                      | Descrição                                                     |
| -------- | -------------------------------------------- | --------------------------------------------------------------|
| Frontend | React (JavaScript)                           | UI para deixar esteticamente bonito o design                  |
| Backend  | EC2 (Docker + Spring Boot)                   | API REST (CRUD campanhas e as doações)                        |
| Banco    | Amazon RDS (MySQL, privada)                  | Persistência relacional; acesso apenas via backend            |
| Gateway  | Amazon API Gateway                           | Roteia `/campanhas` → EC2 e `/report` → Lambda                |
| Função   | AWS Lambda (`/report`)                       | Consolida estatísticas (totais, top arrecadações e top causas)|

---

## 3) Endpoints REST (Backend + Lambda)

| Método | Endpoint               | Descrição                                                                      |
| ------ | ---------------------- | -----------------------------------------------------------------------------  |
| GET    | `/campanhas`           | Lista todas as campanhas com  as doações de cada uma                           |
| GET    | `/campanhas/{id}`      | Retorna campanha específica                                                    |
| POST   | `/campanhas`           | Cria uma nova campanha                                                         |
| PUT    | `/campanhas/{id}`      | Atualiza os dados de uma campanha                                              |
| PUT    | `/campanhas/{id}/doar` | Registra uma doação na campanha específica                                     |
| DELETE | `/campanhas/{id}`      | Exclui uma campanha                                                            |
| GET    | `/report` (Lambda)     | Gera relatório consolidado (totais campanhas/doções, top arrecadações e causas)|

---

## 4) Banco de Dados (Amazon RDS – MySQL)

* **VPC:** RDS em subnet **privada**; sem exposição pública
* **Acesso:** apenas via backend (EC2)

O RDS MySQL guarda todas as informações tanto das campanhas quanto das doações.
Ele foi criado em sub-rede privada, sem acesso direto da internet. Apenas a instância EC2 tem permissão de conexão, controlada pelo Security Group na porta 3306.
O endpoint do banco é lab.cn00y8o8m69e.us-east-1.rds.amazonaws.com

---

## 5) Implementação da Lambda `/report`

A função consome o endpoint do backend (`/api/campanhas`) e retorna um JSON com totais, **Top 3** campanhas por arrecadação e **Top 5** causas por incidência em ordem crescente

**Variáveis de ambiente (Lambda):**

* `BACKEND_BASE_URL` 

### Código 

```js
// index.mjs
export const handler = async () => {
  const base = process.env.BACKEND_BASE_URL || "http://52.90.62.231:8080";
  const url = `${base}/api/campanhas`;
  const N = (v) => Number(v ?? 0);
  const pct = (num, den) => (den > 0 ? ((num / den) * 100) : 0);
  const brl = (v) => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(N(v));

  try {
    const r = await fetch(url, { method: "GET" });
    const data = await r.json();
    if (!Array.isArray(data)) throw new Error("Resposta inválida da API de campanhas");

    const campanhas = data.map(c => {
      const meta = N(c.meta);
      const arrec = N(c.valorArrecadado);
      return {
        id: c.id ?? null,
        nome: c.nome ?? null,
        causa: (c.causa || "Indefinida").trim(),
        meta,
        arrec,
        progresso: pct(arrec, meta)
      };
    });

    let totalCampanhas = 0;
    let totalDoacoesQtd = 0;
    let totalArrecadadoGeral = 0;

    for (const c of data) {
      totalCampanhas += 1;
      totalArrecadadoGeral += N(c.valorArrecadado);
      if (Array.isArray(c.doacoes)) {
        totalDoacoesQtd += c.doacoes.length;
      } else if (c.quantidadeDoacoes != null) {
        totalDoacoesQtd += N(c.quantidadeDoacoes);
      }
    }

    const topArrecadados = [...campanhas]
      .sort((a, b) => b.arrec - a.arrec)
      .slice(0, 3)
      .map(c => ({
        id: c.id,
        nome: c.nome,
        causa: c.causa,
        arrecadado: brl(c.arrec),
        meta: brl(c.meta),
        progresso: `${pct(c.arrec, c.meta).toFixed(2)}%`
      }));

    const causaMap = new Map();
    for (const c of campanhas) {
      const agg = causaMap.get(c.causa) || { causa: c.causa, qtd: 0, meta: 0, arrec: 0 };
      agg.qtd += 1;
      agg.meta += c.meta;
      agg.arrec += c.arrec;
      causaMap.set(c.causa, agg);
    }

    const topCausas = [...causaMap.values()]
      .sort((a, b) => (b.qtd - a.qtd) || (b.arrec - a.arrec))
      .slice(0, 5)
      .map(x => ({
        causa: x.causa,
        campanhas: x.qtd,
        arrecadadoTotal: brl(x.arrec),
        metaTotal: brl(x.meta),
        progressoMedio: `${pct(x.arrec, x.meta).toFixed(2)}%`
      }));

    const payload = {
      totais: {
        campanhas: { total: totalCampanhas },
        doacoes: {
          quantidadeTotal: totalDoacoesQtd,
          valorTotal: brl(totalArrecadadoGeral)
        }
      },
      relatorio: {
        titulo: "Top Arrecadados & Top Causas",
        geradoEm: new Date().toISOString(),
        topArrecadados,
        topCausas
      },
      meta: {
        fonte: "EC2:/api/campanhas",
        backend: { ok: r.ok, status: r.status }
      }
    };

    return {
      statusCode: 200,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "Content-Type,Authorization",
        "Access-Control-Allow-Methods": "GET,OPTIONS"
      },
      body: JSON.stringify(payload)
    };
  } catch (err) {
    return {
      statusCode: 502,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*"
      },
      body: JSON.stringify({ error: "Falha ao gerar relatório", detail: String(err) })
    };
  }
};
```

---

## 6) Comandos Executados na EC2

Após conectar na instância EC2, executar:


cd ~/CloudServices
docker compose up --build


Esses comandos recompilam/rodam os containers do **backend** e **frontend**.

---

## 7) Autenticação e Controle de Acesso

* **Administrador (adm):** CRUD completo em campanhas; páginas de controle ocultas a quem não possui as credenciais
* **Visitantes:** Apenas visualização pública das campanhas, realizar doações, contato, e como funciona
* Backend protege rotas sensíveis

---

## 8) Deploy / Integração com API Gateway

* **Rotas CRUD** (`/campanhas`, `/campanhas/{id}`, …) → **Integration HTTP** apontando para **[http://54.197.119.102:8080](http://54.197.119.102:8080)** (IP, porta 8080)
* **Rota** `/report` → **Integration Lambda** 
* **CORS habilitado** (GET/POST/PUT/DELETE/OPTIONS) no API Gateway

**Variáveis:**

* **Lambda:** `BACKEND_BASE_URL` (ex.: `http://54.197.119.102:8080`) para desacoplar o endpoint do código

---

## 9) Como Rodar Localmente


# 1) Clonar
git clone https://github.com/grazzii/CloudServices.git
cd CloudServices

# 2) Variáveis
cp .env.example .env   # dite credenciais do seu BD

# 3) Subir containers
docker compose up --build


**Endpoints locais**

* Backend:  [http://localhost:8080](http://localhost:8080)
