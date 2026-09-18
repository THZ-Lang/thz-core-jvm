package thz.lang.interpretador;

import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.sintatico.ThzParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class InterpretadorTest {

    private ProgramaAst parse(String fonte) {
        var tokens = new ThzLexer(fonte).tokenize();
        var ast = new ThzParser(tokens).parse();
        var erros = new AnalisadorSemantico(ast).analisar();
        assertTrue(erros.isEmpty(), "Erros semânticos: " + erros);
        return ast;
    }

    @Test
    public void lacoParaComPasso() {
        String code = """
            PROGRAMA TestePara
            REGRA_NEGOCIO R1
                OPERACAO SomarPares() : INTEIRO32
                INICIO
                    VARIAVEL soma : INTEIRO32 <- 0
                    PARA i DE 2 ATE 10 PASSO 2
                        soma <- soma + i
                    FIM_PARA
                    RETORNE soma
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;
        var ast = parse(code);
        var interp = new InterpretadorThz(ast);
        var res = interp.executarOperacao("SomarPares", Map.of());
        // 2 + 4 + 6 + 8 + 10 = 30
        assertEquals("30", interp.formatar(res));
    }

    @Test
    public void lacoEnquantoECondicionalSeSenao() {
        String code = """
            PROGRAMA TesteEnquanto
            REGRA_NEGOCIO R1
                OPERACAO Fatorial(n: INTEIRO32) : INTEIRO32
                INICIO
                    SE n <= 1
                        RETORNE 1
                    SENAO
                        VARIAVEL acc : INTEIRO32 <- 1
                        VARIAVEL i : INTEIRO32 <- n
                        ENQUANTO i > 1
                            acc <- acc * i
                            i <- i - 1
                        FIM_ENQUANTO
                        RETORNE acc
                    FIM_SE
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;
        var ast = parse(code);
        var interp = new InterpretadorThz(ast);
        var res = interp.executarOperacao("Fatorial", Map.of("n", ValorThz.INTEIRO(5)));
        assertEquals("120", interp.formatar(res));
    }

    @Test
    public void stdlibTextoEMatematica() {
        String code = """
            PROGRAMA TesteStdlib
            REGRA_NEGOCIO R1
                OPERACAO ProcessarTexto(s: TEXTO) : TEXTO
                INICIO
                    VARIAVEL alta : TEXTO <- TEXTO.maiusculas(s)
                    RETORNE alta
                FIM
                OPERACAO Potencia(b: INTEIRO32, e: INTEIRO32) : INTEIRO32
                INICIO
                    RETORNE MATEMATICA.potencia(b, e)
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;
        var ast = parse(code);
        var interp = new InterpretadorThz(ast);
        var resTexto = interp.executarOperacao("ProcessarTexto", Map.of("s", ValorThz.TEXTO("thz-lang")));
        assertEquals("THZ-LANG", interp.formatar(resTexto));

        var resPot = interp.executarOperacao("Potencia", Map.of("b", ValorThz.INTEIRO(2), "e", ValorThz.INTEIRO(8)));
        assertEquals("256", interp.formatar(resPot));
    }

    @Test
    public void procedimentoComSaidaExiba() {
        String code = """
            PROGRAMA TesteExiba
            PROCEDIMENTO Principal()
                INICIO
                    EXIBA "Linha 1"
                    EXIBA "Linha 2"
                FIM
            FIM_PROGRAMA
            """;
        var ast = parse(code);
        List<String> linhas = new ArrayList<>();
        var interp = new InterpretadorThz(ast, linhas::add, () -> "");
        interp.executarProcedimento("Principal", Map.of());
        assertEquals(2, linhas.size());
        assertEquals("Linha 1", linhas.get(0));
        assertEquals("Linha 2", linhas.get(1));
    }
}
