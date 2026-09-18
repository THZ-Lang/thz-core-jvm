package thz.lang.interpretador;

/**
 * Exceção interna de fluxo para FALHAR_COM — canal de erro de RESULTADO[T,E].
 * Port exato de SinalFalharCom em interpretador.ts.
 */

public class SinalFalhar extends RuntimeException {
    private final ValorThz valor;

    public SinalFalhar(ValorThz valor) {
        super(null, null, true, false);
        this.valor = valor;
    }

    public ValorThz getValor() {
        return valor;
    }
}
