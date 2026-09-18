package thz.lang.governanca.liberacao;

import java.util.List;

/**
 * Registro de validação executada contra dados reais em modo dry-run (sem gravação de acervo),
 * em conformidade com o critério P02 e P03 da Especificação Técnica.
 */
public record RegistroDadosReaisValidados(
        String identificadorFonte,
        String sha256Dados,
        int totalRegistrosAvaliados,
        int totalConformes,
        int totalPendenciasImpeditivas,
        List<String> pendencias,
        boolean modoValidacaoSemGravacao
) {
    public static RegistroDadosReaisValidados criar(
            String fonte,
            String hash,
            int total,
            int conformes,
            List<String> pendencias
    ) {
        int impeditivas = pendencias != null ? pendencias.size() : 0;
        return new RegistroDadosReaisValidados(
                fonte,
                hash,
                total,
                conformes,
                impeditivas,
                pendencias != null ? List.copyOf(pendencias) : List.of(),
                true
        );
    }
}
