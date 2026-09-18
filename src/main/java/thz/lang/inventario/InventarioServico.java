package thz.lang.inventario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Fronteira transacional da aplicação de referência. Identidade é injetada pelo host. */
public final class InventarioServico {
    public record Item(String codigo, String descricao, String localizacao, long versao) {}
    public record Ocorrencia(int linha, String codigo, String estado, List<String> detalhes) {}
    public record Relatorio(boolean somenteValidar, String origem, String regra, int revisao,
                            String referencia, List<Ocorrencia> ocorrencias) {}
    public record Historico(String codigo, String campo, String anterior, String novo, long versao,
                            String autor, String justificativa, String instante, String origem,
                            String regra, int revisao, String referencia, String operacao, boolean arquivado) {}

    private final Path banco;
    private final String autor;
    private final Clock relogio;
    private final RegrasInventario regras;

    public InventarioServico(Path banco, Principal autenticado) {
        this(banco, autenticado, Clock.systemUTC(), new RegrasInventario());
    }

    public InventarioServico(Path banco, Principal autenticado, Clock relogio, RegrasInventario regras) {
        this.banco = banco.toAbsolutePath().normalize();
        this.autor = autenticado == null ? null : autenticado.getName();
        this.relogio = Objects.requireNonNull(relogio);
        this.regras = Objects.requireNonNull(regras);
    }

    public Relatorio importar(Path arquivo, LocalDate referencia, String justificativa, boolean somenteValidar)
            throws IOException, SQLException {
        exigirAutoria(justificativa);
        var revisao = regras.selecionar(referencia);
        // O mesmo conteúdo lido e identificado pelo hash é validado e processado.
        var planilha = PlanilhaInventario.ler(arquivo);
        var primeiros = new HashMap<String, PlanilhaInventario.Linha>();
        var conflitantes = new HashSet<String>();
        for (var linha : planilha.linhas()) {
            if (linha.codigo() == null || linha.codigo().isBlank()) continue;
            var anterior = primeiros.putIfAbsent(linha.codigo(), linha);
            if (anterior != null && (!Objects.equals(anterior.descricao(), linha.descricao())
                    || !Objects.equals(anterior.localizacao(), linha.localizacao()) || !anterior.erros().equals(linha.erros()))) {
                conflitantes.add(linha.codigo());
            }
        }
        var ocorrencias = new ArrayList<Ocorrencia>();
        try (var conexao = abrir(somenteValidar)) {
            for (var linha : planilha.linhas()) {
                var erros = new ArrayList<>(linha.erros());
                erros.addAll(regras.validar(linha.codigo(), linha.descricao(), linha.localizacao()));
                if (conflitantes.contains(linha.codigo())) erros.add("CONFLITO_ENTRADA: código repetido com valores diferentes.");
                if (!erros.isEmpty()) {
                    ocorrencias.add(new Ocorrencia(linha.numero(), linha.codigo(), "BLOQUEADO", List.copyOf(erros)));
                    continue;
                }
                boolean transacao = false;
                try {
                    if (!somenteValidar) { executar(conexao, "BEGIN IMMEDIATE"); transacao = true; }
                    var atual = consultar(conexao, linha.codigo());
                    String estado;
                    var detalhes = new ArrayList<String>();
                    if (atual == null) {
                        estado = somenteValidar ? "CADASTRAVEL" : "CADASTRADO";
                        if (!somenteValidar) cadastrar(conexao, linha, planilha.origem(), referencia, revisao.numero(), justificativa);
                    } else {
                        if (!atual.descricao().equals(linha.descricao())) detalhes.add("descricao: banco=" + atual.descricao() + "; planilha=" + linha.descricao());
                        if (!atual.localizacao().equals(linha.localizacao())) detalhes.add("localizacao: banco=" + atual.localizacao() + "; planilha=" + linha.localizacao());
                        estado = detalhes.isEmpty() ? "SEM_ALTERACAO" : "DIVERGENTE";
                    }
                    if (transacao) { executar(conexao, "COMMIT"); transacao = false; }
                    ocorrencias.add(new Ocorrencia(linha.numero(), linha.codigo(), estado, List.copyOf(detalhes)));
                } catch (SQLException e) {
                    if (transacao) executar(conexao, "ROLLBACK");
                    String mensagem = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    ocorrencias.add(new Ocorrencia(linha.numero(), linha.codigo(), "FALHA_OPERACIONAL", List.of(mensagem)));
                }
            }
        }
        return new Relatorio(somenteValidar, planilha.origem(), RegrasInventario.IDENTIDADE,
                revisao.numero(), referencia.toString(), List.copyOf(ocorrencias));
    }

