package thz.lang.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DecimalFixo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ThzAnalyticsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("ThzEstatistica deve calcular média, mediana, desvio padrão e correlação de Pearson")
    void testEstatisticaDescritivaECorrelacao() {
        List<Double> valores = List.of(10.0, 20.0, 30.0, 40.0, 50.0);

        DecimalFixo media = ThzEstatistica.media(valores);
        assertEquals("30.0000", media.formatar());

        DecimalFixo mediana = ThzEstatistica.mediana(valores);
        assertEquals("30.0000", mediana.formatar());

        DecimalFixo std = ThzEstatistica.desvioPadrao(valores, true);
        assertEquals("15.8114", std.formatar());

        // Correlação linear perfeita (r = 1.0)
        List<Double> x = List.of(1.0, 2.0, 3.0, 4.0, 5.0);
        List<Double> y = List.of(2.0, 4.0, 6.0, 8.0, 10.0);
        DecimalFixo corr = ThzEstatistica.correlacaoPearson(x, y);
        assertEquals("1.0000", corr.formatar());

        // Regressão Linear: Y = 2X + 0
        var reg = ThzEstatistica.regressaoLinear(x, y);
        assertEquals("2.0000", reg.inclinacao().formatar());
        assertEquals("0.0000", reg.intercepto().formatar());
        assertEquals("1.0000", reg.rQuadrado().formatar());
    }

    @Test
    @DisplayName("ThzEstatistica deve detectar outliers via critério IQR / Tukey e Z-Score")
    void testDeteccaoOutliersEZScore() {
        // Dataset com outlier 999.0
        List<Double> valores = List.of(10.0, 12.0, 11.0, 13.0, 12.0, 14.0, 11.0, 999.0);

        var outliers = ThzEstatistica.detectarOutliers(valores);
        assertFalse(outliers.isEmpty(), "Deve detectar pelo menos 1 outlier!");
        assertEquals("999.0000", outliers.get(0).formatar());

        DecimalFixo z = ThzEstatistica.zScore(999.0, valores);
        assertTrue(Double.parseDouble(z.formatar()) > 2.0, "Z-Score de valor extremo deve ser superior a 2.0!");
    }

    @Test
    @DisplayName("ThzDaxEngine deve calcular YTD, YoY, DISTINCTCOUNT, Ranking e KPIs")
    void testDaxEngine() {
        Map<String, ValorThz> r1 = Map.of("data", ValorThz.TEXTO("2026-01-15"), "venda", ValorThz.DECIMAL(DecimalFixo.deTexto("1000.00", 2)), "vendedor", ValorThz.TEXTO("Alice"));
        Map<String, ValorThz> r2 = Map.of("data", ValorThz.TEXTO("2026-03-20"), "venda", ValorThz.DECIMAL(DecimalFixo.deTexto("2500.00", 2)), "vendedor", ValorThz.TEXTO("Bob"));
        Map<String, ValorThz> r3 = Map.of("data", ValorThz.TEXTO("2025-11-10"), "venda", ValorThz.DECIMAL(DecimalFixo.deTexto("3000.00", 2)), "vendedor", ValorThz.TEXTO("Alice"));

        List<ValorThz.Registro> tabela = List.of(
                new ValorThz.Registro("Venda", r1),
                new ValorThz.Registro("Venda", r2),
                new ValorThz.Registro("Venda", r3)
        );

        // YTD 2026 = 1000 + 2500 = 3500
        DecimalFixo ytd2026 = ThzDaxEngine.totalYtd(tabela, "data", "venda", 2026);
        assertEquals("3500.0000", ytd2026.formatar());

        // DISTINCTCOUNT vendedores = 2 (Alice, Bob)
        long vendedoresUnicos = ThzDaxEngine.contagemDistintos(tabela, "vendedor");
        assertEquals(2, vendedoresUnicos);

        // Variação YoY: de 3000 para 3500 (+16.6667%)
        DecimalFixo yoy = ThzDaxEngine.variacaoPeriodo(3500.0, 3000.0);
        assertEquals("16.6667", yoy.formatar());

        // Ranking por valor de venda (DESC)
        var ranking = ThzDaxEngine.calcularRanking(tabela, "venda", true);
        assertEquals("3000.00", ranking.get(0).campos().get("venda").formatar());
        assertEquals("1", ranking.get(0).campos().get("_ranking").formatar());

        // Avaliação de KPI Corporativo
        var kpi = ThzDaxEngine.avaliarKpi("MargemEbitda", 32.5, 30.0, 5.0);
        assertEquals("VERDE", kpi.campos().get("status").formatar());
    }

    @Test
    @DisplayName("ThzDataQuality deve tratar dados caóticos: moedas PT-BR, datas mistas, CPFs e deduplicação")
    void testDataQualityCaosEmpresarial() {
        // Moedas caóticas do dia a dia
        DecimalFixo d1 = ThzDataQuality.parsearDecimalPtBr(" R$ 1.250.450,75 ");
        assertEquals("1250450.7500", d1.formatar());

        DecimalFixo d2 = ThzDataQuality.parsearDecimalPtBr("(450,20)");
        assertEquals("-450.2000", d2.formatar());

        // Datas mistas
        assertEquals("2026-08-25", ThzDataQuality.parsearDataPtBr("25/08/2026"));
        assertEquals("2026-08-25", ThzDataQuality.parsearDataPtBr("2026-08-25 14:30:00"));
        assertEquals("2026-08-25", ThzDataQuality.parsearDataPtBr("25.08.26"));

        // Validação oficial CPF e CNPJ
        assertTrue(ThzDataQuality.validarCpf("52998224725")); // CPF com DV válido
        assertFalse(ThzDataQuality.validarCpf("11111111111")); // Inválido

        assertTrue(ThzDataQuality.validarCnpj("11222333000181")); // CNPJ com DV válido
        assertFalse(ThzDataQuality.validarCnpj("00000000000000")); // Inválido

        // Mascaramento LGPD
        String mascarado = ThzDataQuality.mascararDadoSensivel("529.982.247-25", 3, 2);
        assertTrue(mascarado.startsWith("529***"), "Deve mascarar miolo do dado!");
        assertTrue(mascarado.endsWith("25"));

        // Deduplicação por chave composta
        Map<String, ValorThz> r1 = Map.of("cnpj", ValorThz.TEXTO("11222333000181"), "mes", ValorThz.TEXTO("2026-08"), "valor", ValorThz.DECIMAL(DecimalFixo.deTexto("100.00", 2)));
        Map<String, ValorThz> r2 = Map.of("cnpj", ValorThz.TEXTO("11222333000181"), "mes", ValorThz.TEXTO("2026-08"), "valor", ValorThz.DECIMAL(DecimalFixo.deTexto("100.00", 2)));
        Map<String, ValorThz> r3 = Map.of("cnpj", ValorThz.TEXTO("11222333000181"), "mes", ValorThz.TEXTO("2026-09"), "valor", ValorThz.DECIMAL(DecimalFixo.deTexto("150.00", 2)));

        List<ValorThz.Registro> duplicados = List.of(
                new ValorThz.Registro("Fatura", r1),
                new ValorThz.Registro("Fatura", r2),
                new ValorThz.Registro("Fatura", r3)
        );

        var limpos = ThzDataQuality.removerDuplicatas(duplicados, List.of("cnpj", "mes"));
        assertEquals(2, limpos.size(), "Deve eliminar a linha duplicada com mesma chave CNPJ+Mês!");
    }

    @Test
    @DisplayName("ThzPlanilhaCsv deve ler, escrever CSV, executar PROCV e gerar Tabela Dinâmica Pivotada")
    void testPlanilhaCsvEProcvEPivot() throws IOException {
        Path arquivoCsv = tempDir.resolve("vendas_producao.csv");
        String conteudoCsv = """
                id;filial;categoria;valor
                1;SP-Capital;Eletronicos;1500.50
                2;RJ-Capital;Servicos;800.00
                3;SP-Capital;Servicos;1200.00
                4;SP-Capital;Eletronicos;3000.00
                """;
        Files.writeString(arquivoCsv, conteudoCsv);

        // 1. Ler CSV
        var tabela = ThzPlanilhaCsv.lerCsv(arquivoCsv, ";");
        assertEquals(4, tabela.size());

        // 2. PROCV / VLOOKUP: Buscar filial do id '2'
        ValorThz filialId2 = ThzPlanilhaCsv.procv(tabela, "id", "2", "filial");
        assertEquals("RJ-Capital", filialId2.formatar());

        // 3. Tabela Dinâmica Pivotada (Linha = filial, Coluna = categoria, Valor = SUM(valor))
        var pivot = ThzPlanilhaCsv.pivotar(tabela, "filial", "categoria", "valor", "SUM");
        assertNotNull(pivot);
        assertFalse(pivot.isEmpty());

        // Procura linha de SP-Capital no Pivot
        var spLinha = pivot.stream().filter(p -> p.campos().get("filial").formatar().equals("SP-Capital")).findFirst();
        assertTrue(spLinha.isPresent());
        // Eletronicos em SP = 1500.50 + 3000.00 = 4500.50
        assertEquals("4500.5000", spLinha.get().campos().get("Eletronicos").formatar());
        // Total SP = 4500.50 + 1200.00 = 5700.50
        assertEquals("5700.5000", spLinha.get().campos().get("_Total").formatar());
    }
}
