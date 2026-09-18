package thz.lang.mensageria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.interpretador.ValorThz;



import static org.junit.jupiter.api.Assertions.*;

public class ThzMessagingBridgeTest {

    @BeforeEach
    void setup() {
        ThzMessagingBridge.resetar();
    }

    @Test
    @DisplayName("ThzMessagingBridge deve operar com driver embutido por padrão")
    void testDriverEmbutido() {
        ThzMessagingBridge.conectar("embutido", "auto");
        assertEquals("EMBUTIDO", ThzMessagingBridge.driverAtivo());
        assertTrue(ThzMessagingBridge.statusConexao());

        String topico = "testes.pedidos";
        ThzMessagingBridge.limparTopico(topico);

        long off1 = ThzMessagingBridge.publicar(topico, ValorThz.TEXTO("Mensagem 1"));
        long off2 = ThzMessagingBridge.publicar(topico, ValorThz.TEXTO("Mensagem 2"));

        assertEquals(1, off1);
        assertEquals(2, off2);
        assertEquals(2, ThzMessagingBridge.tamanhoFila(topico));

        var msg1 = ThzMessagingBridge.consumir(topico, 100);
        assertNotNull(msg1);
        assertEquals("Mensagem 1", msg1.payload().formatar());

        var msg2 = ThzMessagingBridge.consumir(topico, 100);
        assertNotNull(msg2);
        assertEquals("Mensagem 2", msg2.payload().formatar());

        assertEquals(0, ThzMessagingBridge.tamanhoFila(topico));
    }

    @Test
    @DisplayName("ThzMessagingBridge deve instanciar drivers externos corretamente")
    void testDriversExternos() {
        ThzMessagingBridge.conectar("rabbitmq", "amqp://localhost:5672");
        assertEquals("RABBITMQ", ThzMessagingBridge.driverAtivo());
        assertTrue(ThzMessagingBridge.urlAtiva().contains("5672"));

        ThzMessagingBridge.conectar("kafka", "localhost:9092");
        assertEquals("KAFKA", ThzMessagingBridge.driverAtivo());

        ThzMessagingBridge.conectar("sqs", "http://localhost:4566");
        assertEquals("AWS_SQS", ThzMessagingBridge.driverAtivo());

        ThzMessagingBridge.conectar("sns", "arn:aws:sns:us-east-1:123456789012:meu-topico");
        assertEquals("AWS_SNS", ThzMessagingBridge.driverAtivo());
    }
}
