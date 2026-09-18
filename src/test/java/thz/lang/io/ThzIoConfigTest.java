package thz.lang.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thz.lang.config.ThzConfig;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ThzIoConfigTest {

    @Test
    @DisplayName("Deve ler, escrever e listar arquivos com ThzIO")
    void testIoOperacoes(@TempDir Path tempDir) {
        Path arquivo = tempDir.resolve("teste.txt");
        String conteudo = "Linha 1\nLinha 2 de teste UTF-8: Acentuação é válida.";

        ThzIO.escreverTexto(arquivo.toString(), conteudo);
        assertTrue(ThzIO.existe(arquivo.toString()));

        String lido = ThzIO.lerTexto(arquivo.toString());
        assertEquals(conteudo, lido);

        ThzIO.anexarTexto(arquivo.toString(), "\nLinha 3");
        assertTrue(ThzIO.lerTexto(arquivo.toString()).contains("Linha 3"));

        List<String> itens = ThzIO.listarDiretorio(tempDir.toString());
        assertTrue(itens.contains("teste.txt"));

        assertTrue(ThzIO.remover(arquivo.toString()));
        assertFalse(ThzIO.existe(arquivo.toString()));
    }

    @Test
    @DisplayName("Deve carregar e obter configurações com ThzConfig")
    void testConfigOperacoes() {
        ThzConfig.definir("APP_NOME", "THZ-Enterprise");
        ThzConfig.definir("APP_PORTA", "8080");
        ThzConfig.definir("APP_ATIVO", "verdadeiro");

        assertEquals("THZ-Enterprise", ThzConfig.obter("APP_NOME"));
        assertEquals(8080L, ThzConfig.obterInteiro("APP_PORTA", 3000));
        assertTrue(ThzConfig.obterLogico("APP_ATIVO", false));
        assertEquals("padrao", ThzConfig.obter("CHAVE_INEXISTENTE", "padrao"));
    }
}
