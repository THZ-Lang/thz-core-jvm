package thz.lang.interpretador;

import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.formato.Formatador;
import thz.lang.ir.GeradorIr;
import thz.lang.js.ThzJsEmitter;
import thz.lang.lexico.ThzLexer;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.sintatico.ThzParser;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuncaoTest {

    private static ProgramaAst parse(String fonte) {
        return new ThzParser(new ThzLexer(fonte).tokenize()).parse();
    }

    @Test
    void deveAnalisarExecutarEFormatarFuncaoTipada() {
        String fonte = """
                PROGRAMA Funcoes
                FUNCAO somar(a: INTEIRO32, b: INTEIRO32): INTEIRO32
                    RETORNE a + b
                FIM_FUNCAO

                PROCEDIMENTO Principal()
                INICIO
                    VARIAVEL resultado: INTEIRO32 <- somar(20, 22)
                    EXIBA resultado
                FIM
                FIM_PROGRAMA
                """;

        ProgramaAst ast = parse(fonte);
        assertEquals(1, ast.funcoes().size());
        assertEquals("somar", ast.funcoes().getFirst().nome());
        assertTrue(new AnalisadorSemantico(ast).analisar().isEmpty());

        List<String> saida = new ArrayList<>();
        new InterpretadorThz(ast, saida::add, () -> "").executarProcedimento("Principal");
        assertEquals(List.of("42"), saida);

        String formatado = Formatador.formatar(ast);
        assertTrue(formatado.contains("FUNCAO somar(a: INTEIRO32, b: INTEIRO32): INTEIRO32"));
        assertTrue(formatado.contains("FUNCAO somar(a: INTEIRO32, b: INTEIRO32): INTEIRO32 = a + b"));
        assertEquals(formatado, Formatador.formatar(parse(formatado)));
        assertTrue(GeradorIr.baixarParaIr(ast).funcoes().stream().anyMatch(f -> f.nome().equals("somar")));
        assertTrue(ThzJsEmitter.emitir(ast).contains("function somar(a, b)"));
    }

    @Test
    void deveAceitarFuncaoDeExpressaoSemBloco() {
        String fonte = """
                PROGRAMA FuncaoExpressao
                FUNCAO dobrar(valor: INTEIRO32): INTEIRO32 = valor * 2
                PROCEDIMENTO Principal()
                INICIO
                    EXIBA dobrar(21)
                FIM
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parse(fonte);
        assertTrue(new AnalisadorSemantico(ast).analisar().isEmpty());
        List<String> saida = new ArrayList<>();
        new InterpretadorThz(ast, saida::add, () -> "").executarProcedimento("Principal");
        assertEquals(List.of("42"), saida);
        assertTrue(Formatador.formatar(ast).contains("FUNCAO dobrar(valor: INTEIRO32): INTEIRO32 = valor * 2"));
        String llvm = GeradorIr.emitirLlvm(ast);
        assertTrue(llvm.contains("define i32 @dobrar(i32 %valor)"));
        assertTrue(llvm.contains("ret i32 0"), "expressão aritmética dependente de parâmetro aguarda lowering geral");
    }

    @Test
    void deveEmitirAritmeticaConstanteNoLlvm() {
        ProgramaAst ast = parse("PROGRAMA Compacto\nFUNCAO soma(): INTEIRO32 = 2 + 3\nFIM_PROGRAMA");
        String llvm = GeradorIr.emitirLlvm(ast);
        assertTrue(llvm.contains("define i32 @soma()"));
        assertTrue(llvm.contains("%ret = add i32 2, 3"));
        assertTrue(llvm.contains("ret i32 %ret"));
    }

    @Test
    void deveEmitirRetornoDiretoDeParametroNoLlvm() {
        ProgramaAst ast = parse("PROGRAMA Compacto\nFUNCAO identidade(valor: INTEIRO32): INTEIRO32 = valor\nFIM_PROGRAMA");
        assertTrue(GeradorIr.emitirLlvm(ast).contains("ret i32 %valor"));
    }
}
