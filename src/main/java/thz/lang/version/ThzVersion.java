package thz.lang.version;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ThzVersion — Parser e comparador SemVer 2.0.0 e inspetor de runtime GraalVM / JDK 25.
 */
public record ThzVersion(int major, int minor, int patch, String preRelease, String buildMetadata) implements Comparable<ThzVersion> {

    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$"
    );

    public static final ThzVersion ATUAL = carregarVersaoAtual();

    private static ThzVersion carregarVersaoAtual() {
        try (var is = ThzVersion.class.getResourceAsStream("/thz-version.properties")) {
            if (is != null) {
                var props = new java.util.Properties();
                props.load(is);
                String v = props.getProperty("version");
                if (v != null && !v.isBlank() && !v.contains("${")) {
                    return parse(v.trim());
                }
            }
        } catch (Exception ignored) {
        }
        return new ThzVersion(0, 4, 0, null, null);
    }

    public static ThzVersion parse(String versao) {
        if (versao == null || versao.isBlank()) {
            throw new IllegalArgumentException("Versão não pode ser vazia.");
        }
        String limpa = versao.trim();
        if (limpa.startsWith("v") || limpa.startsWith("V")) {
            limpa = limpa.substring(1);
        }
        Matcher m = SEMVER_PATTERN.matcher(limpa);
        if (!m.matches()) {
            // Suporte a formato relaxado major.minor
            if (limpa.matches("^\\d+\\.\\d+$")) {
                String[] partes = limpa.split("\\.");
                return new ThzVersion(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]), 0, null, null);
            }
            throw new IllegalArgumentException("Versão SemVer inválida: '" + versao + "'.");
        }
        int maj = Integer.parseInt(m.group(1));
        int min = Integer.parseInt(m.group(2));
        int pat = Integer.parseInt(m.group(3));
        String pre = m.group(4);
        String build = m.group(5);
        return new ThzVersion(maj, min, pat, pre, build);
    }

    public boolean ehCompativelCom(ThzVersion outra) {
        if (this.major != outra.major) return false;
        if (this.major == 0) {
            return this.minor == outra.minor && this.patch >= outra.patch;
        }
        return this.minor >= outra.minor;
    }

    public static boolean satisfaz(String versaoStr, String especificacao) {
        ThzVersion v = parse(versaoStr);
        String spec = especificacao.trim();
        if (spec.startsWith(">=")) {
            ThzVersion base = parse(spec.substring(2).trim());
            return v.compareTo(base) >= 0;
        } else if (spec.startsWith("<=")) {
            ThzVersion base = parse(spec.substring(2).trim());
            return v.compareTo(base) <= 0;
        } else if (spec.startsWith(">")) {
            ThzVersion base = parse(spec.substring(1).trim());
            return v.compareTo(base) > 0;
        } else if (spec.startsWith("<")) {
            ThzVersion base = parse(spec.substring(1).trim());
            return v.compareTo(base) < 0;
        } else if (spec.startsWith("^")) {
            ThzVersion base = parse(spec.substring(1).trim());
            return v.ehCompativelCom(base);
        } else if (spec.startsWith("=")) {
            ThzVersion base = parse(spec.substring(1).trim());
            return v.compareTo(base) == 0;
        }
        return v.compareTo(parse(spec)) == 0;
    }

    @Override
    public int compareTo(ThzVersion o) {
        if (this.major != o.major) return Integer.compare(this.major, o.major);
        if (this.minor != o.minor) return Integer.compare(this.minor, o.minor);
        if (this.patch != o.patch) return Integer.compare(this.patch, o.patch);
        if (Objects.equals(this.preRelease, o.preRelease)) return 0;
        if (this.preRelease == null) return 1;
        if (o.preRelease == null) return -1;
        return this.preRelease.compareTo(o.preRelease);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append(".").append(minor).append(".").append(patch);
        if (preRelease != null) sb.append("-").append(preRelease);
        if (buildMetadata != null) sb.append("+").append(buildMetadata);
        return sb.toString();
    }

    public static RuntimeInfo obterRuntimeInfo() {
        Runtime r = Runtime.getRuntime();
        boolean isGraal = System.getProperty("org.graalvm.nativeimage.imagecode") != null
                || System.getProperty("java.vm.name", "").contains("GraalVM");
        return new RuntimeInfo(
                ATUAL.toString(),
                System.getProperty("java.version", "25"),
                System.getProperty("java.vendor", "OpenJDK"),
                isGraal,
                System.getProperty("os.name", "unknown"),
                System.getProperty("os.arch", "unknown"),
                r.availableProcessors(),
                r.maxMemory() / (1024 * 1024),
                r.totalMemory() / (1024 * 1024),
                (r.totalMemory() - r.freeMemory()) / (1024 * 1024)
        );
    }

    public record RuntimeInfo(
            String versaoThz,
            String javaVersion,
            String javaVendor,
            boolean ehGraalVm,
            String osName,
            String osArch,
            int cpuCores,
            long memoriaMaxMb,
            long memoriaTotalMb,
            long memoriaUsadaMb
    ) {}
}
