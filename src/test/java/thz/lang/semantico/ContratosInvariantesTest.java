package thz.lang.semantico;

import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ContratosInvariantesTest {

    private ProgramaAst parse(String fonte) {
        var tokens = new ThzLexer(fonte).tokenize();
        var ast = new ThzParser(tokens).parse();
        var erros = new AnalisadorSemantico(ast).analisar();
        assertTrue(erros.isEmpty(), "Erros semânticos: " + erros);
        return ast;
    }

    @Test
    public void contratoExigeSatisfeito() {
        String code = """
            PROGRAMA TesteContrato
            REGRA_NEGOCIO Regra1
                CONTRATO_ENTRADA
                    EXIGE x > 0
                FIM_CONTRATO_ENTRADA
                OPERACAO Dobrar(x: INTEIRO32) : INTEIRO32
                INICIO
                    RETORNE x * 2
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;
        var ast = parse(code);
        var interp = new InterpretadorThz(ast);
        var res = interp.executarOperacao("Dobrar", Map.of("x", ValorThz.INTEIRO(5)));
        assertEquals("10", interp.formatar(res));
    }

    @Test
    public void contratoExigeVioladoLancaErro() {
        String code = """
            PROGRAMA TesteContratoFalha
            REGRA_NEGOCIO Regra1
                CONTRATO_ENTRADA
                    EXIGE x > 0
                FIM_CONTRATO_ENTRADA
                OPERACAO ValidarPositivo(x: INTEIRO32) : INTEIRO32
                INICIO
                    RETORNE x
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;
        var ast = parse(code);
        var interp = new InterpretadorThz(ast);
        assertThrows(RuntimeException.class, () -> {
            interp.executarOperacao("ValidarPositivo", Map.of("x", ValorThz.INTEIRO(-3)));
        });
    }

    @Test
    public void contratoGaranteValidado() {
        String code = """
            PROGRAMA TesteGarante
            ESTRUTURA Dado
                valor : INTEIRO32
            FIM_ESTRUTURA
            REGRA_NEGOCIO RegraG
                CONTRATO_SAIDA
                    GARANTE d.valor >= 0
                FIM_CONTRATO_SAIDA
                OPERACAO Processar(d: FATIA[Dado]) : INTEIRO32
                INICIO
                    RETORNE 42
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;
        var ast = parse(code);
        var interp = new InterpretadorThz(ast);
        var res = interp.executarOperacao("Processar", Map.of("d", new ValorThz.Fatia("Dado", List.of())));
        assertEquals("42", interp.formatar(res));
    }

    @Test
    public void estruturaInvarianteValidaComCriar() {
        String code = """
            PROGRAMA TesteInvariante
            ESTRUTURA Conta
                saldo : DECIMAL(10, 2)
                INVARIANTE saldo >= 0.00
            FIM_ESTRUTURA
            PROCEDIMENTO Principal()
                INICIO
                    VARIAVEL c : Conta <- CRIAR Conta(saldo: 100.00)
                    EXIBA "Saldo: " + c.saldo
                FIM
            FIM_PROGRAMA
            """;
        var ast = parse(code);
        var interp = new InterpretadorThz(ast);
        assertDoesNotThrow(() -> interp.executarProcedimento("Principal", Map.of()));
    }
}
