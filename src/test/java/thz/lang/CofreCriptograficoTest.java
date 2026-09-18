package thz.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.security.ThzSecurity;
import thz.lang.security.ThzVault;
import thz.lang.sintatico.ThzParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CofreCriptograficoTest {

    @Test
    @DisplayName("Deve gerar e verificar hash Argon2id com parâmetros recomendados")
    void testArgon2idHashing() {
        String senha = "MinhaSenhaSuperSecreta!2026";
        String hash = ThzSecurity.hashArgon2(senha);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$argon2id$v=19$"));
        assertTrue(ThzSecurity.verificarArgon2(senha, hash));
        assertFalse(ThzSecurity.verificarArgon2("SenhaIncorreta", hash));
    }

    @Test
    @DisplayName("Deve criptografar e descriptografar com ChaCha20-Poly1305")
    void testChaCha20Poly1305() {
        String segredo = "DadosConfidenciaisBacen2026";
        String chave = "chave-mestra-de-32-caracteres-ok";

        String payload = ThzSecurity.criptografarChaCha20(segredo, chave);
        assertNotNull(payload);
        assertNotEquals(segredo, payload);

        String recuperado = ThzSecurity.descriptografarChaCha20(payload, chave);
        assertEquals(segredo, recuperado);
    }

    @Test
    @DisplayName("Deve salvar e ler cofre ThzVault com validação de senha e rejeição de senha incorreta")
    void testThzVaultOperacoes() throws IOException {
        Path tempVault = Files.createTempFile("segredos_", ".thzvault");
        try {
            String textoSecreto = "Chaves API de Produção: [AI_KEY_991, DB_SECRET_442]";
            String senhaCofre = "Chave-Forte-Vault-2026";

            ThzVault.salvarTexto(tempVault, textoSecreto, senhaCofre);
            assertTrue(Files.size(tempVault) > 0);

            // Leitura com senha correta
            String lido = ThzVault.lerTexto(tempVault, senhaCofre);
            assertEquals(textoSecreto, lido);

            // Leitura com senha incorreta deve falhar
            assertThrows(IOException.class, () -> ThzVault.lerTexto(tempVault, "SenhaInvalida"));
        } finally {
            Files.deleteIfExists(tempVault);
        }
    }

    @Test
    @DisplayName("Deve executar programa THZ utilizando funções nativas de SEGURANCA.argon2 e SEGURANCA.cofre*")
    void testExecucaoDslCofre() {
        String codigo = """
            PROGRAMA TesteCofreDsl
            REGRA_NEGOCIO R1
                OPERACAO TestarArgon2() : LOGICO
                INICIO
                    VARIAVEL senha : TEXTO <- "SenhaMestra123"
                    VARIAVEL hash : TEXTO <- SEGURANCA.argon2(senha)
                    VARIAVEL valido : LOGICO <- SEGURANCA.verificarArgon2(senha, hash)
                    RETORNE valido
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        var tokens = new ThzLexer(codigo).tokenize();
        var parser = new ThzParser(tokens);
        ProgramaAst ast = parser.parse();

        var interpretador = new InterpretadorThz(ast);
        var res = interpretador.executarOperacao("TestarArgon2", java.util.Map.of());
        assertEquals("VERDADEIRO", interpretador.formatar(res));
    }
}
