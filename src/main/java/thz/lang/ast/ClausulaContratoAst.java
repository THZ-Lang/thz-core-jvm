package thz.lang.ast;
public record ClausulaContratoAst(String tipoClausula, ExprAst expressao, String textoCanonico, int linha, int coluna) {}
