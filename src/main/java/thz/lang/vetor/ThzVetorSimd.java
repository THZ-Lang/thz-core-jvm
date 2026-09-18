package thz.lang.vetor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * ThzVetorSimd — Motor de álgebra vetorial e busca semântica SIMD para THZ-LANG.
 * Suporta Similaridade de Cosseno, Distância Euclidiana (L2), Produto Escalar
 * e busca Top-K de vizinhos mais próximos.
 */
public final class ThzVetorSimd {

    public enum Metrica {
        COSSENO,
        EUCLIDIANA,
        PRODUTO_ESCALAR
    }

    public record ItemVetorial(String id, float[] embedding, Object metadados) {}

    public record ResultadoBusca(String id, double pontuacao, Object metadados) implements Comparable<ResultadoBusca> {
        @Override
        public int compareTo(ResultadoBusca o) {
            return Double.compare(o.pontuacao, this.pontuacao); // Ordem decrescente de relevância
        }
    }

    private ThzVetorSimd() {}

    /**
     * Calcula o produto escalar (dot product) entre dois vetores.
     */
    public static double produtoEscalar(float[] a, float[] b) {
        validarDimensoes(a, b);
        double soma = 0.0;
        int i = 0;
        int len = a.length;
        // Loop desenrolado 4x para otimização de pipeline e autovetorização SIMD
        int limiteVetor = len - (len % 4);
        for (; i < limiteVetor; i += 4) {
            soma += (a[i] * b[i]) +
                    (a[i + 1] * b[i + 1]) +
                    (a[i + 2] * b[i + 2]) +
                    (a[i + 3] * b[i + 3]);
        }
        for (; i < len; i++) {
            soma += (a[i] * b[i]);
        }
        return soma;
    }

    /**
     * Calcula a norma euclidiana (magnitude / comprimento) de um vetor.
     */
    public static double norma(float[] a) {
        Objects.requireNonNull(a, "Vetor não pode ser nulo");
        double somaQuadrados = 0.0;
        int i = 0;
        int len = a.length;
        int limiteVetor = len - (len % 4);
        for (; i < limiteVetor; i += 4) {
            somaQuadrados += (a[i] * a[i]) +
                             (a[i + 1] * a[i + 1]) +
                             (a[i + 2] * a[i + 2]) +
                             (a[i + 3] * a[i + 3]);
        }
        for (; i < len; i++) {
            somaQuadrados += (a[i] * a[i]);
        }
        return Math.sqrt(somaQuadrados);
    }

    /**
     * Calcula a similaridade de cosseno entre dois vetores (resultado entre -1.0 e 1.0).
     */
    public static double similaridadeCosseno(float[] a, float[] b) {
        validarDimensoes(a, b);
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        int len = a.length;

        for (int i = 0; i < len; i++) {
            float ai = a[i];
            float bi = b[i];
            dot += ai * bi;
            normA += ai * ai;
            normB += bi * bi;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Calcula a distância euclidiana (L2) entre dois vetores (>= 0.0).
     */
    public static double distanciaEuclidiana(float[] a, float[] b) {
        validarDimensoes(a, b);
        double somaQuadrados = 0.0;
        int len = a.length;
        for (int i = 0; i < len; i++) {
            double diff = a[i] - b[i];
            somaQuadrados += diff * diff;
        }
        return Math.sqrt(somaQuadrados);
    }

    /**
     * Normaliza um vetor para norma unitária (norma = 1.0).
     */
    public static float[] normalizar(float[] a) {
        Objects.requireNonNull(a, "Vetor não pode ser nulo");
        double n = norma(a);
        if (n == 0.0) return a.clone();
        float[] res = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = (float) (a[i] / n);
        }
        return res;
    }

    /**
     * Busca os Top-K vetores mais próximos de um vetor de consulta.
     */
    public static List<ResultadoBusca> buscarTopK(float[] consulta, List<ItemVetorial> base, int topK, Metrica metrica) {
        if (base == null || base.isEmpty() || topK <= 0) {
            return Collections.emptyList();
        }

        List<ResultadoBusca> pontuados = new ArrayList<>(base.size());
        for (ItemVetorial item : base) {
            double score;
            switch (metrica) {
                case COSSENO -> score = similaridadeCosseno(consulta, item.embedding());
                case EUCLIDIANA -> score = -distanciaEuclidiana(consulta, item.embedding()); // Menor distância = maior pontuação
                case PRODUTO_ESCALAR -> score = produtoEscalar(consulta, item.embedding());
                default -> score = similaridadeCosseno(consulta, item.embedding());
            }
            pontuados.add(new ResultadoBusca(item.id(), score, item.metadados()));
        }

        pontuados.sort(Comparator.naturalOrder());
        return pontuados.subList(0, Math.min(topK, pontuados.size()));
    }

    /**
     * Converte uma string no formato "[0.1, 0.2, -0.3]" em array float[].
     */
    public static float[] parseVetor(String texto) {
        if (texto == null || texto.isBlank()) return new float[0];
        String limpo = texto.trim();
        if (limpo.startsWith("[") && limpo.endsWith("]")) {
            limpo = limpo.substring(1, limpo.length() - 1);
        }
        if (limpo.isBlank()) return new float[0];
        String[] partes = limpo.split(",");
        float[] arr = new float[partes.length];
        for (int i = 0; i < partes.length; i++) {
            arr[i] = Float.parseFloat(partes[i].trim());
        }
        return arr;
    }

    /**
     * Formata um vetor float[] como string "[0.1000, 0.2000, ...]".
     */
    public static String formatarVetor(float[] vetor) {
        if (vetor == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vetor.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(vetor[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private static void validarDimensoes(float[] a, float[] b) {
        Objects.requireNonNull(a, "Primeiro vetor não pode ser nulo");
        Objects.requireNonNull(b, "Segundo vetor não pode ser nulo");
        if (a.length != b.length) {
            throw new IllegalArgumentException("Dimensões dos vetores incompatíveis: " + a.length + " != " + b.length);
        }
    }
}
