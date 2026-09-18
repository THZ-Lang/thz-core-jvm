package thz.lang.interpretador;


public class ErroExecucao extends RuntimeException {
    public ErroExecucao(String message) {
        super(message);
    }
    public ErroExecucao(String message, Throwable cause) {
        super(message, cause);
    }
}
