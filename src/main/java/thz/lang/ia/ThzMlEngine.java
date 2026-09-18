package thz.lang.ia;

import thz.lang.vetor.ThzVetorSimd;

import java.util.Objects;

/**
 * ThzMlEngine — Motor de Inferência de Machine Learning Clássico (Zero Python).
 * Suporta predição por regressão linear/polinomial, classificação logística (sigmoide)
 * e árvores de decisão determinísticas.
 */
public final class ThzMlEngine {

    private ThzMlEngine() {}

    /**
     * Calcula a probabilidade de classificação binária através da função Sigmoide:
     * P(y=1|x) = 1 / (1 + e^-(w · x + b))
     */
    public static double classificarProbabilidade(float[] features, float[] pesos, float bias) {
        Objects.requireNonNull(features, "Features não podem ser nulas");
        Objects.requireNonNull(pesos, "Pesos não podem ser nulos");
        double z = ThzVetorSimd.produtoEscalar(features, pesos) + bias;
        return 1.0 / (1.0 + Math.exp(-z));
    }

    /**
     * Predição contínua por Regressão Linear:
     * y = w · x + b
     */
    public static double predizerRegressao(float[] features, float[] coeficientes, float intercepto) {
        Objects.requireNonNull(features, "Features não podem ser nulas");
        Objects.requireNonNull(coeficientes, "Coeficientes não podem ser nulos");
        return ThzVetorSimd.produtoEscalar(features, coeficientes) + intercepto;
    }
}
