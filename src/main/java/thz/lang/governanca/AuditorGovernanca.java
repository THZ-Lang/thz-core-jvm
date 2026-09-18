package thz.lang.governanca;

import thz.lang.ast.*;
import thz.lang.sintatico.ThzParser;

import java.util.*;

/**
 * Auditor de Governança Corporativa, Rastreabilidade e Arquitetura Viva do THZ-LANG (G4).
 */
public final class AuditorGovernanca {

    private AuditorGovernanca() {}

    /**
     * Realiza a auditoria estática da AST e produz um RelatorioAuditoria completo.
     */
    public static RelatorioAuditoria auditar(ProgramaAst ast) {
        if (ast == null) {
            throw new IllegalArgumentException("AST não pode ser nula para auditoria.");
        }

        List<RelatorioAuditoria.ItemRastreabilidade> matriz = new ArrayList<>();
        List<String> pendenciasGlobais = new ArrayList<>();
        List<String> alertasGlobais = new ArrayList<>();

        int totalExige = 0;
        int totalGarante = 0;
        int totalRegrasComRastreio = 0;
        int totalIdempotentes = 0;

        if (ast.regras() != null) {
            for (RegraNegocioAst regra : ast.regras()) {
                String reqId = regra.rastreioRequisito() != null && !regra.rastreioRequisito().isBlank()
                        ? regra.rastreioRequisito()
                        : "NÃO_RASTREADO";
                boolean temRastreio = !"NÃO_RASTREADO".equals(reqId);
                if (temRastreio) totalRegrasComRastreio++;

                List<String> exige = new ArrayList<>();
                if (regra.clausulasEntrada() != null) {
                    for (ClausulaContratoAst c : regra.clausulasEntrada()) {
                        exige.add(c.textoCanonico() != null ? c.textoCanonico() : ThzParser.textoCanonicoDe(c.expressao()));
                    }
                }
                totalExige += exige.size();

                List<String> garante = new ArrayList<>();
                if (regra.clausulasSaida() != null) {
                    for (ClausulaContratoAst c : regra.clausulasSaida()) {
                        garante.add(c.textoCanonico() != null ? c.textoCanonico() : ThzParser.textoCanonicoDe(c.expressao()));
                    }
                }
                totalGarante += garante.size();

                boolean regraIdempotente = regra.idempotente();
                List<String> ops = new ArrayList<>();
                if (regra.operacoes() != null) {
                    for (OperacaoAst op : regra.operacoes()) {
                        ops.add(op.nome());
                        if (op.idempotente() || regraIdempotente) totalIdempotentes++;
                    }
                }

                List<String> pendenciasRegra = new ArrayList<>();
                if (!temRastreio) {
                    pendenciasRegra.add("Regra '" + regra.nome() + "' sem RASTREIO_REQUISITO.");
                    pendenciasGlobais.add("Regra '" + regra.nome() + "' não está vinculada a nenhum requisito de negócio.");
                }
                if (exige.isEmpty()) {
                    alertasGlobais.add("Regra '" + regra.nome() + "' não declara pré-condições (EXIGE).");
                }
                if (garante.isEmpty()) {
                    alertasGlobais.add("Regra '" + regra.nome() + "' não declara pós-condições (GARANTE).");
                }

                boolean conforme = temRastreio && (!exige.isEmpty() || !garante.isEmpty());

                matriz.add(new RelatorioAuditoria.ItemRastreabilidade(
                        reqId,
                        regra.identificador() != null ? regra.identificador() : "S/ID",
                        regra.nome(),
                        List.copyOf(exige),
                        List.copyOf(garante),
                        List.copyOf(ops),
                        conforme,
                        regraIdempotente,
                        regra.chaveIdempotencia(),
                        List.copyOf(pendenciasRegra)
                ));
            }
        }

        // Metadados
        if (ast.metadados() == null) {
            pendenciasGlobais.add("Programa não declara o bloco obrigatório METADADOS_ARQUITETURA.");
        } else {
            if (ast.metadados().dominio() == null || ast.metadados().dominio().isBlank()) {
                pendenciasGlobais.add("Metadados não especificam 'dominio'.");
            }
            if (ast.metadados().sloLatencia() == null || ast.metadados().sloLatencia().isBlank()) {
                alertasGlobais.add("Metadados não especificam 'sloLatencia'.");
            }
            if (ast.metadados().conformidade() == null || ast.metadados().conformidade().isEmpty()) {
                alertasGlobais.add("Metadados não listam diretrizes de conformidade normativa.");
            }
        }

        // Estruturas e Invariantes
        List<RelatorioAuditoria.ItemInvarianteEstrutura> estruturas = new ArrayList<>();
        int totalInvariantes = 0;
        if (ast.estruturas() != null) {
            for (EstruturaAst est : ast.estruturas()) {
                List<String> invs = new ArrayList<>();
                if (est.invariantes() != null) {
                    for (InvarianteAst inv : est.invariantes()) {
                        invs.add(inv.textoCanonico() != null ? inv.textoCanonico() : ThzParser.textoCanonicoDe(inv.expressao()));
                    }
                }
                totalInvariantes += invs.size();
                estruturas.add(new RelatorioAuditoria.ItemInvarianteEstrutura(
                        est.nome(),
                        est.layoutColunar() ? "LAYOUT_COLUNAR" : "LAYOUT_PADRAO",
                        List.copyOf(invs)
                ));

            }
        }

        int totalRegras = ast.regras() != null ? ast.regras().size() : 0;
        double conformidadeRegras = totalRegras > 0 ? ((double) totalRegrasComRastreio / totalRegras) * 60.0 : 60.0;
        double conformidadeMetadados = (ast.metadados() != null ? 20.0 : 0.0);
        double conformidadeContratos = (totalExige + totalGarante + totalInvariantes > 0 ? 20.0 : 0.0);
        double scoreFinal = Math.min(100.0, conformidadeRegras + conformidadeMetadados + conformidadeContratos);

        boolean aprovado = pendenciasGlobais.isEmpty() && scoreFinal >= 80.0;

        RelatorioAuditoria.MetricasGovernanca metricas = new RelatorioAuditoria.MetricasGovernanca(
                totalRegras,
                totalRegrasComRastreio,
                totalExige,
                totalGarante,
                totalInvariantes,
                totalIdempotentes,
                Math.round(scoreFinal * 10.0) / 10.0,
                aprovado,
                List.copyOf(pendenciasGlobais),
                List.copyOf(alertasGlobais)
        );

        return new RelatorioAuditoria(
                ast.nome(),
                ast.versaoLinguagem() != null ? ast.versaoLinguagem() : thz.lang.version.ThzVersion.ATUAL.toString(),
                ast.metadados(),
                List.copyOf(matriz),
                List.copyOf(estruturas),
                metricas
        );
    }

