package thz.lang.log;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ThzLog — Logging estruturado de alto desempenho com suporte a governança corporativa e JSON.
 */
public final class ThzLog {

    public enum Nivel {
        RASTREIO(1),
        INFO(2),
        AVISO(3),
        ERRO(4),
        AUDITORIA(5);

        private final int peso;
        Nivel(int peso) { this.peso = peso; }
        public int getPeso() { return peso; }
    }

    private static Nivel nivelMinimo = Nivel.INFO;
    private static Consumer<String> escritor = System.out::println;
    private static final Map<String, String> CONTEXTO_GLOBAL = new ConcurrentHashMap<>();

    private ThzLog() {}

    public static void setNivelMinimo(Nivel nivel) {
        if (nivel != null) nivelMinimo = nivel;
    }

    public static void setEscritor(Consumer<String> novoEscritor) {
        if (novoEscritor != null) escritor = novoEscritor;
    }

    public static void definirContexto(String chave, String valor) {
        if (chave != null && valor != null) CONTEXTO_GLOBAL.put(chave, valor);
    }

    public static void limparContexto() {
        CONTEXTO_GLOBAL.clear();
    }

    public static void info(String mensagem) {
        log(Nivel.INFO, mensagem, null);
    }

    public static void info(String mensagem, Map<String, Object> dados) {
        log(Nivel.INFO, mensagem, dados);
    }

    public static void aviso(String mensagem) {
        log(Nivel.AVISO, mensagem, null);
    }

    public static void aviso(String mensagem, Map<String, Object> dados) {
        log(Nivel.AVISO, mensagem, dados);
    }

    public static void erro(String mensagem) {
        log(Nivel.ERRO, mensagem, null);
    }

    public static void erro(String mensagem, Map<String, Object> dados) {
        log(Nivel.ERRO, mensagem, dados);
    }

    public static void auditoria(String evento, String usuario, String recurso) {
        log(Nivel.AUDITORIA, evento, Map.of("usuario", usuario != null ? usuario : "anonimo", "recurso", recurso != null ? recurso : "sistema"));
    }

    private static void log(Nivel nivel, String mensagem, Map<String, Object> dados) {
        if (nivel.getPeso() < nivelMinimo.getPeso()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"timestamp\":\"").append(Instant.now().toString()).append("\",");
        sb.append("\"nivel\":\"").append(nivel.name()).append("\",");
        sb.append("\"mensagem\":\"").append(escaparJson(mensagem)).append("\"");

        if (!CONTEXTO_GLOBAL.isEmpty()) {
            sb.append(",\"contexto\":{");
            boolean prim = true;
            for (Map.Entry<String, String> e : CONTEXTO_GLOBAL.entrySet()) {
                if (!prim) sb.append(",");
                prim = false;
                sb.append("\"").append(escaparJson(e.getKey())).append("\":\"").append(escaparJson(e.getValue())).append("\"");
            }
            sb.append("}");
        }

        if (dados != null && !dados.isEmpty()) {
            sb.append(",\"dados\":{");
            boolean prim = true;
            for (Map.Entry<String, Object> e : dados.entrySet()) {
                if (!prim) sb.append(",");
                prim = false;
                sb.append("\"").append(escaparJson(e.getKey())).append("\":\"").append(escaparJson(String.valueOf(e.getValue()))).append("\"");
            }
            sb.append("}");
        }

        sb.append("}");
        escritor.accept(sb.toString());
    }

    private static String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
