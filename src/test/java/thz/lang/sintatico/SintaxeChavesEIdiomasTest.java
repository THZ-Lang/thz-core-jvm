package thz.lang.sintatico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.fachada.ThzCompilerFacade;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.DialetoLinguagem;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SintaxeChavesEIdiomasTest {

    @Test
    @DisplayName("Deve analisar programa corporativo completo utilizando blocos com chaves '{ }'")
    void testProgramaComChaves() {
        String codigo = """
                PROGRAMA SistemaModerno {
                    METADADOS_ARQUITETURA {
                        DOMINIO: "Logistica"
                        CAMADA: "Aplicacao"
                        VERSAO: "1.0.0"
                        AUTOR: "THZ Core Team"
                        SLO_LATENCIA_MAXIMA: "50ms"
                    }
                
                    ESTRUTURA Item {
                        codigo: TEXTO
                        quantidade: INTEIRO
                    }
                
                    REGRA ProcessamentoItem {
                        IDENTIFICADOR_REGRA: "REG-001"
                        RASTREIO_REQUISITO: "REQ-ITEM-001"
                        EXIGE qtd != 0
                        OPERACAO Validar(qtd: INTEIRO): BOOLEANO {
                            SE qtd > 0 {
                                RETORNE VERDADEIRO
                            } SENAO {
                                RETORNE FALSO
                            }
                        }
                    }
                
                    FUNCAO Multiplicar(a: INTEIRO, b: INTEIRO): INTEIRO {
                        RETORNE a * b
                    }
                
                    PROCEDIMENTO Executar() {
                        VARIAVEL total <- 0
                        PARA i DE 1 ATE 5 {
                            total <- total + i
                        }
                    }
                }
                """;

        var analise = ThzCompilerFacade.analisar(codigo, true);
        assertFalse(analise.temErros(), "Compilação com chaves não deve gerar erros: " + analise.textoDiagnosticos());
        ProgramaAst ast = analise.ast();
        assertNotNull(ast);
        assertEquals("SistemaModerno", ast.nome());
        assertEquals(1, ast.estruturas().size());
        assertEquals("Item", ast.estruturas().get(0).nome());
        assertEquals(1, ast.regras().size());
        assertEquals("ProcessamentoItem", ast.regras().get(0).nome());
        assertEquals(1, ast.funcoes().size());
        assertEquals("Multiplicar", ast.funcoes().get(0).nome());
        assertEquals(1, ast.procedimentos().size());
        assertEquals("Executar", ast.procedimentos().get(0).nome());

        // Validação da execução da regra com chaves
        InterpretadorThz interp = new InterpretadorThz(ast);
        ValorThz resTrue = interp.executarOperacao("Validar", Map.of("qtd", ValorThz.INTEIRO(10)));
        assertTrue(((ValorThz.Logico) resTrue).valor());

        ValorThz resFalse = interp.executarOperacao("Validar", Map.of("qtd", ValorThz.INTEIRO(-1)));
        assertFalse(((ValorThz.Logico) resFalse).valor());
    }

    @Test
    @DisplayName("Deve reconhecer cabeçalho canônico 'lang ptbr' e palavra compacta 'regra'")
    void testCabecalhoLangPtBr() {
        String codigo = """
                lang ptbr
                
                PROGRAMA FaturamentoCompacto {
                    METADADOS_ARQUITETURA {
                        DOMINIO: "Financeiro"
                        VERSAO: "1.0.0"
                        AUTOR: "Financeiro"
                        SLO_LATENCIA_MAXIMA: "100ms"
                    }
                
                    regra CalculoImposto {
                        IDENTIFICADOR_REGRA: "REG-IMP-001"
                        RASTREIO_REQUISITO: "REQ-TAX-01"
                        EXIGE base >= 0.0000
                        OPERACAO Calcular(base: DECIMAL): DECIMAL {
                            RETORNE base * 0.1000
                        }
                    }
                }
                """;

        var analise = ThzCompilerFacade.analisar(codigo, true);
        assertFalse(analise.temErros(), "Erros encontrados: " + analise.textoDiagnosticos());
        assertEquals(DialetoLinguagem.PT_BR, analise.ast().dialeto());
        assertEquals(1, analise.ast().regras().size());
        assertEquals("CalculoImposto", analise.ast().regras().get(0).nome());
    }

    @Test
    @DisplayName("Deve reconhecer cabeçalho canônico 'lang enus' e palavra compacta 'rule' com chaves")
    void testCabecalhoLangEnUs() {
        String codigo = """
                lang enus
                
                PROGRAM InvoiceProcessor {
                    ARCHITECTURE_METADATA {
                        DOMAIN: "Billing"
                        VERSION: "2.0.0"
                        AUTHOR: "Billing Team"
                        MAX_LATENCY_SLO: "100ms"
                    }
                
                    STRUCTURE InvoiceItem {
                        sku: TEXT
                        qty: INTEGER
                    }
                
                    rule DiscountRule {
                        RULE_ID: "DISC-001"
                        REQUIREMENT_TRACE: "REQ-DISC-001"
                        REQUIRES price >= 0.0000
                        OPERATION ApplyDiscount(price: DECIMAL): DECIMAL {
                            IF price > 100.0000 {
                                RETURN price * 0.9000
                            } ELSE {
                                RETURN price
                            }
                        }
                    }
                }
                """;

        var analise = ThzCompilerFacade.analisar(codigo, true);
        assertFalse(analise.temErros(), "Erros encontrados no modo en-US: " + analise.textoDiagnosticos());
        assertEquals(DialetoLinguagem.EN_US, analise.ast().dialeto());
        assertEquals("InvoiceProcessor", analise.ast().nome());
        assertEquals(1, analise.ast().regras().size());
        assertEquals("DiscountRule", analise.ast().regras().get(0).nome());
    }

    @Test
    @DisplayName("Deve rejeitar idioma ou dialeto desconhecido com posição exata")
    void testRejeitarIdiomaDesconhecido() {
        String codigo = """
                lang espanhol
                
                PROGRAMA Invalido
                FIM_PROGRAMA
                """;

        var analise = ThzCompilerFacade.analisar(codigo, false);
        assertTrue(analise.temErros());
        assertTrue(analise.diagnosticos().get(0).mensagem().contains("Idioma ou dialeto não reconhecido"));
        assertEquals(1, analise.diagnosticos().get(0).linha());
    }

    @Test
    @DisplayName("Deve rejeitar mistura de palavras-chave entre dialetos")
    void testRejeitarMisturaDialetos() {
        String codigo = """
                lang ptbr
                
                PROGRAM Errado {
                }
                """;

        var analise = ThzCompilerFacade.analisar(codigo, false);
        assertTrue(analise.temErros());
        assertTrue(analise.diagnosticos().get(0).mensagem().contains("dialeto [en-US]"));
    }
}
