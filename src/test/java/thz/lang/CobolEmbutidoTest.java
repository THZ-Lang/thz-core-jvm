package thz.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.fachada.ThzCompilerFacade;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import static org.junit.jupiter.api.Assertions.*;

class CobolEmbutidoTest {

    @Test
    @DisplayName("Deve fazer parsing de BLOCO_COBOL e PROGRAM-ID sem acusar conflito de dialeto em arquivo pt-BR")
    void testBlocoCobolEmbutidoDialetoPtBr() {
        String codigo = """
            PROGRAMA DemonstracaoCobolInterop
            METADADOS_ARQUITETURA
                DOMINIO: "ModernizacaoFinanceira"
                SUBDOMINIO: "InteropCOBOL"
                CAMADA: "Dominio"
                VERSAO: "0.1.0"
                AUTOR: "THZ-LANG Architecture Team"
                SLO_LATENCIA_MAXIMA: "2ms"
            FIM_METADADOS

            BLOCO_COBOL
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CALC-TARIFA.
                DATA DIVISION.
                LINKAGE SECTION.
                01 LK-SALDO     PIC S9(11)V99 COMP-3.
                01 LK-TARIFA    PIC S9(7)V99 COMP-3.
                PROCEDURE DIVISION USING LK-SALDO LK-TARIFA.
                    IF LK-SALDO < 1000.00
                        MOVE 25.50 TO LK-TARIFA
                    ELSE
                        MOVE 10.00 TO LK-TARIFA
                    END-IF.
                    GOBACK.
            FIM_BLOCO_COBOL

            IMPORTAR_COPYBOOK "CLIENTE_BANCO.CPY"

            ESTRUTURA RegistroClienteBanco
                cli_id : INTEIRO32
                cli_nome : TEXTO
                cli_saldo_conta : DECIMAL
            FIM_ESTRUTURA

            REGRA_NEGOCIO ProcessamentoContasBancarias
                IDENTIFICADOR_REGRA: "RN-BANCO-001"
                DESCRICAO: "Executa cálculo de juros e tarifas combinando dados THZ e rotinas COBOL."

                OPERACAO ProcessarConta() : LOGICO
                INICIO
                    VARIAVEL tarifaDevida : DECIMAL <- 0.00
                    COBOL.CALC_TARIFA(850.00, tarifaDevida)
                    EXIBA "Tarifa: " + TEXTO.deDecimal(tarifaDevida)
                    RETORNE VERDADEIRO
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        var resultado = ThzCompilerFacade.analisar(codigo, false);
        assertFalse(resultado.temErros(), () -> "Diagnósticos inesperados: " + resultado.diagnosticos());
        assertNotNull(resultado.ast());

        ProgramaAst ast = resultado.ast();
        assertNotNull(ast.blocosCobol());
        assertEquals(1, ast.blocosCobol().size());
        assertTrue(ast.blocosCobol().get(0).contains("PROGRAM-ID. CALC-TARIFA."));
        assertTrue(ast.blocosCobol().get(0).contains("MOVE 25.50 TO LK-TARIFA"));

        assertNotNull(ast.importacoesCopybook());
        assertEquals(1, ast.importacoesCopybook().size());
        assertEquals("CLIENTE_BANCO.CPY", ast.importacoesCopybook().get(0));
    }

    @Test
    @DisplayName("Deve fazer parsing de COBOL_BLOCK em dialeto EN-US")
    void testCobolBlockDialetoEnUs() {
        String codigoEn = """
            lang enus
            PROGRAM ModernBankingInterop
            METADATA
                DOMAIN: "CoreBanking"
                VERSION: "1.0.0"
                AUTHOR: "FinTech"
            END_METADATA

            COBOL_BLOCK
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CHECK-LIMIT.
                PROCEDURE DIVISION.
                    GOBACK.
            END_COBOL_BLOCK

            IMPORT_COPYBOOK "ACCOUNT_SCHEMA.CPY"

            BUSINESS_RULE LimitRule
                RULE_ID: "BR-LIMIT-001"
                DESCRIPTION: "Check limit"
                OPERATION Check() : BOOLEAN
                BEGIN
                    RETURN TRUE
                END_OPERATION
            END_BUSINESS_RULE
            END_PROGRAM
            """;

        var tokens = new ThzLexer(codigoEn).tokenize();
        var parser = new ThzParser(tokens);
        ProgramaAst ast = parser.parse();

        assertNotNull(ast.blocosCobol());
        assertEquals(1, ast.blocosCobol().size());
        assertTrue(ast.blocosCobol().get(0).contains("CHECK-LIMIT"));
        assertEquals(1, ast.importacoesCopybook().size());
        assertEquals("ACCOUNT_SCHEMA.CPY", ast.importacoesCopybook().get(0));
    }

    @Test
    @DisplayName("Deve analisar com sucesso o arquivo demo_cobol_interop.thz real")
    void testArquivoDemoCobolInteropReal() throws Exception {
        java.nio.file.Path caminho = java.nio.file.Path.of("../thz-lang-cobol/exemplos/thz_com_cobol/demo_cobol_interop.thz");
        if (java.nio.file.Files.exists(caminho)) {
            String conteudo = java.nio.file.Files.readString(caminho);
            var resultado = ThzCompilerFacade.analisar(conteudo, false);
            assertFalse(resultado.temErros(), () -> "Erros no demo real: " + resultado.diagnosticos());
            assertNotNull(resultado.ast());
            assertEquals(1, resultado.ast().blocosCobol().size());
        }
    }
}
