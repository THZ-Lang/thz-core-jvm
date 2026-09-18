package thz.lang.docgen;

import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class DocGenTest {

    private ProgramaAst parsear(String fonte) {
        return new ThzParser(new ThzLexer(fonte).tokenize()).parse();
    }

    @Test
    public void testGerarDocumentacaoFaturamento() throws Exception {
        String fonte = Files.readString(Path.of("exemplos/faturamento.thz"));
        ProgramaAst ast = parsear(fonte);

        String doc = ThzDocGen.gerarDocumentacao(ast);
        assertNotNull(doc);

        // Seções
        assertTrue(doc.contains("# Documentação Arquitetural e de Domínio — ProcessamentoFaturamentoLote"));
        assertTrue(doc.contains("## 1. Metadados de Arquitetura Viva"));
        assertTrue(doc.contains("LogisticaEFaturamento"));
        assertTrue(doc.contains("SOX-404"));

        // Diagramas Mermaid
        assertTrue(doc.contains("```mermaid\nclassDiagram"));
        assertTrue(doc.contains("class ItemFatura"));
        assertTrue(doc.contains("<<LAYOUT_COLUNAR (SoA)>>"));

        assertTrue(doc.contains("```mermaid\ngraph TD"));
        assertTrue(doc.contains("CalculoTributarioLote"));
        assertTrue(doc.contains("ProcessarVetorizado"));

        // Contratos e Regras
        assertTrue(doc.contains("EXIGE itens.quantidade > 0"));
        assertTrue(doc.contains("GARANTE itens.valor_total_liquido >= 0.0000"));
    }

    @Test
    public void testGerarDocumentacaoPedidosDDD() throws Exception {
        String fonte = Files.readString(Path.of("exemplos/pedidos.thz"));
        ProgramaAst ast = parsear(fonte);

        String doc = ThzDocGen.gerarDocumentacao(ast);
        assertNotNull(doc);
        assertTrue(doc.contains("# Documentação Arquitetural e de Domínio — GestaoPedidos"));
        assertTrue(doc.contains("ENUMERACAO StatusPedido"));
        assertTrue(doc.contains("`PENDENTE`, `APROVADO`, `REJEITADO`"));
    }
}