    /**
     * Renderiza o relatório de auditoria em formato Markdown executivo com tabelas GFM e badges.
     */
    public static String gerarMarkdownGovernanca(RelatorioAuditoria r) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Relatório de Auditoria e Governança — ").append(r.nomePrograma()).append("\n\n");

        // 1. Metadados
        sb.append("## 1. Metadados de Arquitetura Viva (ISO/IEC/IEEE 42010)\n\n");
        sb.append("| Atributo | Especificação |\n");
        sb.append("|---|---|\n");
        sb.append("| **Programa** | `").append(r.nomePrograma()).append("` |\n");
        sb.append("| **Versão da Linguagem** | `").append(r.versaoLinguagem()).append("` |\n");
        if (r.metadados() != null) {
            sb.append("| **Domínio / Subdomínio** | `").append(r.metadados().dominio()).append(" / ").append(r.metadados().subdominio() != null ? r.metadados().subdominio() : "—").append("` |\n");
            sb.append("| **Camada Arquitetural** | `").append(r.metadados().camada() != null ? r.metadados().camada() : "—").append("` |\n");
            sb.append("| **Versão do Domínio** | `").append(r.metadados().versao() != null ? r.metadados().versao() : "—").append("` |\n");
            sb.append("| **Autor / Responsável** | `").append(r.metadados().autor() != null ? r.metadados().autor() : "—").append("` |\n");
            sb.append("| **SLO de Latência** | `").append(r.metadados().sloLatencia() != null ? r.metadados().sloLatencia() : "—").append("` |\n");
            String conf = r.metadados().conformidade() != null && !r.metadados().conformidade().isEmpty()
                    ? String.join(", ", r.metadados().conformidade())
                    : "Nenhuma declarada";
            sb.append("| **Diretrizes de Conformidade** | `").append(conf).append("` |\n");
        } else {
            sb.append("| **Status Metadados** | ⚠️ *Não declarados no fonte* |\n");
        }
        sb.append("\n---\n\n");

