package thz.lang.docgen;

import thz.lang.ast.*;
import thz.lang.sintatico.ThzParser;


/**
 * Gerador de Documentação Técnica e Arquitetural (DocGen) a partir da AST do THZ-LANG.
 * Produz documentos em Markdown enriquecidos com diagramas de classe e fluxo em Mermaid.js.
 */
public final class ThzDocGen {

    private ThzDocGen() {}

    /**
     * Gera a documentação Markdown completa do programa THZ-LANG.
     */
    public static String gerarDocumentacao(ProgramaAst ast) {
        if (ast == null) {
            throw new IllegalArgumentException("AST não pode ser nula para geração de documentação.");
        }

        StringBuilder sb = new StringBuilder();

        // 1. Cabeçalho
        TipoModulo tipo = ast.tipoModulo() != null ? ast.tipoModulo() : TipoModulo.PROGRAMA;
        sb.append("# Documentação Arquitetural e de Domínio — ").append(ast.nome()).append("\n\n");
        sb.append("> **Tipo de Módulo:** `").append(tipo.descricao()).append("`  \n");
        sb.append("> **Versão da Linguagem THZ-LANG:** `").append(ast.versaoLinguagem() != null ? ast.versaoLinguagem() : thz.lang.version.ThzVersion.ATUAL.toString()).append("`  \n");
        sb.append("> **Gerado automaticamente pelo compilador THZ-LANG Engine.**\n\n");

        // 2. Metadados de Arquitetura
        sb.append("## 1. Metadados de Arquitetura Viva (ISO/IEC/IEEE 42010)\n\n");
        if (ast.metadados() != null) {
            sb.append("| Atributo | Valor |\n");
            sb.append("|---|---|\n");
            sb.append("| **Domínio** | `").append(ast.metadados().dominio()).append("` |\n");
            sb.append("| **Subdomínio** | `").append(ast.metadados().subdominio() != null ? ast.metadados().subdominio() : "—").append("` |\n");
            sb.append("| **Camada** | `").append(ast.metadados().camada() != null ? ast.metadados().camada() : "—").append("` |\n");
            sb.append("| **Versão do Domínio** | `").append(ast.metadados().versao() != null ? ast.metadados().versao() : "—").append("` |\n");
            sb.append("| **Autor / Responsável** | `").append(ast.metadados().autor() != null ? ast.metadados().autor() : "—").append("` |\n");
            sb.append("| **SLO de Latência Máxima** | `").append(ast.metadados().sloLatencia() != null ? ast.metadados().sloLatencia() : "—").append("` |\n");
            String conf = (ast.metadados().conformidade() != null && !ast.metadados().conformidade().isEmpty())
                    ? String.join(", ", ast.metadados().conformidade())
                    : "Nenhuma declarada";
            sb.append("| **Conformidade Regulatória** | `").append(conf).append("` |\n");
        } else {
            sb.append("*Nenhum bloco de metadados arquiteturais declarado.*\n");
        }
        sb.append("\n---\n\n");

        // 2.1 Módulos Importados
        if (ast.importacoes() != null && !ast.importacoes().isEmpty()) {
            sb.append("## 2. Dependências e Módulos Importados\n\n");
            sb.append("| Módulo | Caminho / Origem |\n");
            sb.append("|---|---|\n");
            for (ImportacaoAst imp : ast.importacoes()) {
                String caminho = imp.caminho() != null ? "`" + imp.caminho() + "`" : "*Resolução padrão*";
                sb.append("| `").append(imp.modulo()).append("` | ").append(caminho).append(" |\n");
            }
            sb.append("\n---\n\n");
        }

        // 3. Diagrama Mermaid de Entidades
        if (ast.estruturas() != null && !ast.estruturas().isEmpty()) {
            sb.append("## 2. Diagrama de Entidades de Domínio (Mermaid)\n\n");
            sb.append("```mermaid\nclassDiagram\n");
            for (EstruturaAst est : ast.estruturas()) {
                sb.append("    class ").append(est.nome()).append(" {\n");
                if (est.layoutColunar()) {
                    sb.append("        <<LAYOUT_COLUNAR (SoA)>>\n");
                }
                for (CampoEstruturaAst campo : est.campos()) {
                    String tipoLimpo = campo.tipo().replaceAll("[\\[\\](),\\s]", "_");
                    sb.append("        +").append(tipoLimpo).append(" ").append(campo.nome()).append("\n");
                }
                sb.append("    }\n");
            }
            sb.append("```\n\n---\n\n");
        }

        // 4. Diagrama Mermaid de Fluxo das Regras de Negócio
        if (ast.regras() != null && !ast.regras().isEmpty()) {
            sb.append("## 3. Diagrama de Rastreabilidade e Fluxo de Regras (Mermaid)\n\n");
            sb.append("```mermaid\ngraph TD\n");
            int idCounter = 1;
            for (RegraNegocioAst regra : ast.regras()) {
                String reqId = (regra.rastreioRequisito() != null && !regra.rastreioRequisito().isBlank())
                        ? regra.rastreioRequisito()
                        : "SEM_RASTREIO";
                String nReq = "Req" + idCounter;
                String nRegra = "Regra" + idCounter;

                sb.append("    ").append(nReq).append("[\"📌 Requisito: ").append(escaparMermaid(reqId)).append("\"] --> ").append(nRegra).append("[\"⚖️ Regra: ").append(escaparMermaid(regra.nome())).append("\"]\n");

                if (regra.clausulasEntrada() != null && !regra.clausulasEntrada().isEmpty()) {
                    String nExige = "Exige" + idCounter;
                    sb.append("    ").append(nRegra).append(" --> ").append(nExige).append("[\"🛡️ EXIGE: ").append(regra.clausulasEntrada().size()).append(" cláusula(s)\"]\n");
                }

                if (regra.operacoes() != null) {
                    for (int opIdx = 0; opIdx < regra.operacoes().size(); opIdx++) {
                        OperacaoAst op = regra.operacoes().get(opIdx);
                        String nOp = "Op" + idCounter + "_" + opIdx;
                        sb.append("    ").append(nRegra).append(" --> ").append(nOp).append("[\"⚡ Operação: ").append(escaparMermaid(op.nome())).append("()\"]\n");
                        if (regra.clausulasSaida() != null && !regra.clausulasSaida().isEmpty()) {
                            String nGarante = "Garante" + idCounter;
                            sb.append("    ").append(nOp).append(" --> ").append(nGarante).append("[\"✅ GARANTE: ").append(regra.clausulasSaida().size()).append(" cláusula(s)\"]\n");
                        }
                    }
                }
                idCounter++;
            }
            sb.append("```\n\n---\n\n");
        }

        // 5. Estruturas de Dados e Invariantes
        sb.append("## 4. Estruturas de Dados e Invariantes Formais\n\n");
        if (ast.estruturas() == null || ast.estruturas().isEmpty()) {
            sb.append("*Nenhuma estrutura declarada.*\n\n");
        } else {
            for (EstruturaAst est : ast.estruturas()) {
                sb.append("### Estrutura: `").append(est.nome()).append("`\n\n");
                sb.append("* **Layout de Memória:** `").append(est.layoutColunar() ? "LAYOUT_COLUNAR (Structure of Arrays / SIMD)" : "LAYOUT_PADRAO (Contíguo em Memória)").append("`\n");
                sb.append("\n| Campo | Tipo |\n|---|---|\n");
                for (CampoEstruturaAst c : est.campos()) {
                    sb.append("| `").append(c.nome()).append("` | `").append(c.tipo()).append("` |\n");
                }
                sb.append("\n");
                if (est.invariantes() != null && !est.invariantes().isEmpty()) {
                    sb.append("**Invariantes de Domínio (validados em toda mutação):**\n");
                    for (InvarianteAst inv : est.invariantes()) {
                        String txt = inv.textoCanonico() != null ? inv.textoCanonico() : ThzParser.textoCanonicoDe(inv.expressao());
                        sb.append("* `INVARIANTE ").append(txt).append("`\n");
                    }
                    sb.append("\n");
                }
            }
        }
        sb.append("---\n\n");

        // 6. Enumerações
        if (ast.enumeracoes() != null && !ast.enumeracoes().isEmpty()) {
            sb.append("## 5. Enumerações de Domínio\n\n");
            for (EnumeracaoAst en : ast.enumeracoes()) {
                sb.append("### `ENUMERACAO ").append(en.nome()).append("`\n\n");
                sb.append("Valores permitidos: `").append(String.join("`, `", en.membros())).append("`\n\n");
            }
            sb.append("---\n\n");
        }

        // 7. Regras de Negócio e Contratos
        sb.append("## 6. Regras de Negócio e Contratos Formais\n\n");
        if (ast.regras() == null || ast.regras().isEmpty()) {
            sb.append("*Nenhuma regra de negócio declarada.*\n\n");
        } else {
            for (RegraNegocioAst regra : ast.regras()) {
                sb.append("### Regra: `").append(regra.nome()).append("`");
                if (regra.identificador() != null) sb.append(" (`").append(regra.identificador()).append("`)");
                sb.append("\n\n");

                if (regra.rastreioRequisito() != null && !regra.rastreioRequisito().isBlank()) {
                    sb.append("* **Rastreio de Requisito:** `").append(regra.rastreioRequisito()).append("`\n");
                }
                if (regra.descricao() != null && !regra.descricao().isBlank()) {
                    sb.append("* **Descrição de Negócio:** ").append(regra.descricao()).append("\n");
                }
                sb.append("\n");

                if (regra.idempotente()) {
                    sb.append("* **Garantia de Idempotência:** 🛡️ `IDEMPOTENTE`");
                    if (regra.chaveIdempotencia() != null) sb.append(" (Chave: `").append(regra.chaveIdempotencia()).append("`)");
                    sb.append("\n");
                }

                if (regra.clausulasEntrada() != null && !regra.clausulasEntrada().isEmpty()) {
                    sb.append("#### Pré-condições (Contrato de Entrada):\n");
                    for (ClausulaContratoAst c : regra.clausulasEntrada()) {
                        String txt = c.textoCanonico() != null ? c.textoCanonico() : ThzParser.textoCanonicoDe(c.expressao());
                        sb.append("* `EXIGE ").append(txt).append("`\n");
                    }
                    sb.append("\n");
                }

                if (regra.clausulasSaida() != null && !regra.clausulasSaida().isEmpty()) {
                    sb.append("#### Pós-condições (Contrato de Saída):\n");
                    for (ClausulaContratoAst c : regra.clausulasSaida()) {
                        String txt = c.textoCanonico() != null ? c.textoCanonico() : ThzParser.textoCanonicoDe(c.expressao());
                        sb.append("* `GARANTE ").append(txt).append("`\n");
                    }
                    sb.append("\n");
                }

                if (regra.operacoes() != null && !regra.operacoes().isEmpty()) {
                    sb.append("#### Operações da Regra:\n");
                    for (OperacaoAst op : regra.operacoes()) {
                        StringBuilder params = new StringBuilder();
                        for (int i = 0; i < op.parametros().size(); i++) {
                            ParametroOperacaoAst p = op.parametros().get(i);
                            params.append(p.nome()).append(": ").append(p.tipo());
                            if (i + 1 < op.parametros().size()) params.append(", ");
                        }
                        sb.append("* `OPERACAO ");
                        if (op.idempotente()) sb.append("IDEMPOTENTE ");
                        sb.append(op.nome()).append("(").append(params).append(")");
                        if (op.tipoRetorno() != null) sb.append(" : ").append(op.tipoRetorno());
                        sb.append("`");
                        if (op.idempotente()) sb.append(" 🛡️ *(Idempotente)*");
                        sb.append("\n");
                    }
                    sb.append("\n");
                }
            }
        }
        sb.append("---\n\n");

        // 8. Procedimentos
        if (ast.procedimentos() != null && !ast.procedimentos().isEmpty()) {
            sb.append("## 7. Procedimentos\n\n");
            for (ProcedimentoAst proc : ast.procedimentos()) {
                StringBuilder params = new StringBuilder();
                for (int i = 0; i < proc.parametros().size(); i++) {
                    ParametroOperacaoAst p = proc.parametros().get(i);
                    params.append(p.nome()).append(": ").append(p.tipo());
                    if (i + 1 < proc.parametros().size()) params.append(", ");
                }
                sb.append("* `PROCEDIMENTO ");
                if (proc.idempotente()) sb.append("IDEMPOTENTE ");
                sb.append(proc.nome()).append("(").append(params).append(")`");
                if (proc.idempotente()) sb.append(" 🛡️ *(Idempotente)*");
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (ast.funcoes() != null && !ast.funcoes().isEmpty()) {
            sb.append("## 8. Funções\n\n");
            for (FuncaoAst funcao : ast.funcoes()) {
                String params = funcao.parametros().stream().map(p -> p.nome() + ": " + p.tipo())
                        .collect(java.util.stream.Collectors.joining(", "));
                sb.append("* `FUNCAO ").append(funcao.nome()).append("(").append(params).append("): ")
                        .append(funcao.tipoRetorno()).append("`\n");
            }
            sb.append("\n");
        }


        return sb.toString();
    }

    private static String escaparMermaid(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").replace("\n", " ");
    }
}
