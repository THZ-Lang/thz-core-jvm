package thz.lang.simd;

import java.util.List;

/**
 * Resultado da análise estática formal de vetorização SIMD (Regras R1 a R5).
 */
public record ResultadoValidacaoSimd(
        String loopIdentificador,
        String variavel,
        List<String> fonte,
        int passoSimd,
        boolean vetorizavel,
        List<String> regrasAtendidas,
        List<String> violacoes,
        List<String> avisos
) {
    public boolean temViolacoes() {
        return violacoes != null && !violacoes.isEmpty();
    }
}
