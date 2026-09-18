package thz.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.ia.ThzIaEngine;
import thz.lang.ia.ThzMlEngine;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import static org.junit.jupiter.api.Assertions.*;

class ThzIaMlTest {

    @Test
    @DisplayName("Deve gerar embeddings semânticos determinísticos e calcular similaridade semântica")
    void testEmbeddingsESimilaridade() {
        String texto1 = "Contrato de prestação de serviços fiscais";
        String texto2 = "Acordo de assessoria tributária e serviços fiscais";
        String textoDivergente = "Receita de bolo de chocolate com morango";

        float[] emb1 = ThzIaEngine.gerarEmbedding(texto1, 128);
        float[] emb2 = ThzIaEngine.gerarEmbedding(texto2, 128);
        assertEquals(128, emb1.length);
        assertEquals(128, emb2.length);

        double simAlta = ThzIaEngine.similaridadeSemantica(texto1, texto2);
        double simBaixa = ThzIaEngine.similaridadeSemantica(texto1, textoDivergente);

        assertTrue(simAlta > simBaixa, "Similaridade entre textos correlatos deve ser superior: " + simAlta + " > " + simBaixa);
        assertTrue(simAlta > 0.40, "Similaridade esperada acima de 0.40");
    }

    @Test
    @DisplayName("Deve calcular classificação probabilística sigmoide e predição linear")
    void testMlClassificacaoERegressao() {
        // Classificação logística
        float[] features = new float[]{1.0f, 2.0f, 3.0f};
        float[] pesos = new float[]{0.5f, -0.2f, 0.8f};
        float bias = -1.0f;

        double prob = ThzMlEngine.classificarProbabilidade(features, pesos, bias);
        assertTrue(prob >= 0.0 && prob <= 1.0);

        // Regressão linear: 1*2 + 2*3 + 3*4 + 10 = 2 + 6 + 12 + 10 = 30
        float[] coef = new float[]{2.0f, 3.0f, 4.0f};
        double pred = ThzMlEngine.predizerRegressao(features, coef, 10.0f);
        assertEquals(30.0, pred, 0.0001);
    }

    @Test
    @DisplayName("Deve executar programa THZ com módulo de IA e ML")
    void testExecucaoProgramaIaMl() {
        String codigo = """
            PROGRAMA TesteIaDsl
            REGRA_NEGOCIO R1
                OPERACAO TestarIa() : LOGICO
                INICIO
                    VARIAVEL t1 : TEXTO <- "Faturamento de Notas Fiscais"
                    VARIAVEL t2 : TEXTO <- "Emissao de Fatura e Nota Fiscal"
                    VARIAVEL sim : DECIMAL <- IA.similaridade(t1, t2)
                    
                    VARIAVEL feat : TEXTO <- "[1.0, 2.0, 3.0]"
                    VARIAVEL coef : TEXTO <- "[2.0, 3.0, 4.0]"
                    VARIAVEL pred : DECIMAL <- ML.predizer(feat, coef, 10.0)
                    
                    SE pred = 30.000000 E sim > 0.300000
                        RETORNE VERDADEIRO
                    SENAO
                        RETORNE FALSO
                    FIM_SE
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        var tokens = new ThzLexer(codigo).tokenize();
        var parser = new ThzParser(tokens);
        ProgramaAst ast = parser.parse();

        var interp = new InterpretadorThz(ast);
        var res = interp.executarOperacao("TestarIa", java.util.Map.of());
        assertEquals("VERDADEIRO", interp.formatar(res));
    }
}
