package thz.lang.ast;

import java.util.List;

/**
 * Representação sintática de uma OPERACAO dentro de uma REGRA_NEGOCIO.
 * Suporta o contrato de IDEMPOTENCIA de larga escala e chave determinística.
 */
public record OperacaoAst(
        String nome,
        List<ParametroOperacaoAst> parametros,
        String tipoRetorno,
        List<ComandoAst> corpo,
        boolean idempotente,
        String chaveIdempotencia
) {
    public OperacaoAst(String nome, List<ParametroOperacaoAst> parametros, String tipoRetorno, List<ComandoAst> corpo) {
        this(nome, parametros, tipoRetorno, corpo, false, null);
    }
}
