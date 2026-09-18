package thz.lang.interpretador;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CasoResultadoTest {

    private ProgramaAst parse(String codigo) {
        return new ThzParser(new ThzLexer(codigo).tokenize()).parse();
    }

    @Test
    @DisplayName("CASO_RESULTADO deve desempacotar canal de SUCESSO corretamente")
    void testCasoResultadoSucesso() {
        String src = """
                PROGRAMA TesteResultadoSucesso
                REGRA_NEGOCIO Calculadora
                    OPERACAO Dividir(a : INTEIRO64, b : INTEIRO64) : RESULTADO[INTEIRO64, TEXTO]
                    INICIO
                        SE b = 0
                            FALHAR_COM "Divisao por zero"
                        FIM_SE
                        RETORNE a / b
                    FIM

                    OPERACAO Executar() : INTEIRO64
                    INICIO
                        VARIAVEL res <- Dividir(10, 2)
                        VARIAVEL out <- 0
                        CASO_RESULTADO res
                            SUCESSO(v) -> out <- v
                            ERRO(err) -> EXIBA err
                        FIM_CASO
                        RETORNE out
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;

        ProgramaAst ast = parse(src);
        List<String> saidas = new ArrayList<>();
        InterpretadorThz interp = new InterpretadorThz(ast, saidas::add, () -> "");
        ValorThz ret = interp.executarOperacao("Executar", Map.of());

        assertTrue(ret instanceof ValorThz.Inteiro, "Retorno deve ser inteiro");
        assertEquals(5L, ((ValorThz.Inteiro) ret).valor().longValue());
    }

    @Test
    @DisplayName("CASO_RESULTADO deve desempacotar canal de ERRO corretamente")
    void testCasoResultadoErro() {
        String src = """
                PROGRAMA TesteResultadoErro
                REGRA_NEGOCIO Calculadora
                    OPERACAO Dividir(a : INTEIRO64, b : INTEIRO64) : RESULTADO[INTEIRO64, TEXTO]
                    INICIO
                        SE b = 0
                            FALHAR_COM "ErroDivisaoPorZero"
                        FIM_SE
                        RETORNE a / b
                    FIM

                    OPERACAO Executar() : TEXTO
                    INICIO
                        VARIAVEL res <- Dividir(10, 0)
                        VARIAVEL msg <- "Sem erro"
                        CASO_RESULTADO res
                            SUCESSO(v) -> msg <- "Deu certo"
                            ERRO(err) -> msg <- err
                        FIM_CASO
                        RETORNE msg
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;

        ProgramaAst ast = parse(src);
        InterpretadorThz interp = new InterpretadorThz(ast, s -> {}, () -> "");
        ValorThz ret = interp.executarOperacao("Executar", Map.of());

        assertTrue(ret instanceof ValorThz.Texto, "Retorno deve ser texto");
        assertEquals("ErroDivisaoPorZero", ((ValorThz.Texto) ret).valor());
    }

    @Test
    @DisplayName("ESCOLHA deve aceitar a sintaxe moderna para RESULTADO")
    void testEscolhaSintaxeModerna() {
        String src = """
                PROGRAMA EscolhaModerna
                REGRA_NEGOCIO Processamento
                    OPERACAO Falhar() : RESULTADO[INTEIRO64, TEXTO]
                    INICIO
                        FALHAR_COM "indisponivel"
                    FIM
                    OPERACAO Executar() : TEXTO
                    INICIO
                        VARIAVEL resultado <- Falhar()
                        ESCOLHA resultado
                            CASO SUCESSO(valor) -> RETORNE "ok"
                            CASO FALHA(erro) -> RETORNE erro
                        FIM_ESCOLHA
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;

        InterpretadorThz interp = new InterpretadorThz(parse(src), s -> {}, () -> "");
        ValorThz retorno = interp.executarOperacao("Executar", Map.of());
        assertEquals("indisponivel", ((ValorThz.Texto) retorno).valor());
    }

    @Test
    @DisplayName("TENTE deve capturar FALHAR_COM e preservar o fluxo explícito")
    void testTenteCapture() {
        String src = """
                PROGRAMA TenteModerno
                REGRA_NEGOCIO Processamento
                    OPERACAO Executar() : TEXTO
                    INICIO
                        VARIAVEL mensagem: TEXTO <- "inicial"
                        TENTE
                            FALHAR_COM "indisponivel"
                        CAPTURE ErroProcessamento
                            mensagem <- "recuperado"
                        FIM_TENTE
                        RETORNE mensagem
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;
        ValorThz retorno = new InterpretadorThz(parse(src), s -> {}, () -> "").executarOperacao("Executar", Map.of());
        assertEquals("recuperado", ((ValorThz.Texto) retorno).valor());
    }
}
