package thz.lang.ast;

import java.util.List;

/**
 * Representação sintática de um PROCEDIMENTO.
 * Suporta o contrato de IDEMPOTENCIA.
 */
public record ProcedimentoAst(
        String nome,
        List<ParametroOperacaoAst> parametros,
        List<ComandoAst> corpo,
        boolean idempotente,
        String chaveIdempotencia
) {
    public ProcedimentoAst(String nome, List<ParametroOperacaoAst> parametros, List<ComandoAst> corpo) {
        this(nome, parametros, corpo, false, null);
    }
}