    public Ocorrencia atualizar(String codigo, long versaoEsperada, String descricao, String localizacao,
                                LocalDate referencia, String justificativa) throws SQLException {
        exigirAutoria(justificativa);
        var revisao = regras.selecionar(referencia);
        var erros = regras.validar(codigo, descricao, localizacao);
        if (!erros.isEmpty()) return new Ocorrencia(0, codigo, "BLOQUEADO", erros);
        if (codigo.length() > PlanilhaInventario.MAX_CAMPO || descricao.length() > PlanilhaInventario.MAX_CAMPO
                || localizacao.length() > PlanilhaInventario.MAX_CAMPO) {
            return new Ocorrencia(0, codigo, "BLOQUEADO", List.of("INVALIDO: campo excede 2048 caracteres."));
        }
        try (var conexao = abrir(false)) {
            executar(conexao, "BEGIN IMMEDIATE");
            try {
                var atual = consultar(conexao, codigo);
                if (atual == null || atual.versao() != versaoEsperada) {
                    executar(conexao, "ROLLBACK");
                    return new Ocorrencia(0, codigo, "BLOQUEADO", List.of(atual == null
                            ? "NAO_ENCONTRADO: item inexistente." : "CONFLITO_VERSAO: releia o item e revise as diferenças."));
                }
                boolean mudaDescricao = !atual.descricao().equals(descricao);
                boolean mudaLocalizacao = !atual.localizacao().equals(localizacao);
                if (!mudaDescricao && !mudaLocalizacao) {
                    executar(conexao, "ROLLBACK");
                    return new Ocorrencia(0, codigo, "SEM_ALTERACAO", List.of());
                }
                long versao = Math.addExact(atual.versao(), 1);
                String operacao = UUID.randomUUID().toString(), instante = relogio.instant().toString();
                if (mudaDescricao) historizar(conexao, codigo, "descricao", atual.descricao(), descricao, versao,
                        justificativa, instante, "atualizacao_explicita", revisao.numero(), referencia, operacao);
                if (mudaLocalizacao) historizar(conexao, codigo, "localizacao", atual.localizacao(), localizacao, versao,
                        justificativa, instante, "atualizacao_explicita", revisao.numero(), referencia, operacao);
                try (var stmt = conexao.prepareStatement("UPDATE inventario_item SET descricao=?, localizacao=?, versao=? WHERE codigo=? AND versao=?")) {
                    stmt.setString(1, descricao); stmt.setString(2, localizacao); stmt.setLong(3, versao);
                    stmt.setString(4, codigo); stmt.setLong(5, versaoEsperada);
                    if (stmt.executeUpdate() != 1) throw new SQLException("Conflito de versão durante a confirmação.");
                }
                executar(conexao, "COMMIT");
                return new Ocorrencia(0, codigo, "ATUALIZADO", List.of("versao=" + versao));
            } catch (SQLException | RuntimeException e) {
                executar(conexao, "ROLLBACK");
                throw e;
            }
        }
    }

    public Item consultar(String codigo) throws SQLException {
        try (var conexao = abrir(true)) { return consultar(conexao, codigo); }
    }

    public List<Historico> historico(String codigo) throws SQLException {
        return historico(codigo, 0, 1000);
    }

