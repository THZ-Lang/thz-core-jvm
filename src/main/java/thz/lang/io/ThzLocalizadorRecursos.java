package thz.lang.io;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * ThzLocalizadorRecursos — Mecanismo de Resolução e Busca Recursiva Inteligente
 * para módulos, arquivos-fonte (.thz, .thzui), manifestos de configuração e bancos de dados.
 *
 * Realiza resolução progressiva:
 * 1. Resolução Direta (Caminho absoluto ou relativo exato)
 * 2. Resolução com Extensões Canônicas (.thz, .thzui, .json, .db)
 * 3. Resolução em Diretórios Convencionais do Projeto (src/, modulos/, exemplos/, etc.)
 * 4. Subida Hierárquica até a Raiz do Workspace
 * 5. Varredura Recursiva em Profundidade com Descarte de Pastas Ocultas/Build (build/, .git/, etc.)
 * 6. Resolução Fuzzy / Case-Insensitive
 */
public final class ThzLocalizadorRecursos {

    private static final List<String> PASTAS_CONVENCIONAIS = List.of(
            ".",
            "src",
            "src/modulos",
            "modulos",
            "lib",
            "exemplos",
            "exemplos/novos_recursos",
            "thz-lang/exemplos",
            "../thz-lang/exemplos",
            "../exemplos",
            "compilador",
            "thz-compiler",
            "../thz-compiler",
            "biblioteca",
            "dados",
            "config",
            "docs",
            "dist"
    );

    private static final Set<String> PASTAS_IGNORADAS = Set.of(
            ".git",
            ".gradle",
            "build",
            "target",
            ".idea",
            ".vscode",
            "node_modules",
            ".gemini",
            ".opencode",
            "tmp",
            "temp",
            "scratch",
            "appdata"
    );

    private ThzLocalizadorRecursos() {}

    /**
     * Localiza um módulo THZ (.thz ou .thzui) através de pesquisa inteligente e recursiva.
     */
    public static Optional<Path> localizarModulo(String nomeOuCaminho, Path diretorioBase) {
        return localizarArquivo(nomeOuCaminho, diretorioBase, List.of(".thz", ".thzui"));
    }

    /**
     * Localiza qualquer arquivo aplicando estratégia progressiva e pesquisa recursiva.
     */
    public static Optional<Path> localizarArquivo(String termo, Path diretorioBase, List<String> extensoes) {
        if (termo == null || termo.isBlank()) {
            return Optional.empty();
        }

        Path base = diretorioBase != null ? diretorioBase.toAbsolutePath().normalize() : Path.of(".").toAbsolutePath().normalize();
        String termoLimpo = termo.trim().replace("\\", "/");

        // 1. Verificação Direta
        Path direto = base.resolve(termoLimpo).normalize();
        if (Files.exists(direto) && !Files.isDirectory(direto)) {
            return Optional.of(direto);
        }
        Path absolutoDireto = Path.of(termoLimpo).toAbsolutePath().normalize();
        if (Files.exists(absolutoDireto) && !Files.isDirectory(absolutoDireto)) {
            return Optional.of(absolutoDireto);
        }

        // 2. Verificação Direta com Extensões
        if (extensoes != null) {
            for (String ext : extensoes) {
                String comExt = termoLimpo.endsWith(ext) ? termoLimpo : termoLimpo + ext;
                Path p = base.resolve(comExt).normalize();
                if (Files.exists(p) && !Files.isDirectory(p)) {
                    return Optional.of(p);
                }
                Path pAbs = Path.of(comExt).toAbsolutePath().normalize();
                if (Files.exists(pAbs) && !Files.isDirectory(pAbs)) {
                    return Optional.of(pAbs);
                }
            }
        }

        // 3. Varredura recursiva a partir do próprio diretório base
        var buscaNoBase = varreduraRecursiva(base, termoLimpo, extensoes, base);
        if (buscaNoBase.isPresent()) {
            return buscaNoBase;
        }

        // 4. Subida Hierárquica até a Raiz do Projeto testando Pastas Convencionais e Varredura
        Path raiz = encontrarRaizProjeto(base);
        Path atual = base;
        int maxSubidas = raiz != null ? 10 : 3;
        int subidas = 0;

        while (atual != null && subidas <= maxSubidas) {
            for (String pasta : PASTAS_CONVENCIONAIS) {
                Path dirTeste = atual.resolve(pasta).normalize();
                if (Files.isDirectory(dirTeste)) {
                    var encontrado = testarEmDiretorio(dirTeste, termoLimpo, extensoes);
                    if (encontrado.isPresent()) {
                        return encontrado;
                    }
                }
            }
            var buscaNivel = varreduraRecursiva(atual, termoLimpo, extensoes, base);
            if (buscaNivel.isPresent()) {
                return buscaNivel;
            }
            if (raiz != null && atual.equals(raiz)) {
                break;
            }
            atual = atual.getParent();
            subidas++;
        }

        return Optional.empty();
    }

