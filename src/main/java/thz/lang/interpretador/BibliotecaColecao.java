package thz.lang.interpretador;



import java.math.BigInteger;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Funções de coleções (FATIA) da stdlib THZ-LANG.
 * Domínio: FATIA.*
 */
public final class BibliotecaColecao {

    private BibliotecaColecao() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        BibliotecaPadrao.registrarPublico(m, "FATIA.adicionar", (args, ctx) -> {
            StdlibHelper.exigirAridade("FATIA.adicionar", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Fatia f)) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA.adicionar exige FATIA.");
            }
            var elementos = new ArrayList<>(f.elementos());
            elementos.add(args.get(1));
            return new ValorThz.Fatia(f.tipoInterno(), List.copyOf(elementos));
        });
        BibliotecaPadrao.registrarPublico(m, "FATIA.tamanho", (args, ctx) -> {
            StdlibHelper.exigirAridade("FATIA.tamanho", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Fatia f) {
                return ValorThz.INTEIRO(BigInteger.valueOf(f.elementos().size()));
            }
            if (args.get(0) instanceof ValorThz.Texto t) {
                return ValorThz.INTEIRO(BigInteger.valueOf(t.valor().length()));
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA.tamanho exige FATIA ou TEXTO.");
        });
        BibliotecaPadrao.registrarPublico(m, "FATIA.primeiro", (args, ctx) -> {
            StdlibHelper.exigirAridade("FATIA.primeiro", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Fatia f) {
                if (f.elementos().isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA vazia.");
                return f.elementos().get(0);
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA.primeiro exige FATIA.");
        });
        BibliotecaPadrao.registrarPublico(m, "FATIA.ultimo", (args, ctx) -> {
            StdlibHelper.exigirAridade("FATIA.ultimo", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Fatia f) {
                if (f.elementos().isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA vazia.");
                return f.elementos().get(f.elementos().size() - 1);
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA.ultimo exige FATIA.");
        });
        BibliotecaPadrao.registrarPublico(m, "FATIA.vazia", (args, ctx) -> {
            StdlibHelper.exigirAridade("FATIA.vazia", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Fatia f) {
                return ValorThz.LOGICO(f.elementos().isEmpty());
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA.vazia exige FATIA.");
        });
    }
}
