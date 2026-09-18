package thz.lang.interpretador;

import thz.lang.ast.ExprAst;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Biblioteca padrão da THZ-LANG — fachada delegadora.
 * Cada domínio é registrado por sub-bibliotecas especializadas,
 * mantendo SRP e facilitando manutenção.
 */
public final class BibliotecaPadrao {

    @FunctionalInterface
    public interface FuncaoStdlib {
        ValorThz apply(List<ValorThz> args, ExprAst ctx, InterpretadorThz interp);
    }

    @FunctionalInterface
    public interface FuncaoSimplesStdlib {
        ValorThz apply(List<ValorThz> args, ExprAst ctx);
    }

    private static final Map<String, FuncaoStdlib> FUNCOES = criarStdlib();

    private BibliotecaPadrao() {}

    public static boolean ehStdlib(String nome) {
        return nome != null && FUNCOES.containsKey(nome);
    }

    public static ValorThz executar(String nome, List<ValorThz> args, ExprAst ctx) {
        return executar(nome, args, ctx, null);
    }

    public static ValorThz executar(String nome, List<ValorThz> args, ExprAst ctx, InterpretadorThz interp) {
        FuncaoStdlib fn = FUNCOES.get(nome);
        if (fn == null) {
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Função de biblioteca desconhecida: '" + nome + "'.");
        }
        return fn.apply(args, ctx, interp);
    }

    /**
     * Ponto de extensão da stdlib: módulos autônomos (thz-gui, thz-cli) registram aqui
     * suas funções nativas (ex.: TELA.*), mantendo o core livre de dependências superiores.
     */
    public static void registrar(String nome, FuncaoStdlib fn) {
        FUNCOES.put(nome, fn);
    }

    /** Usado pelas sub-bibliotecas para registrar funções no mapa compartilhado. */
    public static void registrarPublico(Map<String, FuncaoStdlib> m, String nome, FuncaoSimplesStdlib fn) {
        m.put(nome, (args, ctx, interp) -> fn.apply(args, ctx));
    }

    /** Usado pelas sub-bibliotecas para registrar funções que precisam do interpretador. */
    public static void registrarPublico(Map<String, FuncaoStdlib> m, String nome, FuncaoStdlib fn) {
        m.put(nome, fn);
    }

    private static Map<String, FuncaoStdlib> criarStdlib() {
        Map<String, FuncaoStdlib> m = new HashMap<>();

        BibliotecaTexto.registrar(m);
        BibliotecaColecao.registrar(m);
        BibliotecaMatematica.registrar(m);
        BibliotecaData.registrar(m);
        BibliotecaDocumento.registrar(m);
        BibliotecaArquivo.registrar(m);
        BibliotecaSeguranca.registrar(m);
        BibliotecaBanco.registrar(m);
        BibliotecaBrasil.registrar(m);
        BibliotecaAnalytics.registrar(m);
        BibliotecaInventario.registrar(m);
        BibliotecaMensageria.registrar(m);
        BibliotecaVetorIa.registrar(m);
        ThzWebViewStdlib.registrar(m);

        return new java.util.concurrent.ConcurrentHashMap<>(m);
    }
}
