package thz.lang.analytics;

import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DecimalFixo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * ThzDaxEngine — Motor de Métricas Analíticas estilo DAX / Power BI / Business Intelligence.
 * Suporta cálculos de inteligência temporal (YTD, YoY), agregação com contexto de filtro,
 * contagem de distintos (DISTINCTCOUNT), cálculo de % do Total, Ranking (RANKX) e KPIs corporativos.
 */
public final class ThzDaxEngine {

    private ThzDaxEngine() {}

    /**
     * Calcula o Acumulado no Ano (Year-To-Date / YTD estilo TOTALYTD do DAX).
     */
    public static DecimalFixo totalYtd(List<ValorThz.Registro> linhas, String campoData, String campoValor, int anoAlvo) {
        if (linhas == null || linhas.isEmpty()) return DecimalFixo.ZERO;
        BigDecimal soma = BigDecimal.ZERO;
        for (ValorThz.Registro reg : linhas) {
            ValorThz vData = reg.campos().get(campoData);
            ValorThz vVal = reg.campos().get(campoValor);
            if (vData != null && vVal != null) {
                String dataStr = vData.formatar();
                int ano = extrairAno(dataStr);
                if (ano == anoAlvo) {
                    soma = soma.add(extrairBigDecimal(vVal));
                }
            }
        }
        return DecimalFixo.deTexto(soma.setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
    }

    /**
     * Variação Percentual em relação ao período anterior (YoY / MoM): $\frac{Atual - Anterior}{Anterior} \times 100$.
     */
    public static DecimalFixo variacaoPeriodo(double valorAtual, double valorAnterior) {
        if (valorAnterior == 0.0) {
            return DecimalFixo.ZERO;
        }
        double var = ((valorAtual - valorAnterior) / Math.abs(valorAnterior)) * 100.0;
        return DecimalFixo.deTexto(BigDecimal.valueOf(var).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
    }

    /**
     * Contagem de valores distintos em uma coluna (estilo DISTINCTCOUNT do DAX).
     */
    public static long contagemDistintos(List<ValorThz.Registro> linhas, String campo) {
        if (linhas == null || linhas.isEmpty()) return 0;
        Set<String> valoresUnicos = new HashSet<>();
        for (ValorThz.Registro reg : linhas) {
            ValorThz v = reg.campos().get(campo);
            if (v != null && !(v instanceof ValorThz.Nulo)) {
                valoresUnicos.add(v.formatar());
            }
        }
        return valoresUnicos.size();
    }

    /**
     * Adiciona uma coluna de Ranking ordinal a cada registro baseado em um campo numérico (estilo RANKX do DAX).
     */
    public static List<ValorThz.Registro> calcularRanking(List<ValorThz.Registro> linhas, String campoValor, boolean ordemDesc) {
        if (linhas == null || linhas.isEmpty()) return List.of();

        List<ValorThz.Registro> ordenados = new ArrayList<>(linhas);
        ordenados.sort((r1, r2) -> {
            BigDecimal v1 = extrairBigDecimal(r1.campos().get(campoValor));
            BigDecimal v2 = extrairBigDecimal(r2.campos().get(campoValor));
            return ordemDesc ? v2.compareTo(v1) : v1.compareTo(v2);
        });

        List<ValorThz.Registro> resultado = new ArrayList<>();
        for (int i = 0; i < ordenados.size(); i++) {
            var original = ordenados.get(i);
            Map<String, ValorThz> novosCampos = new LinkedHashMap<>(original.campos());
            novosCampos.put("_ranking", ValorThz.INTEIRO(i + 1));
            resultado.add(new ValorThz.Registro(original.nomeEstrutura(), novosCampos));
        }
        return resultado;
    }

    /**
     * Calcula o percentual de representatividade de cada linha sobre a soma total da coluna (% do Total).
     */
    public static List<ValorThz.Registro> percentualSobreTotal(List<ValorThz.Registro> linhas, String campoValor) {
        if (linhas == null || linhas.isEmpty()) return List.of();

        BigDecimal somaTotal = BigDecimal.ZERO;
        for (ValorThz.Registro reg : linhas) {
            somaTotal = somaTotal.add(extrairBigDecimal(reg.campos().get(campoValor)));
        }

        List<ValorThz.Registro> resultado = new ArrayList<>();
        for (ValorThz.Registro reg : linhas) {
            BigDecimal val = extrairBigDecimal(reg.campos().get(campoValor));
            BigDecimal pct = somaTotal.compareTo(BigDecimal.ZERO) > 0
                    ? val.multiply(BigDecimal.valueOf(100)).divide(somaTotal, 4, RoundingMode.HALF_EVEN)
                    : BigDecimal.ZERO;

            Map<String, ValorThz> novosCampos = new LinkedHashMap<>(reg.campos());
            novosCampos.put("_percentualTotal", ValorThz.DECIMAL(DecimalFixo.deTexto(pct.toPlainString(), 4)));
            resultado.add(new ValorThz.Registro(reg.nomeEstrutura(), novosCampos));
        }
        return resultado;
    }

    /**
     * Avalia um Indicador-Chave de Desempenho (KPI) com status visual (VERDE / AMARELO / VERMELHO).
     */
    public static ValorThz.Registro avaliarKpi(String nome, double valorRealizado, double valorMeta, double toleranciaPct) {
        double desvioAbs = valorRealizado - valorMeta;
        double atingimentoPct = valorMeta != 0.0 ? (valorRealizado / valorMeta) * 100.0 : 100.0;

        String status;
        if (valorRealizado >= valorMeta) {
            status = "VERDE";
        } else if (valorRealizado >= valorMeta * (1.0 - (toleranciaPct / 100.0))) {
            status = "AMARELO";
        } else {
            status = "VERMELHO";
        }

        Map<String, ValorThz> campos = new LinkedHashMap<>();
        campos.put("kpi", ValorThz.TEXTO(nome));
        campos.put("realizado", ValorThz.DECIMAL(DecimalFixo.deTexto(BigDecimal.valueOf(valorRealizado).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4)));
        campos.put("meta", ValorThz.DECIMAL(DecimalFixo.deTexto(BigDecimal.valueOf(valorMeta).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4)));
        campos.put("desvio", ValorThz.DECIMAL(DecimalFixo.deTexto(BigDecimal.valueOf(desvioAbs).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4)));
        campos.put("atingimentoPct", ValorThz.DECIMAL(DecimalFixo.deTexto(BigDecimal.valueOf(atingimentoPct).setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4)));
        campos.put("status", ValorThz.TEXTO(status));

        return new ValorThz.Registro("KPI_Resultado", campos);
    }

    private static BigDecimal extrairBigDecimal(ValorThz v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof ValorThz.Decimal d) return new BigDecimal(d.valor().formatar());
        if (v instanceof ValorThz.Inteiro in) return new BigDecimal(in.valor());
        if (v instanceof ValorThz.Texto t) {
            try { return new BigDecimal(t.valor().trim().replace(",", ".")); } catch (Exception e) { return BigDecimal.ZERO; }
        }
        return BigDecimal.ZERO;
    }

    private static int extrairAno(String dataStr) {
        if (dataStr == null || dataStr.isBlank()) return 0;
        try {
            if (dataStr.contains("-")) {
                // ISO YYYY-MM-DD
                return Integer.parseInt(dataStr.split("-")[0]);
            } else if (dataStr.contains("/")) {
                // DD/MM/YYYY
                String[] p = dataStr.split("/");
                return Integer.parseInt(p[p.length - 1].trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
