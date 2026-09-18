package thz.lang.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * ThzIO — Operações de I/O de alta velocidade utilizando Java NIO.2.
 */
public final class ThzIO {

    private ThzIO() {}

    public static String lerTexto(String caminho) {
        try {
            return Files.readString(Path.of(caminho), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler arquivo '" + caminho + "': " + e.getMessage(), e);
        }
    }

    public static void escreverTexto(String caminho, String conteudo) {
        try {
            Path p = Path.of(caminho);
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            Files.writeString(p, conteudo, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao escrever arquivo '" + caminho + "': " + e.getMessage(), e);
        }
    }

    public static void anexarTexto(String caminho, String conteudo) {
        try {
            Path p = Path.of(caminho);
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            Files.writeString(p, conteudo, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao anexar ao arquivo '" + caminho + "': " + e.getMessage(), e);
        }
    }

    public static boolean existe(String caminho) {
        return Files.exists(Path.of(caminho));
    }

    public static boolean ehDiretorio(String caminho) {
        return Files.isDirectory(Path.of(caminho));
    }

    public static long tamanho(String caminho) {
        try {
            return Files.size(Path.of(caminho));
        } catch (IOException e) {
            return -1;
        }
    }

    public static boolean remover(String caminho) {
        try {
            return Files.deleteIfExists(Path.of(caminho));
        } catch (IOException e) {
            return false;
        }
    }

    public static List<String> listarDiretorio(String caminho) {
        List<String> itens = new ArrayList<>();
        try (Stream<Path> stream = Files.list(Path.of(caminho))) {
            stream.forEach(p -> itens.add(p.getFileName().toString()));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao listar diretório '" + caminho + "': " + e.getMessage(), e);
        }
        return itens;
    }

    public static void criarDiretorio(String caminho) {
        try {
            Files.createDirectories(Path.of(caminho));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao criar diretório '" + caminho + "': " + e.getMessage(), e);
        }
    }
}
