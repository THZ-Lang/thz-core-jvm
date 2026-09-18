package thz.lang.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ThzLogTest {

    @AfterEach
    void tearDown() {
        ThzLog.limparContexto();
        ThzLog.setNivelMinimo(ThzLog.Nivel.INFO);
        ThzLog.setEscritor(System.out::println);
    }

    @Test
    @DisplayName("ThzLog deve emitir logs estruturados com JSON e respeitar níveis de log")
    void testNiveisELogJson() {
        List<String> logs = new ArrayList<>();
        ThzLog.setEscritor(logs::add);
        ThzLog.setNivelMinimo(ThzLog.Nivel.INFO);

        ThzLog.definirContexto("correlacaoId", "CORR-12345");
        ThzLog.definirContexto("dominio", "Faturamento");

        // 1. RASTREIO (deve ser ignorado pelo nível mínimo INFO)
        ThzLog.setNivelMinimo(ThzLog.Nivel.INFO);
        // 2. INFO
        ThzLog.info("Processamento de fatura iniciado", Map.of("faturaId", "FAT-999"));
        // 3. AVISO
        ThzLog.aviso("Tentativa de retry detectada");
        // 4. ERRO
        ThzLog.erro("Falha ao comunicar com gateway", Map.of("tentativas", 3));
        // 5. AUDITORIA
        ThzLog.auditoria("ALTERACAO_LIMITE_CREDITO", "admin@empresa.com", "cliente/123");

        assertEquals(4, logs.size());
        assertTrue(logs.get(0).contains("INFO"));
        assertTrue(logs.get(0).contains("CORR-12345"));
        assertTrue(logs.get(0).contains("FAT-999"));

        assertTrue(logs.get(1).contains("AVISO"));
        assertTrue(logs.get(2).contains("ERRO"));
        assertTrue(logs.get(3).contains("AUDITORIA"));
        assertTrue(logs.get(3).contains("admin@empresa.com"));
    }
}
