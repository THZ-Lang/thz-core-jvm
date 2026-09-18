package thz.lang.lexico;

import java.util.Locale;

/**
 * Dialeto léxico e sintático da linguagem THZ-LANG.
 * Suporta modo canônico em português (pt-BR) e modo equivalente internacional (en-US).
 */
public enum DialetoLinguagem {
    PT_BR("pt-BR", "LINGUAGEM"),
    EN_US("en-US", "LANGUAGE");

    private final String codigo;
    private final String pragma;

    DialetoLinguagem(String codigo, String pragma) {
        this.codigo = codigo;
        this.pragma = pragma;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getPragma() {
        return pragma;
    }

    public static DialetoLinguagem detectar(String texto) {
        return detectarComPosicao(texto, 1, 1);
    }

    public static DialetoLinguagem detectarComPosicao(String texto, int linha, int coluna) {
        if (texto == null || texto.isBlank()) return PT_BR;
        String normalizado = texto.trim().toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "");
        if (normalizado.equals("enus") || normalizado.equals("en")
                || normalizado.startsWith("language") || normalizado.contains("english")) {
            return EN_US;
        }
        if (normalizado.equals("ptbr") || normalizado.equals("pt")
                || normalizado.startsWith("linguagem") || normalizado.contains("portuguese")) {
            return PT_BR;
        }
        throw new ErroLexico(linha, coluna, "Idioma ou dialeto não reconhecido: '" + texto.trim() + "'. Use 'pt-BR' ('ptbr') ou 'en-US' ('enus').");
    }

    @Override
    public String toString() {
        return codigo;
    }
}
