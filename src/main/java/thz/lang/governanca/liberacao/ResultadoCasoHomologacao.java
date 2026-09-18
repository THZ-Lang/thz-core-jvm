package thz.lang.governanca.liberacao;

/**
 * Resultado individual de execução de um caso de homologação.
 */
public record ResultadoCasoHomologacao(
        String identificador,
        String descricao,
        boolean aprovado,
        String detalhe
) {}
