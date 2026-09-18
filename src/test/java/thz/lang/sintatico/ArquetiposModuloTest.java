package thz.lang.sintatico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.ast.TipoModulo;
import thz.lang.docgen.ThzDocGen;
import thz.lang.formato.Formatador;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.OpcoesAnalise;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ArquetiposModuloTest {

    private ProgramaAst parse(String codigo) {
        List<Token> tokens = new ThzLexer(codigo).tokenize();
        return new ThzParser(tokens).parse();
    }

    @Test
    @DisplayName("PROGRAMA padrão deve ser reconhecido e formatado com FIM_PROGRAMA")
    void testProgramaPadrao() {
        String src = """
                PROGRAMA FaturamentoSimples
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parse(src);
        assertEquals(TipoModulo.PROGRAMA, ast.tipoModulo());
        assertEquals("FaturamentoSimples", ast.nome());

        String formatado = Formatador.formatar(ast);
        assertTrue(formatado.contains("PROGRAMA FaturamentoSimples"));
        assertTrue(formatado.contains("FIM_PROGRAMA"));
    }

    @Test
    @DisplayName("PROGRAMA VISUAL deve ser reconhecido com FIM_PROGRAMA")
    void testProgramaVisual() {
        String src = """
                PROGRAMA VISUAL DashboardVendas
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parse(src);
        assertEquals(TipoModulo.PROGRAMA_VISUAL, ast.tipoModulo());
        assertEquals("DashboardVendas", ast.nome());

        String formatado = Formatador.formatar(ast);
        assertTrue(formatado.contains("PROGRAMA VISUAL DashboardVendas"));
        assertTrue(formatado.contains("FIM_PROGRAMA"));
    }

    @Test
    @DisplayName("PROGRAMA NEGOCIO deve ser reconhecido com FIM_PROGRAMA")
    void testProgramaNegocio() {
        String src = """
                PROGRAMA NEGOCIO FaturamentoFiscal
                METADADOS_ARQUITETURA
                    DOMINIO: "Financeiro"
                    SLO_LATENCIA_MAXIMA: "50ms"
                FIM_METADADOS
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parse(src);
        assertEquals(TipoModulo.PROGRAMA_NEGOCIO, ast.tipoModulo());
        assertEquals("FaturamentoFiscal", ast.nome());

        List<ErroSemantico> erros = new AnalisadorSemantico(ast).analisar(new OpcoesAnalise(true));
        assertTrue(erros.isEmpty(), "Não deve haver erros semânticos em programa de negócio válido: " + erros);
    }

    @Test
    @DisplayName("PROGRAMA ARQUITETURA deve ser reconhecido e documentado")
    void testProgramaArquitetura() {
        String src = """
                PROGRAMA ARQUITETURA MapaSistemas
                METADADOS_ARQUITETURA
                    DOMINIO: "Core"
                    CAMADA: "Dominio"
                FIM_METADADOS
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parse(src);
        assertEquals(TipoModulo.PROGRAMA_ARQUITETURA, ast.tipoModulo());
        assertEquals("MapaSistemas", ast.nome());

        String doc = ThzDocGen.gerarDocumentacao(ast);
        assertTrue(doc.contains("PROGRAMA ARQUITETURA"));
        assertTrue(doc.contains("MapaSistemas"));
    }

    @Test
    @DisplayName("BIBLIOTECA deve ser reconhecida com FIM_BIBLIOTECA e formatação idempotente")
    void testBiblioteca() {
        String src = """
                BIBLIOTECA UtilitariosCalculo
                ENUMERACAO Moeda
                    BRL
                    USD
                FIM_ENUMERACAO
                FIM_BIBLIOTECA
                """;
        ProgramaAst ast = parse(src);
        assertEquals(TipoModulo.BIBLIOTECA, ast.tipoModulo());
        assertEquals("UtilitariosCalculo", ast.nome());
        assertEquals(1, ast.enumeracoes().size());

        String fmt1 = Formatador.formatar(ast);
        assertTrue(fmt1.contains("BIBLIOTECA UtilitariosCalculo"));
        assertTrue(fmt1.contains("FIM_BIBLIOTECA"));

        ProgramaAst astFmt = parse(fmt1);
        String fmt2 = Formatador.formatar(astFmt);
        assertEquals(fmt1, fmt2, "Formatação deve ser idempotente");
    }

    @Test
    @DisplayName("EXTENSAO deve ser reconhecida com FIM_EXTENSAO")
    void testExtensao() {
        String src = """
                EXTENSAO DriverKafka
                ESTRUTURA MensagemKafka
                    topico : TEXTO
                    payload : TEXTO
                FIM_ESTRUTURA
                FIM_EXTENSAO
                """;
        ProgramaAst ast = parse(src);
        assertEquals(TipoModulo.EXTENSAO, ast.tipoModulo());
        assertEquals("DriverKafka", ast.nome());
        assertEquals(1, ast.estruturas().size());

        String fmt = Formatador.formatar(ast);
        assertTrue(fmt.contains("EXTENSAO DriverKafka"));
        assertTrue(fmt.contains("FIM_EXTENSAO"));
    }

    @Test
    @DisplayName("FERRAMENTA deve ser reconhecida com FIM_FERRAMENTA")
    void testFerramenta() {
        String src = """
                FERRAMENTA MigradorTabelas
                FIM_FERRAMENTA
                """;
        ProgramaAst ast = parse(src);
        assertEquals(TipoModulo.FERRAMENTA, ast.tipoModulo());
        assertEquals("MigradorTabelas", ast.nome());

        String fmt = Formatador.formatar(ast);
        assertTrue(fmt.contains("FERRAMENTA MigradorTabelas"));
        assertTrue(fmt.contains("FIM_FERRAMENTA"));
    }

    @Test
    @DisplayName("TESTE deve ser reconhecido com FIM_TESTE")
    void testModuloTeste() {
        String src = """
                TESTE TesteFaturamento
                FIM_TESTE
                """;
        ProgramaAst ast = parse(src);
        assertEquals(TipoModulo.TESTE, ast.tipoModulo());
        assertEquals("TesteFaturamento", ast.nome());

        String fmt = Formatador.formatar(ast);
        assertTrue(fmt.contains("TESTE TesteFaturamento"));
        assertTrue(fmt.contains("FIM_TESTE"));
    }

    @Test
    @DisplayName("TELA (.thzui) deve ser reconhecida com FIM_TELA e formatação canônica")
    void testModuloTela() {
        String src = """
                TELA DashboardFinanceiro
                FIM_TELA
                """;
        ProgramaAst ast = parse(src);
        assertEquals(TipoModulo.TELA, ast.tipoModulo());
        assertEquals("DashboardFinanceiro", ast.nome());

        String fmt = Formatador.formatar(ast);
        assertTrue(fmt.contains("TELA DashboardFinanceiro"));
        assertTrue(fmt.contains("FIM_TELA"));
    }

    @Test
    @DisplayName("Erro sintático caso o terminador de bloco não seja compatível com o módulo")
    void testTerminadorIncompativelLancaErroSintatico() {
        String src = """
                BIBLIOTECA MinhaLib
                FIM_PROGRAMA
                """;
        Exception ex = assertThrows(RuntimeException.class, () -> parse(src));
        assertTrue(ex.getMessage().contains("[Erro Sintático]"));
        assertTrue(ex.getMessage().contains("FIM_BIBLIOTECA"));
    }
}