        // 2. Matriz de Rastreabilidade
        sb.append("## 2. Matriz de Rastreabilidade Formal (Requisito ⇄ Regra ⇄ Contratos ⇄ Idempotência)\n\n");
        if (r.matrizRastreio().isEmpty()) {
            sb.append("*Nenhuma regra de negócio declarada.*\n\n");
        } else {
            sb.append("| Rastreio Requisito | Identificador | Regra de Negócio | Pré-condições (EXIGE) | Pós-condições (GARANTE) | Idempotência | Status |\n");
            sb.append("|---|---|---|---|---|---|---|\n");
            for (RelatorioAuditoria.ItemRastreabilidade item : r.matrizRastreio()) {
                String req = "NÃO_RASTREADO".equals(item.requisitoId()) ? "⚠️ `S/ RASTREIO`" : "`" + item.requisitoId() + "`";
                String id = "`" + item.regraIdentificador() + "`";
                String regra = "**" + item.regraNome() + "**";
                String exige = item.exige().isEmpty() ? "—" : item.exige().size() + " cláusula(s):<br>• `" + String.join("`<br>• `", item.exige()) + "`";
                String garante = item.garante().isEmpty() ? "—" : item.garante().size() + " cláusula(s):<br>• `" + String.join("`<br>• `", item.garante()) + "`";
                String idemp = item.idempotente()
                        ? "🛡️ `IDEMPOTENTE`" + (item.chaveIdempotencia() != null ? "<br>Chave: `" + item.chaveIdempotencia() + "`" : "")
                        : "—";
                String status = item.conforme() ? "✅ **Conforme**" : "⚠️ **Não Conforme**";
                sb.append("| ").append(req).append(" | ").append(id).append(" | ").append(regra).append(" | ").append(exige).append(" | ").append(garante).append(" | ").append(idemp).append(" | ").append(status).append(" |\n");
            }
            sb.append("\n");
        }
        sb.append("---\n\n");

        // 3. Entidades & Invariantes
        sb.append("## 3. Invariantes de Entidades & Layouts de Memória (SoA / Arena)\n\n");
        if (r.estruturas().isEmpty()) {
            sb.append("*Nenhuma estrutura de dados declarada.*\n\n");
        } else {
            sb.append("| Estrutura | Layout de Memória | Invariantes Formais |\n");
            sb.append("|---|---|---|\n");
            for (RelatorioAuditoria.ItemInvarianteEstrutura est : r.estruturas()) {
                String invs = est.invariantes().isEmpty() ? "—" : "• `" + String.join("`<br>• `", est.invariantes()) + "`";
                sb.append("| `").append(est.estruturaNome()).append("` | `").append(est.layout()).append("` | ").append(invs).append(" |\n");
            }
            sb.append("\n");
        }
        sb.append("---\n\n");

        // 4. Parecer e Métricas
        sb.append("## 4. Parecer da Auditoria & Score de Governança\n\n");
        RelatorioAuditoria.MetricasGovernanca m = r.metricas();
        sb.append("* **Score de Conformidade:** `").append(m.percentualConformidade()).append("%`\n");
        sb.append("* **Regras Mapeadas:** `").append(m.regrasComRastreio()).append("/").append(m.totalRegras()).append("` com rastreabilidade completa\n");
        sb.append("* **Operações com Idempotência Garantida:** `").append(m.totalOperacoesIdempotentes()).append("` rotina(s)\n");
        sb.append("* **Contratos Formais Ativos:** `").append(m.totalContratosExige() + m.totalContratosGarante() + m.totalInvariantes()).append("` (")
                .append(m.totalContratosExige()).append(" EXIGE, ")
                .append(m.totalContratosGarante()).append(" GARANTE, ")
                .append(m.totalInvariantes()).append(" INVARIANTE)\n");
        sb.append("* **Parecer Final da Auditoria Estática:** ").append(m.aprovado() ? "✅ **CONFORMIDADE ESTÁTICA APROVADA** (Elegível para protocolo de homologação e liberação)" : "❌ **REQUER AJUSTES DE CONFORMIDADE**").append("\n\n");

