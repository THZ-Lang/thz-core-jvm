package thz.lang.governanca.liberacao;

import java.util.List;

/**
 * Laudo oficial do Protocolo de Liberação para Produção.
 * Vincula criptograficamente código-fonte (SHA-256), regras vigentes, dados reais avaliados
 * e resultados de homologação executáveis.
 */
public record LaudoLiberacaoProducao(
        String nomePrograma,
        String sha256CodigoFonte,
        String versaoLinguagem,
        String referenciaTemporal,
        boolean auditoriaEstaticaAprovada,
        double scoreAuditoriaEstatica,
        boolean homologacaoAprovada,
        int totalCasosHomologacao,
        int casosHomologacaoAprovados,
        List<ResultadoCasoHomologacao> resultadosHomologacao,
        boolean dadosReaisAprovados,
        RegistroDadosReaisValidados dadosReais,
        boolean regrasVigentesAprovadas,
        boolean liberadoParaProducao,
        List<String> impedimentos
) {

    public String gerarMarkdownLaudo() {
        StringBuilder sb = new StringBuilder();
        sb.append("# 🏛️ Laudo Oficial de Liberação para Produção — THZ-LANG\n\n");
        sb.append("> **Status Final:** ").append(liberadoParaProducao ? "✅ **LIBERADO PARA PRODUÇÃO**" : "🚫 **LIBERAÇÃO BLOQUEADA**").append("\n\n");

        sb.append("## 1. Identificação Criptográfica e Temporal\n\n");
        sb.append("- **Programa:** `").append(nomePrograma).append("`\n");
        sb.append("- **SHA-256 do Fonte:** `").append(sha256CodigoFonte).append("`\n");
        sb.append("- **Versão do Compilador/Linguagem:** `").append(versaoLinguagem).append("`\n");
        sb.append("- **Referência Temporal:** `").append(referenciaTemporal).append("`\n\n");

        sb.append("## 2. Auditoria Estática de Governança e Arquitetura Viva\n\n");
        sb.append("- **Score de Governança:** `").append(scoreAuditoriaEstatica).append("%`\n");
        sb.append("- **Status:** ").append(auditoriaEstaticaAprovada ? "✅ Conforme" : "❌ Reprovado").append("\n\n");

        sb.append("## 3. Homologação Executável (Casos Positivos e Negativos)\n\n");
        sb.append("- **Total de Casos:** `").append(totalCasosHomologacao).append("`\n");
        sb.append("- **Aprovados:** `").append(casosHomologacaoAprovados).append("/").append(totalCasosHomologacao).append("`\n");
        sb.append("- **Resultado:** ").append(homologacaoAprovada ? "✅ Homologação Concluída com Sucesso" : "❌ Falhas em Homologação").append("\n\n");

        if (resultadosHomologacao != null && !resultadosHomologacao.isEmpty()) {
            sb.append("| Caso | Descrição | Status | Detalhe |\n");
            sb.append("|---|---|---|---|\n");
            for (ResultadoCasoHomologacao r : resultadosHomologacao) {
                sb.append("| `").append(r.identificador()).append("` | ")
                        .append(r.descricao()).append(" | ")
                        .append(r.aprovado() ? "✅ Aprovado" : "❌ Falhou").append(" | ")
                        .append(r.detalhe()).append(" |\n");
            }
            sb.append("\n");
        }

        sb.append("## 4. Validação em Dados Reais (Modo Dry-Run)\n\n");
        if (dadosReais != null) {
            sb.append("- **Fonte de Dados:** `").append(dadosReais.identificadorFonte()).append("`\n");
            sb.append("- **SHA-256 dos Dados:** `").append(dadosReais.sha256Dados()).append("`\n");
            sb.append("- **Registros Avaliados:** `").append(dadosReais.totalRegistrosAvaliados()).append("`\n");
            sb.append("- **Pendências Impeditivas:** `").append(dadosReais.totalPendenciasImpeditivas()).append("`\n");
            sb.append("- **Validação Sem Gravação (Dry-Run):** ").append(dadosReais.modoValidacaoSemGravacao() ? "✅ Garantida" : "❌ Violação").append("\n");
            sb.append("- **Resultado:** ").append(dadosReaisAprovados ? "✅ Aprovado para Dados Reais" : "❌ Pendências Impeditivas").append("\n\n");
        } else {
            sb.append("*Nenhum conjunto de dados reais submetido.*\n\n");
        }

        sb.append("## 5. Revisões das Regras Vigentes\n\n");
        sb.append("- **Status de Vigência:** ").append(regrasVigentesAprovadas ? "✅ Revisões Unívocas e Vigentes" : "❌ Conflito ou Ausência de Vigência").append("\n\n");

        if (!impedimentos.isEmpty()) {
            sb.append("## ⚠️ Impedimentos Críticos para Entrada em Produção\n\n");
            for (String imp : impedimentos) {
                sb.append("- 🚫 ").append(imp).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
