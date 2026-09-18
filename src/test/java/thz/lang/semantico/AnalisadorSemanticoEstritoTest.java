package thz.lang.semantico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AnalisadorSemanticoEstritoTest {

    private ProgramaAst parse(String codigo) {
        return new ThzParser(new ThzLexer(codigo).tokenize()).parse();
    }

    @Test
    @DisplayName("AnalisadorSemantico em modo estrito deve alertar ausência de metadados ou SLO de latência")
    void testModoEstritoMetadados() {
        String src = """
                PROGRAMA SemMetadados
                REGRA_NEGOCIO Calculo
                    OPERACAO Somar() : INTEIRO32
                    INICIO
                        RETORNE 10
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;

        ProgramaAst ast = parse(src);
        AnalisadorSemantico semantico = new AnalisadorSemantico(ast);
        List<ErroSemantico> erros = semantico.analisar(new OpcoesAnalise(true));

        // Em modo estrito deve apontar aviso/erro sobre metadados ou rastreio
        assertFalse(erros.isEmpty());
    }

    @Test
    @DisplayName("Modo reduzido deve aceitar ferramenta sem metadados arquiteturais")
    void testModoReduzidoSemMetadados() {
        String src = """
                FERRAMENTA Utilitario
                PROCEDIMENTO Principal()
                INICIO
                    EXIBA "ok"
                FIM
                FIM_FERRAMENTA
                """;

        List<ErroSemantico> erros = new AnalisadorSemantico(parse(src)).analisar(new OpcoesAnalise(false));
        assertTrue(erros.isEmpty(), "Ferramentas em modo reduzido não devem exigir metadados: " + erros);
    }

    @Test
    @DisplayName("AnalisadorSemantico deve validar tipagem de expresses aritmticas e atribuies")
    void testTipagemExpressoes() {
        String srcValido = """
                PROGRAMA TipagemValida
                METADADOS_ARQUITETURA
                    DOMINIO: "Core"
                    CAMADA: "Dominio"
                    VERSAO: "1.0.0"
                    AUTOR: "Engenharia"
                    SLO_LATENCIA_MAXIMA: "50ms"
                FIM_METADADOS
                REGRA_NEGOCIO Matematica
                    CONTRATO_ENTRADA
                        EXIGE a > 0
                    FIM_CONTRATO_ENTRADA
                    OPERACAO Multiplicar(a : INTEIRO32, b : INTEIRO32) : INTEIRO32
                    INICIO
                        VARIAVEL res <- a * b
                        RETORNE res
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;

        ProgramaAst ast = parse(srcValido);
        AnalisadorSemantico semantico = new AnalisadorSemantico(ast);
        List<ErroSemantico> erros = semantico.analisar(new OpcoesAnalise(false));
        assertTrue(erros.isEmpty(), "Não deve conter erros semânticos: " + erros);
    }
}
