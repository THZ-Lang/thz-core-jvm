package thz.lang.interpretador;


import thz.lang.runtime.DataHoraThz;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.ErroData;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Funções de manipulação de data/hora da stdlib THZ-LANG.
 * Domínio: DATA.*
 */
public final class BibliotecaData {

    private BibliotecaData() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        BibliotecaPadrao.registrarPublico(m, "DATA.hoje", (args, ctx) -> {
            StdlibHelper.exigirAridade("DATA.hoje", args, 0, ctx);
            LocalDate agora = LocalDate.now();
            return ValorThz.DATA(DataThz.deComponentes(agora.getYear(), agora.getMonthValue(), agora.getDayOfMonth()));
        });
        BibliotecaPadrao.registrarPublico(m, "DATA.agora", (args, ctx) -> {
            StdlibHelper.exigirAridade("DATA.agora", args, 0, ctx);
            LocalDateTime agora = LocalDateTime.now();
            return ValorThz.DATA_HORA(DataHoraThz.deComponentes(agora.getYear(), agora.getMonthValue(), agora.getDayOfMonth(), agora.getHour(), agora.getMinute(), agora.getSecond()));
        });
        BibliotecaPadrao.registrarPublico(m, "DATA.criar", (args, ctx) -> {
            StdlibHelper.exigirAridade("DATA.criar", args, 3, ctx);
            int a = StdlibHelper.comoInteiroArg(args.get(0), ctx).intValue();
            int mes = StdlibHelper.comoInteiroArg(args.get(1), ctx).intValue();
            int d = StdlibHelper.comoInteiroArg(args.get(2), ctx).intValue();
            try { return ValorThz.DATA(DataThz.deComponentes(a, mes, d)); }
            catch (ErroData e) { throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] " + e.getMessage()); }
        });
        BibliotecaPadrao.registrarPublico(m, "DATA.criarDataHora", (args, ctx) -> {
            if (args.size() < 5 || args.size() > 6) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.criarDataHora exige 5 ou 6 args");
            int a = StdlibHelper.comoInteiroArg(args.get(0), ctx).intValue();
            int mes = StdlibHelper.comoInteiroArg(args.get(1), ctx).intValue();
            int dia = StdlibHelper.comoInteiroArg(args.get(2), ctx).intValue();
            int h = StdlibHelper.comoInteiroArg(args.get(3), ctx).intValue();
            int mi = StdlibHelper.comoInteiroArg(args.get(4), ctx).intValue();
            int s = args.size() == 6 ? StdlibHelper.comoInteiroArg(args.get(5), ctx).intValue() : 0;
            try { return ValorThz.DATA_HORA(DataHoraThz.deComponentes(a, mes, dia, h, mi, s)); }
            catch (ErroData e) { throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] " + e.getMessage()); }
        });
        BibliotecaPadrao.registrarPublico(m, "DATA.adicionarDias", (args, ctx) -> {
            StdlibHelper.exigirAridade("DATA.adicionarDias", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Data)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.adicionarDias exige DATA");
            BigInteger dias = StdlibHelper.comoInteiroArg(args.get(1), ctx);
            return ValorThz.DATA(((ValorThz.Data) args.get(0)).valor().adicionarDias(dias));
        });
        BibliotecaPadrao.registrarPublico(m, "DATA.adicionarHoras", (args, ctx) -> {
            StdlibHelper.exigirAridade("DATA.adicionarHoras", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.DataHora)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.adicionarHoras exige DATA_HORA");
            BigInteger h = StdlibHelper.comoInteiroArg(args.get(1), ctx);
            return ValorThz.DATA_HORA(((ValorThz.DataHora) args.get(0)).valor().adicionarHoras(h));
        });
        BibliotecaPadrao.registrarPublico(m, "DATA.diferencaDias", (args, ctx) -> {
            StdlibHelper.exigirAridade("DATA.diferencaDias", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Data) || !(args.get(1) instanceof ValorThz.Data))
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.diferencaDias exige duas DATA");
            return ValorThz.INTEIRO(((ValorThz.Data) args.get(0)).valor().diferencaDias(((ValorThz.Data) args.get(1)).valor()));
        });
        BibliotecaPadrao.registrarPublico(m, "DATA.ano", (args, ctx) -> {
            StdlibHelper.exigirAridade("DATA.ano", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.INTEIRO(BigInteger.valueOf(d.valor().getAno()));
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.INTEIRO(BigInteger.valueOf(dh.valor().getData().getAno()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.ano exige DATA ou DATA_HORA");
        });
        BibliotecaPadrao.registrarPublico(m, "DATA.mes", (args, ctx) -> {
            StdlibHelper.exigirAridade("DATA.mes", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.INTEIRO(BigInteger.valueOf(d.valor().getMes()));
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.INTEIRO(BigInteger.valueOf(dh.valor().getData().getMes()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.mes exige DATA ou DATA_HORA");
        });
        BibliotecaPadrao.registrarPublico(m, "DATA.dia", (args, ctx) -> {
            StdlibHelper.exigirAridade("DATA.dia", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.INTEIRO(BigInteger.valueOf(d.valor().getDia()));
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.INTEIRO(BigInteger.valueOf(dh.valor().getData().getDia()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.dia exige DATA ou DATA_HORA");
        });
        BibliotecaPadrao.registrarPublico(m, "DATA.diaDaSemana", (args, ctx) -> {
            StdlibHelper.exigirAridade("DATA.diaDaSemana", args, 1, ctx);
            if (!(args.get(0) instanceof ValorThz.Data)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.diaDaSemana exige DATA");
            return ValorThz.INTEIRO(BigInteger.valueOf(((ValorThz.Data) args.get(0)).valor().diaDaSemana()));
        });
        BibliotecaPadrao.registrarPublico(m, "DATA.texto", (args, ctx) -> {
            StdlibHelper.exigirAridade("DATA.texto", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.TEXTO(d.valor().formatar());
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.TEXTO(dh.valor().formatar());
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.texto exige DATA ou DATA_HORA");
        });
    }
}
