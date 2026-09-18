package thz.lang.ir;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import static org.junit.jupiter.api.Assertions.*;

public class AotLoweringLlvmTest {

    private ProgramaAst parse(String fonte) {
        return new ThzParser(new ThzLexer(fonte).tokenize()).parse();
    }

    @Test
    @DisplayName("Deve emitir lowering LLVM para variáveis, aritmética e condicionais SE")
    void testLoweringVariaveisECondicional() {
        String fonte = """
                PROGRAMA TesteAotCalculo
                METADADOS_ARQUITETURA
                    DOMINIO: "Financas"
                    CAMADA: "Aplicacao"
                    SLO_LATENCIA_MAXIMA: "5ms"
                FIM_METADADOS
                PROCEDIMENTO Principal()
                INICIO
                    VARIAVEL a : INTEIRO <- 10
                    VARIAVEL b : INTEIRO <- 20
                    VARIAVEL soma : INTEIRO <- a + b
                    SE soma > 25
                        EXIBA "MAIOR"
                    SENAO
                        EXIBA "MENOR"
                    FIM_SE
                FIM
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parse(fonte);
        String llvm = GeradorIr.emitirLlvm(ast, true); // Modo estrito!

        assertNotNull(llvm);
        assertTrue(llvm.contains("define void @Principal()"));
        assertTrue(llvm.contains("alloca i64"), "Deve alocar variáveis locais");
        assertTrue(llvm.contains("store i64"), "Deve inicializar variáveis na memória");
        assertTrue(llvm.contains("add i64"), "Deve emitir adição aritmética");
        assertTrue(llvm.contains("icmp sgt i64"), "Deve emitir comparação relacional");
        assertTrue(llvm.contains("br i1"), "Deve emitir salto condicional");
        assertTrue(llvm.contains("call void @thz_exiba_str"), "Deve invocar exibição de string");
    }

    @Test
    @DisplayName("Deve emitir lowering LLVM para laço ENQUANTO com mutação de variável")
    void testLoweringLacoEnquanto() {
        String fonte = """
                PROGRAMA TesteAotLoop
                METADADOS_ARQUITETURA
                    DOMINIO: "Loops"
                    CAMADA: "Core"
                    SLO_LATENCIA_MAXIMA: "1ms"
                FIM_METADADOS
                PROCEDIMENTO Principal()
                INICIO
                    VARIAVEL contador : INTEIRO <- 1
                    ENQUANTO contador <= 5
                        EXIBA contador
                        contador <- contador + 1
                    FIM_ENQUANTO
                FIM
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parse(fonte);
        String llvm = GeradorIr.emitirLlvm(ast, true);

        assertNotNull(llvm);
        assertTrue(llvm.contains("enq.cond"));
        assertTrue(llvm.contains("enq.corpo"));
        assertTrue(llvm.contains("enq.fim"));
        assertTrue(llvm.contains("icmp sle i64"));
        assertTrue(llvm.contains("call void @thz_exiba_i64"));
    }

    @Test
    @DisplayName("Deve emitir lowering LLVM para laço PARA com passo")
    void testLoweringLacoPara() {
        String fonte = """
                PROGRAMA TesteAotPara
                METADADOS_ARQUITETURA
                    DOMINIO: "Loops"
                    CAMADA: "Core"
                    SLO_LATENCIA_MAXIMA: "1ms"
                FIM_METADADOS
                PROCEDIMENTO Principal()
                INICIO
                    PARA i DE 1 ATE 10 PASSO 2
                        EXIBA i
                    FIM_PARA
                FIM
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parse(fonte);
        String llvm = GeradorIr.emitirLlvm(ast, true);

        assertNotNull(llvm);
        assertTrue(llvm.contains("para.cond"));
        assertTrue(llvm.contains("para.corpo"));
        assertTrue(llvm.contains("para.fim"));
        assertTrue(llvm.contains("icmp sle i64"));
    }
}
