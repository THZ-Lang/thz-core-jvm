package thz.lang.diagnosticos;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import thz.lang.semantico.ErroSemantico;

/**
 * Helper centralizado para criação e conversão de diagnósticos.
 * Elimina a duplicação entre ThzCompilerFacade, Repl, CLI e LSP.
 */
public final class DiagnosticoHelper {

    private static final Pattern LINHA_COLUNA = Pattern.compile("\\[Linha (\\d+):(\\d+)\\]");

    private DiagnosticoHelper() {
    }

    public record Diagnostico(int linha, int coluna, String mensagem, String origem, String severidade) {
    }

    public static Diagnostico fromExcecao(Exception e, String origem) {
        LinhaColuna lc = extrairLinhaColuna(e.getMessage());
        return new Diagnostico(lc.linha(), lc.coluna(), e.getMessage(), origem, "erro");
    }

    public static Diagnostico fromErroSemantico(ErroSemantico e) {
        return new Diagnostico(e.linha(), e.coluna(), e.mensagem(), "semantico", "erro");
    }

    public static List<Diagnostico> fromErrosSemanticos(List<ErroSemantico> erros) {
        return erros.stream().map(DiagnosticoHelper::fromErroSemantico).toList();
    }

    public static DiagnosticoEntrada toEntrada(Diagnostico d) {
        return new DiagnosticoEntrada(d.linha(), d.coluna(), d.mensagem());
    }

    public static List<DiagnosticoEntrada> toEntradas(List<Diagnostico> diagnosticos) {
        return diagnosticos.stream().map(DiagnosticoHelper::toEntrada).toList();
    }

    public static List<String> formatar(String fonte, List<Diagnostico> diagnosticos) {
        return Diagnosticos.formatarDiagnosticos(fonte, toEntradas(diagnosticos), "");
    }

    public static LinhaColuna extrairLinhaColuna(String mensagem) {
        if (mensagem == null)
            return new LinhaColuna(1, 1);
        Matcher m = LINHA_COLUNA.matcher(mensagem);
        if (m.find()) {
            return new LinhaColuna(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
        }
        return new LinhaColuna(1, 1);
    }

    public record LinhaColuna(int linha, int coluna) {
    }
}
