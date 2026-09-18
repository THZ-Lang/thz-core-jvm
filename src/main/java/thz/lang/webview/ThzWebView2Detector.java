package thz.lang.webview;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Detector leve de WebView2 / Edge Runtime no Windows — sem JNA/COM.
 * Fase 2: apenas detecção via filesystem/registry; Fase 3 alojará COM host real
 * onde o WebView2Loader.dll será carregado via JNA para janela dedicada.
 */
public final class ThzWebView2Detector {

    private ThzWebView2Detector() {}

    public enum Runtime { EDGE_STABLE, EDGE_BETA, WEBVIEW2_FIXED, CHROME, NONE }

    public record Detectado(Runtime runtime, String caminho, String versao) {}

    public static Detectado detectarWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) return new Detectado(Runtime.NONE, null, null);

        List<String> candidatosEdge = List.of(
                System.getenv("ProgramFiles(x86)") + "\\Microsoft\\Edge\\Application\\msedge.exe",
                System.getenv("ProgramFiles") + "\\Microsoft\\Edge\\Application\\msedge.exe",
                System.getenv("LocalAppData") + "\\Microsoft\\Edge\\Application\\msedge.exe",
                System.getenv("ProgramFiles(x86)") + "\\Microsoft\\Edge Beta\\Application\\msedge.exe",
                System.getenv("ProgramFiles") + "\\Microsoft\\Edge Beta\\Application\\msedge.exe"
        );
        for (String p : candidatosEdge) {
            if (p != null && new File(p).exists()) {
                Runtime r = p.toLowerCase(Locale.ROOT).contains("beta") ? Runtime.EDGE_BETA : Runtime.EDGE_STABLE;
                return new Detectado(r, p, null);
            }
        }
        // WebView2 Fixed Version (app-bundled)
        String wvFixed = System.getenv("ProgramFiles(x86)") + "\\Microsoft\\EdgeWebView\\Application\\msedge.exe";
        if (wvFixed != null && new File(wvFixed).exists()) return new Detectado(Runtime.WEBVIEW2_FIXED, wvFixed, null);
        String wv2 = System.getenv("ProgramFiles") + "\\Microsoft\\EdgeWebView\\Application\\msedge.exe";
        if (wv2 != null && new File(wv2).exists()) return new Detectado(Runtime.WEBVIEW2_FIXED, wv2, null);

        String pf = System.getenv("ProgramFiles");
        String pf86 = System.getenv("ProgramFiles(x86)");
        List<String> chrome = List.of(
                pf != null ? pf + "\\Google\\Chrome\\Application\\chrome.exe" : null,
                pf86 != null ? pf86 + "\\Google\\Chrome\\Application\\chrome.exe" : null
        );
        for (String p : chrome) if (p != null && new File(p).exists()) return new Detectado(Runtime.CHROME, p, null);

        return new Detectado(Runtime.NONE, null, null);
    }

    public static List<String> caminhosWindowsOrdenados() {
        List<String> out = new ArrayList<>();
        String pf = System.getenv("ProgramFiles");
        String pf86 = System.getenv("ProgramFiles(x86)");
        String local = System.getenv("LocalAppData");
        // ordem: Edge stable > Edge Beta > WebView2 Fixed > Chrome
        if (pf86 != null) out.add(pf86 + "\\Microsoft\\Edge\\Application\\msedge.exe");
        if (pf != null) out.add(pf + "\\Microsoft\\Edge\\Application\\msedge.exe");
        if (local != null) out.add(local + "\\Microsoft\\Edge\\Application\\msedge.exe");
        if (pf86 != null) out.add(pf86 + "\\Microsoft\\Edge Beta\\Application\\msedge.exe");
        if (pf != null) out.add(pf + "\\Microsoft\\Edge Beta\\Application\\msedge.exe");
        if (pf86 != null) out.add(pf86 + "\\Microsoft\\EdgeWebView\\Application\\msedge.exe");
        if (pf != null) out.add(pf + "\\Microsoft\\EdgeWebView\\Application\\msedge.exe");
        if (pf != null) out.add(pf + "\\Google\\Chrome\\Application\\chrome.exe");
        if (pf86 != null) out.add(pf86 + "\\Google\\Chrome\\Application\\chrome.exe");
        if (local != null) out.add(local + "\\Google\\Chrome\\Application\\chrome.exe");
        return out;
    }

    public static boolean isWebView2DisponivelWindows() {
        Detectado d = detectarWindows();
        return d.runtime() != Runtime.NONE;
    }
}
