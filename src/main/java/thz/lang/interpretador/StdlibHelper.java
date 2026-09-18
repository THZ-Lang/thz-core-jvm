package thz.lang.interpretador;

import thz.lang.ast.ExprAst;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilitários compartilhados entre as sub-bibliotecas da stdlib.
 * Extraído de BibliotecaPadrao para eliminar duplicação.
 */
public final class StdlibHelper {

    private StdlibHelper() {}

    public static void exigirAridade(String nome, List<ValorThz> args, int esperada, ExprAst ctx) {
        if (args.size() != esperada) {
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Função '" + nome + "' exige " + esperada + " argumento(s), recebidos " + args.size() + ".");
        }
    }

    public static void exigirClasse(String nome, ValorThz v, String classeEsperada, ExprAst ctx) {
        if (!v.classe().equals(classeEsperada)) {
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Função '" + nome + "' exige " + classeEsperada + ", recebido " + v.classe() + ".");
        }
    }

    public static BigInteger comoInteiroArg(ValorThz v, ExprAst ctx) {
        if (v instanceof ValorThz.Inteiro i) return i.valor();
        throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Esperado INTEIRO, recebido " + v.classe() + ".");
    }

    public static float[] extrairVetorArg(ValorThz v, ExprAst ctx) {
        if (v instanceof ValorThz.Texto t) {
            return thz.lang.vetor.ThzVetorSimd.parseVetor(t.valor());
        }
        if (v instanceof ValorThz.Fatia fatia) {
            float[] res = new float[fatia.elementos().size()];
            for (int i = 0; i < fatia.elementos().size(); i++) {
                ValorThz elem = fatia.elementos().get(i);
                if (elem instanceof ValorThz.Decimal d) res[i] = Float.parseFloat(d.valor().formatar());
                else if (elem instanceof ValorThz.Inteiro in) res[i] = in.valor().floatValue();
                else if (elem instanceof ValorThz.Texto tx) res[i] = Float.parseFloat(tx.valor());
            }
            return res;
        }
        throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Esperado TEXTO formatado de vetor ou FATIA numérica, recebido " + v.classe() + ".");
    }

    public static String sliceTexto(String texto, int ini, Integer fim) {
        int len = texto.length();
        int s = ini;
        if (s < 0) s = Math.max(len + s, 0);
        else if (s > len) s = len;
        int e;
        if (fim == null) e = len;
        else {
            e = fim;
            if (e < 0) e = Math.max(len + e, 0);
            else if (e > len) e = len;
        }
        if (e < s) return "";
        return texto.substring(s, e);
    }

    public static List<Double> extrairListaDoubles(ValorThz v, ExprAst ctx) {
        List<Double> lista = new ArrayList<>();
        if (v instanceof ValorThz.Fatia fatia) {
            for (ValorThz elem : fatia.elementos()) {
                if (elem instanceof ValorThz.Decimal d) {
                    lista.add(Double.parseDouble(d.valor().formatar()));
                } else if (elem instanceof ValorThz.Inteiro in) {
                    lista.add(in.valor().doubleValue());
                } else if (elem instanceof ValorThz.Texto t) {
                    try { lista.add(Double.parseDouble(t.valor().trim().replace(",", "."))); } catch (Exception ignored) {}
                }
            }
        }
        return lista;
    }

    public static List<ValorThz.Registro> extrairListaRegistros(ValorThz v, ExprAst ctx) {
        List<ValorThz.Registro> lista = new ArrayList<>();
        if (v instanceof ValorThz.Fatia fatia) {
            for (ValorThz elem : fatia.elementos()) {
                if (elem instanceof ValorThz.Registro r) {
                    lista.add(r);
                }
            }
        }
        return lista;
    }

    public static double extrairDoubleArg(ValorThz v, ExprAst ctx) {
        if (v instanceof ValorThz.Decimal d) return Double.parseDouble(d.valor().formatar());
        if (v instanceof ValorThz.Inteiro in) return in.valor().doubleValue();
        if (v instanceof ValorThz.Texto t) {
            try { return Double.parseDouble(t.valor().trim().replace(",", ".")); } catch (Exception ignored) {}
        }
        return 0.0;
    }

    public static LocalDate extrairDataArg(ValorThz v) {
        if (v instanceof ValorThz.Data d) {
            return LocalDate.of(d.valor().getAno(), d.valor().getMes(), d.valor().getDia());
        }
        if (v instanceof ValorThz.DataHora dh) {
            return LocalDate.of(dh.valor().getData().getAno(), dh.valor().getData().getMes(), dh.valor().getData().getDia());
        }
        String iso = thz.lang.analytics.ThzDataQuality.parsearDataPtBr(v.formatar());
        try {
            return LocalDate.parse(iso);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
