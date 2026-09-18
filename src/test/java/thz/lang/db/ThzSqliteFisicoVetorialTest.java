package thz.lang.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thz.lang.ia.ThzIaEngine;
import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DecimalFixo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ThzSqliteFisicoVetorialTest {

    @TempDir
    Path tempDir;

    private Path arquivoDbFisico;
    private String urlDbFisico;

    @BeforeEach
    void setup() {
        arquivoDbFisico = tempDir.resolve("banco_corporativo_real.db");
        urlDbFisico = "jdbc:sqlite:" + arquivoDbFisico.toAbsolutePath().toString().replace("\\", "/");
        ThzDb.fecharTodas();
        ThzDb.conectar("padrao", urlDbFisico, null, null);
    }

    @AfterEach
    void tearDown() {
        ThzDb.fecharTodas();
    }

    @Test
    @DisplayName("Deve gerar arquivo .db físico no disco, persistir dados e sobreviver ao reinício da conexão")
    void testPersistenciaFisicaEmDisco() throws IOException {
        // 1. Criação de tabela física
        Map<String, String> colunas = new LinkedHashMap<>();
        colunas.put("id", "TEXT PRIMARY KEY");
        colunas.put("razaoSocial", "TEXT NOT NULL");
        colunas.put("faturamentoAnual", "NUMERIC NOT NULL");
        colunas.put("scoreCredito", "INTEGER");
        colunas.put("vetorSegmento", "TEXT");
        ThzDatabaseBridge.criarTabela("empresas_parceiras", colunas);

        // 2. Persistência de dados reais com Embeddings de IA
        float[] embTecnologia = ThzIaEngine.gerarEmbedding("Serviços de Computação em Nuvem e Inteligência Artificial", 128);
        float[] embAgronegocio = ThzIaEngine.gerarEmbedding("Exportação de Grãos, Soja e Tecnologia Agrícola", 128);

        Map<String, ValorThz> emp1 = new LinkedHashMap<>();
        emp1.put("id", ValorThz.TEXTO("CNPJ-001"));
        emp1.put("razaoSocial", ValorThz.TEXTO("Cloud & AI Technologies Ltda"));
        emp1.put("faturamentoAnual", ValorThz.DECIMAL(DecimalFixo.deTexto("12500000.00", 2)));
        emp1.put("scoreCredito", ValorThz.INTEIRO(920));
        emp1.put("vetorSegmento", ValorThz.TEXTO(thz.lang.vetor.ThzVetorSimd.formatarVetor(embTecnologia)));

        Map<String, ValorThz> emp2 = new LinkedHashMap<>();
        emp2.put("id", ValorThz.TEXTO("CNPJ-002"));
        emp2.put("razaoSocial", ValorThz.TEXTO("Agro Soja & Grãos do Brasil S.A."));
        emp2.put("faturamentoAnual", ValorThz.DECIMAL(DecimalFixo.deTexto("48000000.00", 2)));
        emp2.put("scoreCredito", ValorThz.INTEIRO(870));
        emp2.put("vetorSegmento", ValorThz.TEXTO(thz.lang.vetor.ThzVetorSimd.formatarVetor(embAgronegocio)));

        ThzDatabaseBridge.salvar("empresas_parceiras", new ValorThz.Registro("Empresa", emp1));
        ThzDatabaseBridge.salvar("empresas_parceiras", new ValorThz.Registro("Empresa", emp2));

        // 3. Fecha completamente a conexão para forçar flush no disco
        ThzDb.fecharTodas();

        // 4. Verificação FÍSICA no Sistema de Arquivos (disco real)
        assertTrue(Files.exists(arquivoDbFisico), "O arquivo .db físico DEVE existir no sistema de arquivos!");
        long tamanhoBytes = Files.size(arquivoDbFisico);
        assertTrue(tamanhoBytes > 0, "O arquivo .db físico DEVE ter tamanho maior que 0 bytes (tamanho real: " + tamanhoBytes + " bytes)");

        // Verifica cabeçalho oficial do SQLite (Magic Bytes: "SQLite format 3\000")
        byte[] cabecalho;
        try (var is = Files.newInputStream(arquivoDbFisico)) {
            cabecalho = is.readNBytes(16);
        }
        String magicHeader = new String(cabecalho, 0, 15, StandardCharsets.US_ASCII);
        assertEquals("SQLite format 3", magicHeader, "O arquivo gravado DEVE ser um binário SQLite 3 válido!");

        // 5. Reabre a conexão a partir do arquivo .db físico existente
        ThzDb.conectar("padrao", urlDbFisico, null, null);

        // 6. Valida que os dados foram preservados
        var recuperado = ThzDatabaseBridge.buscarPorId("empresas_parceiras", ValorThz.TEXTO("CNPJ-001"));
        assertNotNull(recuperado);
        assertTrue(recuperado instanceof ValorThz.Registro);
        assertEquals("Cloud & AI Technologies Ltda", ((ValorThz.Registro) recuperado).campos().get("razaoSocial").formatar());

        // 7. Busca Vetorial Semântica KNN no banco físico
        float[] consultaVetorial = ThzIaEngine.gerarEmbedding("Computação em Nuvem e Inteligência Artificial", 128);
        var maisProximos = ThzDatabaseBridge.consultarVetorial("empresas_parceiras", "vetorSegmento", consultaVetorial, 1);

        assertEquals(1, maisProximos.size());
        assertEquals("Cloud & AI Technologies Ltda", maisProximos.get(0).campos().get("razaoSocial").formatar());
        assertNotNull(maisProximos.get(0).campos().get("_similaridade"));

        // 8. Raw SQL e transações no banco físico
        var contagem = ThzDb.consultarValor("SELECT COUNT(*) FROM empresas_parceiras", List.of());
        assertEquals("2", contagem.formatar());

        var somaFaturamento = ThzDb.consultarValor("SELECT SUM(faturamentoAnual) FROM empresas_parceiras", List.of());
        assertEquals("60500000", somaFaturamento.formatar().split("\\.")[0]);
    }
}
