package thz.lang.interpretador;



import java.math.BigInteger;
import java.util.Map;

/**
 * Funções matemáticas da stdlib THZ-LANG.
 * Domínio: MATEMATICA.*
 */
public final class BibliotecaMatematica {

    private BibliotecaMatematica() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        BibliotecaPadrao.registrarPublico(m, "MATEMATICA.abs", (args, ctx) -> {
            StdlibHelper.exigirAridade("MATEMATICA.abs", args, 1, ctx);
            ValorThz v = args.get(0);
            if (v instanceof ValorThz.Inteiro i) {
                BigInteger val = i.valor();
                return ValorThz.INTEIRO(val.signum() < 0 ? val.negate() : val);
            }
            if (v instanceof ValorThz.Decimal d) return ValorThz.DECIMAL(d.valor().abs());
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.abs exige numérico");
        });
        BibliotecaPadrao.registrarPublico(m, "MATEMATICA.min", (args, ctx) -> {
            StdlibHelper.exigirAridade("MATEMATICA.min", args, 2, ctx);
            ValorThz a = args.get(0); ValorThz b = args.get(1);
            if (a instanceof ValorThz.Inteiro ia && b instanceof ValorThz.Inteiro ib) {
                return ValorThz.INTEIRO(ia.valor().compareTo(ib.valor()) < 0 ? ia.valor() : ib.valor());
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.min exige dois INTEIROS");
        });
        BibliotecaPadrao.registrarPublico(m, "MATEMATICA.max", (args, ctx) -> {
            StdlibHelper.exigirAridade("MATEMATICA.max", args, 2, ctx);
            ValorThz a = args.get(0); ValorThz b = args.get(1);
            if (a instanceof ValorThz.Inteiro ia && b instanceof ValorThz.Inteiro ib) {
                return ValorThz.INTEIRO(ia.valor().compareTo(ib.valor()) > 0 ? ia.valor() : ib.valor());
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.max exige dois INTEIROS");
        });
        BibliotecaPadrao.registrarPublico(m, "MATEMATICA.potencia", (args, ctx) -> {
            StdlibHelper.exigirAridade("MATEMATICA.potencia", args, 2, ctx);
            double base = StdlibHelper.comoInteiroArg(args.get(0), ctx).doubleValue();
            double exp = StdlibHelper.comoInteiroArg(args.get(1), ctx).doubleValue();
            double pow = Math.pow(base, exp);
            if (Double.isNaN(pow) || Double.isInfinite(pow)) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.potencia resultado inválido");
            }
            long trunc = (long) (pow >= 0 ? Math.floor(pow) : Math.ceil(pow));
            return ValorThz.INTEIRO(BigInteger.valueOf(trunc));
        });
        BibliotecaPadrao.registrarPublico(m, "MATEMATICA.raiz", (args, ctx) -> {
            StdlibHelper.exigirAridade("MATEMATICA.raiz", args, 1, ctx);
            double n = StdlibHelper.comoInteiroArg(args.get(0), ctx).doubleValue();
            if (n < 0) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.raiz exige não-negativo");
            double s = Math.sqrt(n);
            long trunc = (long) Math.floor(s);
            return ValorThz.INTEIRO(BigInteger.valueOf(trunc));
        });
        BibliotecaPadrao.registrarPublico(m, "MATEMATICA.arredondar", (args, ctx) -> {
            if (args.size() != 2) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.arredondar exige 2 args");
            ValorThz d = args.get(0);
            if (!(d instanceof ValorThz.Decimal)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.arredondar exige DECIMAL");
            int casas = StdlibHelper.comoInteiroArg(args.get(1), ctx).intValue();
            return ValorThz.DECIMAL(((ValorThz.Decimal) d).valor().paraEscala(casas));
        });
        BibliotecaPadrao.registrarPublico(m, "MATEMATICA.aleatorio", (args, ctx) -> {
            if (args.size() != 1) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.aleatorio exige 1 arg (limite)");
            double lim = StdlibHelper.comoInteiroArg(args.get(0), ctx).doubleValue();
            long r = (long) Math.floor(Math.random() * lim);
            return ValorThz.INTEIRO(BigInteger.valueOf(r));
        });
    }
}
