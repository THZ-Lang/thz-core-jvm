package thz.lang.pipeline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThzPipelineDataEngineTest {

    @Test
    @DisplayName("ThzPipelineDataEngine deve executar pipeline em modo LOTE com Virtual Threads")
    void testExecutarLote() {
        ThzPipelineDataEngine.FonteConfig fonte = new ThzPipelineDataEngine.FonteConfig("POSTGRESQL", "LOTE", "jdbc:postgresql://localhost:5432/db", "JSONB", Map.of());
        ThzPipelineDataEngine.DestinoConfig destino = new ThzPipelineDataEngine.DestinoConfig("MONGODB", "faturas_agregadas", Map.of());

        List<ThzPipelineDataEngine.RegistroDado> lote = List.of(
                new ThzPipelineDataEngine.RegistroDado(Map.of("id", "TX-1001", "valor", 150.50)),
                new ThzPipelineDataEngine.RegistroDado(Map.of("id", "TX-1002", "valor", 300.00))
        );

        ThzPipelineDataEngine.ResultadoPipeline resultado = ThzPipelineDataEngine.executarLote(fonte, destino, lote);

        assertTrue(resultado.sucesso());
        assertEquals(2, resultado.totalProcessado());
        assertEquals(0, resultado.erros());
    }

    @Test
    @DisplayName("ThzPipelineDataEngine deve executar pipeline no modo STREAMING em tempo real")
    void testSimularStreaming() {
        ThzPipelineDataEngine.FonteConfig fonte = new ThzPipelineDataEngine.FonteConfig("KAFKA", "STREAMING", "kafka://broker:9092/topico", "JSONB", Map.of());
        ThzPipelineDataEngine.DestinoConfig destino = new ThzPipelineDataEngine.DestinoConfig("POSTGRESQL", "faturas_processadas", Map.of());

        ThzPipelineDataEngine.ResultadoPipeline resultado = ThzPipelineDataEngine.simularStreaming(fonte, destino, 100);

        assertTrue(resultado.sucesso());
        assertEquals(100, resultado.totalProcessado());
    }
}
