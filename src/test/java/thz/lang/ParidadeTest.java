package thz.lang;

import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.lexico.TokenType;
import thz.lang.sintatico.ThzParser;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.formato.Formatador;
import thz.lang.runtime.DecimalFixo;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.DataHoraThz;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ParidadeTest {

    @Test
    public void lexerBasico() {
        var tokens = new ThzLexer("PROGRAMA X\nVARIAVEL a : TEXTO <- \"ola\"\nFIM_PROGRAMA").tokenize();
        assertTrue(tokens.stream().anyMatch(t -> t.type()==TokenType.PROGRAMA));
        assertTrue(tokens.stream().anyMatch(t -> t.type()==TokenType.VARIAVEL));
    }

    @Test
    public void parserProgramaMinimo() {
        var ast = parse("PROGRAMA Demo\nFIM_PROGRAMA");
        assertEquals("Demo", ast.nome());
    }

    @Test
    public void decimalBancario() {
        var d = DecimalFixo.deTexto("1.005", 3).paraEscala(2);
        assertEquals("1.00", d.formatar());
        var d2 = DecimalFixo.deTexto("1.015", 3).paraEscala(2);
        assertEquals("1.02", d2.formatar());
    }

    @Test
    public void dataHojeConsistente() {
        var d = DataThz.deComponentes(2026, 8, 23);
        assertEquals("2026-08-23", d.formatar());
        var dh = DataHoraThz.deComponentes(2026,8,24,14,0);
        assertEquals("2026-08-24T14:00", dh.formatar());
    }

    @Test
    public void exemplosPassamNoCheck() throws Exception {
        for (String ex : List.of("exemplos/faturamento.thz","exemplos/pedidos.thz","exemplos/agenda.thz")) {
            String fonte = Files.readString(Path.of(ex), StandardCharsets.UTF_8);
            var tokens = new ThzLexer(fonte).tokenize();
            var ast = new ThzParser(tokens).parse();
            var erros = new AnalisadorSemantico(ast).analisar();
            assertTrue(erros.isEmpty(), "erros em " + ex + ": " + erros);
        }
    }

    @Test
    public void galeriaExemplosValida() throws Exception {
        // Toda a coleção de partida (menu Exemplos da GUI) deve parsear e validar.
        Path colecao = Path.of("exemplos", "colecao");
        assertTrue(Files.isDirectory(colecao), "pasta exemplos/colecao ausente");
        try (var arquivos = Files.list(colecao)) {
            var thzs = arquivos.filter(p -> p.getFileName().toString().endsWith(".thz")).sorted().toList();
            assertTrue(thzs.size() >= 10, "esperados >= 10 exemplos na coleção, encontrados " + thzs.size());
            for (Path p : thzs) {
                String fonte = Files.readString(p, StandardCharsets.UTF_8);
                var tokens = new ThzLexer(fonte).tokenize();
                var ast = new ThzParser(tokens).parse();
                var erros = new AnalisadorSemantico(ast).analisar();
                assertTrue(erros.isEmpty(), "erros em " + p + ": " + erros);
                // formato canônico idempotente para cada exemplo
                String f1 = Formatador.formatar(ast);
                String f2 = Formatador.formatar(parse(f1));
                assertEquals(f1, f2, "fmt não idempotente em " + p);
            }
        }
    }

    @Test
    public void lexerToleraBomUtf8() {
        String comBom = "\uFEFFPROGRAMA Demo\nFIM_PROGRAMA";
        var ast = parse(comBom);
        assertEquals("Demo", ast.nome());
    }

    @Test
    public void fmtIdempotente() throws Exception {
        String fonte = Files.readString(Path.of("exemplos/agenda.thz"), StandardCharsets.UTF_8);
        var ast = parse(fonte);
        String f1 = Formatador.formatar(ast);
        var ast2 = parse(f1);
        String f2 = Formatador.formatar(ast2);
        assertEquals(f1, f2, "fmt deve ser idempotente");
    }

    private ProgramaAst parse(String src) {
        List<Token> t = new ThzLexer(src).tokenize();
        return new ThzParser(t).parse();
    }
}
