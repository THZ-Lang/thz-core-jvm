package thz.lang.otimizador;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.*;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OtimizadorAstTest {

    @Test
    @DisplayName("Deve realizar Constant Folding em operações aritméticas e lógicas constantes")
    void testConstantFolding() {
        // 10 + 20 * 2 -> 10 + 40 -> 50
        ExprAst exprAritmetica = new ExprAst.OpBinaria(
                "+",
                new ExprAst.LiteralInteiro(BigInteger.valueOf(10), 1, 1),
                new ExprAst.OpBinaria("*", new ExprAst.LiteralInteiro(BigInteger.valueOf(20), 1, 1), new ExprAst.LiteralInteiro(BigInteger.valueOf(2), 1, 1), 1, 1),
                1, 1
        );

        ExprAst otimizada = OtimizadorAst.otimizarExpressao(exprAritmetica);

        assertInstanceOf(ExprAst.LiteralInteiro.class, otimizada);
        assertEquals(BigInteger.valueOf(50), ((ExprAst.LiteralInteiro) otimizada).valor());
    }

    @Test
    @DisplayName("Deve eliminar código morto em condicionais SE com condição booleana constante")
    void testDeadCodeEliminationEmCondicionais() {
        // SE FALSO ENTAO exiba("Inalcançável") SENAO exiba("Alcançável") FIM_SE
        ComandoAst cmdCondicional = new ComandoAst.Se(
                new ExprAst.LiteralLogico(false, 1, 1),
                List.of(new ComandoAst.Exiba(new ExprAst.LiteralTexto("Inalcançável", 1, 1), 1, 1)),
                List.of(new ComandoAst.Exiba(new ExprAst.LiteralTexto("Alcançável", 2, 1), 2, 1)),
                1, 1
        );

        List<ComandoAst> otimizados = OtimizadorAst.otimizarComandos(List.of(cmdCondicional));

        assertEquals(1, otimizados.size());
        assertInstanceOf(ComandoAst.Exiba.class, otimizados.get(0));
        assertEquals("Alcançável", ((ExprAst.LiteralTexto) ((ComandoAst.Exiba) otimizados.get(0)).expressao()).valor());
    }
}
