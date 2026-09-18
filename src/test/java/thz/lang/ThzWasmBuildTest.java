package thz.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.driver.ThzCompilerDriver;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThzWasmBuildTest {

    @Test
    @DisplayName("Deve rejeitar compilação direta para WebAssembly no frontend JVM com diagnóstico explícito")
    void testRejeicaoExplicitaBackendWasmNoFrontendJvm() {
        String codigo = """
            PROGRAMA ValidadorFiscalWasm
            ESTRUTURA NotaFiscal
                numero: INTEIRO64
                valor: DECIMAL(18, 2)
            FIM_ESTRUTURA

            REGRA_NEGOCIO ValidarNota
                OPERACAO Validar(nf: NotaFiscal) : LOGICO
                INICIO
                    SE nf.valor > 0.00
                        RETORNE VERDADEIRO
                    SENAO
                        RETORNE FALSO
                    FIM_SE
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        var res = ThzCompilerDriver.compilarOuExecutar(codigo, ThzCompilerDriver.Alvo.WEBASSEMBLY, false, Map.of());
        assertFalse(res.sucesso(), "Frontend JVM deve rejeitar compilação direta para .wasm binário");
        assertFalse(res.erros().isEmpty());
        assertTrue(res.erros().get(0).mensagem().contains("[Backend WebAssembly]"));
        assertTrue(res.erros().get(0).mensagem().contains("não gera bytecode binário WebAssembly"));
    }

    @Test
    @DisplayName("Deve compilar com sucesso para JavaScript quando requisitado")
    void testEmissaoJavascriptEquivalente() {
        String codigo = """
            PROGRAMA ValidadorFiscalJs
            ESTRUTURA NotaFiscal
                numero: INTEIRO64
                valor: DECIMAL(18, 2)
            FIM_ESTRUTURA
            FIM_PROGRAMA
            """;

        var res = ThzCompilerDriver.compilarOuExecutar(codigo, ThzCompilerDriver.Alvo.JAVASCRIPT, false, Map.of());
        assertTrue(res.sucesso());
        assertNotNull(res.saidaTexto());
        assertTrue(res.saidaTexto().contains("class NotaFiscal"));
    }
}
