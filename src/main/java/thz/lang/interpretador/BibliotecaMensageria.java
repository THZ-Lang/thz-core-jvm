package thz.lang.interpretador;



import java.math.BigInteger;
import java.util.Map;

/**
 * Funções de mensageria da stdlib THZ-LANG.
 * Domínio: MENSAGERIA.*
 */
public final class BibliotecaMensageria {

    private BibliotecaMensageria() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        BibliotecaPadrao.registrarPublico(m, "MENSAGERIA.publicar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("MENSAGERIA.publicar", args, 2, ctx);
            StdlibHelper.exigirClasse("MENSAGERIA.publicar", args.get(0), "TEXTO", ctx);
            String topico = ((ValorThz.Texto) args.get(0)).valor();
            ValorThz msg = args.get(1);
            long offset = thz.lang.mensageria.ThzMessagingBridge.publicar(topico, msg);
            return ValorThz.INTEIRO(BigInteger.valueOf(offset));
        });
        BibliotecaPadrao.registrarPublico(m, "MENSAGERIA.consumir", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MENSAGERIA.consumir exige o tópico.");
            StdlibHelper.exigirClasse("MENSAGERIA.consumir", args.get(0), "TEXTO", ctx);
            String topico = ((ValorThz.Texto) args.get(0)).valor();
            long timeout = args.size() > 1 && args.get(1) instanceof ValorThz.Inteiro in ? in.valor().longValue() : 500L;
            var evento = thz.lang.mensageria.ThzMessagingBridge.consumir(topico, timeout);
            return evento != null ? evento.payload() : ValorThz.NULO;
        });
        BibliotecaPadrao.registrarPublico(m, "MENSAGERIA.tamanhoFila", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("MENSAGERIA.tamanhoFila", args, 1, ctx);
            StdlibHelper.exigirClasse("MENSAGERIA.tamanhoFila", args.get(0), "TEXTO", ctx);
            String topico = ((ValorThz.Texto) args.get(0)).valor();
            int sz = thz.lang.mensageria.ThzMessagingBridge.tamanhoFila(topico);
            return ValorThz.INTEIRO(BigInteger.valueOf(sz));
        });
        BibliotecaPadrao.registrarPublico(m, "MENSAGERIA.limparTopico", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("MENSAGERIA.limparTopico", args, 1, ctx);
            StdlibHelper.exigirClasse("MENSAGERIA.limparTopico", args.get(0), "TEXTO", ctx);
            String topico = ((ValorThz.Texto) args.get(0)).valor();
            thz.lang.mensageria.ThzMessagingBridge.limparTopico(topico);
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "MENSAGERIA.driverAtivo", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.mensageria.ThzMessagingBridge.driverAtivo());
        });
        BibliotecaPadrao.registrarPublico(m, "MENSAGERIA.statusConexao", (args, ctx, interp) -> {
            return ValorThz.LOGICO(thz.lang.mensageria.ThzMessagingBridge.statusConexao());
        });
        BibliotecaPadrao.registrarPublico(m, "MENSAGERIA.urlAtiva", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.mensageria.ThzMessagingBridge.urlAtiva());
        });
        BibliotecaPadrao.registrarPublico(m, "MENSAGERIA.conectar", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MENSAGERIA.conectar exige o driver.");
            StdlibHelper.exigirClasse("MENSAGERIA.conectar", args.get(0), "TEXTO", ctx);
            String driver = ((ValorThz.Texto) args.get(0)).valor();
            String url = args.size() > 1 && args.get(1) instanceof ValorThz.Texto t ? t.valor() : "auto";
            thz.lang.mensageria.ThzMessagingBridge.conectar(driver, url);
            return ValorThz.LOGICO(true);
        });
    }
}
