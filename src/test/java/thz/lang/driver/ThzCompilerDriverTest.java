package thz.lang.driver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.interpretador.ValorThz;
import thz.lang.script.ThzLangScript;
import thz.lang.tools.ThzLangTools;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ThzCompilerDriverTest {

    @Test
    @DisplayName("ThzCompilerDriver deve orquestrar compilação e execução para múltiplos alvos")
    void testDriverMultiplosAlvos() {
        String src = """
                PROGRAMA DemoDriver
                REGRA_NEGOCIO Calculo
                    OPERACAO Somar(a : INTEIRO32, b : INTEIRO32) : INTEIRO32
                    INICIO
                        RETORNE a + b
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;

        // 1. Alvo JVM Execution
        var resJvm = ThzCompilerDriver.compilarOuExecutar(src, ThzCompilerDriver.Alvo.EXECUCAO_JVM, false, Map.of(
                "a", ValorThz.INTEIRO(10),
                "b", ValorThz.INTEIRO(20)
        ));
        assertTrue(resJvm.sucesso());
        assertEquals("30", resJvm.resultadoExecucao().formatar());

        // 2. Alvo THZ_IR
        var resIr = ThzCompilerDriver.compilarOuExecutar(src, ThzCompilerDriver.Alvo.THZ_IR, false, null);
        assertTrue(resIr.sucesso());
        assertTrue(resIr.saidaTexto().contains("versaoIr"));

        // 3. Alvo LLVM
        var resLlvm = ThzCompilerDriver.compilarOuExecutar(src, ThzCompilerDriver.Alvo.LLVM, false, null);
        assertTrue(resLlvm.sucesso());
        assertTrue(resLlvm.saidaTexto().contains("ModuleID"));

        // 4. Alvo JAVASCRIPT
        var resJs = ThzCompilerDriver.compilarOuExecutar(src, ThzCompilerDriver.Alvo.JAVASCRIPT, false, null);
        assertTrue(resJs.sucesso());
        assertTrue(resJs.saidaTexto().contains("class Calculo"));
    }

    @Test
    @DisplayName("ThzLangScript deve executar one-liners e scripts com shebang")
    void testScriptRunner() {
        String script = """
                #!/usr/bin/env thz
                EXIBA "Olá do script!"
                """;

        var res = ThzLangScript.executar(script, new String[0]);
        assertTrue(res.sucesso(), "Execução de script deve suceder: " + res.erros());
    }

    @Test
    @DisplayName("ThzLangTools deve diagnosticar regras e dependências")
    void testTools() {
        String src = """
                PROGRAMA DemoTools
                IMPORTAR SharedLib DE "shared.thz"
                ESTRUTURA Cliente
                    id : TEXTO
                FIM_ESTRUTURA
                FIM_PROGRAMA
                """;

        var lints = ThzLangTools.executarLint(src);
        assertNotNull(lints);
        assertFalse(lints.isEmpty());
    }
}
