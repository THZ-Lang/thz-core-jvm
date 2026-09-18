package thz.lang.ui;

/**
 * Tokens de design e estilização para o ThzUiMaker.
 * Suporta temas escuro corporativo (Glassmorphism), claro e customizado.
 */
public record ThzUiTema(
        String nome,
        String corFundo,
        String corFundoCard,
        String corTexto,
        String corTextoSecundario,
        String corPrimaria,
        String corSecundaria,
        String corSucesso,
        String corAviso,
        String corErro,
        String corBorda,
        int raioBordaPx,
        boolean glassmorphism,
        String fonteFamilia
) {
    public static ThzUiTema escuroGlass() {
        return new ThzUiTema(
                "THZ Dark Glass",
                "#0f172a", // Slate 900
                "rgba(30, 41, 59, 0.75)", // Slate 800 com transparência
                "#f8fafc", // Slate 50
                "#94a3b8", // Slate 400
                "#3b82f6", // Blue 500
                "#8b5cf6", // Purple 500
                "#10b981", // Emerald 500
                "#f59e0b", // Amber 500
                "#ef4444", // Red 500
                "rgba(255, 255, 255, 0.12)",
                10,
                true,
                "'Segoe UI', Inter, -apple-system, BlinkMacSystemFont, sans-serif"
        );
    }

    public static ThzUiTema claroModerno() {
        return new ThzUiTema(
                "THZ Light Modern",
                "#f8fafc",
                "#ffffff",
                "#0f172a",
                "#64748b",
                "#2563eb",
                "#7c3aed",
                "#059669",
                "#d97706",
                "#dc2626",
                "#e2e8f0",
                8,
                false,
                "'Segoe UI', Inter, -apple-system, BlinkMacSystemFont, sans-serif"
        );
    }
}
