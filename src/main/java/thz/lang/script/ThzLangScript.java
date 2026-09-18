package thz.lang.script;

import thz.lang.driver.ThzCompilerDriver;

import java.util.Map;

/**
 * ThzLangScript — Executor direto de scripts THZ, one-liners e programas com shebang.
 */
public final class ThzLangScript {

    private ThzLangScript() {}

    public static ThzCompilerDriver.ResultadoCompilacao executar(String codigoFonte, String[] args) {
        String fonteLimpa = tratarShebang(codigoFonte);

        // Se for um trecho simples sem cabeçalho PROGRAMA/BIBLIOTECA, encapsula em um PROGRAMA temporário
        if (!fonteLimpa.contains("PROGRAMA") && !fonteLimpa.contains("BIBLIOTECA") && !fonteLimpa.contains("EXTENSAO") && !fonteLimpa.contains("TESTE")) {
            fonteLimpa = "PROGRAMA ScriptAutoExec\nPROCEDIMENTO Principal()\nINICIO\n" + fonteLimpa + "\nFIM\nFIM_PROGRAMA";
        }

        return ThzCompilerDriver.compilarOuExecutar(
                fonteLimpa,
                ThzCompilerDriver.Alvo.EXECUCAO_JVM,
                false,
                Map.of()
        );
    }

    private static String tratarShebang(String fonte) {
        if (fonte == null) return "";
        if (fonte.startsWith("#!")) {
            int quebra = fonte.indexOf('\n');
            if (quebra > 0) {
                return fonte.substring(quebra + 1);
            }
            return "";
        }
        return fonte;
    }
}
