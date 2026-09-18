package thz.lang.ast;

import java.math.BigInteger;
import java.util.List;

public sealed interface ExprAst permits
        ExprAst.LiteralInteiro,
        ExprAst.LiteralDecimal,
        ExprAst.LiteralTexto,
        ExprAst.LiteralLogico,
        ExprAst.Nulo,
        ExprAst.AcessoCampo,
        ExprAst.Chamada,
        ExprAst.Indexacao,
        ExprAst.FatiaLiteral,
        ExprAst.CriarRegistro,
        ExprAst.OpBinaria,
        ExprAst.OpUnaria,
        ExprAst.ConsultaTipada {

    int linha();
    int coluna();

    record LiteralInteiro(BigInteger valor, int linha, int coluna) implements ExprAst {}
    record LiteralDecimal(BigInteger escalado, int escala, int linha, int coluna) implements ExprAst {}
    record LiteralTexto(String valor, int linha, int coluna) implements ExprAst {}
    record LiteralLogico(boolean valor, int linha, int coluna) implements ExprAst {}
    record Nulo(int linha, int coluna) implements ExprAst {}
    record AcessoCampo(List<String> caminho, int linha, int coluna) implements ExprAst {}
    record Chamada(List<String> caminho, List<ExprAst> argumentos, int linha, int coluna) implements ExprAst {}
    record Indexacao(ExprAst alvo, ExprAst indice, int linha, int coluna) implements ExprAst {}
    record FatiaLiteral(List<ExprAst> elementos, int linha, int coluna) implements ExprAst {}
    record CriarRegistro(String nomeEstrutura, List<CampoValor> campos, int linha, int coluna) implements ExprAst {}
    record OpBinaria(String operador, ExprAst esquerda, ExprAst direita, int linha, int coluna) implements ExprAst {}
    record OpUnaria(String operador, ExprAst operando, int linha, int coluna) implements ExprAst {}
    record ConsultaTipada(ExprAst fonte, ExprAst onde, String campoOrdenacao, boolean asc, ExprAst limite, ExprAst pular, int linha, int coluna) implements ExprAst {}

    record CampoValor(String nome, ExprAst valor) {}
}
