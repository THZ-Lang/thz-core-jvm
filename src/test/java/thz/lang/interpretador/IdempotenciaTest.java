package thz.lang.interpretador;

import org.junit.jupiter.api.Test;
import thz.lang.ast.OperacaoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.ast.RegraNegocioAst;
import thz.lang.docgen.ThzDocGen;
import thz.lang.formato.Formatador;
import thz.lang.governanca.AuditorGovernanca;
import thz.lang.governanca.RelatorioAuditoria;
import thz.lang.ir.GeradorIr;
import thz.lang.ir.IrPrograma;
import thz.lang.lexico.ThzLexer;
import thz.lang.runtime.DecimalFixo;
import thz.lang.runtime.RegistroIdempotencia;
import thz.lang.sintatico.ThzParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de Testes para o conceito de Idempotência Inteligente de Larga Escala (Wide-Language Application).
 * Cobre todos os níveis: Léxico, Sintático, AST, Runtime, Interpretador, Governança G4, THZ-IR G5, Formatador e DocGen.
 */
public class IdempotenciaTest {

    private static final String CODIGO_FONTE_IDEMPOTENTE = """
            PROGRAMA ServicoLiquidacaoFinanceira

            METADADOS_ARQUITETURA
                DOMINIO: "Financeiro"
                SUBDOMINIO: "Liquidacao"
                CAMADA: "Dominio"
                VERSAO: "1.0.0"
                AUTOR: "THZ Architect"
                SLO_LATENCIA_MAXIMA: "15ms"
                CONFORMIDADE: "ISO_10967", "LGPD"
            FIM_METADADOS

            REGRA_NEGOCIO LiquidarTransacao
                IDENTIFICADOR_REGRA: "RN-LIQ-001"
                RASTREIO_REQUISITO: "REQ-FIN-778"
                DESCRICAO: "Liquidacao idempotente de transacao com chave unica de pagamento"
                IDEMPOTENTE: VERDADEIRO
                CHAVE_IDEMPOTENCIA: "TX-REQ-2026-99"

                CONTRATO_ENTRADA
                    EXIGE valor > 0
                FIM_CONTRATO_ENTRADA

                CONTRATO_SAIDA
                    GARANTE total >= 0.00
                FIM_CONTRATO_SAIDA

                OPERACAO IDEMPOTENTE ProcessarPagamento(valor: DECIMAL(12,2), taxa: DECIMAL(12,2)) : DECIMAL(12,2)
                INICIO
                    VARIAVEL total : DECIMAL(12,2) <- valor + taxa
                    RETORNE total
                FIM
            FIM_REGRA_NEGOCIO

            PROCEDIMENTO IDEMPOTENTE EmitirNotificacaoAuditoria(codigo: TEXTO)
            INICIO
                EXIBA "Notificacao auditada: " + codigo
            FIM

            FIM_PROGRAMA
            """;

    private ProgramaAst parsear(String fonte) {
        return new ThzParser(new ThzLexer(fonte).tokenize()).parse();
    }

    @Test
    public void testLexicoESintaticoIdempotencia() {
        ProgramaAst ast = parsear(CODIGO_FONTE_IDEMPOTENTE);

        assertNotNull(ast);
        assertEquals("ServicoLiquidacaoFinanceira", ast.nome());
        assertEquals(1, ast.regras().size());

        RegraNegocioAst regra = ast.regras().get(0);
        assertTrue(regra.idempotente(), "Regra deve ser reconhecida como IDEMPOTENTE");
        assertEquals("TX-REQ-2026-99", regra.chaveIdempotencia());

        assertEquals(1, regra.operacoes().size());
        OperacaoAst op = regra.operacoes().get(0);
        assertTrue(op.idempotente(), "Operação deve ser reconhecida como IDEMPOTENTE");

        assertEquals(1, ast.procedimentos().size());
        assertTrue(ast.procedimentos().get(0).idempotente(), "Procedimento deve ser reconhecido como IDEMPOTENTE");
    }

