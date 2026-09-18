package thz.lang.interpretador;



import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Funções de analytics (estatística, DAX, planilha, dados) da stdlib THZ-LANG.
 * Domínio: ESTATISTICA.*, DAX.*, PLANILHA.*, DADOS.*
 */
public final class BibliotecaAnalytics {

    private BibliotecaAnalytics() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        // ---- ESTATISTICA ----
        BibliotecaPadrao.registrarPublico(m, "ESTATISTICA.media", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ESTATISTICA.media", args, 1, ctx);
            List<Double> valores = StdlibHelper.extrairListaDoubles(args.get(0), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.media(valores));
        });
        BibliotecaPadrao.registrarPublico(m, "ESTATISTICA.mediana", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ESTATISTICA.mediana", args, 1, ctx);
            List<Double> valores = StdlibHelper.extrairListaDoubles(args.get(0), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.mediana(valores));
        });
        BibliotecaPadrao.registrarPublico(m, "ESTATISTICA.moda", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ESTATISTICA.moda", args, 1, ctx);
            List<Double> valores = StdlibHelper.extrairListaDoubles(args.get(0), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.moda(valores));
        });
        BibliotecaPadrao.registrarPublico(m, "ESTATISTICA.desvioPadrao", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] ESTATISTICA.desvioPadrao exige lista de valores.");
            List<Double> valores = StdlibHelper.extrairListaDoubles(args.get(0), ctx);
            boolean amostral = args.size() <= 1 || (args.get(1) instanceof ValorThz.Logico l && l.valor());
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.desvioPadrao(valores, amostral));
        });
        BibliotecaPadrao.registrarPublico(m, "ESTATISTICA.variancia", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] ESTATISTICA.variancia exige lista de valores.");
            List<Double> valores = StdlibHelper.extrairListaDoubles(args.get(0), ctx);
            boolean amostral = args.size() <= 1 || (args.get(1) instanceof ValorThz.Logico l && l.valor());
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.variancia(valores, amostral));
        });
        BibliotecaPadrao.registrarPublico(m, "ESTATISTICA.correlacao", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ESTATISTICA.correlacao", args, 2, ctx);
            List<Double> x = StdlibHelper.extrairListaDoubles(args.get(0), ctx);
            List<Double> y = StdlibHelper.extrairListaDoubles(args.get(1), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.correlacaoPearson(x, y));
        });
        BibliotecaPadrao.registrarPublico(m, "ESTATISTICA.percentil", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ESTATISTICA.percentil", args, 2, ctx);
            List<Double> valores = StdlibHelper.extrairListaDoubles(args.get(0), ctx);
            double p = args.get(1) instanceof ValorThz.Decimal d ? Double.parseDouble(d.valor().formatar()) : ((ValorThz.Inteiro) args.get(1)).valor().doubleValue();
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.percentil(valores, p));
        });
        BibliotecaPadrao.registrarPublico(m, "ESTATISTICA.zScore", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ESTATISTICA.zScore", args, 2, ctx);
            double val = args.get(0) instanceof ValorThz.Decimal d ? Double.parseDouble(d.valor().formatar()) : ((ValorThz.Inteiro) args.get(0)).valor().doubleValue();
            List<Double> amostra = StdlibHelper.extrairListaDoubles(args.get(1), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.zScore(val, amostra));
        });
        BibliotecaPadrao.registrarPublico(m, "ESTATISTICA.outliers", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ESTATISTICA.outliers", args, 1, ctx);
            List<Double> valores = StdlibHelper.extrairListaDoubles(args.get(0), ctx);
            var outliers = thz.lang.analytics.ThzEstatistica.detectarOutliers(valores);
            return new ValorThz.Fatia("DECIMAL", outliers);
        });
        BibliotecaPadrao.registrarPublico(m, "ESTATISTICA.regressao", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ESTATISTICA.regressao", args, 2, ctx);
            List<Double> x = StdlibHelper.extrairListaDoubles(args.get(0), ctx);
            List<Double> y = StdlibHelper.extrairListaDoubles(args.get(1), ctx);
            var reg = thz.lang.analytics.ThzEstatistica.regressaoLinear(x, y);
            Map<String, ValorThz> campos = new LinkedHashMap<>();
            campos.put("inclinacao", ValorThz.DECIMAL(reg.inclinacao()));
            campos.put("intercepto", ValorThz.DECIMAL(reg.intercepto()));
            campos.put("rQuadrado", ValorThz.DECIMAL(reg.rQuadrado()));
            return new ValorThz.Registro("RegressaoResultado", campos);
        });

        // ---- DAX / BI / METRICAS ANALITICAS ----
        BibliotecaPadrao.registrarPublico(m, "DAX.acumuladoAno", (args, ctx, interp) -> {
            if (args.size() < 4) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DAX.acumuladoAno exige tabela, campoData, campoValor e anoAlvo.");
            List<ValorThz.Registro> linhas = StdlibHelper.extrairListaRegistros(args.get(0), ctx);
            String campoData = ((ValorThz.Texto) args.get(1)).valor();
            String campoValor = ((ValorThz.Texto) args.get(2)).valor();
            int anoAlvo = ((ValorThz.Inteiro) args.get(3)).valor().intValue();
            return ValorThz.DECIMAL(thz.lang.analytics.ThzDaxEngine.totalYtd(linhas, campoData, campoValor, anoAlvo));
        });
        BibliotecaPadrao.registrarPublico(m, "DAX.variacaoPeriodo", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DAX.variacaoPeriodo", args, 2, ctx);
            double vAtual = StdlibHelper.extrairDoubleArg(args.get(0), ctx);
            double vAnt = StdlibHelper.extrairDoubleArg(args.get(1), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzDaxEngine.variacaoPeriodo(vAtual, vAnt));
        });
        BibliotecaPadrao.registrarPublico(m, "DAX.contagemDistintos", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DAX.contagemDistintos", args, 2, ctx);
            List<ValorThz.Registro> linhas = StdlibHelper.extrairListaRegistros(args.get(0), ctx);
            String campo = ((ValorThz.Texto) args.get(1)).valor();
            return ValorThz.INTEIRO(thz.lang.analytics.ThzDaxEngine.contagemDistintos(linhas, campo));
        });
        BibliotecaPadrao.registrarPublico(m, "DAX.ranking", (args, ctx, interp) -> {
            if (args.size() < 2) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DAX.ranking exige tabela e campoValor.");
            List<ValorThz.Registro> linhas = StdlibHelper.extrairListaRegistros(args.get(0), ctx);
            String campoValor = ((ValorThz.Texto) args.get(1)).valor();
            boolean desc = args.size() <= 2 || (args.get(2) instanceof ValorThz.Logico l && l.valor());
            var res = thz.lang.analytics.ThzDaxEngine.calcularRanking(linhas, campoValor, desc);
            List<ValorThz> lista = new ArrayList<>(res);
            return ValorThz.FATIA(lista);
        });
        BibliotecaPadrao.registrarPublico(m, "DAX.percentualTotal", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DAX.percentualTotal", args, 2, ctx);
            List<ValorThz.Registro> linhas = StdlibHelper.extrairListaRegistros(args.get(0), ctx);
            String campoValor = ((ValorThz.Texto) args.get(1)).valor();
            var res = thz.lang.analytics.ThzDaxEngine.percentualSobreTotal(linhas, campoValor);
            List<ValorThz> lista = new ArrayList<>(res);
            return ValorThz.FATIA(lista);
        });
        BibliotecaPadrao.registrarPublico(m, "DAX.kpi", (args, ctx, interp) -> {
            if (args.size() < 3) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DAX.kpi exige nome, valorRealizado e valorMeta.");
            String nome = ((ValorThz.Texto) args.get(0)).valor();
            double vReal = StdlibHelper.extrairDoubleArg(args.get(1), ctx);
            double vMeta = StdlibHelper.extrairDoubleArg(args.get(2), ctx);
            double tol = args.size() > 3 ? StdlibHelper.extrairDoubleArg(args.get(3), ctx) : 5.0;
            return thz.lang.analytics.ThzDaxEngine.avaliarKpi(nome, vReal, vMeta, tol);
        });

        // ---- PLANILHA / CSV / TABELAS ----
        BibliotecaPadrao.registrarPublico(m, "PLANILHA.lerCsv", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] PLANILHA.lerCsv exige caminho do arquivo.");
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            String sep = args.size() > 1 && args.get(1) instanceof ValorThz.Texto t ? t.valor() : "auto";
            try {
                var linhas = thz.lang.analytics.ThzPlanilhaCsv.lerCsv(java.nio.file.Path.of(caminho), sep);
                List<ValorThz> lista = new ArrayList<>(linhas);
                return ValorThz.FATIA(lista);
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao ler CSV: " + e.getMessage());
            }
        });
        BibliotecaPadrao.registrarPublico(m, "PLANILHA.escreverCsv", (args, ctx, interp) -> {
            if (args.size() < 2) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] PLANILHA.escreverCsv exige destino e tabela.");
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            List<ValorThz.Registro> linhas = StdlibHelper.extrairListaRegistros(args.get(1), ctx);
            String sep = args.size() > 2 && args.get(2) instanceof ValorThz.Texto t ? t.valor() : ";";
            try {
                boolean ok = thz.lang.analytics.ThzPlanilhaCsv.escreverCsv(java.nio.file.Path.of(caminho), linhas, sep);
                return ValorThz.LOGICO(ok);
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao escrever CSV: " + e.getMessage());
            }
        });
        BibliotecaPadrao.registrarPublico(m, "PLANILHA.procv", (args, ctx, interp) -> {
            if (args.size() < 4) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] PLANILHA.procv exige tabela, campoBusca, valorBusca e campoRetorno.");
            List<ValorThz.Registro> linhas = StdlibHelper.extrairListaRegistros(args.get(0), ctx);
            String campoBusca = ((ValorThz.Texto) args.get(1)).valor();
            String valorBusca = args.get(2).formatar();
            String campoRetorno = ((ValorThz.Texto) args.get(3)).valor();
            return thz.lang.analytics.ThzPlanilhaCsv.procv(linhas, campoBusca, valorBusca, campoRetorno);
        });
        BibliotecaPadrao.registrarPublico(m, "PLANILHA.pivotar", (args, ctx, interp) -> {
            if (args.size() < 4) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] PLANILHA.pivotar exige tabela, campoLinha, campoColuna e campoValor.");
            List<ValorThz.Registro> linhas = StdlibHelper.extrairListaRegistros(args.get(0), ctx);
            String campoLinha = ((ValorThz.Texto) args.get(1)).valor();
            String campoColuna = ((ValorThz.Texto) args.get(2)).valor();
            String campoValor = ((ValorThz.Texto) args.get(3)).valor();
            String op = args.size() > 4 && args.get(4) instanceof ValorThz.Texto t ? t.valor() : "SUM";
            var pivot = thz.lang.analytics.ThzPlanilhaCsv.pivotar(linhas, campoLinha, campoColuna, campoValor, op);
            List<ValorThz> lista = new ArrayList<>(pivot);
            return ValorThz.FATIA(lista);
        });

        // ---- DADOS & DATA QUALITY ----
        BibliotecaPadrao.registrarPublico(m, "DADOS.sanitizar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DADOS.sanitizar", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.analytics.ThzDataQuality.sanitizarTexto(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "DADOS.decimalPtBr", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DADOS.decimalPtBr", args, 1, ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzDataQuality.parsearDecimalPtBr(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "DADOS.dataPtBr", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DADOS.dataPtBr", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.analytics.ThzDataQuality.parsearDataPtBr(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "DADOS.validarCpf", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DADOS.validarCpf", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.analytics.ThzDataQuality.validarCpf(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "DADOS.validarCnpj", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DADOS.validarCnpj", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.analytics.ThzDataQuality.validarCnpj(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "DADOS.mascarar", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DADOS.mascarar exige texto.");
            String texto = args.get(0).formatar();
            int inicio = args.size() > 1 && args.get(1) instanceof ValorThz.Inteiro in ? in.valor().intValue() : 3;
            int fim = args.size() > 2 && args.get(2) instanceof ValorThz.Inteiro in ? in.valor().intValue() : 2;
            return ValorThz.TEXTO(thz.lang.analytics.ThzDataQuality.mascararDadoSensivel(texto, inicio, fim));
        });
        BibliotecaPadrao.registrarPublico(m, "DADOS.removerDuplicatas", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DADOS.removerDuplicatas exige tabela.");
            List<ValorThz.Registro> linhas = StdlibHelper.extrairListaRegistros(args.get(0), ctx);
            List<String> chaves = List.of();
            if (args.size() > 1 && args.get(1) instanceof ValorThz.Fatia f) {
                chaves = f.elementos().stream().map(ValorThz::formatar).toList();
            }
            var dedup = thz.lang.analytics.ThzDataQuality.removerDuplicatas(linhas, chaves);
            List<ValorThz> lista = new ArrayList<>(dedup);
            return ValorThz.FATIA(lista);
        });
        BibliotecaPadrao.registrarPublico(m, "DADOS.imputarNulos", (args, ctx, interp) -> {
            if (args.size() < 3) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DADOS.imputarNulos exige tabela, campo e valorPadrao.");
            List<ValorThz.Registro> linhas = StdlibHelper.extrairListaRegistros(args.get(0), ctx);
            String campo = ((ValorThz.Texto) args.get(1)).valor();
            ValorThz padrao = args.get(2);
            var imp = thz.lang.analytics.ThzDataQuality.imputarNulos(linhas, campo, padrao);
            List<ValorThz> lista = new ArrayList<>(imp);
            return ValorThz.FATIA(lista);
        });
    }
}
