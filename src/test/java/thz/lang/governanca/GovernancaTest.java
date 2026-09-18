package thz.lang.governanca;

import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class GovernancaTest {

    private ProgramaAst parsear(String fonte) {
        return new ThzParser(new ThzLexer(fonte).tokenize()).parse();
    }

    @Test
    public void testAuditarFaturamentoCanonico() throws Exception {
        String fonte = Files.readString(Path.of("exemplos/faturamento.thz"));
        ProgramaAst ast = parsear(fonte);

        RelatorioAuditoria rel = AuditorGovernanca.auditar(ast);
        assertNotNull(rel);
        assertEquals("ProcessamentoFaturamentoLote", rel.nomePrograma());
        assertEquals("LogisticaEFaturamento", rel.metadados().dominio());
        assertTrue(rel.metadados().conformidade().contains("SOX-404"));

        // Matriz
        assertEquals(1, rel.matrizRastreio().size());
        RelatorioAuditoria.ItemRastreabilidade item = rel.matrizRastreio().get(0);
        assertEquals("REQ-FISCAL-9102", item.requisitoId());
        assertEquals("BR-FISCAL-2026-08", item.regraIdentificador());
        assertEquals("CalculoTributarioLote", item.regraNome());
        assertTrue(item.conforme());
        assertEquals(2, item.exige().size());
        assertEquals(1, item.garante().size());
        assertTrue(item.operacoes().contains("ProcessarVetorizado"));

        // Estruturas
        assertEquals(1, rel.estruturas().size());
        assertEquals("ItemFatura", rel.estruturas().get(0).estruturaNome());
        assertEquals("LAYOUT_COLUNAR", rel.estruturas().get(0).layout());
        assertEquals(1, rel.estruturas().get(0).invariantes().size());

        // Métricas
        assertTrue(rel.metricas().aprovado());
        assertEquals(100.0, rel.metricas().percentualConformidade());
        assertTrue(rel.metricas().pendencias().isEmpty());
    }

    @Test
    public void testAuditarPedidosCanonico() throws Exception {
        String fonte = Files.readString(Path.of("exemplos/pedidos.thz"));
        ProgramaAst ast = parsear(fonte);

        RelatorioAuditoria rel = AuditorGovernanca.auditar(ast);
        assertNotNull(rel);
        assertEquals("GestaoPedidos", rel.nomePrograma());
        assertEquals("VendasEFaturamento", rel.metadados().dominio());

        assertFalse(rel.matrizRastreio().isEmpty());
        assertEquals("REQ-VENDAS-3301", rel.matrizRastreio().get(0).requisitoId());
        assertTrue(rel.metricas().aprovado());
    }

    @Test
    public void testGerarMarkdownEJsonGovernanca() throws Exception {
        String fonte = Files.readString(Path.of("exemplos/faturamento.thz"));
        ProgramaAst ast = parsear(fonte);
        RelatorioAuditoria rel = AuditorGovernanca.auditar(ast);

        String md = AuditorGovernanca.gerarMarkdownGovernanca(rel);
        assertNotNull(md);
        assertTrue(md.contains("# Relatório de Auditoria e Governança"));
        assertTrue(md.contains("REQ-FISCAL-9102"));
        assertTrue(md.contains("BR-FISCAL-2026-08"));
        assertTrue(md.contains("LAYOUT_COLUNAR"));
        assertTrue(md.contains("CONFORMIDADE ESTÁTICA APROVADA"));

        String json = AuditorGovernanca.gerarJsonGovernanca(rel);
        assertNotNull(json);
        assertTrue(json.contains("\"programa\": \"ProcessamentoFaturamentoLote\""));
        assertTrue(json.contains("\"scoreConformidade\": 100.0"));
        assertTrue(json.contains("\"requisitoId\": \"REQ-FISCAL-9102\""));
    }

    @Test
    public void testAuditarProgramaSemMetadadosOuRastreio() {
        String fonte = """
                PROGRAMA TesteIncompleto
                REGRA_NEGOCIO RegraSolta
                    DESCRICAO: "Sem rastreio e sem contrato"
                    OPERACAO Executar() : INTEIRO
                    INICIO
                        RETORNE 42
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parsear(fonte);
        RelatorioAuditoria rel = AuditorGovernanca.auditar(ast);

        assertNotNull(rel);
        assertFalse(rel.metricas().aprovado());
        assertTrue(rel.metricas().percentualConformidade() < 80.0);
        assertFalse(rel.metricas().pendencias().isEmpty());
        assertEquals("NÃO_RASTREADO", rel.matrizRastreio().get(0).requisitoId());
    }
}