    @Test
    public void testRegistroIdempotenciaRuntimeDireto() {
        RegistroIdempotencia registro = new RegistroIdempotencia();
        AtomicInteger execucoesContador = new AtomicInteger(0);

        Map<String, ValorThz> args = Map.of(
                "id", new ValorThz.Texto("PED-123"),
                "valor", new ValorThz.Decimal(DecimalFixo.deTexto("100.50", 2))
        );

        // 1ª Execução
        ValorThz v1 = registro.executarOuReutilizar(
                "ProcessarPedido",
                "PED-123",
                args,
                () -> {
                    execucoesContador.incrementAndGet();
                    return new ValorThz.Texto("PROCESSADO_OK");
                },
                null
        );

        assertEquals("PROCESSADO_OK", ((ValorThz.Texto) v1).valor());
        assertEquals(1, execucoesContador.get());
        assertEquals(0, registro.getContadorExecucoesEvitadas());
        assertEquals(1, registro.getTotalRegistros());

        // 2ª Execução com a mesma chave (reuso instantâneo em O(1))
        ValorThz v2 = registro.executarOuReutilizar(
                "ProcessarPedido",
                "PED-123",
                args,
                () -> {
                    execucoesContador.incrementAndGet();
                    return new ValorThz.Texto("PROCESSADO_OK");
                },
                null
        );

        assertSame(v1, v2, "Deve retornar a mesma instância memoizada de resultado");
        assertEquals(1, execucoesContador.get(), "Não deve re-executar o bloco!");
        assertEquals(1, registro.getContadorExecucoesEvitadas());

        // Limpeza O(1)
        registro.limpar();
        assertEquals(0, registro.getTotalRegistros());
        assertEquals(0, registro.getContadorExecucoesEvitadas());
    }

    @Test
    public void testInterpretadorComOperacaoIdempotente() {
        ProgramaAst ast = parsear(CODIGO_FONTE_IDEMPOTENTE);

        List<String> logs = new ArrayList<>();
        InterpretadorThz interp = new InterpretadorThz(ast, new InterpretadorThz.OpcoesInterpretador(logs::add, null, 1000));

        Map<String, ValorThz> args = Map.of(
                "valor", new ValorThz.Decimal(DecimalFixo.deTexto("100.00", 2)),
                "taxa", new ValorThz.Decimal(DecimalFixo.deTexto("5.50", 2))
        );

        // 1ª chamada
        ValorThz r1 = interp.executarOperacao("ProcessarPagamento", args);
        assertNotNull(r1);
        assertEquals("105.50", ((ValorThz.Decimal) r1).valor().formatar());

        // 2ª chamada (mesmos argumentos e chave)
        ValorThz r2 = interp.executarOperacao("ProcessarPagamento", args);
        assertNotNull(r2);
        assertEquals("105.50", ((ValorThz.Decimal) r2).valor().formatar());

        // Verificar estatísticas do motor
        assertEquals(1, interp.getRegistroIdempotencia().getContadorExecucoesEvitadas());
        assertTrue(logs.stream().anyMatch(l -> l.contains("[IDEMPOTÊNCIA] Reutilização de resultado idêntico")),
                "Deve emitir mensagem de auditoria sobre a idempotência");
    }

    @Test
    public void testIdempotenciaNaGovernancaAuditoria() {
        ProgramaAst ast = parsear(CODIGO_FONTE_IDEMPOTENTE);

        RelatorioAuditoria relatorio = AuditorGovernanca.auditar(ast);
        assertNotNull(relatorio);
        assertEquals(1, relatorio.metricas().totalOperacoesIdempotentes());

        String md = AuditorGovernanca.gerarMarkdownGovernanca(relatorio);
        assertTrue(md.contains("🛡️ `IDEMPOTENTE`"));
        assertTrue(md.contains("TX-REQ-2026-99"));
        assertTrue(md.contains("Operações com Idempotência Garantida"));

        String json = AuditorGovernanca.gerarJsonGovernanca(relatorio);
        assertTrue(json.contains("\"idempotente\": true"));
        assertTrue(json.contains("\"chaveIdempotencia\": \"TX-REQ-2026-99\""));
        assertTrue(json.contains("\"totalOperacoesIdempotentes\": 1"));
    }

    @Test
    public void testIdempotenciaNoIrETooling() {
        ProgramaAst ast = parsear(CODIGO_FONTE_IDEMPOTENTE);

        // 1. THZ-IR
        IrPrograma ir = GeradorIr.baixarParaIr(ast);
        assertNotNull(ir);
        assertTrue(ir.funcoes().stream().anyMatch(f -> f.idempotente()), "Função IR deve herdar flag de idempotência");

        String irJson = GeradorIr.serializarIr(ir);
        assertTrue(irJson.contains("\"idempotente\": true"));

        // 2. DocGen
        String docMd = ThzDocGen.gerarDocumentacao(ast);
        assertTrue(docMd.contains("🛡️ `IDEMPOTENTE`"));
        assertTrue(docMd.contains("OPERACAO IDEMPOTENTE ProcessarPagamento"));

        // 3. Formatador
        String fmt = Formatador.formatar(ast);
        assertTrue(fmt.contains("IDEMPOTENTE: VERDADEIRO"));
        assertTrue(fmt.contains("CHAVE_IDEMPOTENCIA: \"TX-REQ-2026-99\""));
        assertTrue(fmt.contains("OPERACAO IDEMPOTENTE ProcessarPagamento"));
    }
}
