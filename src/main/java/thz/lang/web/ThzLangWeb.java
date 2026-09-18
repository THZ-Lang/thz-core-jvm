package thz.lang.web;

import thz.lang.interpretador.ValorThz;
import thz.lang.net.ThzHttpServer;

import java.util.Map;

/**
 * ThzLangWeb — Abstração de alto nível para serviços web, APIs REST e controladores em THZ-LANG.
 */
public final class ThzLangWeb {

    private ThzLangWeb() {}

    public static void get(String rota, java.util.function.Function<ThzHttpServer.Requisicao, ThzHttpServer.Resposta> handler) {
        ThzHttpServer.registrarRota("GET", rota, handler);
    }

    public static void post(String rota, java.util.function.Function<ThzHttpServer.Requisicao, ThzHttpServer.Resposta> handler) {
        ThzHttpServer.registrarRota("POST", rota, handler);
    }

    public static void put(String rota, java.util.function.Function<ThzHttpServer.Requisicao, ThzHttpServer.Resposta> handler) {
        ThzHttpServer.registrarRota("PUT", rota, handler);
    }

    public static void delete(String rota, java.util.function.Function<ThzHttpServer.Requisicao, ThzHttpServer.Resposta> handler) {
        ThzHttpServer.registrarRota("DELETE", rota, handler);
    }

    public static ThzHttpServer.Resposta respostaJson(int status, ValorThz valor) {
        String json = serializarThzParaJson(valor);
        return ThzHttpServer.Resposta.json(status, json);
    }

    public static String serializarThzParaJson(ValorThz valor) {
        if (valor == null || valor instanceof ValorThz.Nulo) return "null";
        if (valor instanceof ValorThz.Texto t) return "\"" + escaparJson(t.valor()) + "\"";
        if (valor instanceof ValorThz.Inteiro i) return i.valor().toString();
        if (valor instanceof ValorThz.Decimal d) return d.valor().formatar();
        if (valor instanceof ValorThz.Monetario m) return "{\"quantia\":\"" + m.valor().formatar() + "\",\"moeda\":\"" + m.valor().moeda.codigo() + "\"}";
        if (valor instanceof ValorThz.Logico l) return l.valor() ? "true" : "false";
        if (valor instanceof ValorThz.Enumerado e) return "\"" + e.valor() + "\"";
        if (valor instanceof ValorThz.Data dt) return "\"" + dt.valor().formatar() + "\"";
        if (valor instanceof ValorThz.DataHora dh) return "\"" + dh.valor().formatar() + "\"";
        if (valor instanceof ValorThz.Resultado r) {
            if (r.sucesso()) {
                return "{\"status\":\"SUCESSO\",\"valor\":" + serializarThzParaJson(r.valor()) + "}";
            } else {
                return "{\"status\":\"ERRO\",\"erro\":" + serializarThzParaJson(r.erro()) + "}";
            }
        }
        if (valor instanceof ValorThz.Fatia f) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < f.elementos().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(serializarThzParaJson(f.elementos().get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        if (valor instanceof ValorThz.Registro reg) {
            StringBuilder sb = new StringBuilder("{");
            boolean prim = true;
            for (Map.Entry<String, ValorThz> e : reg.campos().entrySet()) {
                if (!prim) sb.append(",");
                prim = false;
                sb.append("\"").append(escaparJson(e.getKey())).append("\":").append(serializarThzParaJson(e.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        return "\"" + escaparJson(valor.formatar()) + "\"";
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
