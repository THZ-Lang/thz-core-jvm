package thz.lang.webview;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class ThzWebViewBridgeTest {

    @AfterEach
    void tearDown() {
        ThzWebViewBridge.parar();
    }

    @Test
    @DisplayName("ThzWebViewBridge deve iniciar servidor local, injetar SDK JS e responder a chamadas RPC")
    void testBridgeRpcEInjecao() throws Exception {
        String htmlOriginal = "<h1>Aplicação Visual THZ</h1><p>Conteúdo de teste</p>";
        int porta = ThzWebViewBridge.iniciar(htmlOriginal);
        assertTrue(porta > 0);

        ThzWebViewBridge.registrarCanal("somar", payload -> {
            return "{\"resultado\":42}";
        });

        HttpClient client = HttpClient.newHttpClient();

        // 1. Obter HTML servido e verificar injeção do SDK window.thz
        HttpRequest reqHtml = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + porta + "/"))
                .GET()
                .build();
        HttpResponse<String> respHtml = client.send(reqHtml, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respHtml.statusCode());
        assertTrue(respHtml.body().contains("window.thz ="));
        assertTrue(respHtml.body().contains("Aplicação Visual THZ"));

        // 2. Invocar RPC do JS para o motor THZ
        HttpRequest reqRpc = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + porta + "/thz-bridge/rpc"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"canal\":\"somar\",\"payload\":\"dados\"}"))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> respRpc = client.send(reqRpc, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respRpc.statusCode());
        assertTrue(respRpc.body().contains("42"));
    }

    @Test
    @DisplayName("ThzWebViewBridge deve enfileirar e despachar eventos para o JS")
    void testEventosBridge() throws Exception {
        int porta = ThzWebViewBridge.iniciar("<div></div>");
        ThzWebViewBridge.emitirParaJs("atualizacao_saldo", "{\"saldo\":\"1500.50\"}");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + porta + "/thz-bridge/events"))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("atualizacao_saldo"));
        assertTrue(resp.body().contains("1500.50"));
    }
}
