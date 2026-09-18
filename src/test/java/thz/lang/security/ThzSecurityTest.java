package thz.lang.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThzSecurityTest {

    @Test
    @DisplayName("Deve gerar hashes SHA-256 e SHA-512 consistentes")
    void testHashes() {
        String hash256 = ThzSecurity.sha256("thz-lang-2026");
        assertNotNull(hash256);
        assertEquals(64, hash256.length());

        String hash512 = ThzSecurity.sha512("thz-lang-2026");
        assertNotNull(hash512);
        assertEquals(128, hash512.length());
    }

    @Test
    @DisplayName("Deve calcular HMAC-SHA256")
    void testHmac() {
        String hmac = ThzSecurity.hmacSha256("payload_mensagem", "chave_secreta_super_forte");
        assertNotNull(hmac);
        assertEquals(64, hmac.length());
    }

    @Test
    @DisplayName("Deve criptografar e descriptografar com AES-256-GCM")
    void testAesGcm() {
        String texto = "Dados altamente confidenciais bancários.";
        String chave = "minha_chave_mestra_secreta";

        String cifrado = ThzSecurity.criptografarAes(texto, chave);
        assertNotNull(cifrado);
        assertNotEquals(texto, cifrado);

        String decifrado = ThzSecurity.descriptografarAes(cifrado, chave);
        assertEquals(texto, decifrado);
    }

    @Test
    @DisplayName("Deve gerar e verificar hash de senha com PBKDF2")
    void testSenhaHash() {
        String senha = "SenhaForteCorporativa#2026";
        String hash = ThzSecurity.hashSenha(senha);

        assertTrue(ThzSecurity.verificarSenha(senha, hash));
        assertFalse(ThzSecurity.verificarSenha("SenhaIncorreta", hash));
    }

    @Test
    @DisplayName("Deve gerar tokens seguros e UUIDs")
    void testTokens() {
        String token = ThzSecurity.gerarToken(32);
        assertNotNull(token);
        assertFalse(token.isBlank());

        String uuid = ThzSecurity.gerarUuid();
        assertNotNull(uuid);
        assertEquals(36, uuid.length());
    }
}
