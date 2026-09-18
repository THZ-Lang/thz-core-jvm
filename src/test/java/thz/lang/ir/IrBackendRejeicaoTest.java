package thz.lang.ir;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.driver.ThzCompilerDriver;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class IrBackendRejeicaoTest {

    private ProgramaAst parsear(String fonte) {
        return new ThzParser(new ThzLexer(fonte).tokenize()).parse();
    }

    @Test
    @DisplayName("Deve diagnosticar comandos sem lowering no backend LLVM em modo estrito")
    void testDiagnosticarLimitacoesLlvm() {
        String fonte = """
                PROGRAMA TesteNaoSuportado
                METADADOS_ARQUITETURA
                    DOMINIO: "Teste"
                    CAMADA: "Infra"
                    SLO_LATENCIA_MAXIMA: "10ms"
                FIM_METADADOS
                PROCEDIMENTO Executar()
                INICIO
                    TENTE
                        EXIBA "ola"
                    CAPTURE FalhaSistema
                        EXIBA "erro"
                    FIM_TENTE
                FIM
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parsear(fonte);

        List<String> limitacoes = GeradorIr.validarCapacidadeLlvm(ast);
        assertFalse(limitacoes.isEmpty());
        assertTrue(limitacoes.stream().anyMatch(l -> l.contains("TENTE/CAPTURE")));

        // Em modo estrito, deve lançar exceção explícita
        assertThrows(ErroEmissaoBackendException.class, () -> GeradorIr.emitirLlvm(ast, true));

        // Pelo driver em modo estrito, deve retornar erro semântico de backend
        var resultado = ThzCompilerDriver.compilarOuExecutar(fonte, ThzCompilerDriver.Alvo.LLVM, true, Map.of());
        assertFalse(resultado.sucesso());
        assertTrue(resultado.erros().stream().anyMatch(e -> e.mensagem().contains("[Backend LLVM]")));
    }

    @Test
    @DisplayName("Deve emitir aviso no IR quando executado em modo padrão permissivo")
    void testAvisoEmModoPermissivo() {
        String fonte = """
                PROGRAMA TesteAviso
                PROCEDIMENTO Executar()
                INICIO
                    TENTE
                        EXIBA "ola"
                    CAPTURE FalhaSistema
                        EXIBA "erro"
                    FIM_TENTE
                FIM
                FIM_PROGRAMA
                """;
        ProgramaAst ast = parsear(fonte);
        String llvm = GeradorIr.emitirLlvm(ast, false);
        assertNotNull(llvm);
        assertTrue(llvm.contains("AVISO AOT: comando Tente"));
    }
}
