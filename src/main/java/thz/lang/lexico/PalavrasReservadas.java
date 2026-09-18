package thz.lang.lexico;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tabela e utilitários de palavras reservadas e conectivos do THZ-LANG.
 * Suporta dialeto primário PT-BR e dialeto internacional EN-US com estrita pureza sintática.
 */
public final class PalavrasReservadas {

    public static final String VERSAO_LINGUAGEM_ATUAL = thz.lang.version.ThzVersion.ATUAL.toString();

    public record EntradaPalavra(TokenType token, CategoriaPalavra categoria) {}

    // 1. Tabela Canônica PT-BR
    private static final Map<String, EntradaPalavra> TABELA_PT_BR = Map.ofEntries(
        Map.entry("PROGRAMA", new EntradaPalavra(TokenType.PROGRAMA, CategoriaPalavra.DECLARACAO)),
        Map.entry("VISUAL", new EntradaPalavra(TokenType.VISUAL, CategoriaPalavra.MODIFICADOR)),
        Map.entry("NEGOCIO", new EntradaPalavra(TokenType.NEGOCIO, CategoriaPalavra.MODIFICADOR)),
        Map.entry("ARQUITETURA", new EntradaPalavra(TokenType.ARQUITETURA, CategoriaPalavra.MODIFICADOR)),
        Map.entry("BIBLIOTECA", new EntradaPalavra(TokenType.BIBLIOTECA, CategoriaPalavra.DECLARACAO)),
        Map.entry("EXTENSAO", new EntradaPalavra(TokenType.EXTENSAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("FERRAMENTA", new EntradaPalavra(TokenType.FERRAMENTA, CategoriaPalavra.DECLARACAO)),
        Map.entry("TESTE", new EntradaPalavra(TokenType.TESTE, CategoriaPalavra.DECLARACAO)),
        Map.entry("TELA", new EntradaPalavra(TokenType.TELA, CategoriaPalavra.DECLARACAO)),
        Map.entry("METADADOS_ARQUITETURA", new EntradaPalavra(TokenType.METADADOS_ARQUITETURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("ESTRUTURA", new EntradaPalavra(TokenType.ESTRUTURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("ENUMERACAO", new EntradaPalavra(TokenType.ENUMERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("REGRA_NEGOCIO", new EntradaPalavra(TokenType.REGRA_NEGOCIO, CategoriaPalavra.DECLARACAO)),
        Map.entry("REGRA", new EntradaPalavra(TokenType.REGRA_NEGOCIO, CategoriaPalavra.DECLARACAO)),
        Map.entry("regra", new EntradaPalavra(TokenType.REGRA_NEGOCIO, CategoriaPalavra.DECLARACAO)),
        Map.entry("PROCEDIMENTO", new EntradaPalavra(TokenType.PROCEDIMENTO, CategoriaPalavra.DECLARACAO)),
        Map.entry("FUNCAO", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("OPERACAO", new EntradaPalavra(TokenType.OPERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("VARIAVEL", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("VERSAO_LINGUAGEM", new EntradaPalavra(TokenType.VERSAO_LINGUAGEM, CategoriaPalavra.DECLARACAO)),
        Map.entry("FIM_PROGRAMA", new EntradaPalavra(TokenType.FIM_PROGRAMA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_BIBLIOTECA", new EntradaPalavra(TokenType.FIM_BIBLIOTECA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_EXTENSAO", new EntradaPalavra(TokenType.FIM_EXTENSAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_FERRAMENTA", new EntradaPalavra(TokenType.FIM_FERRAMENTA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_TESTE", new EntradaPalavra(TokenType.FIM_TESTE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_TELA", new EntradaPalavra(TokenType.FIM_TELA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_METADADOS", new EntradaPalavra(TokenType.FIM_METADADOS, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_ESTRUTURA", new EntradaPalavra(TokenType.FIM_ESTRUTURA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_ENUMERACAO", new EntradaPalavra(TokenType.FIM_ENUMERACAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_REGRA_NEGOCIO", new EntradaPalavra(TokenType.FIM_REGRA_NEGOCIO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_REGRA", new EntradaPalavra(TokenType.FIM_REGRA_NEGOCIO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_FUNCAO", new EntradaPalavra(TokenType.FIM_FUNCAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_PARA", new EntradaPalavra(TokenType.FIM_PARA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_BLOCO_MEMORIA", new EntradaPalavra(TokenType.FIM_BLOCO_MEMORIA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_SE", new EntradaPalavra(TokenType.FIM_SE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_ENQUANTO", new EntradaPalavra(TokenType.FIM_ENQUANTO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM", new EntradaPalavra(TokenType.FIM, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_OPERACAO", new EntradaPalavra(TokenType.FIM, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_TENTE", new EntradaPalavra(TokenType.FIM_TENTE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("EXIGE", new EntradaPalavra(TokenType.EXIGE, CategoriaPalavra.CONTRATO)),
        Map.entry("GARANTE", new EntradaPalavra(TokenType.GARANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("INVARIANTE", new EntradaPalavra(TokenType.INVARIANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("CONTRATO_ENTRADA", new EntradaPalavra(TokenType.CONTRATO_ENTRADA, CategoriaPalavra.CONTRATO)),
        Map.entry("FIM_CONTRATO_ENTRADA", new EntradaPalavra(TokenType.FIM_CONTRATO_ENTRADA, CategoriaPalavra.CONTRATO)),
        Map.entry("CONTRATO_SAIDA", new EntradaPalavra(TokenType.CONTRATO_SAIDA, CategoriaPalavra.CONTRATO)),
        Map.entry("FIM_CONTRATO_SAIDA", new EntradaPalavra(TokenType.FIM_CONTRATO_SAIDA, CategoriaPalavra.CONTRATO)),
        Map.entry("INICIO", new EntradaPalavra(TokenType.INICIO, CategoriaPalavra.CONTROLE)),
        Map.entry("SE", new EntradaPalavra(TokenType.SE, CategoriaPalavra.CONTROLE)),
        Map.entry("SENAO", new EntradaPalavra(TokenType.SENAO, CategoriaPalavra.CONTROLE)),
        Map.entry("ENQUANTO", new EntradaPalavra(TokenType.ENQUANTO, CategoriaPalavra.CONTROLE)),
        Map.entry("RETORNE", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
        Map.entry("RETORNAR", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
        Map.entry("FALHAR_COM", new EntradaPalavra(TokenType.FALHAR_COM, CategoriaPalavra.CONTROLE)),
        Map.entry("TENTE", new EntradaPalavra(TokenType.TENTE, CategoriaPalavra.CONTROLE)),
        Map.entry("CAPTURE", new EntradaPalavra(TokenType.CAPTURE, CategoriaPalavra.CONTROLE)),
        Map.entry("EXIBA", new EntradaPalavra(TokenType.EXIBA, CategoriaPalavra.CONTROLE)),
        Map.entry("LER", new EntradaPalavra(TokenType.LER, CategoriaPalavra.CONTROLE)),
        Map.entry("VETORIZAR_PARA", new EntradaPalavra(TokenType.VETORIZAR_PARA, CategoriaPalavra.CONTROLE)),
        Map.entry("USAR_BLOCO_MEMORIA", new EntradaPalavra(TokenType.USAR_BLOCO_MEMORIA, CategoriaPalavra.MEMORIA)),
        Map.entry("LAYOUT_COLUNAR", new EntradaPalavra(TokenType.LAYOUT_COLUNAR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("IMPORTAR", new EntradaPalavra(TokenType.IMPORTAR, CategoriaPalavra.DECLARACAO)),
        Map.entry("CASO_RESULTADO", new EntradaPalavra(TokenType.CASO_RESULTADO, CategoriaPalavra.CONTROLE)),
        Map.entry("ESCOLHA", new EntradaPalavra(TokenType.CASO_RESULTADO, CategoriaPalavra.CONTROLE)),
        Map.entry("CASO", new EntradaPalavra(TokenType.CASO, CategoriaPalavra.CONTROLE)),
        Map.entry("FIM_CASO", new EntradaPalavra(TokenType.FIM_CASO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_ESCOLHA", new EntradaPalavra(TokenType.FIM_CASO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("SUCESSO", new EntradaPalavra(TokenType.SUCESSO, CategoriaPalavra.CONTROLE)),
        Map.entry("ERRO", new EntradaPalavra(TokenType.ERRO, CategoriaPalavra.CONTROLE)),
        Map.entry("FALHA", new EntradaPalavra(TokenType.ERRO, CategoriaPalavra.CONTROLE)),
        Map.entry("IDEMPOTENTE", new EntradaPalavra(TokenType.IDEMPOTENTE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("CHAVE_IDEMPOTENCIA", new EntradaPalavra(TokenType.CHAVE_IDEMPOTENCIA, CategoriaPalavra.CONTRATO)),
        Map.entry("EM", new EntradaPalavra(TokenType.EM, CategoriaPalavra.MODIFICADOR)),
        Map.entry("PIPELINE_DADOS", new EntradaPalavra(TokenType.PIPELINE_DADOS, CategoriaPalavra.DECLARACAO)),
        Map.entry("FONTE_ENTRADA", new EntradaPalavra(TokenType.FONTE_ENTRADA, CategoriaPalavra.DECLARACAO)),
        Map.entry("DESTINO_SAIDA", new EntradaPalavra(TokenType.DESTINO_SAIDA, CategoriaPalavra.DECLARACAO)),
        Map.entry("TRANSFORMACAO", new EntradaPalavra(TokenType.TRANSFORMACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("FIM_PIPELINE", new EntradaPalavra(TokenType.FIM_PIPELINE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_FONTE", new EntradaPalavra(TokenType.FIM_FONTE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_DESTINO", new EntradaPalavra(TokenType.FIM_DESTINO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_TRANSFORMACAO", new EntradaPalavra(TokenType.FIM_TRANSFORMACAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_VETORIZAR", new EntradaPalavra(TokenType.FIM_VETORIZAR, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("ENTAO", new EntradaPalavra(TokenType.ENTAO, CategoriaPalavra.CONTROLE)),
        Map.entry("FACA", new EntradaPalavra(TokenType.FACA, CategoriaPalavra.CONTROLE)),
        Map.entry("STREAMING", new EntradaPalavra(TokenType.STREAMING, CategoriaPalavra.MODIFICADOR)),
        Map.entry("LOTE", new EntradaPalavra(TokenType.LOTE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("CONECTOR", new EntradaPalavra(TokenType.CONECTOR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("FORMATO", new EntradaPalavra(TokenType.FORMATO, CategoriaPalavra.MODIFICADOR)),
        Map.entry("PASSO_SIMD", new EntradaPalavra(TokenType.PASSO_SIMD, CategoriaPalavra.MODIFICADOR)),
        Map.entry("PARA", new EntradaPalavra(TokenType.PARA, CategoriaPalavra.CONTROLE)),
        Map.entry("PASSO", new EntradaPalavra(TokenType.PASSO, CategoriaPalavra.MODIFICADOR)),
        Map.entry("DE", new EntradaPalavra(TokenType.DE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("ATE", new EntradaPalavra(TokenType.ATE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("CRIAR", new EntradaPalavra(TokenType.CRIAR, CategoriaPalavra.DECLARACAO)),
        Map.entry("VERDADEIRO", new EntradaPalavra(TokenType.VERDADEIRO, CategoriaPalavra.LITERAL)),
        Map.entry("FALSO", new EntradaPalavra(TokenType.FALSO, CategoriaPalavra.LITERAL)),
        Map.entry("NULO", new EntradaPalavra(TokenType.NULO, CategoriaPalavra.LITERAL)),
        Map.entry("E", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        Map.entry("OU", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        Map.entry("NAO", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        // Consultas Tipadas (LINQ)
        Map.entry("CONSULTAR", new EntradaPalavra(TokenType.CONSULTAR, CategoriaPalavra.CONTROLE)),
        Map.entry("ONDE", new EntradaPalavra(TokenType.ONDE, CategoriaPalavra.CONTROLE)),
        Map.entry("ORDENAR_POR", new EntradaPalavra(TokenType.ORDENAR_POR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("AGRUPAR_POR", new EntradaPalavra(TokenType.AGRUPAR_POR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("LIMITE", new EntradaPalavra(TokenType.LIMITE, CategoriaPalavra.DECLARACAO)),
        Map.entry("PULAR", new EntradaPalavra(TokenType.PULAR, CategoriaPalavra.DECLARACAO)),
        Map.entry("ASC", new EntradaPalavra(TokenType.ASC, CategoriaPalavra.DECLARACAO)),
        Map.entry("DESC", new EntradaPalavra(TokenType.DESC, CategoriaPalavra.DECLARACAO)),
        Map.entry("BLOCO_NATIVO_RUST", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("NATIVO_RUST", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("CODIGO_RUST", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("FIM_BLOCO_NATIVO", new EntradaPalavra(TokenType.FIM_BLOCO_NATIVO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_NATIVO", new EntradaPalavra(TokenType.FIM_BLOCO_NATIVO, CategoriaPalavra.FIM_BLOCO)),
        // Sintaxe Moderna Dual (PT-BR e Universal)
        Map.entry("programa", new EntradaPalavra(TokenType.PROGRAMA, CategoriaPalavra.DECLARACAO)),
        Map.entry("metadados", new EntradaPalavra(TokenType.METADADOS_ARQUITETURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("METADADOS", new EntradaPalavra(TokenType.METADADOS_ARQUITETURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("estrutura", new EntradaPalavra(TokenType.ESTRUTURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("struct", new EntradaPalavra(TokenType.ESTRUTURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("STRUCT", new EntradaPalavra(TokenType.ESTRUTURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("var", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("VAR", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("val", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("VAL", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("let", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("LET", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("fn", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("FN", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("fun", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("FUN", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("funcao", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("operacao", new EntradaPalavra(TokenType.OPERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("proc", new EntradaPalavra(TokenType.PROCEDIMENTO, CategoriaPalavra.DECLARACAO)),
        Map.entry("PROC", new EntradaPalavra(TokenType.PROCEDIMENTO, CategoriaPalavra.DECLARACAO)),
        Map.entry("procedimento", new EntradaPalavra(TokenType.PROCEDIMENTO, CategoriaPalavra.DECLARACAO)),
        Map.entry("se", new EntradaPalavra(TokenType.SE, CategoriaPalavra.CONTROLE)),
        Map.entry("senao", new EntradaPalavra(TokenType.SENAO, CategoriaPalavra.CONTROLE)),
        Map.entry("enquanto", new EntradaPalavra(TokenType.ENQUANTO, CategoriaPalavra.CONTROLE)),
        Map.entry("para", new EntradaPalavra(TokenType.PARA, CategoriaPalavra.CONTROLE)),
        Map.entry("retorne", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
        Map.entry("ret", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
        Map.entry("RET", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
        Map.entry("exiba", new EntradaPalavra(TokenType.EXIBA, CategoriaPalavra.CONTROLE)),
        Map.entry("print", new EntradaPalavra(TokenType.EXIBA, CategoriaPalavra.CONTROLE)),
        Map.entry("PRINT", new EntradaPalavra(TokenType.EXIBA, CategoriaPalavra.CONTROLE)),
        Map.entry("exige", new EntradaPalavra(TokenType.EXIGE, CategoriaPalavra.CONTRATO)),
        Map.entry("garante", new EntradaPalavra(TokenType.GARANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("invariante", new EntradaPalavra(TokenType.INVARIANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("verdadeiro", new EntradaPalavra(TokenType.VERDADEIRO, CategoriaPalavra.LITERAL)),
        Map.entry("falso", new EntradaPalavra(TokenType.FALSO, CategoriaPalavra.LITERAL)),
        Map.entry("nulo", new EntradaPalavra(TokenType.NULO, CategoriaPalavra.LITERAL)),
        Map.entry("de", new EntradaPalavra(TokenType.DE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("ate", new EntradaPalavra(TokenType.ATE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("passo", new EntradaPalavra(TokenType.PASSO, CategoriaPalavra.MODIFICADOR))
    );

    // 2. Tabela Equivalente EN-US
    private static final Map<String, EntradaPalavra> TABELA_EN_US = Map.ofEntries(
        Map.entry("PROGRAM", new EntradaPalavra(TokenType.PROGRAMA, CategoriaPalavra.DECLARACAO)),
        Map.entry("VISUAL", new EntradaPalavra(TokenType.VISUAL, CategoriaPalavra.MODIFICADOR)),
        Map.entry("BUSINESS", new EntradaPalavra(TokenType.NEGOCIO, CategoriaPalavra.MODIFICADOR)),
        Map.entry("ARCHITECTURE", new EntradaPalavra(TokenType.ARQUITETURA, CategoriaPalavra.MODIFICADOR)),
        Map.entry("LIBRARY", new EntradaPalavra(TokenType.BIBLIOTECA, CategoriaPalavra.DECLARACAO)),
        Map.entry("EXTENSION", new EntradaPalavra(TokenType.EXTENSAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("TOOL", new EntradaPalavra(TokenType.FERRAMENTA, CategoriaPalavra.DECLARACAO)),
        Map.entry("TEST", new EntradaPalavra(TokenType.TESTE, CategoriaPalavra.DECLARACAO)),
        Map.entry("SCREEN", new EntradaPalavra(TokenType.TELA, CategoriaPalavra.DECLARACAO)),
        Map.entry("ARCHITECTURE_METADATA", new EntradaPalavra(TokenType.METADADOS_ARQUITETURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("STRUCTURE", new EntradaPalavra(TokenType.ESTRUTURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("ENUM", new EntradaPalavra(TokenType.ENUMERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("BUSINESS_RULE", new EntradaPalavra(TokenType.REGRA_NEGOCIO, CategoriaPalavra.DECLARACAO)),
        Map.entry("RULE", new EntradaPalavra(TokenType.REGRA_NEGOCIO, CategoriaPalavra.DECLARACAO)),
        Map.entry("rule", new EntradaPalavra(TokenType.REGRA_NEGOCIO, CategoriaPalavra.DECLARACAO)),
        Map.entry("PROCEDURE", new EntradaPalavra(TokenType.PROCEDIMENTO, CategoriaPalavra.DECLARACAO)),
        Map.entry("FUNCTION", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("OPERATION", new EntradaPalavra(TokenType.OPERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("VARIABLE", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("LANGUAGE_VERSION", new EntradaPalavra(TokenType.VERSAO_LINGUAGEM, CategoriaPalavra.DECLARACAO)),
        Map.entry("END_PROGRAM", new EntradaPalavra(TokenType.FIM_PROGRAMA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_LIBRARY", new EntradaPalavra(TokenType.FIM_BIBLIOTECA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_EXTENSION", new EntradaPalavra(TokenType.FIM_EXTENSAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_TOOL", new EntradaPalavra(TokenType.FIM_FERRAMENTA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_TEST", new EntradaPalavra(TokenType.FIM_TESTE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_SCREEN", new EntradaPalavra(TokenType.FIM_TELA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_METADATA", new EntradaPalavra(TokenType.FIM_METADADOS, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_STRUCTURE", new EntradaPalavra(TokenType.FIM_ESTRUTURA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_ENUM", new EntradaPalavra(TokenType.FIM_ENUMERACAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_BUSINESS_RULE", new EntradaPalavra(TokenType.FIM_REGRA_NEGOCIO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_RULE", new EntradaPalavra(TokenType.FIM_REGRA_NEGOCIO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_FUNCTION", new EntradaPalavra(TokenType.FIM_FUNCAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_FOR", new EntradaPalavra(TokenType.FIM_PARA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_MEMORY_BLOCK", new EntradaPalavra(TokenType.FIM_BLOCO_MEMORIA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_IF", new EntradaPalavra(TokenType.FIM_SE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_WHILE", new EntradaPalavra(TokenType.FIM_ENQUANTO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END", new EntradaPalavra(TokenType.FIM, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_OPERATION", new EntradaPalavra(TokenType.FIM, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_TRY", new EntradaPalavra(TokenType.FIM_TENTE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("REQUIRES", new EntradaPalavra(TokenType.EXIGE, CategoriaPalavra.CONTRATO)),
        Map.entry("ENSURES", new EntradaPalavra(TokenType.GARANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("INVARIANT", new EntradaPalavra(TokenType.INVARIANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("INPUT_CONTRACT", new EntradaPalavra(TokenType.CONTRATO_ENTRADA, CategoriaPalavra.CONTRATO)),
        Map.entry("END_INPUT_CONTRACT", new EntradaPalavra(TokenType.FIM_CONTRATO_ENTRADA, CategoriaPalavra.CONTRATO)),
        Map.entry("OUTPUT_CONTRACT", new EntradaPalavra(TokenType.CONTRATO_SAIDA, CategoriaPalavra.CONTRATO)),
        Map.entry("END_OUTPUT_CONTRACT", new EntradaPalavra(TokenType.FIM_CONTRATO_SAIDA, CategoriaPalavra.CONTRATO)),
        Map.entry("START", new EntradaPalavra(TokenType.INICIO, CategoriaPalavra.CONTROLE)),
        Map.entry("BEGIN", new EntradaPalavra(TokenType.INICIO, CategoriaPalavra.CONTROLE)),
        Map.entry("IF", new EntradaPalavra(TokenType.SE, CategoriaPalavra.CONTROLE)),
        Map.entry("ELSE", new EntradaPalavra(TokenType.SENAO, CategoriaPalavra.CONTROLE)),
        Map.entry("WHILE", new EntradaPalavra(TokenType.ENQUANTO, CategoriaPalavra.CONTROLE)),
        Map.entry("RETURN", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
        Map.entry("FAIL_WITH", new EntradaPalavra(TokenType.FALHAR_COM, CategoriaPalavra.CONTROLE)),
        Map.entry("TRY", new EntradaPalavra(TokenType.TENTE, CategoriaPalavra.CONTROLE)),
        Map.entry("CATCH", new EntradaPalavra(TokenType.CAPTURE, CategoriaPalavra.CONTROLE)),
        Map.entry("DISPLAY", new EntradaPalavra(TokenType.EXIBA, CategoriaPalavra.CONTROLE)),
        Map.entry("PRINT", new EntradaPalavra(TokenType.EXIBA, CategoriaPalavra.CONTROLE)),
        Map.entry("READ", new EntradaPalavra(TokenType.LER, CategoriaPalavra.CONTROLE)),
        Map.entry("VECTORIZE_FOR", new EntradaPalavra(TokenType.VETORIZAR_PARA, CategoriaPalavra.CONTROLE)),
        Map.entry("USE_MEMORY_BLOCK", new EntradaPalavra(TokenType.USAR_BLOCO_MEMORIA, CategoriaPalavra.MEMORIA)),
        Map.entry("COLUMNAR_LAYOUT", new EntradaPalavra(TokenType.LAYOUT_COLUNAR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("IMPORT", new EntradaPalavra(TokenType.IMPORTAR, CategoriaPalavra.DECLARACAO)),
        Map.entry("MATCH_RESULT", new EntradaPalavra(TokenType.CASO_RESULTADO, CategoriaPalavra.CONTROLE)),
        Map.entry("END_MATCH", new EntradaPalavra(TokenType.FIM_CASO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("SUCCESS", new EntradaPalavra(TokenType.SUCESSO, CategoriaPalavra.CONTROLE)),
        Map.entry("ERROR", new EntradaPalavra(TokenType.ERRO, CategoriaPalavra.CONTROLE)),
        Map.entry("IDEMPOTENT", new EntradaPalavra(TokenType.IDEMPOTENTE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("IDEMPOTENCY_KEY", new EntradaPalavra(TokenType.CHAVE_IDEMPOTENCIA, CategoriaPalavra.CONTRATO)),
        Map.entry("IN", new EntradaPalavra(TokenType.EM, CategoriaPalavra.MODIFICADOR)),
        Map.entry("DATA_PIPELINE", new EntradaPalavra(TokenType.PIPELINE_DADOS, CategoriaPalavra.DECLARACAO)),
        Map.entry("INPUT_SOURCE", new EntradaPalavra(TokenType.FONTE_ENTRADA, CategoriaPalavra.DECLARACAO)),
        Map.entry("OUTPUT_TARGET", new EntradaPalavra(TokenType.DESTINO_SAIDA, CategoriaPalavra.DECLARACAO)),
        Map.entry("TRANSFORMATION", new EntradaPalavra(TokenType.TRANSFORMACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("END_PIPELINE", new EntradaPalavra(TokenType.FIM_PIPELINE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_SOURCE", new EntradaPalavra(TokenType.FIM_FONTE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_TARGET", new EntradaPalavra(TokenType.FIM_DESTINO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_TRANSFORMATION", new EntradaPalavra(TokenType.FIM_TRANSFORMACAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_VECTORIZE", new EntradaPalavra(TokenType.FIM_VETORIZAR, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("THEN", new EntradaPalavra(TokenType.ENTAO, CategoriaPalavra.CONTROLE)),
        Map.entry("DO", new EntradaPalavra(TokenType.FACA, CategoriaPalavra.CONTROLE)),
        Map.entry("BATCH", new EntradaPalavra(TokenType.LOTE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("CONNECTOR", new EntradaPalavra(TokenType.CONECTOR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("FORMAT", new EntradaPalavra(TokenType.FORMATO, CategoriaPalavra.MODIFICADOR)),
        Map.entry("SIMD_STEP", new EntradaPalavra(TokenType.PASSO_SIMD, CategoriaPalavra.MODIFICADOR)),
        Map.entry("FOR", new EntradaPalavra(TokenType.PARA, CategoriaPalavra.CONTROLE)),
        Map.entry("STEP", new EntradaPalavra(TokenType.PASSO, CategoriaPalavra.MODIFICADOR)),
        Map.entry("FROM", new EntradaPalavra(TokenType.DE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("TO", new EntradaPalavra(TokenType.ATE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("CREATE", new EntradaPalavra(TokenType.CRIAR, CategoriaPalavra.DECLARACAO)),
        Map.entry("TRUE", new EntradaPalavra(TokenType.VERDADEIRO, CategoriaPalavra.LITERAL)),
        Map.entry("FALSE", new EntradaPalavra(TokenType.FALSO, CategoriaPalavra.LITERAL)),
        Map.entry("NULL", new EntradaPalavra(TokenType.NULO, CategoriaPalavra.LITERAL)),
        Map.entry("AND", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        Map.entry("OR", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        Map.entry("NOT", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        // Query DSL EN-US
        Map.entry("QUERY", new EntradaPalavra(TokenType.CONSULTAR, CategoriaPalavra.CONTROLE)),
        Map.entry("WHERE", new EntradaPalavra(TokenType.ONDE, CategoriaPalavra.CONTROLE)),
        Map.entry("ORDER_BY", new EntradaPalavra(TokenType.ORDENAR_POR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("GROUP_BY", new EntradaPalavra(TokenType.AGRUPAR_POR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("LIMIT", new EntradaPalavra(TokenType.LIMITE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("OFFSET", new EntradaPalavra(TokenType.PULAR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("ASC", new EntradaPalavra(TokenType.ASC, CategoriaPalavra.MODIFICADOR)),
        Map.entry("DESC", new EntradaPalavra(TokenType.DESC, CategoriaPalavra.MODIFICADOR)),
        Map.entry("NATIVE_RUST_BLOCK", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("NATIVE_RUST", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("INLINE_RUST", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("END_NATIVE_BLOCK", new EntradaPalavra(TokenType.FIM_BLOCO_NATIVO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_NATIVE", new EntradaPalavra(TokenType.FIM_BLOCO_NATIVO, CategoriaPalavra.FIM_BLOCO)),
        // Modern & Universal Keywords EN-US
        Map.entry("program", new EntradaPalavra(TokenType.PROGRAMA, CategoriaPalavra.DECLARACAO)),
        Map.entry("metadata", new EntradaPalavra(TokenType.METADADOS_ARQUITETURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("METADATA", new EntradaPalavra(TokenType.METADADOS_ARQUITETURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("structure", new EntradaPalavra(TokenType.ESTRUTURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("struct", new EntradaPalavra(TokenType.ESTRUTURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("STRUCT", new EntradaPalavra(TokenType.ESTRUTURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("var", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("VAR", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("val", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("VAL", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("let", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("LET", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("fn", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("FN", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("fun", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("FUN", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("function", new EntradaPalavra(TokenType.FUNCAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("operation", new EntradaPalavra(TokenType.OPERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("proc", new EntradaPalavra(TokenType.PROCEDIMENTO, CategoriaPalavra.DECLARACAO)),
        Map.entry("PROC", new EntradaPalavra(TokenType.PROCEDIMENTO, CategoriaPalavra.DECLARACAO)),
        Map.entry("procedure", new EntradaPalavra(TokenType.PROCEDIMENTO, CategoriaPalavra.DECLARACAO)),
        Map.entry("if", new EntradaPalavra(TokenType.SE, CategoriaPalavra.CONTROLE)),
        Map.entry("else", new EntradaPalavra(TokenType.SENAO, CategoriaPalavra.CONTROLE)),
        Map.entry("while", new EntradaPalavra(TokenType.ENQUANTO, CategoriaPalavra.CONTROLE)),
        Map.entry("for", new EntradaPalavra(TokenType.PARA, CategoriaPalavra.CONTROLE)),
        Map.entry("return", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
        Map.entry("ret", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
        Map.entry("RET", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
        Map.entry("print", new EntradaPalavra(TokenType.EXIBA, CategoriaPalavra.CONTROLE)),
        Map.entry("display", new EntradaPalavra(TokenType.EXIBA, CategoriaPalavra.CONTROLE)),
        Map.entry("requires", new EntradaPalavra(TokenType.EXIGE, CategoriaPalavra.CONTRATO)),
        Map.entry("ensures", new EntradaPalavra(TokenType.GARANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("invariant", new EntradaPalavra(TokenType.INVARIANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("true", new EntradaPalavra(TokenType.VERDADEIRO, CategoriaPalavra.LITERAL)),
        Map.entry("false", new EntradaPalavra(TokenType.FALSO, CategoriaPalavra.LITERAL)),
        Map.entry("null", new EntradaPalavra(TokenType.NULO, CategoriaPalavra.LITERAL)),
        Map.entry("from", new EntradaPalavra(TokenType.DE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("to", new EntradaPalavra(TokenType.ATE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("step", new EntradaPalavra(TokenType.PASSO, CategoriaPalavra.MODIFICADOR))
    );

    // Mapeamentos de Tradução Bidirecional
    private static final Map<String, String> TRADUCAO_PT_PARA_EN = new HashMap<>();
    private static final Map<String, String> TRADUCAO_EN_PARA_PT = new HashMap<>();

    static {
        // Mapeamentos canônicos primários explícitos
        TRADUCAO_PT_PARA_EN.put("PROGRAMA", "PROGRAM");
        TRADUCAO_EN_PARA_PT.put("PROGRAM", "PROGRAMA");
        TRADUCAO_PT_PARA_EN.put("METADADOS_ARQUITETURA", "ARCHITECTURE_METADATA");
        TRADUCAO_EN_PARA_PT.put("ARCHITECTURE_METADATA", "METADADOS_ARQUITETURA");
        TRADUCAO_PT_PARA_EN.put("ESTRUTURA", "STRUCTURE");
        TRADUCAO_EN_PARA_PT.put("STRUCTURE", "ESTRUTURA");
        TRADUCAO_PT_PARA_EN.put("REGRA_NEGOCIO", "BUSINESS_RULE");
        TRADUCAO_EN_PARA_PT.put("BUSINESS_RULE", "REGRA_NEGOCIO");
        TRADUCAO_PT_PARA_EN.put("REGRA", "RULE");
        TRADUCAO_EN_PARA_PT.put("RULE", "REGRA");
        TRADUCAO_PT_PARA_EN.put("regra", "rule");
        TRADUCAO_EN_PARA_PT.put("rule", "regra");
        TRADUCAO_PT_PARA_EN.put("FUNCAO", "FUNCTION");
        TRADUCAO_EN_PARA_PT.put("FUNCTION", "FUNCAO");
        TRADUCAO_PT_PARA_EN.put("OPERACAO", "OPERATION");
        TRADUCAO_EN_PARA_PT.put("OPERATION", "OPERACAO");
        TRADUCAO_PT_PARA_EN.put("PROCEDIMENTO", "PROCEDURE");
        TRADUCAO_EN_PARA_PT.put("PROCEDURE", "PROCEDIMENTO");
        TRADUCAO_PT_PARA_EN.put("VARIAVEL", "VARIABLE");
        TRADUCAO_EN_PARA_PT.put("VARIABLE", "VARIAVEL");
        TRADUCAO_PT_PARA_EN.put("SE", "IF");
        TRADUCAO_EN_PARA_PT.put("IF", "SE");
        TRADUCAO_PT_PARA_EN.put("SENAO", "ELSE");
        TRADUCAO_EN_PARA_PT.put("ELSE", "SENAO");
        TRADUCAO_PT_PARA_EN.put("ENQUANTO", "WHILE");
        TRADUCAO_EN_PARA_PT.put("WHILE", "ENQUANTO");
        TRADUCAO_PT_PARA_EN.put("PARA", "FOR");
        TRADUCAO_EN_PARA_PT.put("FOR", "PARA");
        TRADUCAO_PT_PARA_EN.put("RETORNE", "RETURN");
        TRADUCAO_EN_PARA_PT.put("RETURN", "RETORNE");
        TRADUCAO_PT_PARA_EN.put("EXIBA", "PRINT");
        TRADUCAO_EN_PARA_PT.put("PRINT", "EXIBA");
        TRADUCAO_EN_PARA_PT.put("DISPLAY", "EXIBA");
        TRADUCAO_PT_PARA_EN.put("EXIGE", "REQUIRES");
        TRADUCAO_EN_PARA_PT.put("REQUIRES", "EXIGE");
        TRADUCAO_PT_PARA_EN.put("GARANTE", "ENSURES");
        TRADUCAO_EN_PARA_PT.put("ENSURES", "GARANTE");
        TRADUCAO_PT_PARA_EN.put("INVARIANTE", "INVARIANT");
        TRADUCAO_EN_PARA_PT.put("INVARIANT", "INVARIANTE");
        TRADUCAO_PT_PARA_EN.put("VERDADEIRO", "TRUE");
        TRADUCAO_EN_PARA_PT.put("TRUE", "VERDADEIRO");
        TRADUCAO_PT_PARA_EN.put("FALSO", "FALSE");
        TRADUCAO_EN_PARA_PT.put("FALSE", "FALSO");
        TRADUCAO_PT_PARA_EN.put("NULO", "NULL");
        TRADUCAO_EN_PARA_PT.put("NULL", "NULO");
        TRADUCAO_PT_PARA_EN.put("E", "AND");
        TRADUCAO_EN_PARA_PT.put("AND", "E");
        TRADUCAO_PT_PARA_EN.put("OU", "OR");
        TRADUCAO_EN_PARA_PT.put("OR", "OU");
        TRADUCAO_PT_PARA_EN.put("NAO", "NOT");
        TRADUCAO_EN_PARA_PT.put("NOT", "NAO");

        // Correspondências em minúsculas
        TRADUCAO_PT_PARA_EN.put("programa", "program");
        TRADUCAO_EN_PARA_PT.put("program", "programa");
        TRADUCAO_PT_PARA_EN.put("metadados", "metadata");
        TRADUCAO_EN_PARA_PT.put("metadata", "metadados");
        TRADUCAO_PT_PARA_EN.put("estrutura", "structure");
        TRADUCAO_EN_PARA_PT.put("structure", "estrutura");
        TRADUCAO_PT_PARA_EN.put("se", "if");
        TRADUCAO_EN_PARA_PT.put("if", "se");
        TRADUCAO_PT_PARA_EN.put("senao", "else");
        TRADUCAO_EN_PARA_PT.put("else", "senao");
        TRADUCAO_PT_PARA_EN.put("enquanto", "while");
        TRADUCAO_EN_PARA_PT.put("while", "enquanto");
        TRADUCAO_PT_PARA_EN.put("para", "for");
        TRADUCAO_EN_PARA_PT.put("for", "para");
        TRADUCAO_PT_PARA_EN.put("retorne", "return");
        TRADUCAO_EN_PARA_PT.put("return", "retorne");
        TRADUCAO_PT_PARA_EN.put("exiba", "print");
        TRADUCAO_EN_PARA_PT.put("display", "exiba");
        TRADUCAO_PT_PARA_EN.put("exige", "requires");
        TRADUCAO_EN_PARA_PT.put("requires", "exige");
        TRADUCAO_PT_PARA_EN.put("garante", "ensures");
        TRADUCAO_EN_PARA_PT.put("ensures", "garante");
        TRADUCAO_PT_PARA_EN.put("invariante", "invariant");
        TRADUCAO_EN_PARA_PT.put("invariant", "invariante");
        TRADUCAO_PT_PARA_EN.put("verdadeiro", "true");
        TRADUCAO_EN_PARA_PT.put("true", "verdadeiro");
        TRADUCAO_PT_PARA_EN.put("falso", "false");
        TRADUCAO_EN_PARA_PT.put("false", "falso");
        TRADUCAO_PT_PARA_EN.put("nulo", "null");
        TRADUCAO_EN_PARA_PT.put("null", "nulo");
        TRADUCAO_PT_PARA_EN.put("de", "from");
        TRADUCAO_EN_PARA_PT.put("from", "de");
        TRADUCAO_PT_PARA_EN.put("ate", "to");
        TRADUCAO_EN_PARA_PT.put("to", "ate");
        TRADUCAO_PT_PARA_EN.put("passo", "step");
        TRADUCAO_EN_PARA_PT.put("step", "passo");

        // Palavras universais
        for (String uni : List.of("var", "val", "let", "fn", "fun", "proc", "struct", "ret", "print")) {
            TRADUCAO_PT_PARA_EN.put(uni, uni);
            TRADUCAO_EN_PARA_PT.put(uni, uni);
            TRADUCAO_PT_PARA_EN.put(uni.toUpperCase(), uni.toUpperCase());
            TRADUCAO_EN_PARA_PT.put(uni.toUpperCase(), uni.toUpperCase());
        }

        // Inicializa mapeamento bi-direcional através da correspondência de TokenType
        for (var entryPt : TABELA_PT_BR.entrySet()) {
            for (var entryEn : TABELA_EN_US.entrySet()) {
                if (entryPt.getValue().token() == entryEn.getValue().token()) {
                    TRADUCAO_PT_PARA_EN.putIfAbsent(entryPt.getKey(), entryEn.getKey());
                    TRADUCAO_EN_PARA_PT.putIfAbsent(entryEn.getKey(), entryPt.getKey());
                }
            }
        }
    }

    private PalavrasReservadas() {}

    public static boolean ehPalavraReservada(String palavra, DialetoLinguagem dialeto) {
        if (palavra == null) return false;
        Map<String, EntradaPalavra> tab = (dialeto == DialetoLinguagem.EN_US) ? TABELA_EN_US : TABELA_PT_BR;
        return tab.containsKey(palavra);
    }

    public static boolean ehPalavraReservada(String palavra) {
        return ehPalavraReservada(palavra, DialetoLinguagem.PT_BR) || ehPalavraReservada(palavra, DialetoLinguagem.EN_US);
    }

    public static TokenType tokenDe(String palavra, DialetoLinguagem dialeto) {
        if (palavra == null) return null;
        Map<String, EntradaPalavra> tab = (dialeto == DialetoLinguagem.EN_US) ? TABELA_EN_US : TABELA_PT_BR;
        EntradaPalavra e = tab.get(palavra);
        return e == null ? null : e.token();
    }

    public static TokenType tokenDe(String palavra) {
        return tokenDe(palavra, DialetoLinguagem.PT_BR);
    }

    public static CategoriaPalavra categoriaDe(String palavra, DialetoLinguagem dialeto) {
        if (palavra == null) return null;
        Map<String, EntradaPalavra> tab = (dialeto == DialetoLinguagem.EN_US) ? TABELA_EN_US : TABELA_PT_BR;
        EntradaPalavra e = tab.get(palavra);
        return e == null ? null : e.categoria();
    }

    public static CategoriaPalavra categoriaDe(String palavra) {
        return categoriaDe(palavra, DialetoLinguagem.PT_BR);
    }

    public static Set<String> palavras(DialetoLinguagem dialeto) {
        return (dialeto == DialetoLinguagem.EN_US) ? TABELA_EN_US.keySet() : TABELA_PT_BR.keySet();
    }

    public static Set<String> palavras() {
        return palavras(DialetoLinguagem.PT_BR);
    }

    public static Map<String, EntradaPalavra> tabela(DialetoLinguagem dialeto) {
        return (dialeto == DialetoLinguagem.EN_US) ? TABELA_EN_US : TABELA_PT_BR;
    }

    public static Map<String, EntradaPalavra> tabela() {
        return TABELA_PT_BR;
    }

    public static String traduzir(String palavra, DialetoLinguagem destino) {
        if (palavra == null) return "";
        if (destino == DialetoLinguagem.EN_US) {
            return TRADUCAO_PT_PARA_EN.getOrDefault(palavra, palavra);
        } else {
            return TRADUCAO_EN_PARA_PT.getOrDefault(palavra, palavra);
        }
    }

    public static void validarPurezaDialeto(String palavra, DialetoLinguagem dialetoAtivo, int linha, int col) {
        if (palavra == null) return;
        if (dialetoAtivo == DialetoLinguagem.EN_US) {
            if (TABELA_PT_BR.containsKey(palavra) && !TABELA_EN_US.containsKey(palavra)) {
                String equivalente = traduzir(palavra, DialetoLinguagem.EN_US);
                throw new ErroLexico(linha, col,
                        "Keyword '" + palavra + "' belongs to dialect [pt-BR] and cannot be used in a file configured for [en-US]. " +
                        "Use the English equivalent '" + equivalente + "' or set the directive to 'LANGUAGE: pt-BR' ('lang ptbr').");
            }
        } else {
            if (TABELA_EN_US.containsKey(palavra) && !TABELA_PT_BR.containsKey(palavra)) {
                String equivalente = traduzir(palavra, DialetoLinguagem.PT_BR);
                throw new ErroLexico(linha, col,
                        "Palavra-chave '" + palavra + "' pertence ao dialeto [en-US] e não pode ser usada em arquivo configurado para [pt-BR]. " +
                        "Use a palavra equivalente em português '" + equivalente + "' ou declare 'LANGUAGE: en-US' ('lang enus') no cabeçalho.");
            }
        }
    }
}
