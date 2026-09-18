package thz.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.dap.ThzDapServer;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ThzDapServerTest {

    @Test
    @DisplayName("Deve interceptar breakpoint, inspecionar variáveis locais e avançar com step-over")
    void testDepuracaoComBreakpointsEVariaveis() throws Exception {
        String codigo = """
            PROGRAMA TesteDebug
            REGRA_NEGOCIO RegraCalculo
                OPERACAO Calcular() : INTEIRO
                INICIO
                    VARIAVEL a : INTEIRO <- 10
                    VARIAVEL b : INTEIRO <- 20
                    VARIAVEL c : INTEIRO <- a + b
                    RETORNE c
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        try (ThzDapServer dap = new ThzDapServer()) {
            // Adiciona breakpoint na linha de atribuição de 'c' (linha 7)
            dap.adicionarBreakpoint(7);

            var futuro = dap.depurarProgramaAsync(codigo);

            // Aguarda atingir o breakpoint
            long deadline = System.currentTimeMillis() + 3000;
            while (!dap.isPausado() && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }

            assertTrue(dap.isPausado(), "Depurador deveria ter pausado no breakpoint");
            assertEquals(7, dap.getLinhaAtual(), "Linha atual deve ser 7");

            // Inspeciona variáveis no escopo pausado
            Map<String, String> variaveis = dap.inspecionarVariaveis();
            assertEquals("10", variaveis.get("a"));
            assertEquals("20", variaveis.get("b"));

            // Avança para o próximo passo (Step Over)
            dap.comandoStepOver();
            Thread.sleep(50);

            // Continua até a finalização
            dap.comandoContinuar();

            var resultado = futuro.get(3, TimeUnit.SECONDS);
            assertEquals("30", resultado.formatar());
        }
    }
}
