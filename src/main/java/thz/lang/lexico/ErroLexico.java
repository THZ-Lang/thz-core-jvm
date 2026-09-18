package thz.lang.lexico;


public class ErroLexico extends RuntimeException {
    private final int linha;
    private final int coluna;
    public ErroLexico(int linha, int coluna, String mensagem) {
        super("[Erro Léxico][Linha " + linha + ":" + coluna + "] " + mensagem);
        this.linha = linha;
        this.coluna = coluna;
    }
    public int linha() { return linha; }
    public int coluna() { return coluna; }
}
