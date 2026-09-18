package thz.lang.inventario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import thz.lang.fachada.ThzCompilerFacade;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;

class InventarioServicoTest {
    @TempDir Path temporario;
    private final Principal autor = () -> "analista.autenticado";
    private final Clock relogio = Clock.fixed(Instant.parse("2026-09-11T10:15:30Z"), ZoneOffset.UTC);

    private InventarioServico servico() {
        return new InventarioServico(temporario.resolve("inventario.db"), autor, relogio, new RegrasInventario());
    }

    @Test
    void deveCadastrarConciliarAtualizarEPreservarAuditoria() throws Exception {
        Path csv = temporario.resolve("inventario.csv");
        Files.writeString(csv, "codigo;descricao;localizacao\nARQ-1;Ata;Caixa 1\n");
        var servico = servico();

        var previa = servico.importar(csv, LocalDate.parse("2026-09-11"), "Carga inicial", true);
        assertEquals("CADASTRAVEL", previa.ocorrencias().getFirst().estado());
        assertFalse(Files.exists(temporario.resolve("inventario.db")), "ValidaÃ§Ã£o nÃ£o deve criar ou alterar o banco.");

        var carga = servico.importar(csv, LocalDate.parse("2026-09-11"), "Carga inicial", false);
        assertEquals("CADASTRADO", carga.ocorrencias().getFirst().estado());
        assertTrue(carga.origem().contains("#sha256="));
        assertEquals(1, servico.consultar("ARQ-1").versao());

        var semMudanca = servico.atualizar("ARQ-1", 1, "Ata", "Caixa 1",
                LocalDate.parse("2026-09-11"), "ConferÃªncia sem mudanÃ§a");
        assertEquals("SEM_ALTERACAO", semMudanca.estado());
        assertEquals(3, servico.historico("ARQ-1").size(), "Cadastro registra origem dos trÃªs campos.");

        var atualizacao = servico.atualizar("ARQ-1", 1, "Ata revisada", "Caixa 2",
                LocalDate.parse("2026-09-11"), "CorreÃ§Ã£o catalogrÃ¡fica");
        assertEquals("ATUALIZADO", atualizacao.estado());
        assertEquals(2, servico.consultar("ARQ-1").versao());
        var historico = servico.historico("ARQ-1");
        assertEquals(5, historico.size());
        assertTrue(historico.stream().allMatch(h -> h.autor().equals("analista.autenticado")));
        assertTrue(historico.stream().anyMatch(h -> "Ata".equals(h.anterior()) && "Ata revisada".equals(h.novo())));

        var conflito = servico.atualizar("ARQ-1", 1, "Outra", "Caixa 3",
                LocalDate.parse("2026-09-11"), "Tentativa concorrente");
        assertEquals("BLOQUEADO", conflito.estado());
        assertTrue(conflito.detalhes().getFirst().startsWith("CONFLITO_VERSAO"));
        assertEquals("Ata revisada", servico.consultar("ARQ-1").descricao());
    }

    @Test
    void deveBloquearDuplicidadesConflitantesAntesDeGravar() throws Exception {
        Path csv = temporario.resolve("duplicado.csv");
        Files.writeString(csv, "codigo;descricao;localizacao\nARQ-1;Ata;Caixa 1\nARQ-2;Foto;Caixa 2\nARQ-1;Ata divergente;Caixa 1\n");
        var servico = servico();
        var resultado = servico.importar(csv, LocalDate.parse("2026-09-11"), "Carga controlada", false);
        assertEquals(List.of("BLOQUEADO", "CADASTRADO", "BLOQUEADO"),
                resultado.ocorrencias().stream().map(InventarioServico.Ocorrencia::estado).toList());
        assertNull(servico.consultar("ARQ-1"));
        assertNotNull(servico.consultar("ARQ-2"));
    }

