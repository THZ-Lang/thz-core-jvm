package thz.lang.inventario;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** CSV delimitado por ';', com aspas, quebras em campos e limites antes da gravação. */
public record PlanilhaInventario(List<Linha> linhas, String origem) {
    public static final int MAX_BYTES = 4 * 1024 * 1024;
    public static final int MAX_LINHAS = 10_000;
    public static final int MAX_CAMPO = 2_048;
    public record Linha(int numero, String codigo, String descricao, String localizacao, List<String> erros) {}

    public static PlanilhaInventario ler(Path caminho) throws IOException {
        byte[] bytes;
        try (var entrada = Files.newInputStream(caminho)) {
            bytes = entrada.readNBytes(MAX_BYTES + 1);
        }
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("CSV excede o limite de 4 MiB.");
        String texto = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        if (texto.startsWith("\uFEFF")) texto = texto.substring(1);
        var registros = parsear(texto);
        if (registros.isEmpty() || !registros.getFirst().campos().equals(List.of("codigo", "descricao", "localizacao"))) {
            throw new IllegalArgumentException("Cabeçalho esperado: codigo;descricao;localizacao.");
        }
        var linhas = new ArrayList<Linha>();
        for (var registro : registros.subList(1, registros.size())) {
            var c = registro.campos();
            var erros = c.size() == 3 ? List.<String>of() : List.of("INVALIDO: esperadas três colunas.");
            linhas.add(new Linha(registro.numero(), c.getFirst(), c.size() > 1 ? c.get(1) : null,
                    c.size() > 2 ? c.get(2) : null, erros));
        }
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            return new PlanilhaInventario(List.copyOf(linhas), caminho.toAbsolutePath().normalize() + "#sha256=" + hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private record Registro(int numero, List<String> campos) {}

    private static List<Registro> parsear(String texto) {
        var registros = new ArrayList<Registro>();
        var campos = new ArrayList<String>();
        var campo = new StringBuilder();
        boolean aspas = false, fechado = false, iniciado = false;
        int linha = 1, inicio = 1;
        for (int i = 0; i <= texto.length(); i++) {
            boolean fim = i == texto.length();
            char c = fim ? '\n' : texto.charAt(i);
            if (fim && aspas) throw new IllegalArgumentException("CSV inválido: aspas abertas na linha " + inicio + ".");
            if (fim && !iniciado && campos.isEmpty() && campo.isEmpty()) break;
            if (aspas) {
                if (c == '"') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') { campo.append('"'); i++; }
                    else { aspas = false; fechado = true; }
                } else {
                    campo.append(c);
                    if (c == '\n') linha++;
                }
            } else if (c == ';' || c == '\n' || c == '\r') {
                campos.add(campo.toString()); campo.setLength(0); fechado = false; iniciado = false;
                if (c != ';') {
                    registros.add(new Registro(inicio, List.copyOf(campos))); campos.clear();
                    if (registros.size() > MAX_LINHAS + 1) throw new IllegalArgumentException("CSV excede 10000 registros.");
                    if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') i++;
                    linha++; inicio = linha;
                }
            } else if (c == '"' && !iniciado && campo.isEmpty()) {
                aspas = true; iniciado = true;
            } else {
                if (fechado || c == '"' || c == '\0') throw new IllegalArgumentException("CSV inválido na linha " + linha + ".");
                iniciado = true; campo.append(c);
            }
            if (campo.length() > MAX_CAMPO || campos.size() > 3) {
                throw new IllegalArgumentException("CSV excede o limite de campo ou colunas na linha " + linha + ".");
            }
        }
        return registros;
    }
}
