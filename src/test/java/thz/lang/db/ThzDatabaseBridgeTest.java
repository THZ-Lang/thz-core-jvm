package thz.lang.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DecimalFixo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ThzDatabaseBridgeTest {

    @BeforeEach
    void setup() {
        ThzDb.conectar("padrao", "jdbc:sqlite::memory:", null, null);
    }

    @Test
    @DisplayName("ThzDatabaseBridge deve criar tabela e realizar persistência estilo JPA")
    void testJpaPersistencia() {
        // 1. DDL dinâmica
        Map<String, String> colunas = new LinkedHashMap<>();
        colunas.put("id", "TEXT PRIMARY KEY");
        colunas.put("nome", "TEXT");
        colunas.put("valor", "DECIMAL");
        ThzDatabaseBridge.criarTabela("produtos", colunas);

        // 2. Salvar (INSERT)
        Map<String, ValorThz> p1 = new LinkedHashMap<>();
        p1.put("id", ValorThz.TEXTO("PROD-01"));
        p1.put("nome", ValorThz.TEXTO("Servidor Blade"));
        p1.put("valor", ValorThz.DECIMAL(DecimalFixo.deTexto("12500.00", 2)));
        ThzDatabaseBridge.salvar("produtos", new ValorThz.Registro("Produto", p1));

        // 3. Buscar por ID
        var buscado = ThzDatabaseBridge.buscarPorId("produtos", ValorThz.TEXTO("PROD-01"));
        assertNotNull(buscado);
        assertTrue(buscado instanceof ValorThz.Registro);
        assertEquals("Servidor Blade", ((ValorThz.Registro) buscado).campos().get("nome").formatar());

        // 4. Salvar (UPDATE)
        p1.put("nome", ValorThz.TEXTO("Servidor Blade Rack"));
        ThzDatabaseBridge.salvar("produtos", new ValorThz.Registro("Produto", p1));

        var atualizado = ThzDatabaseBridge.buscarPorId("produtos", ValorThz.TEXTO("PROD-01"));
        assertEquals("Servidor Blade Rack", ((ValorThz.Registro) atualizado).campos().get("nome").formatar());

        // 5. Remover por ID
        boolean removido = ThzDatabaseBridge.removerPorId("produtos", ValorThz.TEXTO("PROD-01"));
        assertTrue(removido);

        var aposRemocao = ThzDatabaseBridge.buscarPorId("produtos", ValorThz.TEXTO("PROD-01"));
        assertTrue(aposRemocao instanceof ValorThz.Nulo);
    }

    @Test
    @DisplayName("ThzDatabaseBridge deve realizar busca vetorial KNN por similaridade de cosseno")
    void testBuscaVetorialKnn() {
        Map<String, String> colunas = new LinkedHashMap<>();
        colunas.put("id", "TEXT PRIMARY KEY");
        colunas.put("titulo", "TEXT");
        colunas.put("vetor", "TEXT");
        ThzDatabaseBridge.criarTabela("documentos", colunas);

        // Inserção de vetores
        ThzDb.executar("INSERT INTO documentos (id, titulo, vetor) VALUES (?, ?, ?)",
                List.of(ValorThz.TEXTO("D1"), ValorThz.TEXTO("Doc Fiscal Tributario"), ValorThz.TEXTO("[1.0, 0.0, 0.0]")));
        ThzDb.executar("INSERT INTO documentos (id, titulo, vetor) VALUES (?, ?, ?)",
                List.of(ValorThz.TEXTO("D2"), ValorThz.TEXTO("Doc RH Folha"), ValorThz.TEXTO("[0.0, 1.0, 0.0]")));

        // Busca vetorial por similaridade com [0.95, 0.05, 0.0] (mais próximo de D1)
        float[] consulta = new float[] { 0.95f, 0.05f, 0.0f };
        var resultados = ThzDatabaseBridge.consultarVetorial("documentos", "vetor", consulta, 1);

        assertEquals(1, resultados.size());
        assertEquals("Doc Fiscal Tributario", resultados.get(0).campos().get("titulo").formatar());
        assertNotNull(resultados.get(0).campos().get("_similaridade"));
    }

    @Test
    @DisplayName("ThzDb deve suportar transações explícitas e consultas escalares")
    void testRawSqlAvancado() {
        ThzDb.executarScript("CREATE TABLE contas (id TEXT, saldo NUMERIC); INSERT INTO contas VALUES ('C1', 500);");

        // Consulta Escalar
        var count = ThzDb.consultarValor("SELECT COUNT(*) FROM contas", List.of());
        assertEquals("1", count.formatar());

        // Transação com Commit
        ThzDb.iniciarTransacao();
        ThzDb.executar("INSERT INTO contas VALUES ('C2', 1200)", List.of());
        ThzDb.confirmarTransacao();

        var total = ThzDb.consultarValor("SELECT COUNT(*) FROM contas", List.of());
        assertEquals("2", total.formatar());

        // Transação com Rollback
        ThzDb.iniciarTransacao();
        ThzDb.executar("INSERT INTO contas VALUES ('C3', 9999)", List.of());
        ThzDb.cancelarTransacao();

        var totalAposRollback = ThzDb.consultarValor("SELECT COUNT(*) FROM contas", List.of());
        assertEquals("2", totalAposRollback.formatar());
    }
}
