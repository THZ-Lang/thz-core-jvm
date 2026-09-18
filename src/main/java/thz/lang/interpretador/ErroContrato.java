package thz.lang.interpretador;

/**
 * Violação de contrato formal — EXIGE / GARANTE / INVARIANTE.
 * Estende ErroExecucao para compatibilidade com tratamento unificado,
 * mas permite captura específica de violações contratuais.
 */

public class ErroContrato extends ErroExecucao {
    public ErroContrato(String message) {
        super(message);
    }
}
