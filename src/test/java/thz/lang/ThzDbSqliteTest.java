package thz.lang;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.db.ThzDb;
import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DecimalFixo;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThzDbSqliteTest {

    private Path tempDb;

    @BeforeEach
    void setup() throws Exception {
        tempDb = Files.createTempFile("thz_test_", ".db");
        ThzDb.conectar("jdbc:sqlite:" + tempDb.toAbsolutePath());
    }

    @AfterEach
    void cleanup() {
        ThzDb.fechar();
        try {
            Files.deleteIfExists(tempDb);
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Deve criar tabela, inserir dados com tipos THZ e consultar com precisão decimal exata")
    void testSqliteOperacoesBasicas() {
        // Criação de tabela
        long r1 = ThzDb.executar(
                "CREATE TABLE produtos (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  nome TEXT NOT NULL," +
                "  preco NUMERIC NOT NULL," +
                "  ativo BOOLEAN NOT NULL" +
                ");",
                List.of()
        );
        assertEquals(0, r1);

        // Inserção com parâmetros tipados THZ
        long r2 = ThzDb.executar(
                "INSERT INTO produtos (nome, preco, ativo) VALUES (?, ?, ?);",
                List.of(
                        ValorThz.TEXTO("Servidor Blade"),
                        ValorThz.DECIMAL(DecimalFixo.deTexto("4599.9000", 4)),
                        ValorThz.LOGICO(true)
                )
        );
        assertEquals(1, r2);

        // Consulta e mapeamento de tipos
        List<ValorThz.Registro> linhas = ThzDb.consultar("SELECT id, nome, preco, ativo FROM produtos WHERE id = ?;", List.of(ValorThz.INTEIRO(1)));
        assertEquals(1, linhas.size());

        ValorThz.Registro linha = linhas.get(0);
        assertEquals(ValorThz.TEXTO("Servidor Blade"), linha.campos().get("nome"));
        assertTrue(linha.campos().get("preco") instanceof ValorThz.Decimal);
        assertEquals("4599.9000", ((ValorThz.Decimal) linha.campos().get("preco")).valor().formatar());
        assertEquals(ValorThz.LOGICO(true), linha.campos().get("ativo"));
    }
}
