package thz.lang.ia;

import thz.lang.vetor.ThzVetorSimd;

import java.nio.charset.StandardCharsets;


/**
 * ThzIaEngine — Motor de Inteligência Artificial e Geração de Embeddings On-Device.
 * Permite extração de vetores semânticos e RAG sem dependências de nuvem ou Python.
 */
public final class ThzIaEngine {

    public static final int DIMENSAO_PADRAO = 128;

    private ThzIaEngine() {}

    /**
     * Gera um vetor de embedding determinístico de dimensão fixa para um texto.
     */
    public static float[] gerarEmbedding(String texto, int dimensoes) {
        if (dimensoes <= 0) dimensoes = DIMENSAO_PADRAO;
        float[] vetor = new float[dimensoes];
        if (texto == null || texto.isBlank()) {
            return vetor;
        }

        String[] palavras = texto.trim().split("\\s+");
        if (palavras.length == 0) return vetor;

        for (String palavra : palavras) {
            String limpa = palavra.toLowerCase().replaceAll("[^a-zA-Z0-9áéíóúâêîôûãõçÁÉÍÓÚÂÊÎÔÛÃÕÇ]", "");
            if (limpa.isEmpty()) continue;

            byte[] bytes = limpa.getBytes(StandardCharsets.UTF_8);

            // Hash FNV-1a 64-bit
            long hash = 0xcbf29ce484222325L;
            for (byte b : bytes) {
                hash ^= (b & 0xff);
                hash *= 0x100000001b3L;
            }

            int idx = (int) (Math.abs(hash) % dimensoes);
            float sinal = ((hash >>> 32) & 1) == 0 ? 1.0f : -1.0f;
            vetor[idx] += sinal;

            // Sub-palavras (tri-gramas de caracteres para semântica em língua portuguesa)
            if (bytes.length >= 3) {
                for (int i = 0; i <= bytes.length - 3; i++) {
                    long subHash = 0xcbf29ce484222325L;
                    for (int j = 0; j < 3; j++) {
                        subHash ^= (bytes[i + j] & 0xff);
                        subHash *= 0x100000001b3L;
                    }
                    int subIdx = (int) (Math.abs(subHash) % dimensoes);
                    float subSinal = ((subHash >>> 32) & 1) == 0 ? 0.5f : -0.5f;
                    vetor[subIdx] += subSinal;
                }
            }
        }

        return ThzVetorSimd.normalizar(vetor);
    }

    /**
     * Calcula a similaridade semântica direta entre dois textos (0.0 a 1.0).
     */
    public static double similaridadeSemantica(String texto1, String texto2) {
        float[] emb1 = gerarEmbedding(texto1, DIMENSAO_PADRAO);
        float[] emb2 = gerarEmbedding(texto2, DIMENSAO_PADRAO);
        return Math.max(0.0, ThzVetorSimd.similaridadeCosseno(emb1, emb2));
    }
}
