package thz.lang.interpretador;

/**
 * Exceção interna de fluxo para RETORNE — nunca atravessa o usuário.
 * Port exato de SinalRetorne em interpretador.ts.
 */
public class SinalRetorne extends RuntimeException {
    private final ValorThz valor;

    public SinalRetorne(ValorThz valor) {
        super(null, null, true, false);
        this.valor = valor;
    }

    public ValorThz getValor() {
        return valor;
    }
}
