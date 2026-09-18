package thz.lang.sintatico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ImportacaoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.formato.Formatador;
import thz.lang.lexico.ThzLexer;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.ResolvedorModulos;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ImportacaoModulosTest {

    private ProgramaAst parse(String codigo) {
        return new ThzParser(new ThzLexer(codigo).tokenize()).parse();
    }

    @Test
    @DisplayName("Deve fazer parsing de cláusulas IMPORTAR com e sem DE")
    void testParsingImportacoes() {
        String src = """
                PROGRAMA FaturamentoComImports
                IMPORTAR TiposFiscais
                IMPORTAR UtilitariosCalculo DE "shared/util.thz"
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parse(src);
        assertEquals(2, ast.importacoes().size());

        ImportacaoAst imp1 = ast.importacoes().get(0);
        assertEquals("TiposFiscais", imp1.modulo());
        assertNull(imp1.caminho());

        ImportacaoAst imp2 = ast.importacoes().get(1);
        assertEquals("UtilitariosCalculo", imp2.modulo());
        assertEquals("shared/util.thz", imp2.caminho());

        String formatado = Formatador.formatar(ast);
        assertTrue(formatado.contains("IMPORTAR TiposFiscais"));
        assertTrue(formatado.contains("IMPORTAR UtilitariosCalculo DE \"shared/util.thz\""));
    }

    @Test
    @DisplayName("Analisador semântico deve fundir estruturas de módulos importados via ResolvedorModulos")
    void testFusaoDeSimbolosImportados() {
        String libFonte = """
                BIBLIOTECA ModelosCompartilhados
                ESTRUTURA Cliente
                    id : TEXTO
                    ativo : LOGICO
                FIM_ESTRUTURA
                FIM_BIBLIOTECA
                """;

        String progFonte = """
                PROGRAMA GestaoClientes
                IMPORTAR ModelosCompartilhados DE "modelos.thz"
                PROCEDIMENTO Processar(c : Cliente)
                INICIO
                    EXIBA c.id
                FIM
                FIM_PROGRAMA
                """;

        ResolvedorModulos resolvedor = new ResolvedorModulos();
        resolvedor.registrarModuloVirtual("modelos.thz", libFonte);

        ProgramaAst progAst = parse(progFonte);
        AnalisadorSemantico analisador = new AnalisadorSemantico(progAst, resolvedor);
        List<ErroSemantico> erros = analisador.analisar();

        assertTrue(erros.isEmpty(), "Não deve haver erros após importar a estrutura Cliente: " + erros);
    }
}
