package thz.lang.ast;

import java.util.List;

/** Representa um cálculo reutilizável com retorno tipado explícito. */
public record FuncaoAst(
        String nome,
        List<ParametroOperacaoAst> parametros,
        String tipoRetorno,
        List<ComandoAst> corpo
) {}
