package thz.lang.ast;

/**
 * Nó da AST que representa uma cláusula de importação de módulo.
 */
public record ImportacaoAst(
        String modulo,
        String caminho,
        int linha,
        int coluna
) {}
