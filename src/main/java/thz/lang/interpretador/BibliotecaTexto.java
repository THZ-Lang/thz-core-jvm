package thz.lang.interpretador;



import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Funções de manipulação de texto da stdlib THZ-LANG.
 * Domínio: TEXTO.*
 */
public final class BibliotecaTexto {

    private BibliotecaTexto() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        BibliotecaPadrao.registrarPublico(m, "TEXTO.comprimento", (args, ctx) -> {
            StdlibHelper.exigirAridade("TEXTO.comprimento", args, 1, ctx);
            StdlibHelper.exigirClasse("TEXTO.comprimento", args.get(0), "TEXTO", ctx);
            return ValorThz.INTEIRO(BigInteger.valueOf(((ValorThz.Texto) args.get(0)).valor().length()));
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.maiusculas", (args, ctx) -> {
            StdlibHelper.exigirAridade("TEXTO.maiusculas", args, 1, ctx);
            StdlibHelper.exigirClasse("TEXTO.maiusculas", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(((ValorThz.Texto) args.get(0)).valor().toUpperCase());
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.minusculas", (args, ctx) -> {
            StdlibHelper.exigirAridade("TEXTO.minusculas", args, 1, ctx);
            StdlibHelper.exigirClasse("TEXTO.minusculas", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(((ValorThz.Texto) args.get(0)).valor().toLowerCase());
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.aparar", (args, ctx) -> {
            StdlibHelper.exigirAridade("TEXTO.aparar", args, 1, ctx);
            StdlibHelper.exigirClasse("TEXTO.aparar", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(((ValorThz.Texto) args.get(0)).valor().trim());
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.contem", (args, ctx) -> {
            if (args.size() != 2)
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TEXTO.contem exige 2 args");
            StdlibHelper.exigirClasse("TEXTO.contem", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("TEXTO.contem", args.get(1), "TEXTO", ctx);
            return ValorThz.LOGICO(((ValorThz.Texto) args.get(0)).valor().contains(((ValorThz.Texto) args.get(1)).valor()));
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.subtexto", (args, ctx) -> {
            if (args.size() < 2 || args.size() > 3)
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TEXTO.subtexto exige 2 ou 3 args (texto, inicio, [fim])");
            StdlibHelper.exigirClasse("TEXTO.subtexto", args.get(0), "TEXTO", ctx);
            int ini = StdlibHelper.comoInteiroArg(args.get(1), ctx).intValue();
            Integer fim = args.size() == 3 ? StdlibHelper.comoInteiroArg(args.get(2), ctx).intValue() : null;
            String texto = ((ValorThz.Texto) args.get(0)).valor();
            return ValorThz.TEXTO(StdlibHelper.sliceTexto(texto, ini, fim));
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.substituir", (args, ctx) -> {
            StdlibHelper.exigirAridade("TEXTO.substituir", args, 3, ctx);
            StdlibHelper.exigirClasse("TEXTO.substituir", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("TEXTO.substituir", args.get(1), "TEXTO", ctx);
            StdlibHelper.exigirClasse("TEXTO.substituir", args.get(2), "TEXTO", ctx);
            String base = ((ValorThz.Texto) args.get(0)).valor();
            String alvo = ((ValorThz.Texto) args.get(1)).valor();
            String repl = ((ValorThz.Texto) args.get(2)).valor();
            if (alvo.isEmpty()) {
                List<String> chars = new ArrayList<>();
                for (char c : base.toCharArray()) chars.add(String.valueOf(c));
                return ValorThz.TEXTO(String.join(repl, chars));
            }
            return ValorThz.TEXTO(base.replace(alvo, repl));
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.dividir", (args, ctx) -> {
            StdlibHelper.exigirAridade("TEXTO.dividir", args, 2, ctx);
            StdlibHelper.exigirClasse("TEXTO.dividir", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("TEXTO.dividir", args.get(1), "TEXTO", ctx);
            String base = ((ValorThz.Texto) args.get(0)).valor();
            String sep = ((ValorThz.Texto) args.get(1)).valor();
            List<ValorThz> partes;
            if (sep.isEmpty()) {
                partes = new ArrayList<>();
                for (char c : base.toCharArray()) partes.add(ValorThz.TEXTO(String.valueOf(c)));
                if (base.isEmpty()) partes = List.of();
            } else {
                String[] arr = base.split(Pattern.quote(sep), -1);
                partes = new ArrayList<>();
                for (String p : arr) partes.add(ValorThz.TEXTO(p));
            }
            return new ValorThz.Fatia("TEXTO", List.copyOf(partes));
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.juntar", (args, ctx) -> {
            StdlibHelper.exigirAridade("TEXTO.juntar", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Fatia))
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TEXTO.juntar exige FATIA[TEXTO] como 1º arg");
            StdlibHelper.exigirClasse("TEXTO.juntar", args.get(1), "TEXTO", ctx);
            List<ValorThz> elems = ((ValorThz.Fatia) args.get(0)).elementos();
            String sep = ((ValorThz.Texto) args.get(1)).valor();
            List<String> strs = new ArrayList<>();
            for (ValorThz e : elems) {
                if (e instanceof ValorThz.Texto t) strs.add(t.valor());
                else strs.add("");
            }
            return ValorThz.TEXTO(String.join(sep, strs));
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.de", (args, ctx) -> {
            StdlibHelper.exigirAridade("TEXTO.de", args, 1, ctx);
            return ValorThz.TEXTO(args.get(0).formatar());
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.deDecimal", (args, ctx) -> {
            StdlibHelper.exigirAridade("TEXTO.deDecimal", args, 1, ctx);
            return ValorThz.TEXTO(args.get(0).formatar());
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.deInteiro", (args, ctx) -> {
            StdlibHelper.exigirAridade("TEXTO.deInteiro", args, 1, ctx);
            return ValorThz.TEXTO(args.get(0).formatar());
        });
        BibliotecaPadrao.registrarPublico(m, "TEXTO.deLogico", (args, ctx) -> {
            StdlibHelper.exigirAridade("TEXTO.deLogico", args, 1, ctx);
            return ValorThz.TEXTO(args.get(0).formatar());
        });
    }
}
