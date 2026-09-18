package thz.lang.net;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class ThzHttpServerTest {

    @AfterEach
    void tearDown() {
        ThzHttpServer.parar();
    }

    @Test
    @DisplayName("Deve iniciar servidor HTTP com Virtual Threads e responder requisições GET/POST")
    void testServidorHttp() throws Exception {
        ThzHttpServer.iniciar(0); // Porta aleatória livre
        int porta = ThzHttpServer.getPortaAtiva();
        assertTrue(porta > 0);
        assertTrue(ThzHttpServer.estaRodando());

        ThzHttpServer.registrarRota("GET", "/api/ping", req -> {
            return ThzHttpServer.Resposta.ok("{\"status\":\"pong\"}");
        });

        ThzHttpServer.registrarRota("POST", "/api/eco", req -> {
            return ThzHttpServer.Resposta.ok("{\"recebido\":\"" + req.corpo() + "\"}");
        });

        HttpClient client = HttpClient.newHttpClient();

        // 1. Teste GET /api/ping
        HttpRequest reqGet = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta + "/api/ping"))
                .GET()
                .build();
        HttpResponse<String> respGet = client.send(reqGet, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respGet.statusCode());
        assertTrue(respGet.body().contains("pong"));

        // 2. Teste POST /api/eco
        HttpRequest reqPost = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta + "/api/eco"))
                .POST(HttpRequest.BodyPublishers.ofString("dados_de_teste"))
                .build();
        HttpResponse<String> respPost = client.send(reqPost, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respPost.statusCode());
        assertTrue(respPost.body().contains("dados_de_teste"));
    }
}
