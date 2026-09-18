# Exemplos THZ-LANG — Coleção Canônica & Galeria Desktop

Programas autossuficientes e demonstradores que exercitam a gramática canônica e o paradigma dual moderno da linguagem THZ-LANG (v0.4.0), além das extensões visuais, analíticas e documentais do motor JVM.
Todos passam em checagem estrita (`./gradlew cli --args="check <arquivo> --estrito"`) e executam de forma determinística.

---

## Como Executar

### 1. Via Gradle Wrapper (Canônico)
```bash
# Executar programa da coleção
./gradlew cli --args="run exemplos/colecao/01-ola-mundo.thz"
./gradlew cli --args="run exemplos/exportacao_documentos.thz"
./gradlew cli --args="run exemplos/gestao_pedidos_moderno.thz"

# Checagem estrita de conformidade arquitetural e tipos
./gradlew cli --args="check exemplos/colecao/05-decimal-financeiro.thz --estrito"

# Auditoria de governança G4
./gradlew cli --args="audit exemplos/faturamento.thz"

# Geração de documentação técnica em Markdown + Mermaid
./gradlew cli --args="doc exemplos/faturamento.thz --saida docs/"

# Iniciar a IDE Desktop Swing
./gradlew gui
```

### 2. Via Distribuição Autônoma (.exe)
```powershell
dist\bin\thz.exe run   exemplos\colecao\01-ola-mundo.thz
dist\bin\thz.exe check exemplos\colecao\05-decimal-financeiro.thz
dist\bin\thz.exe audit exemplos\faturamento.thz
dist\bin\thz-desktop.exe
```

### 3. Na IDE Desktop Swing (`thz gui`)
Todos os exemplos desta pasta são detectados dinamicamente e listados no menu **Exemplos** da IDE Desktop. Clique em qualquer um para carregar com realce léxico imediato, formatação, inspeção de AST, auditoria de governança e compilação de telas visuais.

---

## Índice da Coleção Básica (`exemplos/colecao/`)

| # | Arquivo | Tema Principal | Construtos e Funcionalidades Demonstradas |
|:---:|---|---|---|
| **01** | `01-ola-mundo.thz` | Programa Mínimo | `PROGRAMA`, `METADADOS_ARQUITETURA`, `PROCEDIMENTO Principal`, `EXIBA`. |
| **02** | `02-tipos-estruturas.thz` | Tipos e Estruturas | Primitivos (`TEXTO`, `NATURAL32`, `DECIMAL`, `LOGICO`, `DATA`), `CRIAR`, acesso a campos, mutação com `<-`, `INVARIANTE`. |
| **03** | `03-enumeracoes.thz` | Enumerações | `ENUMERACAO`, membros globais tipados, comparações verbais e conjunção `E`. |
| **04** | `04-controle-fluxo.thz` | Fluxo de Controle | `SE/SENAO/FIM_SE`, `ENQUANTO`, `PARA..DE..ATE [PASSO]`, conectivo verbal `NAO`. |
| **05** | `05-decimal-financeiro.thz` | Decimais Exatos | `DECIMAL(p,s)` BigInteger escalado, arredondamento bancário half-even via `MATEMATICA.arredondar`, `abs/min/max/raiz/potencia`. |
| **06** | `06-texto-datas.thz` | Stdlib TEXTO e DATA | `comprimento/aparar/contem/subtexto/substituir/dividir/juntar`, `DATA.hoje/agora/somarDias/diferencaDias/texto`, indexação `dividir(...)[i]`. |
| **07** | `07-resultado-ddd.thz` | DDD & Contratos Formais | `RESULTADO[T,E]`, `FALHAR_COM`, pré-condições `EXIGE` e pós-condições `GARANTE` quantificadas sobre fatia ($\forall$). |
| **08** | `08-vetorizado-simd.thz` | Lote Vetorizado SIMD | Estrutura `LAYOUT_COLUNAR` (SoA), laço `VETORIZAR_PARA..PASSO_SIMD`, acumuladores `DECIMAL(18,4)`. |
| **09** | `09-bloco-memoria.thz` | Memória Efêmera ($O(1)$) | `USAR_BLOCO_MEMORIA..FIM_BLOCO_MEMORIA`, alocação linear contígua, descarte instantâneo sem pressão de GC. |
| **10** | `10-entrada-interativa.thz` | Entrada Interativa | `LER`, fallback condicional e conversão para inteiros. |
| **11** | `11-idempotencia-larga-escala.thz` | Idempotência Inteligente | Cláusulas `IDEMPOTENTE`, `CHAVE_IDEMPOTENCIA`, memoização transacional $O(1)$ e supressão de execuções duplicadas. |

---

## Exemplos de Interfaces Visuais e Exportação (`exemplos/`)

| Arquivo | Descrição e Funcionalidades |
|---|---|
| `cadastro_cliente_gui.thz` | Formulário de cadastro de clientes com validação de CPF, email, combos e submissão vinculada à regra de negócio. |
| `cadastro_produto_gui.thz` | Tela de cadastro de mercadorias com campos numéricos de preço, estoque e tabela de fornecedores. |
| `pedido_vendas_gui.thz` | Interface complexa com tabela dinâmica interativa (`FATIA[ItemPedido]`), cálculo em tempo real e fechamento de pedido. |
| `showcase_widgets_gui.thz` | Demonstração exaustiva de todos os widgets do subsistema `TELA` (texto, senha, cores, arquivos, sliders, spinners, combos, radios, tabelas e textareas). |
| `simulador_credito_gui.thz` | Simulador financeiro com sliders de valor, radios de score de crédito e emissão de proposta. |
| `exportacao_documentos.thz` | Demonstração do motor corporativo `DOCUMENTO`, gerando relatório em **PDF**, planilha **Excel (.xlsx)** e documento **Word (.docx)**. |
| `gestao_pedidos_moderno.thz` | Demonstração do paradigma dual moderno ("Kotlin/Rust Corporativo"): blocos `{ ... }`, `fn main(): Int`, `struct` e atribuição direta. |

---

## Exemplos Canônicos de Paridade Multi-Plataforma (JVM ⇄ Rust AOT)

* `faturamento.thz`: Processamento tributário em lote com aceleração vetorial SIMD e injeção de lote demo.
* `pedidos.thz`: Domínio rico DDD com enumerações de status, tipos de resultado e validação de contratos.
* `agenda.thz`: Procedimentos com manipulação de fatias literais de estruturas e módulo de datas.
* `gestao_pedidos_moderno.thz`: Execução 100% equivalente em JVM interpretada e código nativo compilado AOT via LLVM Clang.

---

## Convenções da Linguagem THZ-LANG

1. **Programa:** Todo programa inicia com a declaração do arquétipo de módulo (ex.: `PROGRAMA` ou `programa Nome { ... }`).
2. **Metadados:** Bloco `METADADOS_ARQUITETURA` (ou `metadados { ... }`) com domínio, autor, SLO e conformidade regulatória.
3. **Fluxo de Atribuição:** Operadores `<-`, `=` ou `:=` conforme estilo canônico ou moderno.
4. **Comparação e Conectivos:** Igualdade com `=`, operadores de comparação padrão e conectivos lógicos em português (`E`, `OU`, `NAO`).
5. **Comentários:** Linhas ou trechos iniciados por `#` ou `//` são comentários (preservados no código e descartados no formatador canônico).

