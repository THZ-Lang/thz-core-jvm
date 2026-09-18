package thz.lang.webview;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Function;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import thz.lang.interpretador.ValorThz;

/**
 * ThzWebViewBridge — Ponte de comunicação bidirecional de alta velocidade entre
 * THZ-LANG e JavaScript na WebView.
 * Fornece servidor local em Virtual Threads com injeção automática de SDK
 * JavaScript e barramento RPC.
 */
public final class ThzWebViewBridge {

    private static HttpServer server;
    private static int porta = 0;
    private static String htmlAtual = "";
    private static final Map<String, Function<String, String>> CANAIS_RPC = new ConcurrentHashMap<>();
    private static final Queue<String> EVENTOS_PENDENTES = new ConcurrentLinkedQueue<>();
    private static final Map<String, List<Function<ValorThz, ValorThz>>> LISTENERS_EVENTOS = new ConcurrentHashMap<>();

    private ThzWebViewBridge() {
    }

    public static synchronized int iniciar(String htmlInicial) {
        htmlAtual = htmlInicial != null ? htmlInicial : "";
        if (server != null) {
            return porta;
        }

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            try {
                server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            } catch (Throwable t) {
                server.setExecutor(Executors.newCachedThreadPool(r -> {
                    Thread th = new Thread(r, "thz-webview-" + System.nanoTime());
                    th.setDaemon(true);
                    return th;
                }));
            }

            server.createContext("/", new DespachanteHtml());
            server.createContext("/thz-bridge/rpc", new DespachanteRpc());
            server.createContext("/thz-bridge/events", new DespachanteEventos());

            server.start();
            porta = server.getAddress().getPort();
            try {
                Runtime.getRuntime().addShutdownHook(new Thread(ThzWebViewBridge::parar, "thz-bridge-shutdown"));
            } catch (Exception ignore) {
            }
            return porta;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao iniciar ThzWebViewBridge: " + e.getMessage(), e);
        }
    }

    public static String getUrl() {
        return "http://127.0.0.1:" + porta + "/";
    }

    public static synchronized void atualizarHtml(String novoHtml) {
        htmlAtual = novoHtml;
    }

    private static volatile Function<String, String> fallbackRpcHandler = null;

    public static void setFallbackRpcHandler(Function<String, String> handler) {
        fallbackRpcHandler = handler;
    }

    public static void registrarCanal(String canal, Function<String, String> handler) {
        CANAIS_RPC.put(canal, handler);
    }

    public static void registrarListener(String evento, Function<ValorThz, ValorThz> listener) {
        LISTENERS_EVENTOS.computeIfAbsent(evento, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public static void emitirParaJs(String evento, String dadosJson) {
        EVENTOS_PENDENTES.offer("{\"evento\":\"" + escaparJson(evento) + "\",\"dados\":"
                + (dadosJson != null ? dadosJson : "null") + "}");
    }

    public static synchronized void parar() {
        if (server != null) {
            server.stop(0);
            server = null;
            porta = 0;
            CANAIS_RPC.clear();
            EVENTOS_PENDENTES.clear();
            LISTENERS_EVENTOS.clear();
        }
    }

    public static String injetarSdkThz(String htmlOriginal) {
        String sdk = """
                <script>
                // THZ Native WebView Bridge SDK
                window.thz = {
                  async invocar(canal, payload = null) {
                    const resp = await fetch('/thz-bridge/rpc', {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({ canal, payload })
                    });
                    return await resp.json();
                  },
                  ouvir(evento, callback) {
                    window.addEventListener('thz:' + evento, (e) => callback(e.detail));
                  },
                  dispararEvento(evento, dados) {
                    return this.invocar('__evento__', { evento, dados });
                  }
                };

                // Polling contínuo de eventos THZ -> JS via Virtual Threads
                (function iniciarEventLoop() {
                  setInterval(async () => {
                    try {
                      const res = await fetch('/thz-bridge/events');
                      if (res.ok) {
                        const lista = await res.json();
                        for (const item of lista) {
                          window.dispatchEvent(new CustomEvent('thz:' + item.evento, { detail: item.dados }));
                        }
                      }
                    } catch(e) {}
                  }, 100);
                })();
                </script>
                """;

        if (htmlOriginal.contains("</head>")) {
            return htmlOriginal.replace("</head>", sdk + "</head>");
        } else if (htmlOriginal.contains("<body>")) {
            return htmlOriginal.replace("<body>", "<body>" + sdk);
        } else {
            return "<!DOCTYPE html><html><head>" + sdk + "</head><body>" + htmlOriginal + "</body></html>";
        }
    }

    private static class DespachanteHtml implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String htmlInjetado = injetarSdkThz(htmlAtual);
            byte[] bytes = htmlInjetado.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static class DespachanteRpc implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String corpo;
            try (InputStream is = exchange.getRequestBody()) {
                corpo = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            String canal = ThzJson.extrairCampo(corpo, "canal");
            String payload = ThzJson.extrairBruto(corpo, "payload");

            String respostaJson = ThzJson.ok(null);
            if ("__evento__".equals(canal)) {
                String evento = ThzJson.extrairCampo(payload, "evento");
                String dados = ThzJson.extrairBruto(payload, "dados");
                List<Function<ValorThz, ValorThz>> listeners = LISTENERS_EVENTOS.get(evento);
                if (listeners != null) {
                    for (var l : listeners) {
                        try {
                            l.apply(ValorThz.TEXTO(dados));
                        } catch (Exception ignored) {
                        }
                    }
                }
            } else {
                Function<String, String> handler = CANAIS_RPC.get(canal);
                if (handler != null) {
                    try {
                        String ret = handler.apply(payload);
                        respostaJson = ret != null ? ret : ThzJson.ok(null);
                    } catch (Exception e) {
                        respostaJson = ThzJson.erro(e.getMessage());
                    }
                } else if (fallbackRpcHandler != null) {
                    try {
                        String ret = fallbackRpcHandler.apply(canal);
                        respostaJson = ret != null ? ret : ThzJson.ok(null);
                    } catch (Exception e) {
                        respostaJson = ThzJson.erro(e.getMessage());
                    }
                } else {
                    respostaJson = ThzJson.erro("Canal RPC não encontrado: " + canal);
                }
            }

            byte[] bytes = respostaJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static class DespachanteEventos implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> eventos = new ArrayList<>();
            while (!EVENTOS_PENDENTES.isEmpty()) {
                String ev = EVENTOS_PENDENTES.poll();
                if (ev != null)
                    eventos.add(ev);
            }

            String json = "[" + String.join(",", eventos) + "]";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    public static String extrairCampoJson(String json, String campo) {
        if (json == null)
            return "";
        String busca = "\"" + campo + "\":";
        int idx = json.indexOf(busca);
        if (idx < 0)
            return "";
        int ini = idx + busca.length();
        while (ini < json.length() && (json.charAt(ini) == ' ' || json.charAt(ini) == '"'))
            ini++;
        int fim = ini;
        while (fim < json.length() && json.charAt(fim) != '"' && json.charAt(fim) != ',' && json.charAt(fim) != '}')
            fim++;
        return json.substring(ini, fim).trim();
    }

    public static String extrairObjetoOuValorJson(String json, String campo) {
        if (json == null)
            return "";
        String busca = "\"" + campo + "\":";
        int idx = json.indexOf(busca);
        if (idx < 0)
            return "";
        int ini = idx + busca.length();
        while (ini < json.length() && Character.isWhitespace(json.charAt(ini)))
            ini++;
        int fim = json.lastIndexOf('}');
        if (fim > ini)
            return json.substring(ini, fim).trim();
        return json.substring(ini).trim();
    }

    private static String escaparJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
