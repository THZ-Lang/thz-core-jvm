package thz.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.interpretador.InterpretadorThz;

import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import static org.junit.jupiter.api.Assertions.*;

class ConsultaTipadaTest {

    @Test
    @DisplayName("Deve adicionar item a FATIA sem mutar a coleção original")
    void testFatiaAdicionar() {
        String codigo = """
            PROGRAMA TesteFatia
            REGRA_NEGOCIO R
                OPERACAO Obter() : INTEIRO
                INICIO
                    VARIAVEL base : FATIA[INTEIRO] <- [10]
                    VARIAVEL ampliada : FATIA[INTEIRO] <- FATIA.adicionar(base, 20)
                    RETORNE FATIA.tamanho(ampliada)
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;
        ProgramaAst ast = new ThzParser(new ThzLexer(codigo).tokenize()).parse();
        var interp = new InterpretadorThz(ast);
        assertEquals("2", interp.formatar(interp.executarOperacao("Obter", java.util.Map.of())));
    }

    @Test
    @DisplayName("Deve filtrar e ordenar coleção usando CONSULTAR (LINQ nativo)")
    void testConsultaTipadaFiltroEOrdenacao() {
        String codigo = """
            PROGRAMA TesteLinq
            ESTRUTURA Cliente
                nome: TEXTO
                idade: INTEIRO
                limite: DECIMAL
            FIM_ESTRUTURA

            REGRA_NEGOCIO RegraConsulta
                OPERACAO Executar() : INTEIRO
                INICIO
                    VARIAVEL c1 : Cliente <- CRIAR Cliente(nome: "Carlos", idade: 40, limite: 1500.00)
                    VARIAVEL c2 : Cliente <- CRIAR Cliente(nome: "Ana", idade: 25, limite: 8000.00)
                    VARIAVEL c3 : Cliente <- CRIAR Cliente(nome: "Bruno", idade: 30, limite: 5000.00)
                    VARIAVEL c4 : Cliente <- CRIAR Cliente(nome: "Zilda", idade: 60, limite: 12000.00)
                    
                    VARIAVEL lista : FATIA[Cliente] <- [c1, c2, c3, c4]
                    
                    # Filtra clientes com limite >= 5000.00, ordena por nome ASC e limita em 2
                    VARIAVEL vip : FATIA[Cliente] <- CONSULTAR DE lista ONDE limite >= 5000.00 ORDENAR_POR nome ASC LIMITE 2
                    
                    VARIAVEL totalVips : INTEIRO <- FATIA.tamanho(vip)
                    RETORNE totalVips
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        var tokens = new ThzLexer(codigo).tokenize();
        var parser = new ThzParser(tokens);
        ProgramaAst ast = parser.parse();

        var interp = new InterpretadorThz(ast);
        var res = interp.executarOperacao("Executar", java.util.Map.of());
        assertEquals("2", interp.formatar(res));
    }

    @Test
    @DisplayName("Deve paginar com PULAR e LIMITE")
    void testConsultaPaginada() {
        String codigo = """
            PROGRAMA TestePaginacao
            REGRA_NEGOCIO R
                OPERACAO ObterSegundaPagina() : INTEIRO
                INICIO
                    VARIAVEL itens : FATIA[INTEIRO] <- [10, 20, 30, 40, 50, 60]
                    VARIAVEL pag : FATIA[INTEIRO] <- CONSULTAR DE itens PULAR 2 LIMITE 2
                    RETORNE pag[0]
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        var tokens = new ThzLexer(codigo).tokenize();
        var parser = new ThzParser(tokens);
        ProgramaAst ast = parser.parse();

        var interp = new InterpretadorThz(ast);
        var res = interp.executarOperacao("ObterSegundaPagina", java.util.Map.of());
        assertEquals("30", interp.formatar(res));
    }
}
