package thz.lang.interpretador;

import java.math.BigInteger;
import java.util.regex.Pattern;

import thz.lang.ast.ExprAst;
import thz.lang.runtime.DataHoraThz;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.DecimalFixo;
import thz.lang.runtime.ErroData;
import thz.lang.runtime.ErroMonetario;
import thz.lang.runtime.Monetario;

/**
 * Utilitários estáticos para conversão e formatação de valores THZ.
 * Extraído de InterpretadorThz para responsabilidade única.
 */
public final class ValorThzUtils {

    private ValorThzUtils() {
    }

    public static String formatar(ValorThz v) {
        if (v == null)
            return "NULO";
        return switch (v) {
            case ValorThz.Inteiro i -> i.valor().toString();
            case ValorThz.Decimal d -> d.valor().formatar();
            case ValorThz.Monetario m -> m.valor().formatar();
            case ValorThz.Texto t -> t.valor();
            case ValorThz.Logico l -> l.valor() ? "VERDADEIRO" : "FALSO";
            case ValorThz.Nulo _ -> "NULO";
            case ValorThz.Data d -> d.valor().formatar();
            case ValorThz.DataHora dh -> dh.valor().formatar();
            case ValorThz.Enumerado e -> e.valor();
            case ValorThz.Resultado r -> r.sucesso()
                    ? "SUCESSO(" + (r.valor() != null ? formatar(r.valor()) : "NULO") + ")"
                    : "FALHA(" + (r.erro() != null ? formatar(r.erro()) : "NULO") + ")";
            case ValorThz.Registro reg -> reg.nomeEstrutura() + "{...}";
            case ValorThz.Fatia f -> "FATIA[" + f.tipoInterno() + "](" + f.elementos().size() + ")";
        };
    }

    public static boolean exigirLogico(ValorThz v, String contexto) {
        if (!(v instanceof ValorThz.Logico l))
            throw new ErroExecucao("[Erro de Execução] Esperado valor lógico em " + contexto + ".");
        return l.valor();
    }

    public static boolean ehNumerico(ValorThz v) {
        return v instanceof ValorThz.Inteiro || v instanceof ValorThz.Decimal;
    }

