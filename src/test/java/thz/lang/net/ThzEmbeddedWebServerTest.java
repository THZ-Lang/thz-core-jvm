package thz.lang.net;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class ThzEmbeddedWebServerTest {

    private ThzEmbeddedWebServer server;

    @AfterEach
    void tearDown() {
        if (server != null && server.estaRodando()) {
            server.parar();
        }
    }

    @Test
    @DisplayName("ThzEmbeddedWebServer deve iniciar na porta livre e responder rotas GET e Health Check")
    void testIniciarEServirPagina() throws Exception {
        String codigoThz = """
                TELA PainelControle
                METADADOS_ARQUITETURA
                    DOMINIO: "Operacional"
                    CAMADA: "Interface"
                    VERSAO: "3.0.0"
                    AUTOR: "DevOps"
                    SLO_LATENCIA_MAXIMA: "20ms"
                FIM_METADADOS
                
                PROCEDIMENTO ReiniciarServico()
                INICIO
                    EXIBA "Serviço reiniciado com sucesso"
                FIM
                FIM_TELA
                """;

        ProgramaAst ast = new ThzParser(new ThzLexer(codigoThz).tokenize()).parse();

        server = new ThzEmbeddedWebServer();
        String url = server.iniciarComAst(ast, ThzEmbeddedWebServer.ConfiguracaoServidor.padrao(0));

        assertNotNull(url);
        assertTrue(server.estaRodando());
        assertTrue(server.getPortaAtiva() > 0);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        // 1. GET / -> Retorna página HTML5 Glassmorphism
        HttpRequest reqHome = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> respHome = client.send(reqHome, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, respHome.statusCode());
        assertTrue(respHome.body().contains("<!DOCTYPE html>"));
        assertTrue(respHome.body().contains("PainelControle"));
        assertTrue(respHome.body().contains("ReiniciarServico"));

        // 2. GET /api/health -> Health Check
        HttpRequest reqHealth = HttpRequest.newBuilder()
                .uri(URI.create(url + "api/health"))
                .GET()
                .build();
        HttpResponse<String> respHealth = client.send(reqHealth, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, respHealth.statusCode());
        assertTrue(respHealth.body().contains("\"status\":\"UP\""));
        assertTrue(respHealth.body().contains("\"virtualThreads\":true"));
    }

    @Test
    @DisplayName("ThzEmbeddedWebServer deve processar chamadas RPC /api/rpc/invocar e atualizar estado")
    void testInvocacaoRpcEContratos() throws Exception {
        String codigoThz = """
                PROGRAMA VISUAL GestaoVendas
                METADADOS_ARQUITETURA
                    DOMINIO: "Comercial"
                    CAMADA: "Vendas"
                    VERSAO: "3.0.0"
                    AUTOR: "Engenharia"
                    SLO_LATENCIA_MAXIMA: "15ms"
                FIM_METADADOS
                
                REGRA_NEGOCIO Faturamento
                    IDENTIFICADOR_REGRA: "BR-FAT-01"
                    RASTREIO_REQUISITO: "REQ-100"
                    
                    CONTRATO_ENTRADA
                        EXIGE quantidade > 0
                    FIM_CONTRATO_ENTRADA
                    
                    OPERACAO CalcularTotal(quantidade : INTEIRO32, preco_unitario : INTEIRO32) : INTEIRO32
                    INICIO
                        RETORNE quantidade * preco_unitario
                    FIM
                FIM_REGRA_NEGOCIO
                
                PROCEDIMENTO SalvarPedido()
                INICIO
                    EXIBA "Pedido gravado com sucesso"
                FIM
                FIM_PROGRAMA
                """;

        ProgramaAst ast = new ThzParser(new ThzLexer(codigoThz).tokenize()).parse();

        server = new ThzEmbeddedWebServer();
        String url = server.iniciarComAst(ast, ThzEmbeddedWebServer.ConfiguracaoServidor.padrao(0));

        HttpClient client = HttpClient.newHttpClient();

        // 1. Invocar Procedimento SalvarPedido
        String jsonRpcProc = """
                {"acao":"SalvarPedido","estado":{"cliente":"Empresa ABC"}}
                """;
        HttpRequest reqRpc = HttpRequest.newBuilder()
                .uri(URI.create(url + "api/rpc/invocar"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRpcProc))
                .build();

        HttpResponse<String> respRpc = client.send(reqRpc, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respRpc.statusCode());
        assertTrue(respRpc.body().contains("\"status\":\"ok\""));

        // 2. Invocar Operação CalcularTotal com argumentos
        String jsonRpcOp = """
                {"acao":"CalcularTotal","estado":{"quantidade":5,"preco_unitario":100}}
                """;
        HttpRequest reqRpcOp = HttpRequest.newBuilder()
                .uri(URI.create(url + "api/rpc/invocar"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRpcOp))
                .build();

        HttpResponse<String> respRpcOp = client.send(reqRpcOp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respRpcOp.statusCode());
        assertTrue(respRpcOp.body().contains("\"status\":\"ok\""));

        // 3. Consultar /api/estado
        HttpRequest reqEstado = HttpRequest.newBuilder()
                .uri(URI.create(url + "api/estado"))
                .GET()
                .build();
        HttpResponse<String> respEstado = client.send(reqEstado, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respEstado.statusCode());
        assertTrue(respEstado.body().contains("\"cliente\":\"Empresa ABC\""));
    }
}