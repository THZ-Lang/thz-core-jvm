package thz.lang.net;

import com.sun.net.httpserver.HttpExchange;

import com.sun.net.httpserver.HttpServer;
import thz.lang.ast.ProcedimentoAst;
import thz.lang.ast.ProgramaAst;

import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.sintatico.ThzParser;

import thz.lang.ui.ThzUiMaker;
import thz.lang.ui.ThzUiTema;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ThzEmbeddedWebServer — Servidor Web Embutido Oficial (Zero Config / Batteries-Included).
 * <p>
 * Suporta Virtual Threads (Java 25), renderização declarativa de interfaces .thzui (HTML5 Glassmorphism),
 * despacho de chamadas RPC com execução de contratos EXIGE/GARANTE e sincronização bidirecional de estado.
 */
public final class ThzEmbeddedWebServer {

    public record ConfiguracaoServidor(
            int portaDesejada,
            String host,
            boolean autoAlocarPortaLivre,
            boolean abrirNavegador,
            ThzUiTema tema,
            boolean usarVaadin
    ) {
        public static ConfiguracaoServidor padrao(int porta) {
            return new ConfiguracaoServidor(porta, "0.0.0.0", true, false, ThzUiTema.escuroGlass(), false);
        }

        public static ConfiguracaoServidor vaadin(int porta) {
            return new ConfiguracaoServidor(porta, "0.0.0.0", true, false, ThzUiTema.escuroGlass(), true);
        }
    }

    private HttpServer servidor;
    private int portaAtiva = -1;
    private final AtomicBoolean rodando = new AtomicBoolean(false);
    private final Map<String, Object> estadoGlobal = new ConcurrentHashMap<>();
    private ProgramaAst astAtiva;
    private InterpretadorThz interpretador;
    public ThzEmbeddedWebServer() {}

    /**
     * Inicia o servidor embutido a partir de um código-fonte ou arquivo .thz / .thzui.
     */
    public synchronized String iniciar(Path caminhoArquivo, ConfiguracaoServidor config) throws IOException {
        String fonte = Files.readString(caminhoArquivo, StandardCharsets.UTF_8);
        List<Token> tokens = new ThzLexer(fonte).tokenize();
        this.astAtiva = new ThzParser(tokens).parse();
        this.interpretador = new InterpretadorThz(astAtiva, System.out::println, null);

        return iniciarComAst(astAtiva, config);
    }

    /**
     * Inicia o servidor embutido a partir de uma AST já analisada.
     */
    public synchronized String iniciarComAst(ProgramaAst ast, ConfiguracaoServidor config) throws IOException {
        if (rodando.get()) {
            throw new IllegalStateException("Servidor embutido já está ativo na porta " + portaAtiva);
        }

        this.astAtiva = ast;
        if (this.interpretador == null) {
            this.interpretador = new InterpretadorThz(ast, System.out::println, null);
        }

        ConfiguracaoServidor cfg = (config != null) ? config : ConfiguracaoServidor.padrao(8080);
        int portaFinal = resolverPortaDisponivel(cfg.portaDesejada(), cfg.autoAlocarPortaLivre());

        this.servidor = HttpServer.create(new InetSocketAddress(cfg.host(), portaFinal), 0);
        // Concorrência massiva com Virtual Threads do Java 25
        this.servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        configurarRotas(cfg);

        this.servidor.start();
        this.portaAtiva = portaFinal;
        this.rodando.set(true);

        String url = "http://localhost:" + portaAtiva + "/";

        if (cfg.abrirNavegador()) {
            abrirNavegadorPadrao(url);
        }

        return url;
    }

