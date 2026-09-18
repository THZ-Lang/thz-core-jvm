package thz.lang.analytics;

import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DecimalFixo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/**
 * ThzEstatistica — Motor de Estatística Descritiva, Inferencial e Preditiva.
 * Suporta cálculos de tendência central, dispersão, correlação linear de Pearson,
 * detecção de outliers (critério IQR / Tukey) e regressão linear de mínimos quadrados.
 */
public final class ThzEstatistica {

    private static final MathContext MC = new MathContext(18, RoundingMode.HALF_EVEN);

    private ThzEstatistica() {}

    /**
     * Média aritmética simples de uma lista de valores numéricos ou decimais.
     */
    public static DecimalFixo media(List<Double> valores) {
        if (valores == null || valores.isEmpty()) {
            return DecimalFixo.ZERO;
        }
        BigDecimal soma = BigDecimal.ZERO;
        for (Double v : valores) {
            soma = soma.add(BigDecimal.valueOf(v));
        }
        BigDecimal m = soma.divide(BigDecimal.valueOf(valores.size()), MC);
        return DecimalFixo.deTexto(m.setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
    }

    /**
     * Mediana estatística (valor central ou média dos dois valores centrais).
     */
    public static DecimalFixo mediana(List<Double> valores) {
        if (valores == null || valores.isEmpty()) {
            return DecimalFixo.ZERO;
        }
        List<Double> ordenados = new ArrayList<>(valores);
        Collections.sort(ordenados);
        int n = ordenados.size();
        BigDecimal med;
        if (n % 2 == 1) {
            med = BigDecimal.valueOf(ordenados.get(n / 2));
        } else {
            BigDecimal a = BigDecimal.valueOf(ordenados.get(n / 2 - 1));
            BigDecimal b = BigDecimal.valueOf(ordenados.get(n / 2));
            med = a.add(b).divide(BigDecimal.valueOf(2), MC);
        }
        return DecimalFixo.deTexto(med.setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
    }

    /**
     * Moda estatística (valor mais frequente). Retorna o primeiro em caso de multimodal.
     */
    public static DecimalFixo moda(List<Double> valores) {
        if (valores == null || valores.isEmpty()) {
            return DecimalFixo.ZERO;
        }
        Map<Double, Integer> freq = new HashMap<>();
        for (Double v : valores) {
            freq.put(v, freq.getOrDefault(v, 0) + 1);
        }
        double modaVal = valores.get(0);
        int maxFreq = 0;
        for (var entry : freq.entrySet()) {
            if (entry.getValue() > maxFreq) {
                maxFreq = entry.getValue();
                modaVal = entry.getKey();
            }
        }
        return DecimalFixo.deTexto(BigDecimal.valueOf(modaVal).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
    }

    /**
     * Variância amostral (ou populacional se amostral = false).
     */
    public static DecimalFixo variancia(List<Double> valores, boolean amostral) {
        if (valores == null || valores.size() < 2) {
            return DecimalFixo.ZERO;
        }
        int n = valores.size();
        BigDecimal m = new BigDecimal(media(valores).formatar());
        BigDecimal somaQuadrados = BigDecimal.ZERO;
        for (Double v : valores) {
            BigDecimal diff = BigDecimal.valueOf(v).subtract(m);
            somaQuadrados = somaQuadrados.add(diff.multiply(diff));
        }
        int divisor = amostral ? (n - 1) : n;
        BigDecimal var = somaQuadrados.divide(BigDecimal.valueOf(divisor), MC);
        return DecimalFixo.deTexto(var.setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
    }

    /**
     * Desvio padrão.
     */
    public static DecimalFixo desvioPadrao(List<Double> valores, boolean amostral) {
        BigDecimal var = new BigDecimal(variancia(valores, amostral).formatar());
        double std = Math.sqrt(var.doubleValue());
        return DecimalFixo.deTexto(BigDecimal.valueOf(std).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
    }

    /**
     * Coeficiente de Correlação Linear de Pearson entre duas variáveis X e Y ($r \in [-1, 1]$).
     */
    public static DecimalFixo correlacaoPearson(List<Double> x, List<Double> y) {
        if (x == null || y == null || x.size() != y.size() || x.size() < 2) {
            return DecimalFixo.ZERO;
        }
        int n = x.size();
        double mediaX = Double.parseDouble(media(x).formatar());
        double mediaY = Double.parseDouble(media(y).formatar());

        double numerador = 0.0;
        double somaQuadX = 0.0;
        double somaQuadY = 0.0;

        for (int i = 0; i < n; i++) {
            double diffX = x.get(i) - mediaX;
            double diffY = y.get(i) - mediaY;
            numerador += diffX * diffY;
            somaQuadX += diffX * diffX;
            somaQuadY += diffY * diffY;
        }

        double denominador = Math.sqrt(somaQuadX * somaQuadY);
        if (denominador == 0.0) {
            return DecimalFixo.ZERO;
        }
        double r = numerador / denominador;
        return DecimalFixo.deTexto(BigDecimal.valueOf(r).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
    }

    /**
     * Cálculo de percentil $P$ ($0 \le P \le 100$).
     */
    public static DecimalFixo percentil(List<Double> valores, double p) {
        if (valores == null || valores.isEmpty()) return DecimalFixo.ZERO;
        List<Double> ordenados = new ArrayList<>(valores);
        Collections.sort(ordenados);
        if (p <= 0) return DecimalFixo.deTexto(BigDecimal.valueOf(ordenados.get(0)).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
        if (p >= 100) return DecimalFixo.deTexto(BigDecimal.valueOf(ordenados.get(ordenados.size() - 1)).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);

        double rank = (p / 100.0) * (ordenados.size() - 1);
        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);
        double weight = rank - lowerIndex;

        double val = ordenados.get(lowerIndex) + weight * (ordenados.get(upperIndex) - ordenados.get(lowerIndex));
        return DecimalFixo.deTexto(BigDecimal.valueOf(val).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
    }

    /**
     * Calcula o Z-Score de um valor em relação à amostra: $Z = \frac{X - \mu}{\sigma}$.
     */
    public static DecimalFixo zScore(double valor, List<Double> amostra) {
        double med = Double.parseDouble(media(amostra).formatar());
        double std = Double.parseDouble(desvioPadrao(amostra, true).formatar());
        if (std == 0.0) return DecimalFixo.ZERO;
        double z = (valor - med) / std;
        return DecimalFixo.deTexto(BigDecimal.valueOf(z).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
    }

    /**
     * Detecta valores discrepantes (Outliers) utilizando o critério de amplitude interquartil (IQR / Tukey).
     * Retorna a lista de valores considerados anômalos.
     */
    public static List<ValorThz> detectarOutliers(List<Double> valores) {
        if (valores == null || valores.size() < 4) return List.of();
        double q1 = Double.parseDouble(percentil(valores, 25).formatar());
        double q3 = Double.parseDouble(percentil(valores, 75).formatar());
        double iqr = q3 - q1;
        double limiteInferior = q1 - 1.5 * iqr;
        double limiteSuperior = q3 + 1.5 * iqr;

        List<ValorThz> outliers = new ArrayList<>();
        for (Double v : valores) {
            if (v < limiteInferior || v > limiteSuperior) {
                outliers.add(ValorThz.DECIMAL(DecimalFixo.deTexto(BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4)));
            }
        }
        return outliers;
    }

    public record RegressaoResultado(DecimalFixo inclinacao, DecimalFixo intercepto, DecimalFixo rQuadrado) {}

    /**
     * Regressão Linear Simples via Mínimos Quadrados: $Y = aX + b$.
     */
    public static RegressaoResultado regressaoLinear(List<Double> x, List<Double> y) {
        if (x == null || y == null || x.size() != y.size() || x.size() < 2) {
            return new RegressaoResultado(DecimalFixo.ZERO, DecimalFixo.ZERO, DecimalFixo.ZERO);
        }
        int n = x.size();
        double mediaX = Double.parseDouble(media(x).formatar());
        double mediaY = Double.parseDouble(media(y).formatar());

        double sxx = 0.0;
        double sxy = 0.0;
        double syy = 0.0;

        for (int i = 0; i < n; i++) {
            double dx = x.get(i) - mediaX;
            double dy = y.get(i) - mediaY;
            sxx += dx * dx;
            sxy += dx * dy;
            syy += dy * dy;
        }

        if (sxx == 0.0) {
            return new RegressaoResultado(DecimalFixo.ZERO, DecimalFixo.deTexto(BigDecimal.valueOf(mediaY).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4), DecimalFixo.ZERO);
        }

        double inclinacao = sxy / sxx;
        double intercepto = mediaY - (inclinacao * mediaX);
        double r2 = (syy > 0.0) ? (sxy * sxy) / (sxx * syy) : 0.0;

        return new RegressaoResultado(
                DecimalFixo.deTexto(BigDecimal.valueOf(inclinacao).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4),
                DecimalFixo.deTexto(BigDecimal.valueOf(intercepto).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4),
                DecimalFixo.deTexto(BigDecimal.valueOf(r2).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4)
        );
    }
}
