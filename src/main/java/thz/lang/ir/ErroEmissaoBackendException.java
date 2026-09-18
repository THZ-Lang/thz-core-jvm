package thz.lang.ir;

/**
 * Exceção lançada quando o emissor de backend (LLVM IR / AOT) encontra comandos
 * ou construções sintáticas para as quais ainda não existe lowering direto para máquina alvo.
 */
public class ErroEmissaoBackendException extends RuntimeException {

    private final String backend;
    private final String construcaoNaoSuportada;

    public ErroEmissaoBackendException(String backend, String construcaoNaoSuportada, String mensagem) {
        super("[" + backend + "] " + mensagem);
        this.backend = backend;
        this.construcaoNaoSuportada = construcaoNaoSuportada;
    }

    public String backend() {
        return backend;
    }

    public String construcaoNaoSuportada() {
        return construcaoNaoSuportada;
    }
}