    private void configurarRotas(ConfiguracaoServidor cfg) {
        // 1. Rota Raiz: Renderiza a página HTML5 Glassmorphism
        this.servidor.createContext("/", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                enviarResposta(exchange, 405, "{\"erro\":\"Método não permitido\"}", "application/json");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path != null && !path.equals("/") && !path.equals("/index.html")) {
                enviarResposta(exchange, 404, "{\"erro\":\"Recurso não encontrado\"}", "application/json");
                return;
            }

            String html = gerarHtmlPagina(cfg);
            enviarResposta(exchange, 200, html, "text/html; charset=utf-8");
        });

        // 2. Rota RPC: Invocação de Procedimentos / Ações com Validação de Contratos
        this.servidor.createContext("/api/rpc/invocar", exchange -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                enviarCabecalhosCors(exchange, 204, 0);
                exchange.close();
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                enviarResposta(exchange, 405, "{\"status\":\"erro\",\"erro\":\"Método POST esperado\"}", "application/json");
                return;
            }

            try {
                String corpo;
                try (InputStream is = exchange.getRequestBody()) {
                    corpo = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }

                Map<String, Object> payload = parseJsonSimples(corpo);
                String acao = String.valueOf(payload.getOrDefault("acao", payload.getOrDefault("procedimento", "")));
                Object rawEstado = payload.get("estado");

                if (rawEstado instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> e : map.entrySet()) {
                        if (e.getKey() != null) {
                            estadoGlobal.put(String.valueOf(e.getKey()), e.getValue());
                        }
                    }
                }

                if (acao.isBlank()) {
                    enviarResposta(exchange, 400, "{\"status\":\"erro\",\"erro\":\"Nome da ação ou procedimento não informado\"}", "application/json");
                    return;
                }

                // Executa no Interpretador com captura de contratos
                var respostaRpc = executarAcaoThz(acao);
                enviarResposta(exchange, 200, respostaRpc, "application/json; charset=utf-8");
            } catch (Exception ex) {
                String erroJson = String.format("{\"status\":\"erro\",\"erro\":\"%s\"}", escaparJson(ex.getMessage()));
                enviarResposta(exchange, 500, erroJson, "application/json; charset=utf-8");
            }
        });

        // 3. Rota de Estado Atual
        this.servidor.createContext("/api/estado", exchange -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                enviarCabecalhosCors(exchange, 204, 0);
                exchange.close();
                return;
            }

            StringBuilder sb = new StringBuilder("{");
            boolean prim = true;
            for (Map.Entry<String, Object> e : estadoGlobal.entrySet()) {
                if (!prim) sb.append(",");
                prim = false;
                sb.append("\"").append(escaparJson(e.getKey())).append("\":");
                if (e.getValue() instanceof Number || e.getValue() instanceof Boolean) {
                    sb.append(e.getValue());
                } else {
                    sb.append("\"").append(escaparJson(String.valueOf(e.getValue()))).append("\"");
                }
            }
            sb.append("}");
            enviarResposta(exchange, 200, sb.toString(), "application/json; charset=utf-8");
        });

        // 4. Rota Health Check
        this.servidor.createContext("/api/health", exchange -> {
            String json = """
                    {"status":"UP","runtime":"THZ-LANG Engine v3.0.0","virtualThreads":true,"servidor":"Embedded Zero-Config"}
                    """;
            enviarResposta(exchange, 200, json.trim(), "application/json; charset=utf-8");
        });
    }

    private String gerarHtmlPagina(ConfiguracaoServidor cfg) {
        ThzUiTema t = (cfg != null && cfg.tema() != null) ? cfg.tema() : ThzUiTema.escuroGlass();
        String titulo = (astAtiva != null) ? astAtiva.nome() : "THZ Application";

        var maker = ThzUiMaker.container("app_raiz", c -> {
            c.adicionar(ThzUiMaker.card("card_principal", titulo, card -> {
                card.adicionar(ThzUiMaker.alerta("alerta_servidor", "info",
                        "Aplicação THZ servida via Embedded Engine com Virtual Threads (Java 25)"));

                if (astAtiva != null && astAtiva.procedimentos() != null) {
                    for (ProcedimentoAst p : astAtiva.procedimentos()) {
                        card.adicionar(ThzUiMaker.botao("btn_" + p.nome(), p.nome(), p.nome()));
                    }
                }
            }));
        });

        if (cfg != null && cfg.usarVaadin()) {
            return thz.lang.ui.ThzUiVaadinEmitter.renderizarPaginaVaadin(titulo, maker.construir(), true);
        }

        return maker.renderizarHtml(titulo, t);
    }

    private String executarAcaoThz(String acao) {
        if (astAtiva == null || interpretador == null) {
            return "{\"status\":\"erro\",\"erro\":\"Nenhum programa ou interpretador ativo\"}";
        }

        ProcedimentoAst proc = null;
        if (astAtiva.procedimentos() != null) {
            proc = astAtiva.procedimentos().stream()
                    .filter(p -> p.nome().equalsIgnoreCase(acao))
                    .findFirst()
                    .orElse(null);
        }

        if (proc != null) {
            try {
                Map<String, ValorThz> args = new LinkedHashMap<>();
                for (var param : proc.parametros()) {
                    Object val = estadoGlobal.get(param.nome());
                    if (val != null) {
                        args.put(param.nome(), converterValor(val));
                    }
                }

                interpretador.executarProcedimento(proc.nome(), args);
                return String.format("{\"status\":\"ok\",\"resultado\":\"Procedimento '%s' executado com sucesso.\",\"acao\":\"%s\"}",
                        escaparJson(proc.nome()), escaparJson(acao));
            } catch (Exception e) {
                return String.format("{\"status\":\"erro\",\"erro\":\"%s\",\"acao\":\"%s\"}",
                        escaparJson(e.getMessage()), escaparJson(acao));
            }
        }

        // Tenta encontrar em operações de regra de negócio
        var operacoes = interpretador.listarOperacoesExecutaveis();
        for (var opEntry : operacoes) {
            if (opEntry.operacao().nome().equalsIgnoreCase(acao)) {
                try {
                    Map<String, ValorThz> args = new LinkedHashMap<>();
                    for (var param : opEntry.operacao().parametros()) {
                        Object val = estadoGlobal.get(param.nome());
                        if (val != null) {
                            args.put(param.nome(), converterValor(val));
                        }
                    }

                    ValorThz res = interpretador.executarOperacao(opEntry.operacao().nome(), args);
                    String resStr = res != null ? interpretador.formatar(res) : "Sucesso";
                    return String.format("{\"status\":\"ok\",\"resultado\":\"%s\",\"acao\":\"%s\"}",
                            escaparJson(resStr), escaparJson(acao));
                } catch (Exception e) {
                    return String.format("{\"status\":\"erro\",\"erro\":\"%s\",\"acao\":\"%s\"}",
                            escaparJson(e.getMessage()), escaparJson(acao));
                }
            }
        }

        return String.format("{\"status\":\"ok\",\"resultado\":\"Ação '%s' recebida pelo servidor.\",\"acao\":\"%s\"}",
                escaparJson(acao), escaparJson(acao));
    }

    private static ValorThz converterValor(Object val) {
        if (val == null) return ValorThz.NULO;
        if (val instanceof Boolean b) return ValorThz.LOGICO(b);
        if (val instanceof Integer i) return ValorThz.INTEIRO(i.longValue());
        if (val instanceof Long l) return ValorThz.INTEIRO(l);
        if (val instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return ValorThz.INTEIRO(d.longValue());
            }
            return ValorThz.DECIMAL(d.toString());
        }
        if (val instanceof String s) {
            if (s.equalsIgnoreCase("verdadeiro") || s.equalsIgnoreCase("true")) return ValorThz.LOGICO(true);
            if (s.equalsIgnoreCase("falso") || s.equalsIgnoreCase("false")) return ValorThz.LOGICO(false);
            if (s.matches("^-?\\d+$")) {
                try {
                    return ValorThz.INTEIRO(Long.parseLong(s));
                } catch (NumberFormatException ignored) {}
            }
            if (s.matches("^-?\\d+\\.\\d+$")) {
                try {
                    return ValorThz.DECIMAL(s);
                } catch (Exception ignored) {}
            }
            return ValorThz.TEXTO(s);
        }
        return ValorThz.TEXTO(String.valueOf(val));
    }

    public synchronized void parar() {
        if (servidor != null) {
            servidor.stop(0);
            servidor = null;
            portaAtiva = -1;
            rodando.set(false);
            estadoGlobal.clear();
        }
    }

    public boolean estaRodando() {
        return rodando.get();
    }

    public int getPortaAtiva() {
        return portaAtiva;
    }

    public String getUrl() {
        return estaRodando() ? "http://localhost:" + portaAtiva + "/" : null;
    }

    public void atualizarEstado(String chave, Object valor) {
        if (chave != null) {
            estadoGlobal.put(chave, valor != null ? valor : "");
        }
    }

    public Object obterEstado(String chave) {
        return estadoGlobal.get(chave);
    }

    private static int resolverPortaDisponivel(int portaInicial, boolean autoAlocar) {
        if (portaInicial <= 0) portaInicial = 8080;

        if (!autoAlocar) return portaInicial;

        int porta = portaInicial;
        for (int tentativa = 0; tentativa < 100; tentativa++) {
            try (ServerSocket ss = new ServerSocket(porta)) {
                ss.setReuseAddress(true);
                return porta;
            } catch (IOException e) {
                porta++;
            }
        }
        return portaInicial;
    }

    private static void enviarResposta(HttpExchange exchange, int status, String corpo, String contentType) throws IOException {
        byte[] bytes = (corpo != null) ? corpo.getBytes(StandardCharsets.UTF_8) : new byte[0];
        exchange.getResponseHeaders().set("Content-Type", contentType);
        enviarCabecalhosCors(exchange, status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void enviarCabecalhosCors(HttpExchange exchange, int status, long length) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        exchange.sendResponseHeaders(status, length);
    }

    private static void abrirNavegadorPadrao(String url) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception ignored) {}
    }

    private static String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static Map<String, Object> parseJsonSimples(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        Map<String, Object> mapa = new LinkedHashMap<>();
        try {
            String limpo = json.trim();
            if (limpo.startsWith("{") && limpo.endsWith("}")) {
                limpo = limpo.substring(1, limpo.length() - 1).trim();
            }

            // Parser simplificado para chave: valor
            var matcher = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|\\{[^\\}]*\\}|[0-9.-]+|true|false|null)").matcher(limpo);
            while (matcher.find()) {
                String k = matcher.group(1);
                String v = matcher.group(2);
                if (v.startsWith("\"") && v.endsWith("\"")) {
                    mapa.put(k, v.substring(1, v.length() - 1));
                } else if (v.startsWith("{")) {
                    mapa.put(k, parseJsonSimples(v));
                } else if ("true".equalsIgnoreCase(v)) {
                    mapa.put(k, true);
                } else if ("false".equalsIgnoreCase(v)) {
                    mapa.put(k, false);
                } else if ("null".equalsIgnoreCase(v)) {
                    mapa.put(k, null);
                } else {
                    try {
                        mapa.put(k, Double.parseDouble(v));
                    } catch (NumberFormatException e) {
                        mapa.put(k, v);
                    }
                }
            }
        } catch (Exception ignored) {}
        return mapa;
    }
}