    @Test
    void deveAcumularAusenciasEExigirAutoriaJustificativaEVigenciaUnica() throws Exception {
        Path csv = temporario.resolve("invalido.csv");
        Files.writeString(csv, "codigo;descricao;localizacao\n;;\n");
        var servico = servico();
        var resultado = servico.importar(csv, LocalDate.parse("2026-09-11"), "ValidaÃ§Ã£o", false);
        assertEquals(3, resultado.ocorrencias().getFirst().detalhes().size());
        assertThrows(IllegalArgumentException.class,
                () -> servico.importar(csv, LocalDate.parse("2026-09-11"), " ", false));
        var semAutor = new InventarioServico(temporario.resolve("outro.db"), null, relogio, new RegrasInventario());
        assertThrows(IllegalStateException.class,
                () -> semAutor.importar(csv, LocalDate.parse("2026-09-11"), "ValidaÃ§Ã£o", false));

        var ambiguas = new RegrasInventario(List.of(
                new RegrasInventario.Revisao(1, LocalDate.parse("2026-01-01"), null),
                new RegrasInventario.Revisao(2, LocalDate.parse("2026-06-01"), null)));
        var servicoAmbiguo = new InventarioServico(temporario.resolve("ambiguo.db"), autor, relogio, ambiguas);
        assertThrows(IllegalArgumentException.class,
                () -> servicoAmbiguo.importar(csv, LocalDate.parse("2026-09-11"), "ValidaÃ§Ã£o", false));
    }

    @Test
    void deveArquivarLogicamenteValoresQueSaemDaJanelaRecente() throws Exception {
        Path csv = temporario.resolve("janela.csv");
        Files.writeString(csv, "codigo;descricao;localizacao\nARQ-1;DescriÃ§Ã£o 0;Caixa\n");
        var servico = servico();
        servico.importar(csv, LocalDate.parse("2026-09-11"), "Carga", false);
        for (int versao = 1; versao <= 11; versao++) {
            assertEquals("ATUALIZADO", servico.atualizar("ARQ-1", versao, "DescriÃ§Ã£o " + versao, "Caixa",
                    LocalDate.parse("2026-09-11"), "RevisÃ£o " + versao).estado());
        }
        var mudancasDescricao = servico.historico("ARQ-1").stream()
                .filter(h -> h.campo().equals("descricao") && h.anterior() != null).toList();
        assertEquals(11, mudancasDescricao.size());
        assertEquals(1, mudancasDescricao.stream().filter(InventarioServico.Historico::arquivado).count());
    }

    @Test
    void programaThzDeveExecutarImportacaoComCapacidadeAutenticadaDoHost() throws Exception {
        Path fonte = Path.of("../thz-lang/exemplos/inventario_auditavel.thz");
        if (!Files.exists(fonte)) fonte = Path.of("../../exemplos/inventario_auditavel.thz");
        if (!Files.exists(fonte)) fonte = Path.of("exemplos/inventario_auditavel.thz");
        String codigo = Files.readString(fonte);
        var analise = ThzCompilerFacade.analisar(codigo, true);
        assertFalse(analise.temErros(), () -> "Programa de referÃªncia invÃ¡lido: " + analise.diagnosticos());

        Path csv = temporario.resolve("via-thz.csv");
        Files.writeString(csv, "codigo;descricao;localizacao\nARQ-THZ;Documento;Caixa 9\n");
        var servico = servico();
        var interpretador = new InterpretadorThz(analise.ast(),
                new InterpretadorThz.OpcoesInterpretador(null, null, null), servico);
        ValorThz resultado = interpretador.executarOperacao("Executar", java.util.Map.of(
                "arquivo", ValorThz.TEXTO(csv.toString()),
                "referencia", ValorThz.TEXTO("2026-09-11"),
                "justificativa", ValorThz.TEXTO("ExecuÃ§Ã£o pelo programa THZ")));

        assertInstanceOf(ValorThz.Registro.class, resultado);
        assertEquals("Documento", servico.consultar("ARQ-THZ").descricao());
    }
}

