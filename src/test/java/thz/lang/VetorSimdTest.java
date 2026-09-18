package thz.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.interpretador.InterpretadorThz;

import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;
import thz.lang.vetor.ThzVetorSimd;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VetorSimdTest {

    @Test
    @DisplayName("Deve calcular similaridade de cosseno exata para vetores idênticos, ortogonais e opostos")
    void testSimilaridadeCossenoMatematica() {
        float[] v1 = new float[]{1.0f, 0.0f, 0.0f};
        float[] v2 = new float[]{1.0f, 0.0f, 0.0f};
        float[] vOrtogonal = new float[]{0.0f, 1.0f, 0.0f};
        float[] vOposto = new float[]{-1.0f, 0.0f, 0.0f};

        assertEquals(1.0, ThzVetorSimd.similaridadeCosseno(v1, v2), 0.0001);
        assertEquals(0.0, ThzVetorSimd.similaridadeCosseno(v1, vOrtogonal), 0.0001);
        assertEquals(-1.0, ThzVetorSimd.similaridadeCosseno(v1, vOposto), 0.0001);
    }

    @Test
    @DisplayName("Deve calcular distância euclidiana e produto escalar corretamente")
    void testDistanciaEuclidianaEProdutoEscalar() {
        float[] a = new float[]{1.0f, 2.0f, 3.0f, 4.0f};
        float[] b = new float[]{1.0f, 2.0f, 3.0f, 4.0f};

        assertEquals(0.0, ThzVetorSimd.distanciaEuclidiana(a, b), 0.0001);
        assertEquals(30.0, ThzVetorSimd.produtoEscalar(a, b), 0.0001);
    }

    @Test
    @DisplayName("Deve buscar os Top-K vizinhos mais similares em ordem decrescente de pontuação")
    void testBuscaTopK() {
        float[] consulta = new float[]{1.0f, 0.0f, 0.0f};

        var base = List.of(
                new ThzVetorSimd.ItemVetorial("doc_exato", new float[]{1.0f, 0.0f, 0.0f}, "Exato"),
                new ThzVetorSimd.ItemVetorial("doc_medio", new float[]{0.7f, 0.7f, 0.0f}, "Médio"),
                new ThzVetorSimd.ItemVetorial("doc_longe", new float[]{0.0f, 1.0f, 0.0f}, "Longe")
        );

        List<ThzVetorSimd.ResultadoBusca> resultados = ThzVetorSimd.buscarTopK(
                consulta,
                base,
                2,
                ThzVetorSimd.Metrica.COSSENO
        );

        assertEquals(2, resultados.size());
        assertEquals("doc_exato", resultados.get(0).id());
        assertEquals("doc_medio", resultados.get(1).id());
        assertTrue(resultados.get(0).pontuacao() > resultados.get(1).pontuacao());
    }

    @Test
    @DisplayName("Deve executar programa THZ utilizando funções nativas de VETOR.*")
    void testExecucaoDslVetor() {
        String codigo = """
            PROGRAMA TesteVetorDsl
            REGRA_NEGOCIO R1
                OPERACAO TestarSimilaridade() : DECIMAL
                INICIO
                    VARIAVEL v1 : TEXTO <- "[1.0, 0.0, 0.0]"
                    VARIAVEL v2 : TEXTO <- "[1.0, 0.0, 0.0]"
                    VARIAVEL sim : DECIMAL <- VETOR.similaridadeCosseno(v1, v2)
                    RETORNE sim
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        var tokens = new ThzLexer(codigo).tokenize();
        var parser = new ThzParser(tokens);
        ProgramaAst ast = parser.parse();

        var interpretador = new InterpretadorThz(ast);
        var res = interpretador.executarOperacao("TestarSimilaridade", java.util.Map.of());
        assertEquals("1.000000", interpretador.formatar(res));
    }
}
