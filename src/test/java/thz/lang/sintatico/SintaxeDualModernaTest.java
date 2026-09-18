package thz.lang.sintatico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ComandoAst;
import thz.lang.ast.EstruturaAst;
import thz.lang.ast.FuncaoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.ast.RegraNegocioAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.semantico.AnalisadorSemantico;

import static org.junit.jupiter.api.Assertions.*;

public class SintaxeDualModernaTest {

    private ProgramaAst parse(String codigo) {
        ThzLexer lexer = new ThzLexer(codigo);
        ThzParser parser = new ThzParser(lexer.tokenize(), lexer.getDialeto());
        return parser.parse();
    }

    @Test
    @DisplayName("Deve fazer parsing de programa com estilo Kotlin/Rust Corporativo (chaves, var, fn, struct, =)")
    void deveFazerParsingEstiloKotlinRustCorporativo() {
        String codigo = """
            programa GestaoPedidos {
                metadados {
                    DOMINIO: "Logistica"
                    VERSAO: "2.5.0"
                    AUTOR: "THZ Team"
                }

                struct Item {
                    id: Int,
                    descricao: String,
                    quantidade: Int32,
                    preco: Decimal(12, 2)
                }

                fn calcularSubtotal(qtd: Int32, preco: Decimal(12, 2)): Decimal(12, 2) = qtd * preco

                regra ProcessarPedido {
                    exige total > 0
                    garante total > 0

                    operacao aplicarDesconto(total: Decimal(12, 2), taxa: Decimal(5, 2)): Decimal(12, 2) {
                        var desconto = total * taxa
                        var liquido: Decimal(12, 2) = total - desconto
                        se liquido < 0 {
                            liquido = 0
                        }
                        retorne liquido
                    }
                }

                fn main(): Int {
                    var x = 10
                    val limite: Int = 100
                    let multiplicador = 2
                    x = x * multiplicador

                    se x == 20 {
                        print "x atingiu o valor esperado"
                    } senao {
                        print "valor divergente"
                    }

                    retorne x
                }
            }
            """;

        ProgramaAst ast = parse(codigo);
        assertNotNull(ast);
        assertEquals("GestaoPedidos", ast.nome());
        assertNotNull(ast.metadados());
        assertEquals("Logistica", ast.metadados().dominio());

        // Estrutura
        assertEquals(1, ast.estruturas().size());
        EstruturaAst item = ast.estruturas().get(0);
        assertEquals("Item", item.nome());
        assertEquals(4, item.campos().size());
        assertEquals("INTEIRO", item.campos().get(0).tipo());
        assertEquals("TEXTO", item.campos().get(1).tipo());
        assertEquals("INTEIRO32", item.campos().get(2).tipo());
        assertEquals("DECIMAL(12,2)", item.campos().get(3).tipo());

        // Funções de topo
        assertEquals(2, ast.funcoes().size());
        FuncaoAst fnSubtotal = ast.funcoes().get(0);
        assertEquals("calcularSubtotal", fnSubtotal.nome());
        assertEquals("DECIMAL(12,2)", fnSubtotal.tipoRetorno());
        assertEquals(1, fnSubtotal.corpo().size());
        assertTrue(fnSubtotal.corpo().get(0) instanceof ComandoAst.Retorne);

        // Regra de Negócio
        assertEquals(1, ast.regras().size());
        RegraNegocioAst regra = ast.regras().get(0);
        assertEquals("ProcessarPedido", regra.nome());
        assertEquals(1, regra.clausulasEntrada().size());
        assertEquals(1, regra.clausulasSaida().size());
        assertEquals(1, regra.operacoes().size());

        // Validação semântica estrita
        AnalisadorSemantico semantico = new AnalisadorSemantico(ast);
        var erros = semantico.analisar();
        assertTrue(erros.isEmpty(), "Não deve haver erros semânticos: " + erros);
    }

    @Test
    @DisplayName("Deve suportar fn dentro de REGRA_NEGOCIO com corpo de expressão = e blocos { }")
    void deveSuportarFnDentroDeRegraComExpressaoOuBloco() {
        String codigo = """
            programa NEGOCIO Validador {
                regra RegraCalculo {
                    exige base > 0

                    fn dobrar(base: Int): Int = base * 2

                    fn triplicar(base: Int): Int {
                        var res: Int = base * 3
                        retorne res
                    }
                }
            }
            """;

        ProgramaAst ast = parse(codigo);
        assertEquals(1, ast.regras().size());
        var operacoes = ast.regras().get(0).operacoes();
        assertEquals(2, operacoes.size());
        assertEquals("dobrar", operacoes.get(0).nome());
        assertEquals("triplicar", operacoes.get(1).nome());
    }

    @Test
    @DisplayName("Deve aceitar laços para e enquanto com blocos { } e operadores modernos")
    void deveAceitarLacosComChaves() {
        String codigo = """
            programa LoopsModernos {
                fn somarAte(n: Int): Int {
                    var soma: Int = 0
                    var i = 1
                    enquanto i <= n {
                        soma = soma + i
                        i = i + 1
                    }

                    para j de 1 ate 10 passo 2 {
                        soma = soma + j
                    }

                    retorne soma
                }
            }
            """;

        ProgramaAst ast = parse(codigo);
        assertEquals(1, ast.funcoes().size());
        var corpo = ast.funcoes().get(0).corpo();
        assertTrue(corpo.stream().anyMatch(c -> c instanceof ComandoAst.Enquanto));
        assertTrue(corpo.stream().anyMatch(c -> c instanceof ComandoAst.Para));
    }
}
