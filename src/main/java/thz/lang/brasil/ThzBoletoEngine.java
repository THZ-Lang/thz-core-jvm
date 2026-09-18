package thz.lang.brasil;

import thz.lang.runtime.DecimalFixo;

import java.math.BigDecimal;

/**
 * Motor de Boletos Bancários — linha digitável, código de barras e valor.
 */
public final class ThzBoletoEngine {

    private ThzBoletoEngine() {}

    public static boolean validarLinhaDigitavel(String linha) {
        if (linha == null) return false;
        String digitos = linha.replaceAll("\\D", "");
        if (digitos.length() != 47) return false;

        if (!validarModulo10(digitos.substring(0, 9), digitos.charAt(9) - '0')) return false;
        if (!validarModulo10(digitos.substring(10, 20), digitos.charAt(20) - '0')) return false;
        if (!validarModulo10(digitos.substring(21, 31), digitos.charAt(31) - '0')) return false;

        return true;
    }

    public static String linhaDigitavelParaCodigoBarras(String linha) {
        if (linha == null) return "";
        String d = linha.replaceAll("\\D", "");
        if (d.length() != 47) return "";

        String bancoEMoeda = d.substring(0, 4);
        String dvGeral = d.substring(32, 33);
        String fatorEValor = d.substring(33, 47);
        String campoLivre1 = d.substring(4, 9);
        String campoLivre2 = d.substring(10, 20);
        String campoLivre3 = d.substring(21, 31);

        return bancoEMoeda + dvGeral + fatorEValor + campoLivre1 + campoLivre2 + campoLivre3;
    }

    public static DecimalFixo extrairValorBoleto(String linha) {
        if (linha == null) return DecimalFixo.ZERO;
        String d = linha.replaceAll("\\D", "");
        if (d.length() < 47) return DecimalFixo.ZERO;
        String valorCentavosStr = d.substring(37, 47);
        try {
            long centavos = Long.parseLong(valorCentavosStr);
            BigDecimal valor = BigDecimal.valueOf(centavos).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_EVEN);
            return DecimalFixo.deTexto(valor.toPlainString(), 4);
        } catch (Exception e) {
            return DecimalFixo.ZERO;
        }
    }

    private static boolean validarModulo10(String bloco, int dvEsperado) {
        int soma = 0;
        int peso = 2;
        for (int i = bloco.length() - 1; i >= 0; i--) {
            int num = (bloco.charAt(i) - '0') * peso;
            if (num > 9) num = (num / 10) + (num % 10);
            soma += num;
            peso = (peso == 2) ? 1 : 2;
        }
        int dezenaSuperior = ((soma + 9) / 10) * 10;
        int dvCalculado = dezenaSuperior - soma;
        if (dvCalculado == 10) dvCalculado = 0;
        return dvCalculado == dvEsperado;
    }
}
