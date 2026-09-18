package thz.lang.interpretador;



import java.util.Map;

/**
 * Funções de utilidades brasileiras da stdlib THZ-LANG.
 * Domínio: BRASIL.*
 */
public final class BibliotecaBrasil {

    private BibliotecaBrasil() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        BibliotecaPadrao.registrarPublico(m, "BRASIL.cep", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.cep", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.formatarCep(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.consultarCep", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.consultarCep", args, 1, ctx);
            return thz.lang.brasil.ThzBrasilEngine.consultarCep(args.get(0).formatar());
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.cadastrarCep", (args, ctx, interp) -> {
            if (args.size() < 7) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BRASIL.cadastrarCep exige 7 argumentos: cep, logradouro, bairro, cidade, uf, ibge, ddd.");
            boolean ok = thz.lang.brasil.ThzInternalDatabase.cadastrarCep(
                    args.get(0).formatar(), args.get(1).formatar(), args.get(2).formatar(),
                    args.get(3).formatar(), args.get(4).formatar(), args.get(5).formatar(), args.get(6).formatar()
            );
            return ValorThz.LOGICO(ok);
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.validarUf", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.validarUf", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarUf(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.regiaoUf", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.regiaoUf", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.regiaoUf(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.formatarEndereco", (args, ctx, interp) -> {
            if (args.size() < 7) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BRASIL.formatarEndereco exige 7 argumentos: logradouro, numero, complemento, bairro, cidade, uf, cep.");
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.formatarEndereco(
                    args.get(0).formatar(), args.get(1).formatar(), args.get(2).formatar(),
                    args.get(3).formatar(), args.get(4).formatar(), args.get(5).formatar(), args.get(6).formatar()
            ));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.pixCopiaECola", (args, ctx, interp) -> {
            if (args.size() < 5) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BRASIL.pixCopiaECola exige chave, nome, cidade, valor, txId.");
            String chave = args.get(0).formatar();
            String nome = args.get(1).formatar();
            String cidade = args.get(2).formatar();
            java.math.BigDecimal valor = new java.math.BigDecimal(String.valueOf(StdlibHelper.extrairDoubleArg(args.get(3), ctx)));
            String txId = args.get(4).formatar();
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.gerarPixCopiaECola(chave, nome, cidade, valor, txId));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.validarChavePix", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BRASIL.validarChavePix exige a chave.");
            String tipo = args.size() > 1 ? args.get(1).formatar() : "AUTO";
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarChavePix(args.get(0).formatar(), tipo));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.validarLinhaDigitavel", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.validarLinhaDigitavel", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarLinhaDigitavel(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.linhaParaCodigoBarras", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.linhaParaCodigoBarras", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.linhaDigitavelParaCodigoBarras(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.valorBoleto", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.valorBoleto", args, 1, ctx);
            return ValorThz.DECIMAL(thz.lang.brasil.ThzBrasilEngine.extrairValorBoleto(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.formatarCpf", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.formatarCpf", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.formatarCpf(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.formatarCnpj", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.formatarCnpj", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.formatarCnpj(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.formatarTelefone", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.formatarTelefone", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.formatarTelefone(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.validarTituloEleitor", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.validarTituloEleitor", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarTituloEleitor(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.validarCnh", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.validarCnh", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarCnh(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.validarPis", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.validarPis", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarPis(args.get(0).formatar()));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.ehFeriadoNacional", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.ehFeriadoNacional", args, 1, ctx);
            java.time.LocalDate dt = StdlibHelper.extrairDataArg(args.get(0));
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.ehFeriadoNacional(dt));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.ehDiaUtil", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.ehDiaUtil", args, 1, ctx);
            java.time.LocalDate dt = StdlibHelper.extrairDataArg(args.get(0));
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.ehDiaUtil(dt));
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.proximoDiaUtil", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.proximoDiaUtil", args, 1, ctx);
            java.time.LocalDate dt = StdlibHelper.extrairDataArg(args.get(0));
            java.time.LocalDate prox = thz.lang.brasil.ThzBrasilEngine.proximoDiaUtil(dt);
            return ValorThz.TEXTO(prox.toString());
        });
        BibliotecaPadrao.registrarPublico(m, "BRASIL.valorPorExtenso", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("BRASIL.valorPorExtenso", args, 1, ctx);
            java.math.BigDecimal val = new java.math.BigDecimal(String.valueOf(StdlibHelper.extrairDoubleArg(args.get(0), ctx)));
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.valorPorExtenso(val));
        });
    }
}
