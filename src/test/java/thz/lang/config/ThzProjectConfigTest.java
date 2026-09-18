package thz.lang.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;


import static org.junit.jupiter.api.Assertions.*;

public class ThzProjectConfigTest {

    @Test
    @DisplayName("ThzProjectConfig deve carregar valores padrão quando arquivo não existir")
    void testConfigPadrao() {
        var cfg = ThzProjectConfig.criarPadrao(null);
        assertNotNull(cfg);
        assertEquals("pt-BR", cfg.projeto().dialeto());
        assertEquals("auto", cfg.banco().driver());
        assertEquals("auto", cfg.mensageria().driver());
        assertTrue(cfg.governanca().conformidade().contains("ISO-IEC-10967"));
    }

    @Test
    @DisplayName("ThzProjectConfig deve parsear JSON customizado corretamente")
    void testParsingJsonCustomizado() {
        String json = """
        {
          "projeto": {
            "nome": "FintechApp",
            "versao": "2.1.0",
            "autor": "Lucas Thomaz",
            "dialeto": "pt-BR",
            "descricao": "Sistema de Pagamentos"
          },
          "banco": {
            "driver": "postgres",
            "url": "jdbc:postgresql://localhost:5432/financeiro",
            "usuario": "postgres",
            "senha": "secret",
            "poolMin": 5,
            "poolMax": 20,
            "autoMigracao": true,
            "vetorial": "pgvector"
          },
          "mensageria": {
            "driver": "rabbitmq",
            "url": "amqp://localhost:5672",
            "host": "localhost",
            "porta": 5672,
            "topicoPadrao": "pagamentos.eventos",
            "autoCriarFilas": true
          },
          "ia": {
            "motorEmbeddings": "local-fnv1a",
            "dimensaoVetor": 256,
            "armazenamentoVetorial": "pgvector"
          },
          "governanca": {
            "modoEstrito": true,
            "sloLatencia": "5ms",
            "conformidade": ["ISO-IEC-10967", "PCI-DSS", "LGPD-Art7"]
          }
        }
        """;

        var cfg = ThzProjectConfig.parsearJson(json, Path.of("thz.config.json"));
        assertEquals("FintechApp", cfg.projeto().nome());
        assertEquals("2.1.0", cfg.projeto().versao());
        assertEquals("postgres", cfg.banco().driver());
        assertEquals("jdbc:postgresql://localhost:5432/financeiro", cfg.banco().url());
        assertEquals(5, cfg.banco().poolMin());
        assertEquals("rabbitmq", cfg.mensageria().driver());
        assertEquals(5672, cfg.mensageria().porta());
        assertEquals(256, cfg.ia().dimensaoVetor());
        assertTrue(cfg.governanca().modoEstrito());
        assertTrue(cfg.governanca().conformidade().contains("PCI-DSS"));
    }
}
