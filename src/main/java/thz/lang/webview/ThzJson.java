package thz.lang.webview;

import java.util.*;
import java.math.BigInteger;

/**
 * Utilitário JSON mínimo sem dependência externa — compatível com GraalVM native-image.
 * Suporta objetos simples, strings, números, booleanos, arrays rasos e parsing leve de payloads RPC.
 */
public final class ThzJson {

    private ThzJson() {}

    public static String stringify(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String s) return "\"" + escape(s) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map<?,?> m) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (var e : m.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escape(String.valueOf(e.getKey()))).append("\":");
                sb.append(stringify(e.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof List<?> l) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(stringify(l.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escape(String.valueOf(obj)) + "\"";
    }

    public static String ok(Object data) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("status", "ok");
        if (data != null) m.put("dados", data);
        return stringify(m);
    }

    public static String okMensagem(String msg) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("status", "ok");
        m.put("mensagem", msg);
        return stringify(m);
    }

    public static String erro(String msg) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("status", "erro");
        m.put("erro", msg != null ? msg : "erro desconhecido");
        return stringify(m);
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t");
    }

    // --- parsing helpers já existentes em ThzWebViewBridge, centralizados aqui ---

    /**
     * Extrai valor string simples de um JSON raso: "campo": "valor"
     */
    public static String extrairCampo(String json, String campo) {
        if (json == null) return "";
        String busca = "\"" + campo + "\":";
        int idx = json.indexOf(busca);
        if (idx < 0) return "";
        int ini = idx + busca.length();
        while (ini < json.length() && (json.charAt(ini) == ' ' || json.charAt(ini) == '\"')) ini++;
        int fim = ini;
        while (fim < json.length() && json.charAt(fim) != '\"' && json.charAt(fim) != ',' && json.charAt(fim) != '}') fim++;
        // se começou com aspas, procura fim de aspas
        if (ini > 0 && json.charAt(ini-1) == '\"') {
            fim = json.indexOf('\"', ini);
            if (fim < 0) fim = json.length();
            return json.substring(ini, fim);
        }
        return json.substring(ini, fim).trim().replace("\"","");
    }

    /**
     * Extrai objeto/valor bruto após "campo": até o penúltimo } do envelope.
     */
    public static String extrairBruto(String json, String campo) {
        if (json == null) return "";
        String busca = "\"" + campo + "\":";
        int idx = json.indexOf(busca);
        if (idx < 0) return "";
        int ini = idx + busca.length();
        while (ini < json.length() && Character.isWhitespace(json.charAt(ini))) ini++;
        // se é objeto/array, pega balanceado
        if (ini < json.length() && (json.charAt(ini) == '{' || json.charAt(ini) == '[')) {
            char abre = json.charAt(ini);
            char fecha = abre == '{' ? '}' : ']';
            int depth = 0;
            int fim = ini;
            for (int i = ini; i < json.length(); i++) {
                if (json.charAt(i) == abre) depth++;
                else if (json.charAt(i) == fecha) depth--;
                if (depth == 0) { fim = i; break; }
            }
            return json.substring(ini, fim+1);
        }
        // primitivo até , ou }
        int fim = ini;
        while (fim < json.length() && json.charAt(fim) != ',' && json.charAt(fim) != '}') fim++;
        String val = json.substring(ini, fim).trim();
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) val = val.substring(1, val.length()-1);
        return val;
    }

    /**
     * Converte payload JSON raso {"chave": valor, ...} em Map<String,String> (valores crus).
     * Útil para mapear estado do formulário -> tipos THZ.
     */
    public static Map<String, String> parseObjetoRaso(String json) {
        Map<String,String> out = new LinkedHashMap<>();
        if (json == null) return out;
        String t = json.trim();
        if (t.isEmpty() || t.equals("null")) return out;
        if (t.startsWith("{")) t = t.substring(1);
        if (t.endsWith("}")) t = t.substring(0, t.length()-1);
        // split por vírgula fora de aspas (simplificado)
        List<String> pares = splitJsonPares(t);
        for (String par : pares) {
            int colon = par.indexOf(':');
            if (colon < 0) continue;
            String k = par.substring(0, colon).trim();
            String v = par.substring(colon+1).trim();
            if (k.startsWith("\"") && k.endsWith("\"")) k = k.substring(1, k.length()-1);
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >=2) v = v.substring(1, v.length()-1).replace("\\\"","\"").replace("\\\\","\\");
            out.put(k, v);
        }
        return out;
    }

    private static List<String> splitJsonPares(String s) {
        List<String> res = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        int depthObj = 0, depthArr = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\"' && (i == 0 || s.charAt(i-1) != '\\')) inStr = !inStr;
            if (!inStr) {
                if (c == '{') depthObj++;
                else if (c == '}') depthObj--;
                else if (c == '[') depthArr++;
                else if (c == ']') depthArr--;
                else if (c == ',' && depthObj == 0 && depthArr == 0) {
                    res.add(cur.toString());
                    cur.setLength(0);
                    continue;
                }
            }
            cur.append(c);
        }
        if (cur.length() > 0) res.add(cur.toString());
        return res;
    }

    public static Object coerceParaNumero(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        try {
            if (t.contains(".") || t.contains(",")) {
                String norm = t.replace(",", ".");
                return Double.parseDouble(norm);
            }
            return new BigInteger(t);
        } catch (Exception e) {
            return raw;
        }
    }
}