    public List<Historico> historico(String codigo, int deslocamento, int limite) throws SQLException {
        if (deslocamento < 0 || limite < 1 || limite > 1000) throw new IllegalArgumentException("Histórico exige deslocamento >= 0 e limite entre 1 e 1000.");
        try (var conexao = abrir(true); var stmt = conexao.prepareStatement("""
                SELECT *, CASE WHEN anterior IS NULL THEN 0 ELSE
                  (SELECT COUNT(*) FROM inventario_historico h2 WHERE h2.codigo=h.codigo
                   AND h2.campo=h.campo AND h2.anterior IS NOT NULL AND h2.id>h.id) >= 10 END AS arquivado
                FROM inventario_historico h WHERE codigo=? ORDER BY id LIMIT ? OFFSET ?
                """)) {
            stmt.setString(1, codigo);
            stmt.setInt(2, limite); stmt.setInt(3, deslocamento);
            var resultado = new ArrayList<Historico>();
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) resultado.add(new Historico(rs.getString("codigo"), rs.getString("campo"),
                        rs.getString("anterior"), rs.getString("novo"), rs.getLong("versao"), rs.getString("autor"),
                        rs.getString("justificativa"), rs.getString("instante"), rs.getString("origem"),
                        rs.getString("regra"), rs.getInt("revisao"), rs.getString("referencia"),
                        rs.getString("operacao"), rs.getBoolean("arquivado")));
            }
            return List.copyOf(resultado);
        }
    }

    private void exigirAutoria(String justificativa) {
        if (autor == null || autor.isBlank()) throw new IllegalStateException("Autor autenticado obrigatório; o host deve fornecer a identidade.");
        if (justificativa == null || justificativa.isBlank()) throw new IllegalArgumentException("Justificativa obrigatória.");
        if (justificativa.length() > PlanilhaInventario.MAX_CAMPO) throw new IllegalArgumentException("Justificativa excede 2048 caracteres.");
    }

    private Connection abrir(boolean leitura) throws SQLException {
        boolean existe = Files.exists(banco);
        if (!leitura && banco.getParent() != null) {
            try {
                Files.createDirectories(banco.getParent());
            } catch (IOException e) {
                throw new SQLException("Não foi possível preparar o diretório do inventário: " + banco.getParent(), e);
            }
        }
        // Uma validação inicial usa apenas um banco efêmero, sem criar o arquivo do acervo.
        String url = leitura ? (existe ? "jdbc:sqlite:" + banco.toUri() + "?mode=ro" : "jdbc:sqlite::memory:")
                : "jdbc:sqlite:" + banco;
        var conexao = DriverManager.getConnection(url);
        try {
            executar(conexao, "PRAGMA busy_timeout=5000");
            executar(conexao, "PRAGMA foreign_keys=ON");
            if (!leitura || !existe) inicializar(conexao);
            return conexao;
        } catch (SQLException e) { conexao.close(); throw e; }
    }

    private static void inicializar(Connection c) throws SQLException {
        executar(c, """
                CREATE TABLE IF NOT EXISTS inventario_item (
                  codigo TEXT PRIMARY KEY NOT NULL CHECK(length(trim(codigo))>0),
                  descricao TEXT NOT NULL CHECK(length(trim(descricao))>0),
                  localizacao TEXT NOT NULL CHECK(length(trim(localizacao))>0),
                  versao INTEGER NOT NULL CHECK(versao>0))
                """);
        executar(c, """
                CREATE TABLE IF NOT EXISTS inventario_historico (
                  id INTEGER PRIMARY KEY, codigo TEXT NOT NULL REFERENCES inventario_item(codigo),
                  campo TEXT NOT NULL, anterior TEXT, novo TEXT NOT NULL, versao INTEGER NOT NULL,
                  autor TEXT NOT NULL CHECK(length(trim(autor))>0),
                  justificativa TEXT NOT NULL CHECK(length(trim(justificativa))>0),
                  instante TEXT NOT NULL, origem TEXT NOT NULL, regra TEXT NOT NULL,
                  revisao INTEGER NOT NULL, referencia TEXT NOT NULL, operacao TEXT NOT NULL,
                  UNIQUE(codigo, campo, versao))
                """);
        executar(c, "CREATE INDEX IF NOT EXISTS inventario_historico_campo ON inventario_historico(codigo,campo,id)");
        executar(c, """
                CREATE TRIGGER IF NOT EXISTS inventario_codigo_imutavel BEFORE UPDATE OF codigo ON inventario_item
                WHEN NEW.codigo <> OLD.codigo BEGIN SELECT RAISE(ABORT, 'Código de inventário imutável.'); END
                """);
        executar(c, """
                CREATE TRIGGER IF NOT EXISTS inventario_historico_imutavel BEFORE UPDATE ON inventario_historico
                BEGIN SELECT RAISE(ABORT, 'Histórico imutável.'); END
                """);
        executar(c, """
                CREATE TRIGGER IF NOT EXISTS inventario_historico_preservado BEFORE DELETE ON inventario_historico
                BEGIN SELECT RAISE(ABORT, 'Histórico não pode ser descartado.'); END
                """);
    }

    private static Item consultar(Connection c, String codigo) throws SQLException {
        try (var stmt = c.prepareStatement("SELECT codigo, descricao, localizacao, versao FROM inventario_item WHERE codigo=?")) {
            stmt.setString(1, codigo);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? new Item(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4)) : null;
            }
        }
    }

    private void cadastrar(Connection c, PlanilhaInventario.Linha linha, String origem, LocalDate referencia,
                           int revisao, String justificativa) throws SQLException {
        try (var stmt = c.prepareStatement("INSERT INTO inventario_item VALUES(?,?,?,1)")) {
            stmt.setString(1, linha.codigo()); stmt.setString(2, linha.descricao()); stmt.setString(3, linha.localizacao());
            stmt.executeUpdate();
        }
        String operacao = UUID.randomUUID().toString(), instante = relogio.instant().toString();
        origem += ":linha=" + linha.numero();
        historizar(c, linha.codigo(), "codigo", null, linha.codigo(), 1, justificativa, instante, origem, revisao, referencia, operacao);
        historizar(c, linha.codigo(), "descricao", null, linha.descricao(), 1, justificativa, instante, origem, revisao, referencia, operacao);
        historizar(c, linha.codigo(), "localizacao", null, linha.localizacao(), 1, justificativa, instante, origem, revisao, referencia, operacao);
    }

    private void historizar(Connection c, String codigo, String campo, String anterior, String novo, long versao,
                            String justificativa, String instante, String origem, int revisao, LocalDate referencia,
                            String operacao) throws SQLException {
        try (var stmt = c.prepareStatement("""
                INSERT INTO inventario_historico(codigo,campo,anterior,novo,versao,autor,justificativa,
                  instante,origem,regra,revisao,referencia,operacao) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            stmt.setString(1, codigo); stmt.setString(2, campo); stmt.setString(3, anterior); stmt.setString(4, novo);
            stmt.setLong(5, versao); stmt.setString(6, autor); stmt.setString(7, justificativa); stmt.setString(8, instante);
            stmt.setString(9, origem); stmt.setString(10, RegrasInventario.IDENTIDADE); stmt.setInt(11, revisao);
            stmt.setString(12, referencia.toString()); stmt.setString(13, operacao); stmt.executeUpdate();
        }
    }

    private static void executar(Connection c, String sql) throws SQLException {
        try (var stmt = c.createStatement()) { stmt.execute(sql); }
    }
}
