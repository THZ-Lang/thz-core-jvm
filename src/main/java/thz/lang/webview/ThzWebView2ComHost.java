package thz.lang.webview;

import java.io.File;
import java.util.Locale;

/**
 * Fase 3: Host COM WebView2 dedicado no Windows.
 *
 * Tenta carregar WebView2 via JNA (User32/Ole32 + WebView2Loader.dll) para janela sem dependência
 * de Edge/Chrome --app. Se JNA ou WebView2 não estiver disponível, retorna false e o
 * ThzWebViewLauncher cai para --app / rundll32.
 *
 * Design sem dependência obrigatória: JNA é carregado via reflection. Se não estiver no
 * classpath (ex.: native-image sem jna), o host simplesmente não é usado — zero quebra.
 *
 * Uso futuro: quando net.java.dev.jna:jna estiver no classpath e WebView2Loader.dll
 * em dist/thz/WebView2Loader.dll, este host cria janela Win32 dedicada.
 */
public final class ThzWebView2ComHost {

    private static boolean jnaDetectado = false;
    private static boolean tentouDetectar = false;

    private ThzWebView2ComHost() {}

    public static boolean isDisponivel() {
        if (tentouDetectar) return jnaDetectado;
        tentouDetectar = true;
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) return false;
        // Checa WebView2 runtime instalado
        if (!ThzWebView2Detector.isWebView2DisponivelWindows()) return false;
        // Tenta carregar JNA via reflection (opcional)
        try {
            Class.forName("com.sun.jna.Native");
            // Se JNA existe, consideramos host potencialmente disponível (Fase 3 parcial)
            jnaDetectado = true;
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Tenta abrir janela COM dedicada. Retorna true se conseguiu, false para fallback.
     * Implementação completa requer JNA + WebView2 SDK; aqui valida disponibilidade e
     * registra intenção. O launcher real --app ainda é usado até o COM estar linkado.
     */
    public static boolean tentarAbrir(String url, String titulo, int largura, int altura) {
        if (!isDisponivel()) return false;
        // Fase 3 stub: loga que o host COM seria usado e delega para --app até a
        // implementação JNA completa (User32.CreateWindowEx + Ole32.CoInitialize + WebView2Loader.CreateCoreWebView2Environment)
        // ser finalizada no thz_runtime.c / thz_webview2.c nativo.
        System.err.println("[THZ WebView2] Host COM Fase 3 detectado para: " + url + " (" + largura + "x" + altura + ") — usando --app até link JNA completo.");
        // Retorna false para que Lancador caia no --app existente (transição suave)
        // Quando a implementação nativa estiver pronta, retornará true após CreateWindowEx + Navigate(url)
        return false;
    }

    /**
     * Pré-checa se o processo pode usar WebView2Loader.dll bundled.
     * Útil para jpackage validar dist/thz/WebView2Loader.dll.
     */
    public static File localizarLoaderBundled() {
        String[] candidates = {
                "dist/thz/WebView2Loader.dll",
                "dist/thz/app/WebView2Loader.dll",
                System.getProperty("user.dir") + "/dist/thz/WebView2Loader.dll"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f;
        }
        return null;
    }
}
