package thz.lang.governanca;

import thz.lang.ast.MetadadosArquiteturaAst;
import java.util.List;

/**
 * Representação imutável do relatório de auditoria e governança de um programa THZ-LANG.
 */
public record RelatorioAuditoria(
        String nomePrograma,
        String versaoLinguagem,
        MetadadosArquiteturaAst metadados,
        List<ItemRastreabilidade> matrizRastreio,
        List<ItemInvarianteEstrutura> estruturas,
        MetricasGovernanca metricas
) {
    public record ItemRastreabilidade(
            String requisitoId,
            String regraIdentificador,
            String regraNome,
            List<String> exige,
            List<String> garante,
            List<String> operacoes,
            boolean conforme,
            boolean idempotente,
            String chaveIdempotencia,
            List<String> pendencias
    ) {
        public ItemRastreabilidade(
                String requisitoId,
                String regraIdentificador,
                String regraNome,
                List<String> exige,
                List<String> garante,
                List<String> operacoes,
                boolean conforme,
                List<String> pendencias
        ) {
            this(requisitoId, regraIdentificador, regraNome, exige, garante, operacoes, conforme, false, null, pendencias);
        }
    }

    public record ItemInvarianteEstrutura(
            String estruturaNome,
            String layout,
            List<String> invariantes
    ) {}

    public record MetricasGovernanca(
            int totalRegras,
            int regrasComRastreio,
            int totalContratosExige,
            int totalContratosGarante,
            int totalInvariantes,
            int totalOperacoesIdempotentes,
            double percentualConformidade,
            boolean aprovado,
            List<String> pendencias,
            List<String> alertas
    ) {
        public MetricasGovernanca(
                int totalRegras,
                int regrasComRastreio,
                int totalContratosExige,
                int totalContratosGarante,
                int totalInvariantes,
                double percentualConformidade,
                boolean aprovado,
                List<String> pendencias,
                List<String> alertas
        ) {
            this(totalRegras, regrasComRastreio, totalContratosExige, totalContratosGarante, totalInvariantes, 0, percentualConformidade, aprovado, pendencias, alertas);
        }
    }
}
