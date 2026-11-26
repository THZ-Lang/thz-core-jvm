package thz.lang.sintatico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.driver.ThzCompilerDriver;
import thz.lang.lexico.SintaxeEnxuta;
import thz.lang.lexico.ThzLexer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SintaxeModernaDualidadeTest {

    @Test
    @DisplayName("Deve dessugarizar interpolacao de strings f\"...\" e $\"...\"")
    void deveDessugarizarInterpolacao() {
        String entrada = "print(f\"Ola {nome}, saldo: {saldo}\")";
        String saida = SintaxeEnxuta.dessugarizarInterpolacao(entrada);
        assertEquals("print(\"Ola \" + nome + \", saldo: \" + saldo)", saida);

        String entradaDolar = "val msg = $\"Total: {total}\"";
        String saidaDolar = SintaxeEnxuta.dessugarizarInterpolacao(entradaDolar);
        assertEquals("val msg = \"Total: \" + total", saidaDolar);
    }

    @Test
    @DisplayName("Deve compilar script Top-Level moderno com val, print e interpolacao")
    void deveCompilarTopLevelScript() {
        String codigoModerno = """
                val nome = "Lucas"
                val saldo = 1500.50
                print(f"Usuario: {nome}, Saldo: {saldo}")
                """;

        String normalizado = SintaxeEnxuta.normalizarTopLevel(codigoModerno);
        ThzLexer lexer = new ThzLexer(normalizado);
        ThzParser parser = new ThzParser(lexer.tokenize(), lexer.getDialeto());
        ProgramaAst ast = parser.parse();

        assertNotNull(ast);
        assertEquals("ScriptAutoExec", ast.nome());
        assertFalse(ast.procedimentos().isEmpty(), "Deve conter o procedimento Principal auto-gerado");

        var principal = ast.procedimentos().get(0);
        assertEquals("Principal", principal.nome());
        assertTrue(principal.corpo().size() >= 3, "Deve conter 3 declaracoes/instrucoes no corpo");
    }

    @Test
    @DisplayName("Deve converter compact record para ESTRUTURA na AST")
    void deveConverterRecordParaEstrutura() {
        String codigo = """
                record Cliente(id: Inteiro, nome: Texto, saldo: Decimal)

                val c = CRIAR Cliente(id: 1, nome: "Empresa X", saldo: 250.00)
                print(c.nome)
                """;

        String normalizado = SintaxeEnxuta.normalizarTopLevel(codigo);
        ThzLexer lexer = new ThzLexer(normalizado);
        ThzParser parser = new ThzParser(lexer.tokenize(), lexer.getDialeto());
        ProgramaAst ast = parser.parse();

        assertNotNull(ast);
        assertEquals(1, ast.estruturas().size(), "Deve conter 1 estrutura gerada a partir do record");
        var est = ast.estruturas().get(0);
        assertEquals("Cliente", est.nome());
        assertEquals(3, est.campos().size(), "Deve conter 3 campos (id, nome, saldo)");
    }

    @Test
    @DisplayName("Dualidade: Sintaxe Foundational e Sintaxe Moderna executam com sucesso")
    void deveExecutarDualidadeComSucesso() {
        // 1. Foundational
        String codigoFoundational = """
                PROGRAMA DemoFoundational
                PROCEDIMENTO Principal()
                INICIO
                    VARIAVEL x : INTEIRO <- 42
                    EXIBA x
                FIM
                FIM_PROGRAMA
                """;
        var res1 = ThzCompilerDriver.compilarOuExecutar(codigoFoundational, ThzCompilerDriver.Alvo.EXECUCAO_JVM, false, Map.of());
        assertTrue(res1.sucesso(), "Foundational deve executar com sucesso: " + res1.erros());

        // 2. Moderna (Top-Level Minimal)
        String codigoModerno = """
                val x = 42
                print(x)
                """;
        var res2 = ThzCompilerDriver.compilarOuExecutar(codigoModerno, ThzCompilerDriver.Alvo.EXECUCAO_JVM, false, Map.of());
        assertTrue(res2.sucesso(), "Moderna deve executar com sucesso: " + res2.erros());
    }
}