    /**
     * Localiza a raiz do projeto corporativo baseado em marcadores estruturais.
     * Retorna a raiz encontrada ou null caso nenhum marcador esteja presente na árvore de diretórios.
     */
    public static Path encontrarRaizProjeto(Path diretorioInicial) {
        Path inicial = (diretorioInicial != null ? diretorioInicial : Path.of(".")).toAbsolutePath().normalize();
        Path atual = inicial;
        while (atual != null) {
            if (Files.exists(atual.resolve("thz.config.json")) ||
                Files.exists(atual.resolve("thz.json")) ||
                Files.exists(atual.resolve(".git")) ||
                Files.exists(atual.resolve("settings.gradle.kts")) ||
                Files.exists(atual.resolve("settings.gradle")) ||
                Files.exists(atual.resolve("build.gradle.kts")) ||
                Files.exists(atual.resolve("build.gradle"))) {
                return atual;
            }
            atual = atual.getParent();
        }
        return null;
    }

    /**
     * Ajusta URLs SQLite para encontrar bancos existentes ou garantir caminho de diretório válido.
     */
    public static String resolverUrlBancoSqlite(String url, Path diretorioBase) {
        if (url == null || !url.startsWith("jdbc:sqlite:") || url.contains(":memory:")) {
            return url;
        }

        String caminhoBruto = url.substring("jdbc:sqlite:".length());
        Path base = diretorioBase != null ? diretorioBase : Path.of(".");

        // Tenta localizar arquivo existente recursivamente
        var dbEncontrado = localizarArquivo(caminhoBruto, base, List.of(".db", ".sqlite", ".sqlite3"));
        if (dbEncontrado.isPresent()) {
            return "jdbc:sqlite:" + dbEncontrado.get().toAbsolutePath().toString().replace("\\", "/");
        }

        // Se não existir, ancora na raiz do projeto ou diretório base e cria diretório pai
        Path raiz = encontrarRaizProjeto(base);
        Path ancora = raiz != null ? raiz : base;
        Path destino = ancora.resolve(caminhoBruto).normalize();
        try {
            if (destino.getParent() != null) {
                Files.createDirectories(destino.getParent());
            }
        } catch (IOException ignored) {}

        return "jdbc:sqlite:" + destino.toAbsolutePath().toString().replace("\\", "/");
    }

    private static Optional<Path> testarEmDiretorio(Path diretorio, String termo, List<String> extensoes) {
        Path p = diretorio.resolve(termo).normalize();
        if (Files.exists(p) && !Files.isDirectory(p)) {
            return Optional.of(p);
        }
        if (extensoes != null) {
            for (String ext : extensoes) {
                String nomeComExt = termo.endsWith(ext) ? termo : termo + ext;
                Path pExt = diretorio.resolve(nomeComExt).normalize();
                if (Files.exists(pExt) && !Files.isDirectory(pExt)) {
                    return Optional.of(pExt);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Path> varreduraRecursiva(Path raiz, String termo, List<String> extensoes, Path origem) {
        if (raiz == null || !Files.isDirectory(raiz)) {
            return Optional.empty();
        }

        String nomeAlvo = Path.of(termo).getFileName().toString();
        String nomeSemExt = removerExtensoesConhecidas(nomeAlvo);

        List<Path> candidatos = new ArrayList<>();

        try {
            Files.walkFileTree(raiz, EnumSet.noneOf(FileVisitOption.class), 8, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (ehCaminhoIgnorado(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String nomeArq = file.getFileName().toString();
                    String nomeArqSemExt = removerExtensoesConhecidas(nomeArq);

                    // Match exato com termo ou nome
                    if (nomeArq.equals(nomeAlvo) || nomeArq.equalsIgnoreCase(nomeAlvo)) {
                        candidatos.add(file);
                        return FileVisitResult.CONTINUE;
                    }

                    // Match sem extensão (ex: 'faturamento' encontra 'faturamento.thz')
                    if (nomeArqSemExt.equalsIgnoreCase(nomeSemExt)) {
                        if (extensoes == null || extensoes.isEmpty() || extensoes.stream().anyMatch(nomeArq::endsWith)) {
                            candidatos.add(file);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception ignored) {}

        if (candidatos.isEmpty()) {
            return Optional.empty();
        }

        // Ordena por menor distância em relação ao diretório base de origem
        candidatos.sort(Comparator.comparingInt(p -> calcularDistancia(origem, p)));
        return Optional.of(candidatos.get(0));
    }

    private static boolean ehCaminhoIgnorado(Path path) {
        if (path == null || path.getFileName() == null) return false;
        String nome = path.getFileName().toString().toLowerCase();
        return PASTAS_IGNORADAS.contains(nome);
    }

    private static String removerExtensoesConhecidas(String nome) {
        if (nome.endsWith(".thz")) return nome.substring(0, nome.length() - 4);
        if (nome.endsWith(".thzui")) return nome.substring(0, nome.length() - 6);
        if (nome.endsWith(".json")) return nome.substring(0, nome.length() - 5);
        if (nome.endsWith(".db")) return nome.substring(0, nome.length() - 3);
        return nome;
    }

    private static int calcularDistancia(Path base, Path candidato) {
        try {
            return base.relativize(candidato).getNameCount();
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }
}
