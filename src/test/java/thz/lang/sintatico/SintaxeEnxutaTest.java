package thz.lang.sintatico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.ast.TipoModulo;
import thz.lang.formato.Formatador;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.ir.GeradorIr;
import thz.lang.js.ThzJsEmitter;
import thz.lang.lexico.ErroLexico;
import thz.lang.lexico.SintaxeEnxuta;
import thz.lang.lexico.ThzLexer;
import thz.lang.semantico.AnalisadorSemantico;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;

class SintaxeEnxutaTest {

    private ProgramaAst parse(String fonte) {
        return new ThzParser(new ThzLexer(fonte).tokenize()).parse();
    }

    @Test
    @DisplayName("Sintaxe enxuta deve cobrir todos os arquétipos especiais de módulo")
    void deveCobrirArquetiposEspeciaisDeModulo() {
        assertEquals(TipoModulo.EXTENSAO, parse("extensao Auditoria:\n").tipoModulo());
        assertEquals(TipoModulo.PIPELINE_DADOS, parse("pipeline_dados Importacao:\n").tipoModulo());
    }

    @Test
    @DisplayName("Sintaxe enxuta deve produzir a mesma AST estrutural da forma legada")
    void deveProduzirAstEquivalente() {
        String enxuta = """
                programa Calculo:
                    estrutura Item:
                        quantidade: INTEIRO
                        valor: DECIMAL(12, 2)

                    funcao subtotal(item: Item) -> DECIMAL(12, 2):
                        total := item.quantidade * item.valor
                        retorne total
                """;
        String legada = """
                PROGRAMA Calculo
                ESTRUTURA Item
                    quantidade: INTEIRO
                    valor: DECIMAL(12, 2)
                FIM_ESTRUTURA
                FUNCAO subtotal(item: Item): DECIMAL(12, 2)
                    VARIAVEL total <- item.quantidade * item.valor
                    RETORNE total
                FIM_FUNCAO
                FIM_PROGRAMA
                """;

        ProgramaAst moderna = parse(enxuta);
        ProgramaAst antiga = parse(legada);

        assertEquals(antiga.nome(), moderna.nome());
        assertEquals(antiga.estruturas(), moderna.estruturas());
        assertEquals(Formatador.formatar(antiga), Formatador.formatar(moderna),
                "As formas equivalentes devem produzir a mesma representação canônica da AST");
    }

    @Test
    @DisplayName("Sintaxe enxuta deve aceitar contratos corporativos sem blocos cerimoniais")
    void deveAceitarContratosDiretos() {
        String fonte = """
                programa NEGOCIO Faturamento:
                    regra EmitirFatura:
                        exige valor > 0
                        garante valor > 0
                        operacao emitir(valor: DECIMAL(12, 2)) -> DECIMAL(12, 2):
                            retorne valor
                """;

        ProgramaAst ast = parse(fonte);
        assertEquals(1, ast.regras().size());
        assertEquals(1, ast.regras().getFirst().clausulasEntrada().size());
        assertEquals(1, ast.regras().getFirst().clausulasSaida().size());
        assertEquals(1, ast.regras().getFirst().operacoes().size());
        assertTrue(new AnalisadorSemantico(ast).analisar().isEmpty());
    }

    @Test
    @DisplayName("Dessugarização deve preservar a linha do diagnóstico")
    void devePreservarLinhaDoDiagnostico() {
        String fonte = """
                programa Demo:
                    funcao dobro(valor: INTEIRO) -> INTEIRO:
                        @
                """;
        assertTrue(SintaxeEnxuta.detectar(fonte));
        ErroLexico erro = assertThrows(ErroLexico.class, () -> new ThzLexer(fonte).tokenize());
        assertTrue(erro.getMessage().contains("[Linha 3:9]"), erro.getMessage());
    }

    @Test
    @DisplayName("Validador deve impedir ambiguidades de indentação na forma enxuta")
    void deveDiagnosticarIndentacaoAmbigua() {
        String fonte = """
                programa Demo:
                      procedimento Principal():
                        \texiba "fora do nível"
                """;

        var diagnosticos = SintaxeEnxuta.validarIndentacao(fonte);

        assertFalse(diagnosticos.isEmpty());
        assertTrue(diagnosticos.stream().anyMatch(d -> d.mensagem().contains("múltiplos de 4 espaços")));
        assertTrue(diagnosticos.stream().anyMatch(d -> d.mensagem().contains("tabulação")));
        assertTrue(diagnosticos.stream().allMatch(d -> d.mensagem().startsWith("[Erro Sintático][Linha ")));
    }

    @Test
    @DisplayName("Formatador enxuto deve ser idempotente e eliminar terminadores")
    void formatadorEnxutoDeveSerIdempotente() {
        String legada = """
                PROGRAMA Demo
                FUNCAO dobro(valor: INTEIRO): INTEIRO
                    RETORNE valor * 2
                FIM_FUNCAO
                FIM_PROGRAMA
                """;
        String primeira = Formatador.formatarEnxuto(parse(legada));
        String segunda = Formatador.formatarEnxuto(parse(primeira));

        assertEquals(primeira, segunda);
        assertTrue(primeira.contains("funcao dobro(valor: INTEIRO) -> INTEIRO:"));
        assertFalse(primeira.contains("FIM_"));
    }

    @Test
    @DisplayName("Sintaxe enxuta deve executar e alimentar IR, LLVM e JavaScript")
    void deveExecutarEAlimentarBackends() {
        String fonte = """
                programa Funcoes:
                    funcao somar(a: INTEIRO32, b: INTEIRO32) -> INTEIRO32:
                        retorne a + b

                    procedimento Principal():
                        resultado: INTEIRO32 := somar(20, 22)
                        exiba resultado
                """;
        ProgramaAst ast = parse(fonte);
        List<String> saida = new ArrayList<>();
        new InterpretadorThz(ast, saida::add, () -> "").executarProcedimento("Principal");

        assertEquals(List.of("42"), saida);
        assertTrue(GeradorIr.baixarParaIr(ast).funcoes().stream().anyMatch(f -> f.nome().equals("somar")));
        assertTrue(GeradorIr.emitirLlvm(ast).contains("define i32 @somar"));
        assertTrue(ThzJsEmitter.emitir(ast).contains("function somar(a, b)"));
    }

    @Test
    @DisplayName("Formatador enxuto deve preservar blocos avançados")
    void devePreservarBlocosAvancados() {
        String fonte = """
                PROGRAMA Avancado
                REGRA_NEGOCIO Processamento
                    OPERACAO Executar(): TEXTO
                    INICIO
                        VARIAVEL mensagem: TEXTO <- "inicial"
                        TENTE
                            FALHAR_COM "erro"
                        CAPTURE ErroProcessamento
                            mensagem <- "recuperado"
                        FIM_TENTE
                        RETORNE mensagem
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;
        ProgramaAst original = parse(fonte);
        String enxuta = Formatador.formatarEnxuto(original);
        ProgramaAst reparsada = parse(enxuta);

        assertEquals(Formatador.formatar(original), Formatador.formatar(reparsada));
        assertTrue(enxuta.contains("TENTE:"));
        assertFalse(enxuta.contains("FIM_TENTE"));
    }
}
