package thz.lang.ui;

/**
 * Utilitários de escape HTML/JS compartilhados entre emitters.
 * Extraído de ThzUiHtmlEmitter, ThzUiVaadinEmitter e BibliotecaConsole para eliminar duplicação.
 */
public final class HtmlEscape {

    private HtmlEscape() {}

    public static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"");
    }
}
