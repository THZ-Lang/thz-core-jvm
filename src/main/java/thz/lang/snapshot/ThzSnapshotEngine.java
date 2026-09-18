package thz.lang.snapshot;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ThzSnapshotEngine — Motor de Compactação e Congelamento de Estado / Snapshots da THZ-LANG.
 *
 * Invariantes Obrigatórios:
 * 1. Mantém estritamente apenas 1 único snapshot ativo por workspace/runtime.
 * 2. Cota rígida de tamanho: o snapshot gerado DEVE ser menor que 100MB (104.857.600 bytes).
 * 3. Utiliza formato binário de alta compressão (Deflater nível 9) com cabeçalho mágico 'THZSNAP\x01'.
 */
public final class ThzSnapshotEngine {

    public static final byte[] MAGIC_HEADER = new byte[]{'T', 'H', 'Z', 'S', 'N', 'A', 'P', 0x01};
    public static final long MAX_BYTES_SNAPSHOT = 100L * 1024L * 1024L; // 100 MB
    public static final String NOME_SNAPSHOT_PADRAO = "active_workspace.thzsnap";

    private ThzSnapshotEngine() {}

    public static Path obterCaminhoSnapshotPadrao() {
        Path raiz = Path.of(".").toAbsolutePath().normalize();
        Path dir = raiz.resolve(".thz").resolve("internal");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {}
        return dir.resolve(NOME_SNAPSHOT_PADRAO);
    }

    /**
     * Cria um snapshot comprimido a partir de um diretório de origem.
     * Sobrescreve o snapshot ativo anterior mantendo estritamente 1 único arquivo.
     */
    public static Path criarSnapshot(Path origemDir, Path destinoArquivo) throws IOException {
        if (!Files.exists(origemDir)) {
            throw new IllegalArgumentException("[Erro Snapshot] Diretório de origem não existe: " + origemDir);
        }

        Path destinoFinal = (destinoArquivo != null) ? destinoArquivo : obterCaminhoSnapshotPadrao();
        if (destinoFinal.getParent() != null) {
            Files.createDirectories(destinoFinal.getParent());
        }

        Path arquivoTemp = destinoFinal.resolveSibling(destinoFinal.getFileName().toString() + ".tmp");

        try (OutputStream fos = Files.newOutputStream(arquivoTemp);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            // 1. Escreve Cabeçalho Mágico
            bos.write(MAGIC_HEADER);

            // 2. Escreve Stream Zip comprimido com compressão máxima
            try (ZipOutputStream zos = new ZipOutputStream(bos)) {
                zos.setLevel(Deflater.BEST_COMPRESSION);

                Files.walkFileTree(origemDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                        // Ignora diretórios git, build pesados e snapshots temporários
                        if (name.equals(".git") || name.equals("build") || name.equals(".gradle") || name.equals("node_modules")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        // Não inclui arquivos de snapshot no próprio snapshot
                        if (file.toString().endsWith(".thzsnap") || file.toString().endsWith(".tmp")) {
                            return FileVisitResult.CONTINUE;
                        }

                        Path rel = origemDir.relativize(file);
                        String entryName = rel.toString().replace("\\", "/");
                        ZipEntry entry = new ZipEntry(entryName);
                        entry.setTime(attrs.lastModifiedTime().toMillis());
                        zos.putNextEntry(entry);
                        Files.copy(file, zos);
                        zos.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }

        long tamanho = Files.size(arquivoTemp);
        if (tamanho >= MAX_BYTES_SNAPSHOT) {
            Files.deleteIfExists(arquivoTemp);
            throw new IllegalStateException(String.format(
                    "[Erro Snapshot] Tamanho do snapshot (%d bytes / %.2f MB) excedeu a cota máxima permitida de 100MB.",
                    tamanho, tamanho / (1024.0 * 1024.0)
            ));
        }

        // Move atomicamente para o destino final (substituindo o anterior, mantendo sempre 1)
        try {
            Files.move(arquivoTemp, destinoFinal, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(arquivoTemp, destinoFinal, StandardCopyOption.REPLACE_EXISTING);
        }

        return destinoFinal;
    }

    /**
     * Restaura os arquivos de um snapshot em um diretório de destino.
     */
    public static boolean restaurarSnapshot(Path snapshotArquivo, Path destinoDir) throws IOException {
        Path snap = (snapshotArquivo != null) ? snapshotArquivo : obterCaminhoSnapshotPadrao();
        if (!Files.exists(snap)) return false;

        Files.createDirectories(destinoDir);

        try (InputStream fis = Files.newInputStream(snap);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            // 1. Valida cabeçalho mágico
            byte[] header = new byte[MAGIC_HEADER.length];
            int lidos = bis.read(header);
            if (lidos != MAGIC_HEADER.length || !Arrays.equals(header, MAGIC_HEADER)) {
                throw new IOException("[Erro Snapshot] Arquivo de snapshot corrompido ou formato inválido: cabeçalho mágico THZSNAP ausente.");
            }

            // 2. Extrai entradas
            try (ZipInputStream zis = new ZipInputStream(bis)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path saida = destinoDir.resolve(entry.getName()).normalize();
                    if (!saida.startsWith(destinoDir)) {
                        throw new SecurityException("[Erro Snapshot] Tentativa de Zip Slip detectada: " + entry.getName());
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(saida);
                    } else {
                        if (saida.getParent() != null) Files.createDirectories(saida.getParent());
                        Files.copy(zis, saida, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }
        }
        return true;
    }

    /**
     * Retorna o tamanho do snapshot ativo em bytes (-1 se não existir).
     */
    public static long obterTamanhoSnapshot() {
        Path p = obterCaminhoSnapshotPadrao();
        if (!Files.exists(p)) return -1;
        try {
            return Files.size(p);
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Limpa o snapshot ativo do workspace.
     */
    public static boolean limparSnapshot() {
        Path p = obterCaminhoSnapshotPadrao();
        try {
            return Files.deleteIfExists(p);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Valida a integridade do arquivo de snapshot.
     */
    public static boolean verificarIntegridade(Path snapshotArquivo) {
        Path snap = (snapshotArquivo != null) ? snapshotArquivo : obterCaminhoSnapshotPadrao();
        if (!Files.exists(snap)) return false;
        try (InputStream fis = Files.newInputStream(snap);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            byte[] header = new byte[MAGIC_HEADER.length];
            int lidos = bis.read(header);
            return lidos == MAGIC_HEADER.length && Arrays.equals(header, MAGIC_HEADER);
        } catch (Exception e) {
            return false;
        }
    }
}
