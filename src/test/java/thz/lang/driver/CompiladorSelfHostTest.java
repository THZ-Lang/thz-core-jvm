package thz.lang.driver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.interpretador.ValorThz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CompiladorSelfHostTest {

    private String lerFonte(String caminhoRelativo) throws IOException {
        Path p = Paths.get("..", "thz-compiler", caminhoRelativo);
        if (!Files.exists(p)) {
            p = Paths.get("..", "thz-lang", "compilador", caminhoRelativo);
        }
        if (!Files.exists(p)) {
            p = Paths.get("compilador", caminhoRelativo);
        }
        if (!Files.exists(p)) {
            p = Paths.get("..", "..", "compilador", caminhoRelativo);
        }
        if (!Files.exists(p)) {
            p = Paths.get("JVM", "thz-core-jvm", "exemplos", "compilador", caminhoRelativo);
        }
        assertTrue(Files.exists(p), "Arquivo do compilador não encontrado: " + caminhoRelativo);
        return Files.readString(p);
    }

    @Test
    @DisplayName("compilador/tokens.thz deve ser verificado sem erros pelo ThzCompilerDriver")
    void testTokensSelfHost() throws IOException {
        String src = lerFonte("tokens.thz");
        var res = ThzCompilerDriver.compilarOuExecutar(src, ThzCompilerDriver.Alvo.AUDITORIA, false, Map.of());
        assertTrue(res.sucesso(), "tokens.thz deve ser analisado com sucesso: " + res.erros());
    }

    @Test
    @DisplayName("compilador/ast.thz deve ser verificado sem erros pelo ThzCompilerDriver")
    void testAstSelfHost() throws IOException {
        String src = lerFonte("ast.thz");
        var res = ThzCompilerDriver.compilarOuExecutar(src, ThzCompilerDriver.Alvo.AUDITORIA, false, Map.of());
        assertTrue(res.sucesso(), "ast.thz deve ser analisado com sucesso: " + res.erros());
    }

    @Test
    @DisplayName("compilador/lexer.thz deve ser verificado e executado sem erros pelo ThzCompilerDriver")
    void testLexerSelfHost() throws IOException {
        String src = lerFonte("lexer.thz");
        var res = ThzCompilerDriver.compilarOuExecutar(src, ThzCompilerDriver.Alvo.EXECUCAO_JVM, false, Map.of("tamanho_fonte", ValorThz.INTEIRO(100)));
        assertTrue(res.sucesso(), "lexer.thz deve ser analisado e executado com sucesso: " + res.erros());
    }

    @Test
    @DisplayName("compilador/parser.thz deve ser verificado e executado sem erros pelo ThzCompilerDriver")
    void testParserSelfHost() throws IOException {
        String src = lerFonte("parser.thz");
        var res = ThzCompilerDriver.compilarOuExecutar(src, ThzCompilerDriver.Alvo.EXECUCAO_JVM, false, Map.of("total_caracteres", ValorThz.INTEIRO(100)));
        assertTrue(res.sucesso(), "parser.thz deve ser analisado e executado com sucesso: " + res.erros());
    }

    @Test
    @DisplayName("compilador/codegen.thz deve ser verificado e executado sem erros pelo ThzCompilerDriver")
    void testCodegenSelfHost() throws IOException {
        String src = lerFonte("codegen.thz");
        var res = ThzCompilerDriver.compilarOuExecutar(src, ThzCompilerDriver.Alvo.EXECUCAO_JVM, false, Map.of("total_nos_ast", ValorThz.INTEIRO(8)));
        assertTrue(res.sucesso(), "codegen.thz deve ser analisado e executado com sucesso: " + res.erros());
    }

    @Test
    @DisplayName("compilador/driver.thz deve orquestrar a execução do compilador self-hosted com sucesso")
    void testDriverSelfHost() throws IOException {
        String src = lerFonte("driver.thz");
        var res = ThzCompilerDriver.compilarOuExecutar(src, ThzCompilerDriver.Alvo.EXECUCAO_JVM, false, Map.of("tamanho_fonte", ValorThz.INTEIRO(100)));
        assertTrue(res.sucesso(), "driver.thz deve ser analisado e executado com sucesso: " + res.erros());
    }

    @Test
    @DisplayName("compilador/driver.thz deve gerar código LLVM IR nativo sem erros")
    void testDriverLlvmIrGeneration() throws IOException {
        String src = lerFonte("driver.thz");
        var res = ThzCompilerDriver.compilarOuExecutar(src, ThzCompilerDriver.Alvo.LLVM, false, Map.of());
        assertTrue(res.sucesso(), "driver.thz deve gerar LLVM IR com sucesso: " + res.erros());
        assertNotNull(res.saidaTexto());
        assertTrue(res.saidaTexto().contains("ModuleID"), "Saída LLVM IR deve conter ModuleID");
    }
}