    public static DecimalFixo comoDecimal(ValorThz v, ExprAst ctx) {
        if (v instanceof ValorThz.Decimal d)
            return d.valor();
        if (v instanceof ValorThz.Inteiro i)
            return DecimalFixo.deInteiro(i.valor());
        throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna()
                + "] Esperado valor numérico, recebido " + v.classe() + ".");
    }

    public static Integer ordemNumerica(ValorThz x, ValorThz y, ExprAst ctx) {
        if (x instanceof ValorThz.Inteiro xi && y instanceof ValorThz.Inteiro yi) {
            int cmp = xi.valor().compareTo(yi.valor());
            return Integer.compare(cmp, 0);
        }
        try {
            DecimalFixo dx = comoDecimal(x, ctx);
            DecimalFixo dy = comoDecimal(y, ctx);
            int cmp = dx.comparar(dy);
            return cmp;
        } catch (ErroExecucao e) {
            return null;
        }
    }

    public static ValorThz aritmetica(ValorThz a, ValorThz b, String operador, ExprAst ctx) {
        if ("+".equals(operador) && (a instanceof ValorThz.Texto || b instanceof ValorThz.Texto)) {
            return ValorThz.TEXTO(formatar(a) + formatar(b));
        }
        if (a instanceof ValorThz.Inteiro ia && b instanceof ValorThz.Inteiro ib) {
            switch (operador) {
                case "+":
                    return ValorThz.INTEIRO(ia.valor().add(ib.valor()));
                case "-":
                    return ValorThz.INTEIRO(ia.valor().subtract(ib.valor()));
                case "*":
                    return ValorThz.INTEIRO(ia.valor().multiply(ib.valor()));
                case "/":
                    if (ib.valor().equals(BigInteger.ZERO))
                        throw new ErroExecucao(
                                "[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Divisão por zero.");
                    return ValorThz.INTEIRO(ia.valor().divide(ib.valor()));
                case "%":
                    if (ib.valor().equals(BigInteger.ZERO))
                        throw new ErroExecucao(
                                "[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Módulo por zero.");
                    return ValorThz.INTEIRO(ia.valor().remainder(ib.valor()));
            }
        }

        if (ehNumerico(a) && ehNumerico(b)) {
            DecimalFixo da = comoDecimal(a, ctx);
            DecimalFixo db = comoDecimal(b, ctx);
            return switch (operador) {
                case "+" -> ValorThz.DECIMAL(da.somar(db));
                case "-" -> ValorThz.DECIMAL(da.subtrair(db));
                case "*" -> ValorThz.DECIMAL(da.multiplicar(db));
                case "/" -> {
                    if (db.getValorEscalado().equals(BigInteger.ZERO))
                        throw new ErroExecucao(
                                "[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Divisão por zero.");
                    yield ValorThz.DECIMAL(da.dividir(db));
                }
                case "%" -> throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna()
                        + "] Operador '%' suportado apenas entre inteiros.");
                default -> throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna()
                        + "] Operação " + operador + " inválida entre " + a.classe() + " e " + b.classe() + ".");
            };
        }
        if (a instanceof ValorThz.Monetario ma && b instanceof ValorThz.Monetario mb) {
            switch (operador) {
                case "+":
                    return ValorThz.MONETARIO(ma.valor().somar(mb.valor()));
                case "-":
                    return ValorThz.MONETARIO(ma.valor().subtrair(mb.valor()));
            }
        }
        if (a instanceof ValorThz.Monetario ma2 && b instanceof ValorThz.Decimal db) {
            if ("*".equals(operador))
                return ValorThz.MONETARIO(ma2.valor().multiplicar(db.valor()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna()
                    + "] Monetário só admite multiplicação por fator decimal.");
        }
        if (a instanceof ValorThz.Decimal da2 && b instanceof ValorThz.Monetario mb2 && "*".equals(operador)) {
            return ValorThz.MONETARIO(mb2.valor().multiplicar(da2.valor()));
        }
        throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Operação " + operador
                + " inválida entre " + a.classe() + " e " + b.classe() + ".");
    }

    public static boolean comparar(ValorThz a, ValorThz b, String operador, ExprAst ctx) {
        if (a instanceof ValorThz.Texto ta && b instanceof ValorThz.Texto tb) {
            int cmp = ta.valor().compareTo(tb.valor());
            return switch (operador) {
                case "=" -> cmp == 0;
                case "<>" -> cmp != 0;
                case "<" -> cmp < 0;
                case "<=" -> cmp <= 0;
                case ">" -> cmp > 0;
                case ">=" -> cmp >= 0;
                default -> false;
            };
        }
        if (a instanceof ValorThz.Logico la && b instanceof ValorThz.Logico lb) {
            if ("=".equals(operador))
                return la.valor() == lb.valor();
            if ("<>".equals(operador))
                return la.valor() != lb.valor();
        } else if (a instanceof ValorThz.Logico || b instanceof ValorThz.Logico) {
        }
        if (a instanceof ValorThz.Monetario ma && b instanceof ValorThz.Monetario mb) {
            int c = ma.valor().comparar(mb.valor());
            return switch (operador) {
                case "=" -> c == 0;
                case "<>" -> c != 0;
                case "<" -> c < 0;
                case "<=" -> c <= 0;
                case ">" -> c > 0;
                case ">=" -> c >= 0;
                default -> false;
            };
        }
        if (a instanceof ValorThz.Data da && b instanceof ValorThz.Data db) {
            int c = da.valor().comparar(db.valor());
            return switch (operador) {
                case "=" -> c == 0;
                case "<>" -> c != 0;
                case "<" -> c < 0;
                case "<=" -> c <= 0;
                case ">" -> c > 0;
                case ">=" -> c >= 0;
                default -> false;
            };
        }
        if (a instanceof ValorThz.DataHora dha && b instanceof ValorThz.DataHora dhb) {
            int c = dha.valor().comparar(dhb.valor());
            return switch (operador) {
                case "=" -> c == 0;
                case "<>" -> c != 0;
                case "<" -> c < 0;
                case "<=" -> c <= 0;
                case ">" -> c > 0;
                case ">=" -> c >= 0;
                default -> false;
            };
        }
        if (ehNumerico(a) && ehNumerico(b)) {
            Integer ord = ordemNumerica(a, b, ctx);
            if (ord != null) {
                return switch (operador) {
                    case "=" -> ord == 0;
                    case "<>" -> ord != 0;
                    case "<" -> ord < 0;
                    case "<=" -> ord <= 0;
                    case ">" -> ord > 0;
                    case ">=" -> ord >= 0;
                    default -> false;
                };
            }
        }
        if (a instanceof ValorThz.Enumerado ea && b instanceof ValorThz.Enumerado eb) {
            boolean mesma = ea.nomeEnumeracao().equals(eb.nomeEnumeracao());
            if ("=".equals(operador))
                return mesma && ea.valor().equals(eb.valor());
            if ("<>".equals(operador))
                return !(mesma && ea.valor().equals(eb.valor()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna()
                    + "] ENUMERACAO suporta apenas os operadores = e <>.");
        }
        if (a instanceof ValorThz.Logico && b instanceof ValorThz.Logico) {
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna()
                    + "] Comparação entre tipos incompatíveis (" + a.classe() + " " + operador + " " + b.classe()
                    + ").");
        }
        throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna()
                + "] Comparação entre tipos incompatíveis (" + a.classe() + " " + operador + " " + b.classe() + ").");
    }

    /**
     * Converte valores (de fixtures) para o universo THZ segundo um tipo declarado.
     * Port exato de valorThzDe em interpretador.ts.
     */
    public static ValorThz valorThzDe(String tipoDado, Object bruto) {
        if (tipoDado.startsWith("NATURAL") || tipoDado.startsWith("INTEIRO")) {
            if (bruto instanceof BigInteger bi)
                return ValorThz.INTEIRO(bi);
            if (bruto instanceof Number n) {
                double d = n.doubleValue();
                long trunc = (long) (d >= 0 ? Math.floor(d) : Math.ceil(d));
                return ValorThz.INTEIRO(BigInteger.valueOf(trunc));
            }
            String s = String.valueOf(bruto).trim();
            if (s.contains("."))
                s = s.substring(0, s.indexOf('.'));
            return ValorThz.INTEIRO(new BigInteger(s));
        }
        if (tipoDado.startsWith("MONETARIO")) {
            Pattern p = Pattern.compile("^MONETARIO\\s*\\(\\s*\"?([A-Z]{3})\"?\\s*\\)");
            var m = p.matcher(tipoDado);
            String codigoMoeda = null;
            if (m.find())
                codigoMoeda = m.group(1);
            if (codigoMoeda == null)
                throw new ErroMonetario("[Erro Monetário] Tipo '" + tipoDado
                        + "' exige código ISO 4217: MONETARIO(\"BRL\") por exemplo.");
            if (bruto instanceof BigInteger bi)
                return ValorThz.MONETARIO(Monetario.deInteiro(bi, codigoMoeda));
            return ValorThz.MONETARIO(Monetario.deTexto(String.valueOf(bruto), codigoMoeda));
        }
        if (tipoDado.startsWith("DECIMAL")) {
            Pattern pEscala = Pattern.compile(",\\s*(\\d+)\\s*\\)\\s*$");
            var m = pEscala.matcher(tipoDado);
            int escala = DecimalFixo.ESCALA_PADRAO;
            if (m.find())
                escala = Math.min(Integer.parseInt(m.group(1), 10), DecimalFixo.ESCALA_PADRAO);
            String numero;
            if (bruto instanceof String s)
                numero = s;
            else
                numero = String.format(java.util.Locale.US, "%." + escala + "f", ((Number) bruto).doubleValue());
            return ValorThz.DECIMAL(DecimalFixo.deTexto(numero, escala));
        }
        if ("DATA".equals(tipoDado)) {
            if (bruto instanceof String s)
                return ValorThz.DATA(DataThz.deTexto(s));
            throw new ErroData("[Erro Data] Valor para DATA deve ser texto 'AAAA-MM-DD'.");
        }
        if ("DATA_HORA".equals(tipoDado)) {
            if (bruto instanceof String s)
                return ValorThz.DATA_HORA(DataHoraThz.deTexto(s));
            throw new ErroData("[Erro DataHora] Valor para DATA_HORA deve ser texto 'AAAA-MM-DDTHH:MM[:SS]'.");
        }
        if ("TEXTO".equals(tipoDado))
            return ValorThz.TEXTO(String.valueOf(bruto));
        if ("LOGICO".equals(tipoDado)) {
            String s = String.valueOf(bruto).toUpperCase();
            return ValorThz.LOGICO(s.equals("VERDADEIRO") || s.equals("TRUE") || s.equals("1"));
        }
        return ValorThz.TEXTO(String.valueOf(bruto));
    }
}
