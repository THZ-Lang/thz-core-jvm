package thz.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RustEmbutidoTest {

    @Test
    @DisplayName("Deve fazer parsing de BLOCO_NATIVO_RUST e executar chamadas NATIVO.*")
    void testBlocoNativoRustEChamadas() {
        String codigo = """
            PROGRAMA TesteRustInline
            METADADOS_ARQUITETURA
                DOMINIO: "AltaPerformance"
                CAMADA: "Servico"
                VERSAO: "3.0.0"
                AUTOR: "Engenharia"
            FIM_METADADOS

            BLOCO_NATIVO_RUST
                #[no_mangle]
                pub extern "C" fn somar_rapido(a: i64, b: i64) -> i64 {
                    a + b
                }
            FIM_BLOCO_NATIVO

            REGRA_NEGOCIO CalculoNativo
                OPERACAO Executar() : INTEIRO
                INICIO
                    VARIAVEL resSoma : INTEIRO <- NATIVO.somar_rapido(100, 250)
                    VARIAVEL resHash : INTEIRO <- NATIVO.calcular_hash_customizado(42)
                    EXIBA "Hash: " + TEXTO.deInteiro(resHash)
                    RETORNE resSoma
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        var tokens = new ThzLexer(codigo).tokenize();
        var parser = new ThzParser(tokens);
        ProgramaAst ast = parser.parse();

        // 1. Validação AST
        assertNotNull(ast.blocosRust());
        assertEquals(1, ast.blocosRust().size());
        assertTrue(ast.blocosRust().get(0).contains("somar_rapido"));

        // 2. Execução
        var interp = new InterpretadorThz(ast);
        var res = interp.executarOperacao("Executar", Map.of());
        assertEquals("350", interp.formatar(res));
    }
}
