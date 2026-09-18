package thz.lang.ast;

import java.util.List;

/**
 * Representação sintática de uma REGRA_NEGOCIO corporativa.
 * Suporta metadados de auditoria e invariante de IDEMPOTENCIA.
 */
public record RegraNegocioAst(
        String nome,
        String identificador,
        String rastreioRequisito,
        String descricao,
        List<ClausulaContratoAst> clausulasEntrada,
        List<ClausulaContratoAst> clausulasSaida,
        List<OperacaoAst> operacoes,
        boolean idempotente,
        String chaveIdempotencia
) {
    public RegraNegocioAst(
            String nome,
            String identificador,
            String rastreioRequisito,
            String descricao,
            List<ClausulaContratoAst> clausulasEntrada,
            List<ClausulaContratoAst> clausulasSaida,
            List<OperacaoAst> operacoes
    ) {
        this(nome, identificador, rastreioRequisito, descricao, clausulasEntrada, clausulasSaida, operacoes, false, null);
    }
}
