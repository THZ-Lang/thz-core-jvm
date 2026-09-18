package thz.lang.interpretador;



import java.util.Map;

/**
 * Funções de vetores SIMD, IA e ML da stdlib THZ-LANG.
 * Domínio: VETOR.*, IA.*, ML.*
 */
public final class BibliotecaVetorIa {

    private BibliotecaVetorIa() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        // ---- VETOR (Álgebra & Busca Semântica SIMD) ----
        BibliotecaPadrao.registrarPublico(m, "VETOR.criar", (args, ctx, interp) -> {
            if (args.isEmpty()) return ValorThz.TEXTO("[]");
            if (args.get(0) instanceof ValorThz.Fatia fatia) {
                float[] v = new float[fatia.elementos().size()];
                for (int i = 0; i < fatia.elementos().size(); i++) {
                    ValorThz elem = fatia.elementos().get(i);
                    if (elem instanceof ValorThz.Decimal d) v[i] = Float.parseFloat(d.valor().formatar());
                    else if (elem instanceof ValorThz.Inteiro in) v[i] = in.valor().floatValue();
                    else if (elem instanceof ValorThz.Texto t) v[i] = Float.parseFloat(t.valor());
                }
                return ValorThz.TEXTO(thz.lang.vetor.ThzVetorSimd.formatarVetor(v));
            }
            return ValorThz.TEXTO(args.get(0).formatar());
        });
        BibliotecaPadrao.registrarPublico(m, "VETOR.similaridadeCosseno", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("VETOR.similaridadeCosseno", args, 2, ctx);
            float[] a = StdlibHelper.extrairVetorArg(args.get(0), ctx);
            float[] b = StdlibHelper.extrairVetorArg(args.get(1), ctx);
            double sim = thz.lang.vetor.ThzVetorSimd.similaridadeCosseno(a, b);
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", sim), 6));
        });
        BibliotecaPadrao.registrarPublico(m, "VETOR.distanciaEuclidiana", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("VETOR.distanciaEuclidiana", args, 2, ctx);
            float[] a = StdlibHelper.extrairVetorArg(args.get(0), ctx);
            float[] b = StdlibHelper.extrairVetorArg(args.get(1), ctx);
            double dist = thz.lang.vetor.ThzVetorSimd.distanciaEuclidiana(a, b);
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", dist), 6));
        });
        BibliotecaPadrao.registrarPublico(m, "VETOR.produtoEscalar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("VETOR.produtoEscalar", args, 2, ctx);
            float[] a = StdlibHelper.extrairVetorArg(args.get(0), ctx);
            float[] b = StdlibHelper.extrairVetorArg(args.get(1), ctx);
            double dot = thz.lang.vetor.ThzVetorSimd.produtoEscalar(a, b);
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", dot), 6));
        });
        BibliotecaPadrao.registrarPublico(m, "VETOR.normalizar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("VETOR.normalizar", args, 1, ctx);
            float[] a = StdlibHelper.extrairVetorArg(args.get(0), ctx);
            float[] norm = thz.lang.vetor.ThzVetorSimd.normalizar(a);
            return ValorThz.TEXTO(thz.lang.vetor.ThzVetorSimd.formatarVetor(norm));
        });

        // ---- IA & ML ON-DEVICE (Zero Python) ----
        BibliotecaPadrao.registrarPublico(m, "IA.embedding", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] IA.embedding exige o texto como 1º argumento.");
            StdlibHelper.exigirClasse("IA.embedding", args.get(0), "TEXTO", ctx);
            String texto = ((ValorThz.Texto) args.get(0)).valor();
            int dim = args.size() > 1 && args.get(1) instanceof ValorThz.Inteiro in ? in.valor().intValue() : thz.lang.ia.ThzIaEngine.DIMENSAO_PADRAO;
            float[] emb = thz.lang.ia.ThzIaEngine.gerarEmbedding(texto, dim);
            return ValorThz.TEXTO(thz.lang.vetor.ThzVetorSimd.formatarVetor(emb));
        });
        BibliotecaPadrao.registrarPublico(m, "IA.similaridade", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("IA.similaridade", args, 2, ctx);
            StdlibHelper.exigirClasse("IA.similaridade", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("IA.similaridade", args.get(1), "TEXTO", ctx);
            double sim = thz.lang.ia.ThzIaEngine.similaridadeSemantica(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            );
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", sim), 6));
        });
        BibliotecaPadrao.registrarPublico(m, "ML.classificar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ML.classificar", args, 3, ctx);
            float[] features = StdlibHelper.extrairVetorArg(args.get(0), ctx);
            float[] pesos = StdlibHelper.extrairVetorArg(args.get(1), ctx);
            float bias = args.get(2) instanceof ValorThz.Decimal d ? Float.parseFloat(d.valor().formatar()) : ((ValorThz.Inteiro) args.get(2)).valor().floatValue();
            double prob = thz.lang.ia.ThzMlEngine.classificarProbabilidade(features, pesos, bias);
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", prob), 6));
        });
        BibliotecaPadrao.registrarPublico(m, "ML.predizer", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ML.predizer", args, 3, ctx);
            float[] features = StdlibHelper.extrairVetorArg(args.get(0), ctx);
            float[] coeficientes = StdlibHelper.extrairVetorArg(args.get(1), ctx);
            float intercepto = args.get(2) instanceof ValorThz.Decimal d ? Float.parseFloat(d.valor().formatar()) : ((ValorThz.Inteiro) args.get(2)).valor().floatValue();
            double pred = thz.lang.ia.ThzMlEngine.predizerRegressao(features, coeficientes, intercepto);
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", pred), 6));
        });
    }
}
