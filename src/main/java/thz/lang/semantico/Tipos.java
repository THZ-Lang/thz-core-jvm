package thz.lang.semantico;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Tipos {
    public static final TipoThz TIPO_LITERAL_INTEIRO = new TipoThz("<literal-inteiro>", CategoriaTipo.PRIMITIVO);

    private static final Map<String, TipoThz> PRIMITIVOS = Map.of(
        "TEXTO", new TipoThz("TEXTO", CategoriaTipo.PRIMITIVO),
        "LOGICO", new TipoThz("LOGICO", CategoriaTipo.PRIMITIVO),
        "UUID", new TipoThz("UUID", CategoriaTipo.PRIMITIVO),
        "DATA", new TipoThz("DATA", CategoriaTipo.PRIMITIVO),
        "DATA_HORA", new TipoThz("DATA_HORA", CategoriaTipo.PRIMITIVO)
    );
    private static final Map<String, TipoThz> INTEIROS = Map.of(
        "NATURAL8", new TipoThz("NATURAL8", CategoriaTipo.INTEIRO),
        "NATURAL16", new TipoThz("NATURAL16", CategoriaTipo.INTEIRO),
        "NATURAL32", new TipoThz("NATURAL32", CategoriaTipo.INTEIRO),
        "NATURAL64", new TipoThz("NATURAL64", CategoriaTipo.INTEIRO),
        "INTEIRO8", new TipoThz("INTEIRO8", CategoriaTipo.INTEIRO),
        "INTEIRO16", new TipoThz("INTEIRO16", CategoriaTipo.INTEIRO),
        "INTEIRO32", new TipoThz("INTEIRO32", CategoriaTipo.INTEIRO),
        "INTEIRO64", new TipoThz("INTEIRO64", CategoriaTipo.INTEIRO)
    );

    public static final Map<String, TipoThz> TIPOS_PRIMITIVOS;
    static {
        var m = new java.util.HashMap<String, TipoThz>();
        m.putAll(PRIMITIVOS);
        m.putAll(INTEIROS);
        TIPOS_PRIMITIVOS = Map.copyOf(m);
    }

    private Tipos() {}

    public static TipoThz analisarNomeTipo(String verbatim) {
        TipoThz prim = TIPOS_PRIMITIVOS.get(verbatim);
        if (prim != null) return prim;
        if ("DECIMAL".equals(verbatim)) return new TipoThz("DECIMAL", CategoriaTipo.DECIMAL, 4, null, null, null);
        if ("INTEIRO".equals(verbatim)) return new TipoThz("INTEIRO64", CategoriaTipo.INTEIRO);
        if ("NATURAL".equals(verbatim)) return new TipoThz("NATURAL64", CategoriaTipo.INTEIRO);
        if ("MONETARIO".equals(verbatim) || "DINHEIRO".equals(verbatim)) return new TipoThz("MONETARIO", CategoriaTipo.MONETARIO, null, "BRL", null, null);
        if ("REGISTRO".equals(verbatim) || "RECORD".equals(verbatim)) return new TipoThz("REGISTRO", CategoriaTipo.REGISTRO, null, null, "REGISTRO", null);
        if ("MAPA".equals(verbatim) || "MAP".equals(verbatim)) return new TipoThz("MAPA", CategoriaTipo.MAPA, null, null, "MAPA", null);
        Matcher m;
        m = Pattern.compile("^DECIMAL\\s*\\(\\s*\\d+\\s*,\\s*(\\d+)\\s*\\)$").matcher(verbatim);
        if (m.matches()) return new TipoThz(verbatim.replaceAll("\\s+", ""), CategoriaTipo.DECIMAL, Integer.parseInt(m.group(1)), null, null, null);
        m = Pattern.compile("^MONETARIO\\s*\\(\\s*\"?([A-Z]{3})\"?\\s*\\)$").matcher(verbatim);
        if (m.matches()) return new TipoThz(verbatim.replaceAll("\\s+", ""), CategoriaTipo.MONETARIO, null, m.group(1), null, null);
        m = Pattern.compile("^RESULTADO\\s*\\[\\s*([^,\\]]+?)\\s*,\\s*([^,\\]]+?)\\s*\\]$").matcher(verbatim);
        if (m.matches()) return new TipoThz(verbatim.replaceAll("\\s+", ""), CategoriaTipo.RESULTADO, null, null, m.group(1).trim(), m.group(2).trim());
        m = Pattern.compile("^FATIA\\s*\\[\\s*(\\w+)\\s*\\]$").matcher(verbatim);
        if (m.matches()) return new TipoThz(verbatim.replaceAll("\\s+", ""), CategoriaTipo.FATIA, null, null, m.group(1), null);
        return null;
    }

    public static boolean ehInteiro(TipoThz t) {
        return t != null && (t.categoria() == CategoriaTipo.INTEIRO || t == TIPO_LITERAL_INTEIRO);
    }
    public static boolean ehNumerico(TipoThz t) {
        return ehInteiro(t) || (t != null && t.categoria() == CategoriaTipo.DECIMAL);
    }
    public static boolean saoCompativeis(TipoThz origem, TipoThz destino) {
        if (origem == null || destino == null) return true;
        if (origem.nome().equals(destino.nome())) return true;
        if (ehInteiro(origem) && ehInteiro(destino)) return true;
        if (ehNumerico(origem) && destino.categoria() == CategoriaTipo.DECIMAL) return true;
        if (origem.categoria() == CategoriaTipo.REGISTRO && destino.categoria() == CategoriaTipo.REGISTRO) return true;
        if (origem.categoria() == CategoriaTipo.MAPA && destino.categoria() == CategoriaTipo.MAPA) return true;
        return false;
    }
    public static String descrever(TipoThz t) {
        return t != null ? "'" + t.nome() + "'" : "<desconhecido>";
    }
}