        if (!m.pendencias().isEmpty()) {
            sb.append("### ⚠️ Pendências Críticas\n");
            for (String p : m.pendencias()) sb.append("* ❌ ").append(p).append("\n");
            sb.append("\n");
        }
        if (!m.alertas().isEmpty()) {
            sb.append("### 💡 Alertas e Recomendações\n");
            for (String a : m.alertas()) sb.append("* ⚠️ ").append(a).append("\n");
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Renderiza o relatório em JSON determinístico.
     */
    public static String gerarJsonGovernanca(RelatorioAuditoria r) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"programa\": \"").append(escapar(r.nomePrograma())).append("\",\n");
        sb.append("  \"versaoLinguagem\": \"").append(escapar(r.versaoLinguagem())).append("\",\n");
        sb.append("  \"aprovado\": ").append(r.metricas().aprovado()).append(",\n");
        sb.append("  \"scoreConformidade\": ").append(r.metricas().percentualConformidade()).append(",\n");
        sb.append("  \"metricas\": {\n");
        sb.append("    \"totalRegras\": ").append(r.metricas().totalRegras()).append(",\n");
        sb.append("    \"regrasComRastreio\": ").append(r.metricas().regrasComRastreio()).append(",\n");
        sb.append("    \"totalExige\": ").append(r.metricas().totalContratosExige()).append(",\n");
        sb.append("    \"totalGarante\": ").append(r.metricas().totalContratosGarante()).append(",\n");
        sb.append("    \"totalInvariantes\": ").append(r.metricas().totalInvariantes()).append(",\n");
        sb.append("    \"totalOperacoesIdempotentes\": ").append(r.metricas().totalOperacoesIdempotentes()).append("\n");
        sb.append("  },\n");

        // matriz
        sb.append("  \"matrizRastreabilidade\": [\n");
        for (int i = 0; i < r.matrizRastreio().size(); i++) {
            RelatorioAuditoria.ItemRastreabilidade item = r.matrizRastreio().get(i);
            sb.append("    {\n");
            sb.append("      \"requisitoId\": \"").append(escapar(item.requisitoId())).append("\",\n");
            sb.append("      \"regraIdentificador\": \"").append(escapar(item.regraIdentificador())).append("\",\n");
            sb.append("      \"regraNome\": \"").append(escapar(item.regraNome())).append("\",\n");
            sb.append("      \"conforme\": ").append(item.conforme()).append(",\n");
            sb.append("      \"idempotente\": ").append(item.idempotente()).append(",\n");
            if (item.chaveIdempotencia() != null) {
                sb.append("      \"chaveIdempotencia\": \"").append(escapar(item.chaveIdempotencia())).append("\",\n");
            }
            sb.append("      \"exige\": [").append(listaJson(item.exige())).append("],\n");
            sb.append("      \"garante\": [").append(listaJson(item.garante())).append("],\n");
            sb.append("      \"operacoes\": [").append(listaJson(item.operacoes())).append("]\n");
            sb.append("    }").append(i + 1 < r.matrizRastreio().size() ? "," : "").append("\n");
        }
        sb.append("  ],\n");

        // pendencias e alertas
        sb.append("  \"pendencias\": [").append(listaJson(r.metricas().pendencias())).append("],\n");
        sb.append("  \"alertas\": [").append(listaJson(r.metricas().alertas())).append("]\n");
        sb.append("}\n");
        return sb.toString();
    }


    private static String listaJson(List<String> itens) {
        if (itens == null || itens.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < itens.size(); i++) {
            sb.append("\"").append(escapar(itens.get(i))).append("\"");
            if (i + 1 < itens.size()) sb.append(", ");
        }
        return sb.toString();
    }

    private static String escapar(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
