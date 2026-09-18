package thz.lang.interpretador;



import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Funções de banco de dados da stdlib THZ-LANG.
 * Domínio: BANCO.*
 */
public final class BibliotecaBanco {

    private BibliotecaBanco() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        BibliotecaPadrao.registrarPublico(m, "BANCO.conectar", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.conectar exige pelo menos a URL de conexão.");
            }
            String url = ((ValorThz.Texto) args.get(0)).valor();
            if (args.size() == 1) {
                thz.lang.db.ThzDb.conectar(url);
            } else if (args.size() == 3) {
                thz.lang.db.ThzDb.conectar("padrao", url, ((ValorThz.Texto) args.get(1)).valor(), ((ValorThz.Texto) args.get(2)).valor());
            } else if (args.size() >= 4) {
                thz.lang.db.ThzDb.conectar(((ValorThz.Texto) args.get(0)).valor(), ((ValorThz.Texto) args.get(1)).valor(), ((ValorThz.Texto) args.get(2)).valor(), ((ValorThz.Texto) args.get(3)).valor());
            }
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.executar", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.executar exige SQL.");
            }
            String sql = ((ValorThz.Texto) args.get(0)).valor();
            List<ValorThz> params = args.size() > 1 && args.get(1) instanceof ValorThz.Fatia f ? f.elementos() : java.util.Collections.emptyList();
            long afetadas = thz.lang.db.ThzDb.executar(sql, params);
            return ValorThz.INTEIRO(afetadas);
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.executarEm", (args, ctx, interp) -> {
            if (args.size() < 2) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.executarEm exige conexaoNome e SQL.");
            }
            String nomeConn = ((ValorThz.Texto) args.get(0)).valor();
            String sql = ((ValorThz.Texto) args.get(1)).valor();
            List<ValorThz> params = args.size() > 2 && args.get(2) instanceof ValorThz.Fatia f ? f.elementos() : java.util.Collections.emptyList();
            long afetadas = thz.lang.db.ThzDb.executarEm(nomeConn, sql, params);
            return ValorThz.INTEIRO(afetadas);
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.consultar", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.consultar exige SQL.");
            }
            String sql = ((ValorThz.Texto) args.get(0)).valor();
            List<ValorThz> params = args.size() > 1 && args.get(1) instanceof ValorThz.Fatia f ? f.elementos() : java.util.Collections.emptyList();
            var linhas = thz.lang.db.ThzDb.consultar(sql, params);
            List<ValorThz> lista = new ArrayList<>(linhas);
            return ValorThz.FATIA(lista);
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.consultarEm", (args, ctx, interp) -> {
            if (args.size() < 2) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.consultarEm exige conexaoNome e SQL.");
            }
            String nomeConn = ((ValorThz.Texto) args.get(0)).valor();
            String sql = ((ValorThz.Texto) args.get(1)).valor();
            List<ValorThz> params = args.size() > 2 && args.get(2) instanceof ValorThz.Fatia f ? f.elementos() : java.util.Collections.emptyList();
            var linhas = thz.lang.db.ThzDb.consultarEm(nomeConn, sql, params);
            List<ValorThz> lista = new ArrayList<>(linhas);
            return ValorThz.FATIA(lista);
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.consultarValor", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.consultarValor exige SQL.");
            }
            String sql = ((ValorThz.Texto) args.get(0)).valor();
            List<ValorThz> params = args.size() > 1 && args.get(1) instanceof ValorThz.Fatia f ? f.elementos() : java.util.Collections.emptyList();
            return thz.lang.db.ThzDb.consultarValor(sql, params);
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.iniciarTransacao", (args, ctx, interp) -> {
            if (args.isEmpty()) thz.lang.db.ThzDb.iniciarTransacao();
            else thz.lang.db.ThzDb.iniciarTransacaoEm(((ValorThz.Texto) args.get(0)).valor());
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.confirmarTransacao", (args, ctx, interp) -> {
            if (args.isEmpty()) thz.lang.db.ThzDb.confirmarTransacao();
            else thz.lang.db.ThzDb.confirmarTransacaoEm(((ValorThz.Texto) args.get(0)).valor());
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.cancelarTransacao", (args, ctx, interp) -> {
            if (args.isEmpty()) thz.lang.db.ThzDb.cancelarTransacao();
            else thz.lang.db.ThzDb.cancelarTransacaoEm(((ValorThz.Texto) args.get(0)).valor());
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.executarScript", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BANCO.executarScript", args, 1, ctx);
            StdlibHelper.exigirClasse("BANCO.executarScript", args.get(0), "TEXTO", ctx);
            thz.lang.db.ThzDb.executarScript(((ValorThz.Texto) args.get(0)).valor());
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.driverAtivo", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.db.ThzDatabaseBridge.driverAtivo());
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.salvar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BANCO.salvar", args, 2, ctx);
            StdlibHelper.exigirClasse("BANCO.salvar", args.get(0), "TEXTO", ctx);
            String tabela = ((ValorThz.Texto) args.get(0)).valor();
            return thz.lang.db.ThzDatabaseBridge.salvar(tabela, args.get(1));
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.buscarPorId", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BANCO.buscarPorId", args, 2, ctx);
            StdlibHelper.exigirClasse("BANCO.buscarPorId", args.get(0), "TEXTO", ctx);
            String tabela = ((ValorThz.Texto) args.get(0)).valor();
            return thz.lang.db.ThzDatabaseBridge.buscarPorId(tabela, args.get(1));
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.removerPorId", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BANCO.removerPorId", args, 2, ctx);
            StdlibHelper.exigirClasse("BANCO.removerPorId", args.get(0), "TEXTO", ctx);
            String tabela = ((ValorThz.Texto) args.get(0)).valor();
            return ValorThz.LOGICO(thz.lang.db.ThzDatabaseBridge.removerPorId(tabela, args.get(1)));
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.criarTabela", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BANCO.criarTabela", args, 2, ctx);
            StdlibHelper.exigirClasse("BANCO.criarTabela", args.get(0), "TEXTO", ctx);
            String tabela = ((ValorThz.Texto) args.get(0)).valor();
            Map<String, String> colunas = new LinkedHashMap<>();
            if (args.get(1) instanceof ValorThz.Registro reg) {
                reg.campos().forEach((k, v) -> colunas.put(k, v.formatar()));
            } else if (args.get(1) instanceof ValorThz.Texto t) {
                for (String par : t.valor().split(",")) {
                    String[] kv = par.trim().split(":");
                    if (kv.length == 2) colunas.put(kv[0].trim(), kv[1].trim());
                }
            }
            return ValorThz.LOGICO(thz.lang.db.ThzDatabaseBridge.criarTabela(tabela, colunas));
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.consultarVetorial", (args, ctx, interp) -> {
            if (args.size() < 3) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.consultarVetorial exige tabela, colunaVetor e vetorBusca.");
            StdlibHelper.exigirClasse("BANCO.consultarVetorial", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("BANCO.consultarVetorial", args.get(1), "TEXTO", ctx);
            String tabela = ((ValorThz.Texto) args.get(0)).valor();
            String colunaVetor = ((ValorThz.Texto) args.get(1)).valor();
            float[] vetorBusca = StdlibHelper.extrairVetorArg(args.get(2), ctx);
            int limite = args.size() > 3 && args.get(3) instanceof ValorThz.Inteiro in ? in.valor().intValue() : 5;
            var registros = thz.lang.db.ThzDatabaseBridge.consultarVetorial(tabela, colunaVetor, vetorBusca, limite);
            List<ValorThz> lista = new ArrayList<>(registros);
            return ValorThz.FATIA(lista);
        });
        BibliotecaPadrao.registrarPublico(m, "BANCO.fechar", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                thz.lang.db.ThzDb.fechar();
            } else {
                thz.lang.db.ThzDb.fechar(((ValorThz.Texto) args.get(0)).valor());
            }
            return ValorThz.LOGICO(true);
        });
    }
}
