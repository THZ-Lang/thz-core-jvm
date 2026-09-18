package thz.lang.brasil;

import java.math.BigDecimal;

/**
 * Motor de Valores por Extenso em moeda corrente (Real / Centavos).
 */
public final class ThzExtensoEngine {

    private static final String[] UNIDADES = {
            "", "um", "dois", "três", "quatro", "cinco", "seis", "sete", "oito", "nove", "dez",
            "onze", "doze", "treze", "quatorze", "quinze", "dezesseis", "dezessete", "dezoito", "dezenove"
    };
    private static final String[] DEZENAS = {
            "", "", "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa"
    };
    private static final String[] CENTENAS = {
            "", "cento", "duzentos", "trezentos", "quatrocentos", "quinhentos", "seiscentos", "setecentos", "oitocentos", "novecentos"
    };

    private ThzExtensoEngine() {}

    public static String valorPorExtenso(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) == 0) {
            return "zero reais";
        }

        boolean negativo = valor.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal positivo = valor.abs().setScale(2, java.math.RoundingMode.HALF_EVEN);
        long inteiros = positivo.longValue();
        int centavos = positivo.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();

        StringBuilder sb = new StringBuilder();
        if (negativo) sb.append("menos ");

        if (inteiros > 0) {
            sb.append(escreverNumeroExtenso(inteiros));
            sb.append(inteiros == 1 ? " real" : " reais");
        }

        if (centavos > 0) {
            if (inteiros > 0) sb.append(" e ");
            sb.append(escreverNumeroExtenso(centavos));
            sb.append(centavos == 1 ? " centavo" : " centavos");
        }

        return sb.toString();
    }

    static String escreverNumeroExtenso(long n) {
        if (n == 0) return "zero";
        if (n == 100) return "cem";

        if (n >= 1_000_000_000) {
            long bi = n / 1_000_000_000;
            long resto = n % 1_000_000_000;
            String termo = bi == 1 ? "um bilhão" : escreverNumeroExtenso(bi) + " bilhões";
            return resto > 0 ? termo + (resto <= 100 || resto % 100 == 0 ? " e " : " ") + escreverNumeroExtenso(resto) : termo;
        }

        if (n >= 1_000_000) {
            long mi = n / 1_000_000;
            long resto = n % 1_000_000;
            String termo = mi == 1 ? "um milhão" : escreverNumeroExtenso(mi) + " milhões";
            return resto > 0 ? termo + (resto <= 100 || resto % 100 == 0 ? " e " : " ") + escreverNumeroExtenso(resto) : termo;
        }

        if (n >= 1000) {
            long mil = n / 1000;
            long resto = n % 1000;
            String termo = mil == 1 ? "mil" : escreverNumeroExtenso(mil) + " mil";
            return resto > 0 ? termo + (resto <= 100 || resto % 100 == 0 ? " e " : " ") + escreverNumeroExtenso(resto) : termo;
        }

        if (n >= 100) {
            int c = (int) (n / 100);
            long resto = n % 100;
            String termo = CENTENAS[c];
            return resto > 0 ? termo + " e " + escreverNumeroExtenso(resto) : termo;
        }

        if (n >= 20) {
            int d = (int) (n / 10);
            long resto = n % 10;
            String termo = DEZENAS[d];
            return resto > 0 ? termo + " e " + escreverNumeroExtenso(resto) : termo;
        }

        return UNIDADES[(int) n];
    }
}
