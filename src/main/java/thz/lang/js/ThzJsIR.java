package thz.lang.js;

import thz.lang.ir.GeradorIr;
import thz.lang.ir.IrPrograma;

/**
 * ThzJsIR — Emissor de JavaScript intermediário e serializador de representações executáveis.
 */
public final class ThzJsIR {

    private ThzJsIR() {}

    public static String gerarJsRuntime(IrPrograma ir) {
        StringBuilder sb = new StringBuilder();
        sb.append("// THZ-IR JS Runtime Container\n");
        sb.append("const THZ_IR_METADADOS = ").append(GeradorIr.serializarIrJson(ir)).append(";\n");
        return sb.toString();
    }
}
