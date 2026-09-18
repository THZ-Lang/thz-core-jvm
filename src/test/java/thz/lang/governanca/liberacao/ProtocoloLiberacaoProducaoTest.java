package thz.lang.governanca.liberacao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocoloLiberacaoProducaoTest {

    private static final String FONTE_VALIDO = """
            PROGRAMA ServicoFaturamento
            METADADOS_ARQUITETURA
                DOMINIO: "Faturamento"
                CAMADA: "Dominio"
                VERSAO: "1.0.0"
                AUTOR: "Engenharia"
                SLO_LATENCIA_MAXIMA: "10ms"
                CONFORMIDADE: "SOX-404"
            FIM_METADADOS

            ESTRUTURA Fatura
                id: TEXTO
                total: DECIMAL(18, 2)
                INVARIANTE total >= 0.00
            FIM_ESTRUTURA

            REGRA_NEGOCIO RegraCalculo
                IDENTIFICADOR_REGRA: "REG-001"
                RASTREIO_REQUISITO: "REQ-001"
                CONTRATO_ENTRADA
                    EXIGE total > 0.00
                FIM_CONTRATO_ENTRADA
                CONTRATO_SAIDA
                    GARANTE total > 0.00
                FIM_CONTRATO_SAIDA
                OPERACAO Processar(f: Fatura) : DECIMAL(18, 2)
                INICIO
                    RETORNE f.total
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

    private ProgramaAst parse(String fonte) {
        return new ThzParser(new ThzLexer(fonte).tokenize()).parse();
    }

    @Test
    @DisplayName("P01 & P03: Deve liberar para produção quando homologação positiva e negativa são atendidas e dados reais sem pendências")
    void testLiberacaoAprovadaComEvidenciasCompletas() {
        ProgramaAst ast = parse(FONTE_VALIDO);

        List<CasoHomologacao> homologacao = List.of(
                CasoHomologacao.casoPositivo("HOM-001", "Cálculo nominal de fatura", () -> {
                    // Execução bem-sucedida
                }),
                CasoHomologacao.casoNegativo("HOM-002", "Tentativa com valor zero deve ser rejeitada", "valor_invalido", () -> {
                    // Simula lançamento de erro conforme esperado
                    throw new IllegalArgumentException("valor_invalido: total deve ser positivo");
                })
        );

        RegistroDadosReaisValidados dados = RegistroDadosReaisValidados.criar(
                "faturas_lote_setembro.csv",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                1500,
                1500,
                List.of()
        );

        SolicitacaoLiberacao solicitacao = new SolicitacaoLiberacao(
                ast.nome(),
                FONTE_VALIDO,
                ast,
                homologacao,
                dados,
                "2026-09-11T18:30:00Z",
                Map.of("RegraCalculo", "v1.2")
        );

        LaudoLiberacaoProducao laudo = ProtocoloLiberacaoProducao.avaliar(solicitacao);

        assertNotNull(laudo);
        assertTrue(laudo.liberadoParaProducao(), "Laudo deve declarar LIBERADO PARA PRODUÇÃO");
        assertTrue(laudo.auditoriaEstaticaAprovada());
        assertTrue(laudo.homologacaoAprovada());
        assertEquals(2, laudo.casosHomologacaoAprovados());
        assertTrue(laudo.dadosReaisAprovados());
        assertTrue(laudo.regrasVigentesAprovadas());
        assertTrue(laudo.impedimentos().isEmpty());

        String markdown = laudo.gerarMarkdownLaudo();
        assertTrue(markdown.contains("LIBERADO PARA PRODUÇÃO"));
        assertTrue(markdown.contains(laudo.sha256CodigoFonte()));
        assertTrue(markdown.contains("HOM-001"));
        assertTrue(markdown.contains("HOM-002"));
    }

    @Test
    @DisplayName("P01: Deve bloquear liberação caso um teste negativo de homologação suceda indevidamente")
    void testBloqueioQuandoCasoNegativoNaoFalha() {
        ProgramaAst ast = parse(FONTE_VALIDO);

        List<CasoHomologacao> homologacao = List.of(
                CasoHomologacao.casoNegativo("HOM-NEG-001", "Deveria falhar mas não falhou", "erro_esperado", () -> {
                    // Não lança exceção (falha da garantia negativa)
                })
        );

        RegistroDadosReaisValidados dados = RegistroDadosReaisValidados.criar("dados.csv", "hash123", 100, 100, List.of());

        SolicitacaoLiberacao solicitacao = new SolicitacaoLiberacao(
                ast.nome(),
                FONTE_VALIDO,
                ast,
                homologacao,
                dados,
                "2026-09-11T18:30:00Z",
                Map.of("RegraCalculo", "v1.0")
        );

        LaudoLiberacaoProducao laudo = ProtocoloLiberacaoProducao.avaliar(solicitacao);

        assertFalse(laudo.liberadoParaProducao());
        assertFalse(laudo.homologacaoAprovada());
        assertTrue(laudo.impedimentos().stream().anyMatch(i -> i.contains("não foi rejeitado conforme esperado")));
    }

    @Test
    @DisplayName("P02: Deve bloquear liberação quando dados reais possuírem pendências impeditivas")
    void testBloqueioQuandoDadosReaisPossuemPendencias() {
        ProgramaAst ast = parse(FONTE_VALIDO);

        List<CasoHomologacao> homologacao = List.of(
                CasoHomologacao.casoPositivo("HOM-001", "Sucesso", () -> {})
        );

        RegistroDadosReaisValidados dados = RegistroDadosReaisValidados.criar(
                "dados.csv",
                "hash123",
                100,
                98,
                List.of("Código duplicado na linha 42", "Campo obrigatório ausente na linha 77")
        );

        SolicitacaoLiberacao solicitacao = new SolicitacaoLiberacao(
                ast.nome(),
                FONTE_VALIDO,
                ast,
                homologacao,
                dados,
                "2026-09-11T18:30:00Z",
                Map.of("RegraCalculo", "v1.0")
        );

        LaudoLiberacaoProducao laudo = ProtocoloLiberacaoProducao.avaliar(solicitacao);

        assertFalse(laudo.liberadoParaProducao());
        assertFalse(laudo.dadosReaisAprovados());
        assertTrue(laudo.impedimentos().stream().anyMatch(i -> i.contains("pendência(s) impeditiva(s)")));
    }

    @Test
    @DisplayName("P03: Deve bloquear caso a validação tente gravar no acervo antes da liberação")
    void testBloqueioQuandoModoValidacaoTentaGravar() {
        ProgramaAst ast = parse(FONTE_VALIDO);

        List<CasoHomologacao> homologacao = List.of(
                CasoHomologacao.casoPositivo("HOM-001", "Sucesso", () -> {})
        );

        RegistroDadosReaisValidados dadosComGravacaoIndevida = new RegistroDadosReaisValidados(
                "dados.csv", "hash123", 100, 100, 0, List.of(), false // false = violou modo dry-run!
        );

        SolicitacaoLiberacao solicitacao = new SolicitacaoLiberacao(
                ast.nome(),
                FONTE_VALIDO,
                ast,
                homologacao,
                dadosComGravacaoIndevida,
                "2026-09-11T18:30:00Z",
                Map.of("RegraCalculo", "v1.0")
        );

        LaudoLiberacaoProducao laudo = ProtocoloLiberacaoProducao.avaliar(solicitacao);

        assertFalse(laudo.liberadoParaProducao());
        assertFalse(laudo.dadosReaisAprovados());
        assertTrue(laudo.impedimentos().stream().anyMatch(i -> i.contains("violou a política de acervo")));
    }

    @Test
    @DisplayName("R01/R02: Deve bloquear liberação se alguma regra não possuir revisão vigente explícita")
    void testBloqueioQuandoRegraSemVigenciaUnivoca() {
        ProgramaAst ast = parse(FONTE_VALIDO);

        List<CasoHomologacao> homologacao = List.of(CasoHomologacao.casoPositivo("HOM-001", "Ok", () -> {}));
        RegistroDadosReaisValidados dados = RegistroDadosReaisValidados.criar("dados.csv", "hash", 10, 10, List.of());

        // Mapa de revisões vazio (sem vigência para RegraCalculo)
        SolicitacaoLiberacao solicitacao = new SolicitacaoLiberacao(
                ast.nome(),
                FONTE_VALIDO,
                ast,
                homologacao,
                dados,
                "2026-09-11T18:30:00Z",
                Map.of()
        );

        LaudoLiberacaoProducao laudo = ProtocoloLiberacaoProducao.avaliar(solicitacao);

        assertFalse(laudo.liberadoParaProducao());
        assertFalse(laudo.regrasVigentesAprovadas());
        assertTrue(laudo.impedimentos().stream().anyMatch(i -> i.contains("não possui revisão vigente unívoca")));
    }
}
