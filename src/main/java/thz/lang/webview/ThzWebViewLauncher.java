package thz.lang.webview;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ThzWebViewLauncher — host de janela gráfica para a WebView sem depender de AWT/Swing/Desktop.
 *
 * Compatível com GraalVM Native Image (sem java.awt) e execução HotSpot.
 * Tenta lançar Edge/Chrome em App Mode (--app=); fallback usa comando do SO sem Desktop API.
 */
public final class ThzWebViewLauncher {

    public record JanelaConfig(String titulo, String urlOuHtml, int largura, int altura) {
        public static JanelaConfig padrao(String titulo, String urlOuHtml) {
            return new JanelaConfig(titulo, urlOuHtml, 1024, 768);
        }
    }

    private static Process processoNativo = null;

    private ThzWebViewLauncher() {}

    public static synchronized String abrir(JanelaConfig config) {
        String url;
        if (config.urlOuHtml().startsWith("http://") || config.urlOuHtml().startsWith("https://")) {
            url = config.urlOuHtml();
        } else {
            ThzWebViewBridge.iniciar(config.urlOuHtml());
            url = ThzWebViewBridge.getUrl();
        }

        if (!lancarNativo(url, config)) {
            abrirNavegadorFallback(url);
        }
        return url;
    }

    public static synchronized String abrirHtml(String titulo, String html, int largura, int altura) {
        // Fecha processo anterior do Edge se estiver rodando (evita conflito de perfil)
        if (processoNativo != null && processoNativo.isAlive()) {
            try { processoNativo.destroyForcibly(); Thread.sleep(300); } catch (Exception ignore) {}
            processoNativo = null;
        }

        // Sempre serve via bridge para ter porta conhecida
        ThzWebViewBridge.iniciar(html);
        String url = ThzWebViewBridge.getUrl();

        // Aguarda servidor HTTP estar pronto antes de abrir o navegador
        for (int i = 0; i < 10; i++) {
            try {
                var conn = new java.net.URL(url).openConnection();
                conn.setConnectTimeout(500);
                conn.setReadTimeout(500);
                conn.connect();
                conn.getInputStream().close();
                break;
            } catch (Exception e) {
                try { Thread.sleep(200); } catch (InterruptedException ignore) {}
            }
        }

        JanelaConfig cfg = new JanelaConfig(titulo, url, largura, altura);
        if (!lancarNativo(url, cfg)) {
            abrirNavegadorFallback(url);
        }
        return url;
    }

    public static synchronized void fechar() {
        if (processoNativo != null && processoNativo.isAlive()) {
            try { processoNativo.destroyForcibly(); } catch (Exception ignore) {}
            processoNativo = null;
        }
        ThzWebViewBridge.parar();
    }

    public static boolean estaAberta() {
        return processoNativo != null && processoNativo.isAlive();
    }

    private static boolean lancarNativo(String url, JanelaConfig config) {
        // Fase 3: tenta host COM dedicado primeiro (sem --app, janela Win32 pura)
        if (ThzWebView2ComHost.tentarAbrir(url, config.titulo(), config.largura(), config.altura())) {
            return true;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        List<String> cmd = new ArrayList<>();

        if (os.contains("win")) {
            String tmp = System.getProperty("java.io.tmpdir", System.getenv("TEMP") != null ? System.getenv("TEMP") : "C:\\Temp");
            String userDataDir = tmp.replaceAll("\\\\$", "") + "\\thz_webview_profile";
            // Fase 2: detecção ordenada via ThzWebView2Detector (Edge stable > Beta > WebView2 Fixed > Chrome)
            for (String caminho : ThzWebView2Detector.caminhosWindowsOrdenados()) {
                if (caminho != null && new File(caminho).exists()) {
                    cmd.add(caminho);
                    cmd.add("--app=" + url);
                    cmd.add("--window-size=" + config.largura() + "," + config.altura());
                    cmd.add("--user-data-dir=" + userDataDir);
                    // Fase 3: isola perfil por versão para evitar conflito WebView2
                    cmd.add("--class=THZ-WebView-" + Math.abs(url.hashCode() % 10000));
                    System.err.println("[THZ WebView] Runtime detectado: " + caminho);
                    break;
                }
            }
            if (cmd.isEmpty()) {
                System.err.println("[THZ WebView] Nenhum runtime Edge/Chrome detectado. Tentando fallback do sistema.");
            }
        } else if (os.contains("mac")) {
            String chromeMac = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
            if (new File(chromeMac).exists()) {
                cmd.add(chromeMac);
                cmd.add("--app=" + url);
                cmd.add("--window-size=" + config.largura() + "," + config.altura());
            }
        } else {
            String[] binsLinux = {"chromium", "chromium-browser", "google-chrome", "google-chrome-stable"};
            for (String b : binsLinux) {
                // Testa existência via which seria ideal, mas tenta o primeiro e se falhar tenta próximo no fallback
                cmd.add(b);
                cmd.add("--app=" + url);
                cmd.add("--window-size=" + config.largura() + "," + config.altura());
                break;
            }
        }

        if (cmd.isEmpty()) return false;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            // Fase 3 preparo: WebView2Loader COM host virá aqui (JNA). Por ora, --app mode é o nativo.
            processoNativo = pb.start();
            // Verifica se processo não morreu imediatamente (caminho inválido)
            Thread.sleep(200);
            if (!processoNativo.isAlive() && processoNativo.exitValue() != 0) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fallback sem java.awt.Desktop — usa comandos nativos do SO.
     */
    private static void abrirNavegadorFallback(String url) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                // rundll32 é o mais compatível; alternativa: cmd /c start "" "url"
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (IOException ignored) {
            System.err.println("[THZ WebView] Não foi possível abrir navegador automaticamente. Acesse: " + url);
        }
    }
}
