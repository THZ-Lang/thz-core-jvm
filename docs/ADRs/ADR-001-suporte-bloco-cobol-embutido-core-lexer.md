# ADR-001: Suporte a Blocos COBOL Embutidos e Ingestão de Copybooks no Compilador Core

- **Status:** Aceito
- **Data:** 2026-10-11
- **Decisores:** Time de Arquitetura THZ-LANG & Lucas Thomaz
- **Contexto:** Ecossistema THZ-LANG (v0.5.0), módulo `thz-lang-cobol` e Language Server Protocol (`thz-lsp-jvm`)

---

## 1. Contexto e Problema

Com a criação do módulo `thz-lang-cobol` e a definição da arquitetura de modernização de sistemas legados de missão crítica (ADR-008 em `thz-lang` e ADR-001 em `thz-vscode`), a linguagem THZ estabeleceu o suporte a blocos de código COBOL legados declarados dentro de arquivos `.thz` (`BLOCO_COBOL ... FIM_BLOCO_COBOL`), além da ingestão de schemas via `IMPORTAR_COPYBOOK`.

No entanto, o compilador central (`thz-core-jvm`) não possuía reconhecimento de blocos brutos COBOL em seu analisador léxico (`ThzLexer`), tratando o conteúdo interno do COBOL como tokens e palavras-chave da própria gramática THZ.

Isso provocava falhas críticas em arquivos configurados no dialeto primário `[pt-BR]`, tais como:
1. `PROGRAM-ID. CALC-TARIFA.` era particionado pelo scanner, detectando `PROGRAM` como palavra reservada do dialeto internacional `[en-US]`.
2. O validador de pureza léxica (`PalavrasReservadas.validarPurezaDialeto`) interrompia a compilação com `[Erro Léxico] Palavra-chave 'PROGRAM' pertence ao dialeto [en-US] e não pode ser usada em arquivo configurado para [pt-BR]`.
3. Comandos e cláusulas do COBOL (`MOVE`, `PIC`, `COMP-3`, `END-IF.`) geravam erros léxicos e sintáticos de declaração inválida.
4. Chamadas C-ABI `COBOL.<ROTINA>(...)` eram tratadas como símbolos desconhecidos pelo analisador semântico.

---

## 2. Decisão Arquitetural

1. **Tokens Oficiais de Interoperabilidade:**
   - Adicionados os tokens `BLOCO_COBOL`, `FIM_BLOCO_COBOL` e `IMPORTAR_COPYBOOK` no enum `TokenType`.
   - Adicionados os equivalentes canônicos em ambos os dialetos em `PalavrasReservadas`:
     - PT-BR: `BLOCO_COBOL`, `bloco_cobol`, `FIM_BLOCO_COBOL`, `fim_bloco_cobol`, `IMPORTAR_COPYBOOK`, `importar_copybook`.
     - EN-US: `COBOL_BLOCK`, `cobol_block`, `END_COBOL_BLOCK`, `end_cobol_block`, `IMPORT_COPYBOOK`, `import_copybook`.
     - Mapeamento bidirecional automático no dicionário de tradução entre dialetos.

2. **Scanner e Isolamento de Bloco Bruto (Raw Block Scanner):**
   - Implementado em `ThzLexer` o método `readRawCobolBlock()`, análogo ao suporte pré-existente para blocos nativos Rust.
   - Todo o corpo interno do bloco COBOL é consumido verbatim como `TokenType.STRING_LITERAL` até encontrar o delimitador de encerramento (`FIM_BLOCO_COBOL` ou `END_COBOL_BLOCK`), isolando completamente a gramática COBOL da checagem de palavras reservadas e pureza de dialeto do THZ.

3. **AST e Parser de Módulos:**
   - Adicionados `List<String> blocosCobol` e `List<String> importacoesCopybook` no record `ProgramaAst`, preservando total retrocompatibilidade nos construtores existentes.
   - `ThzParser` agora ingere blocos `BLOCO_COBOL` e diretivas `IMPORTAR_COPYBOOK` no nível do módulo.

4. **Análise Semântica de Chamadas C-ABI:**
   - O analisador semântico (`AnalisadorSemantico`) reconhece invocações com o prefixo `COBOL.*` como chamadas de procedimento externo/C-ABI válidas, inferindo seus argumentos e evitando falsos positivos de chamada desconhecida.

5. **Formatação Canônica e Tooling:**
   - `Formatador.java` passa a serializar e preservar blocos `BLOCO_COBOL` e cláusulas `IMPORTAR_COPYBOOK`.
   - O Language Server (`thz-lsp-jvm`) expõe as novas palavras-chave no catálogo de autocompletação.

---

## 3. Consequências e Benefícios

- **Compatibilidade Plena com `thz-lang-cobol`**: Arquivos híbridos como `demo_cobol_interop.thz` compilam sem nenhum erro ou diagnóstico falso.
- **Pureza de Dialeto Preservada**: Arquivos em `pt-BR` podem incorporar rotinas COBOL originais com `PROGRAM-ID` e demais divisões de forma 100% isolada e segura.
- **Zero Regressão**: Toda a suíte de testes de `thz-core-jvm` e `thz-lsp-jvm` permanece verde com 100% de aprovação.
