package thz.lang.net;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * ThzHttpServer — Servidor HTTP embutido de altíssimo desempenho com Virtual Threads (Java 25).
 */
public final class ThzHttpServer {

    public record Requisicao(
            String metodo,
            String caminho,
            String query,
            Map<String, String> parametros,
            Map<String, String> cabecalhos,
            String corpo
    ) {}

    public record Resposta(
            int status,
            String corpo,
            String contentType,
            Map<String, String> cabecalhos
    ) {
        public static Resposta ok(String corpo) {
            return new Resposta(200, corpo, "application/json; charset=utf-8", Map.of());
        }
        public static Resposta json(int status, String corpoJson) {
            return new Resposta(status, corpoJson, "application/json; charset=utf-8", Map.of());
        }
        public static Resposta erro(int status, String mensagem) {
            return new Resposta(status, "{\"erro\":\"" + mensagem.replace("\"", "\\\"") + "\"}", "application/json; charset=utf-8", Map.of());
        }
    }

    private static HttpServer servidor;
    private static final Map<String, Function<Requisicao, Resposta>> ROTAS = new ConcurrentHashMap<>();
    private static int portaAtiva = 0;

    private ThzHttpServer() {}

    public static synchronized void iniciar(int porta) {
        iniciar(porta, "0.0.0.0");
    }

    public static synchronized void iniciar(int porta, String host) {
        if (servidor != null) {
            throw new IllegalStateException("Servidor HTTP já está em execução na porta " + portaAtiva);
        }
        try {
            servidor = HttpServer.create(new InetSocketAddress(host, porta), 0);
            // Java 25 Virtual Threads para concorrência massiva O(1)
            servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            servidor.createContext("/", new DespachanteGeral());
            servidor.start();
            portaAtiva = servidor.getAddress().getPort();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao iniciar ThzHttpServer na porta " + porta + ": " + e.getMessage(), e);
        }
    }

    public static void registrarRota(String metodo, String caminho, Function<Requisicao, Resposta> handler) {
        String chave = metodo.toUpperCase() + ":" + caminho;
        ROTAS.put(chave, handler);
    }

    public static synchronized void parar() {
        if (servidor != null) {
            servidor.stop(0);
            servidor = null;
            portaAtiva = 0;
            ROTAS.clear();
        }
    }

    public static boolean estaRodando() {
        return servidor != null;
    }

    public static int getPortaAtiva() {
        return portaAtiva;
    }

    private static class DespachanteGeral implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String metodo = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();

            Map<String, String> cabecalhos = new HashMap<>();
            exchange.getRequestHeaders().forEach((k, v) -> {
                if (!v.isEmpty()) cabecalhos.put(k.toLowerCase(), v.get(0));
            });

            String corpo = "";
            try (InputStream is = exchange.getRequestBody()) {
                corpo = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            Map<String, String> params = parseQuery(query);

            Requisicao req = new Requisicao(metodo, path, query, params, cabecalhos, corpo);
            String chave = metodo + ":" + path;
            Function<Requisicao, Resposta> handler = ROTAS.get(chave);

            // Fallback para rotas gerais ou 404
            Resposta resp;
            if (handler != null) {
                try {
                    resp = handler.apply(req);
                } catch (Exception e) {
                    resp = Resposta.erro(500, "Erro interno: " + e.getMessage());
                }
            } else if ("OPTIONS".equals(metodo)) {
                resp = new Resposta(204, "", "text/plain", Map.of(
                        "Access-Control-Allow-Origin", "*",
                        "Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS",
                        "Access-Control-Allow-Headers", "*"
                ));
            } else {
                resp = Resposta.erro(404, "Rota não encontrada: " + metodo + " " + path);
            }

            byte[] bytes = resp.corpo() != null ? resp.corpo().getBytes(StandardCharsets.UTF_8) : new byte[0];
            exchange.getResponseHeaders().set("Content-Type", resp.contentType() != null ? resp.contentType() : "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            if (resp.cabecalhos() != null) {
                resp.cabecalhos().forEach((k, v) -> exchange.getResponseHeaders().set(k, v));
            }

            exchange.sendResponseHeaders(resp.status(), bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private Map<String, String> parseQuery(String query) {
            if (query == null || query.isBlank()) return Map.of();
            Map<String, String> map = new LinkedHashMap<>();
            for (String par : query.split("&")) {
                int eq = par.indexOf('=');
                if (eq > 0) {
                    map.put(par.substring(0, eq), par.substring(eq + 1));
                } else if (!par.isBlank()) {
                    map.put(par, "");
                }
            }
            return map;
        }
    }
}
