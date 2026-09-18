package thz.lang.ast;

import java.util.List;

public sealed interface ComandoAst permits
        ComandoAst.DeclVariavel,
        ComandoAst.Atribuicao,
        ComandoAst.Se,
        ComandoAst.Enquanto,
        ComandoAst.VetorizarPara,
        ComandoAst.Para,
        ComandoAst.BlocoMemoria,
        ComandoAst.Exiba,
        ComandoAst.Ler,
        ComandoAst.Chamada,
        ComandoAst.Retorne,
        ComandoAst.FalharCom,
        ComandoAst.Tente,
        ComandoAst.CasoResultado {

    int linha();
    int coluna();

    record DeclVariavel(String nome, String tipoDado, ExprAst inicializacao, int linha, int coluna) implements ComandoAst {}
    record Atribuicao(List<String> alvo, ExprAst expressao, int linha, int coluna) implements ComandoAst {}
    record Se(ExprAst condicao, List<ComandoAst> entao, List<ComandoAst> senao, int linha, int coluna) implements ComandoAst {}
    record Enquanto(ExprAst condicao, List<ComandoAst> corpo, int linha, int coluna) implements ComandoAst {}
    record VetorizarPara(String variavel, List<String> fonte, Integer passoSimd, List<ComandoAst> corpo, int linha, int coluna) implements ComandoAst {}
    record Para(String variavel, ExprAst inicio, ExprAst fim, ExprAst passo, List<ComandoAst> corpo, int linha, int coluna) implements ComandoAst {}
    record BlocoMemoria(String nome, List<ComandoAst> corpo, int linha, int coluna) implements ComandoAst {}
    record Exiba(ExprAst expressao, int linha, int coluna) implements ComandoAst {}
    record Ler(List<String> alvo, int linha, int coluna) implements ComandoAst {}
    record Chamada(ExprAst expressao, int linha, int coluna) implements ComandoAst {}
    record Retorne(ExprAst expressao, int linha, int coluna) implements ComandoAst {}
    record FalharCom(ExprAst expressao, int linha, int coluna) implements ComandoAst {}
    record Tente(List<ComandoAst> corpoTente, String tipoCaptura, List<ComandoAst> corpoCapture, int linha, int coluna) implements ComandoAst {}
    record CasoResultado(ExprAst alvo, String varSucesso, List<ComandoAst> corpoSucesso, String varErro, List<ComandoAst> corpoErro, int linha, int coluna) implements ComandoAst {}
}
