package thz.lang.js;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import static org.junit.jupiter.api.Assertions.*;

public class ThzJsEmitterTest {

    private ProgramaAst parse(String codigo) {
        return new ThzParser(new ThzLexer(codigo).tokenize()).parse();
    }

    @Test
    @DisplayName("Deve emitir código JavaScript ES2023 limpo com runtime DecimalFixo")
    void testEmissaoJs() {
        String src = """
                PROGRAMA FaturamentoSimples
                ESTRUTURA Item
                    descricao : TEXTO
                    valor : DECIMAL(14, 2)
                FIM_ESTRUTURA

                REGRA_NEGOCIO CalculoFaturamento
                    OPERACAO CalcularTotal(preco : DECIMAL(14, 2), qtd : INTEIRO32) : DECIMAL(14, 2)
                    INICIO
                        VARIAVEL total <- preco * qtd
                        RETORNE total
                    FIM
                FIM_REGRA_NEGOCIO

                PROCEDIMENTO Principal()
                INICIO
                    EXIBA "Executando no JS"
                FIM
                FIM_PROGRAMA
                """;

        ProgramaAst ast = parse(src);
        String js = ThzJsEmitter.emitir(ast);

        assertNotNull(js);
        assertTrue(js.contains("class DecimalFixo"));
        assertTrue(js.contains("class Item"));
        assertTrue(js.contains("class CalculoFaturamento"));
        assertTrue(js.contains("static CalcularTotal(preco, qtd)"));
        assertTrue(js.contains("function Principal()"));
    }
}
