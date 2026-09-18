# thz-core — Núcleo da Linguagem THZ-LANG (Java 25)

Biblioteca central do motor JVM da THZ-LANG: linguagem corporativa orientada a domínio (DDD) com contratos formais de governança, aritmética decimal exata, layout colunar SoA, aceleração vetorial SIMD e processamento de alta performance.

**Autônoma por design:** este módulo não conhece GUI nem CLI. Os módulos de apresentação (`thz-cli-jvm`, `thz-gui-jvm`, `thz-agent-jvm`, `thz-api-jvm` — neste mesmo build Gradle) consomem sua API pública via fachada (`ThzCompilerFacade`) e registram extensões via `BibliotecaPadrao.registrar(nome, fn)`.

---

## Módulos Internos (`src/main/java/thz/lang/`)

| Pacote | Responsabilidade Principal |
|---|---|
| `ast` | Árvore de Sintaxe Abstrata (sealed records imutáveis para programas canônicos e modernos) |
| `lexico` | Scanner determinístico com tolerância a BOM UTF-8, dialetos PT-BR/EN-US e `PalavrasReservadas` |
| `sintatico` | Parser recursivo descendente com precedência de operadores, blocos `{ ... }` e recuperação |
| `semantico` | Checagem estática de tipos, inferência local, contratos `EXIGE`/`GARANTE` e invariantes |
| `interpretador` | Tree-walking interpreter de alta performance + stdlib extensível (`BibliotecaPadrao`) |
| `runtime` | Decimal exato (`DecimalFixo`, ISO/IEC 10967), `Monetario` (ISO 4217), datas e `BlocoMemoria` |
| `brasil` | Módulo Brasil Digital: PIX (EMVco BR Code), boletos bancários, CEPs, feriados e documentos |
| `db` | Conectores universais de banco (`BANCO.*`), persistência JPA-like, Raw SQL e busca KNN |
| `mensageria` | Barramento de eventos assíncrono e bridges universais (RabbitMQ, Kafka, AWS SQS/SNS) |
| `analytics` | Suíte DAX, funções estatísticas, interoperabilidade com planilhas Excel e sanitização de dados |
| `ia` | Modelos tabulares, regressão linear, classificação sigmoide e embeddings semânticos determinísticos |
| `snapshot` | Motor de snapshot e compactação de workspace em formato binário `THZSNAP\x01` |
| `dap` | Servidor de depuração nativo DAP (Debug Adapter Protocol) para breakpoints e inspeção |
| `governanca` | Auditoria de arquitetura viva G4, matriz de requisitos e protocolo de liberação (`governanca.liberacao`) |
| `docgen` | Gerador de documentação viva Markdown + diagramas Mermaid diretamente da AST |
| `documento` | Exportação corporativa de relatórios em PDF, planilhas Excel (.xlsx) e Word (.docx) |
| `ir` | Representação intermediária `thz-ir/1` + gerador de LLVM IR para compilação AOT |
| `simd` | Validação formal de vetorização e alinhamento de loops colunares (regras R1–R5) |
| `formato` | Formatador canônico idempotente de código-fonte e serializador JSON da AST |
| `diagnosticos` | Emissor de diagnósticos com precisão cirúrgica `[Categoria][Linha L:C]` e carets |
| `version` | Resolução e validação semântica de versões (`ThzVersion`, SemVer 2.0.0) |

---

## Build e Testes

```bash
# Executar a suíte de testes JUnit 5 do core
./gradlew :thz-core-jvm:test

# Publicar artefato no repositório local (~/.m2)
./gradlew :thz-core-jvm:publishToMavenLocal
```

---

## Consumo no Monorepo

Os demais submódulos JVM (`thz-cli-jvm`, `thz-gui-jvm`, `thz-agent-jvm`, etc.) declaram:
```kotlin
implementation("thz.lang:thz-core:0.4.0")
```
A resolução é efetuada automaticamente via **Gradle Composite Build** (`includeBuild("../thz-core-jvm")`). Fora do monorepo, a dependência pode ser consumida a partir do Maven Local após a publicação.

---

## Stack Técnica

- **Linguagem & Toolchain:** Java 25 (OpenJDK / GraalVM JDK)
- **Documentação & Office:** Apache POI 5.3, OpenPDF 2.0
- **Logging & Testes:** Log4j 2.23, JUnit 5.11
- **Build System:** Gradle 8.x (Kotlin DSL)
