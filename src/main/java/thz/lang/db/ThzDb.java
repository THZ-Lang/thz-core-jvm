package thz.lang.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DataHoraThz;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.DecimalFixo;

/**
 * ThzDb — Conexão, consultas e transações de banco de dados com mapeamento
 * exato de tipos THZ.
 */
public final class ThzDb {

    private static final Map<String, Connection> CONEXOES = new ConcurrentHashMap<>();
    private static Connection conexaoPadrao = null;

    private ThzDb() {
    }

    public static synchronized void conectar(String url) {
        conectar("padrao", url, null, null);
    }

    public static synchronized void conectar(String nome, String url, String usuario, String senha) {
        try {
            Connection conn;
            if (usuario != null && senha != null) {
                conn = DriverManager.getConnection(url, usuario, senha);
            } else {
                conn = DriverManager.getConnection(url);
            }
            CONEXOES.put(nome, conn);
            if (conexaoPadrao == null || "padrao".equals(nome)) {
                conexaoPadrao = conn;
            }
            // Otimizações automáticas para SQLite (Foreign Keys & WAL)
            if (url != null && url.startsWith("jdbc:sqlite:") && !url.contains(":memory:")) {
                try (var s = conn.createStatement()) {
                    s.execute("PRAGMA foreign_keys = ON;");
                    s.execute("PRAGMA journal_mode = WAL;");
                    s.execute("PRAGMA synchronous = NORMAL;");
                } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao conectar ao banco '" + nome + "': " + e.getMessage(), e);
        }
    }

    public static synchronized void fechar() {
        fechar("padrao");
    }

    public static synchronized void fechar(String nome) {
        Connection conn = CONEXOES.remove(nome != null ? nome : "padrao");
        if (conn != null) {
            try {
                if (!conn.isClosed()) conn.close();
            } catch (SQLException ignored) {}
        }
        if (conexaoPadrao == conn) {
            conexaoPadrao = CONEXOES.values().stream().findFirst().orElse(null);
        }
    }

    public static boolean temConexao(String nome) {
        Connection conn = nome != null ? CONEXOES.get(nome) : conexaoPadrao;
        try {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static Connection obterConexao(String nome) {
        Connection conn = nome != null ? CONEXOES.get(nome) : conexaoPadrao;
        if (conn == null)
            conn = conexaoPadrao;
        if (conn == null) {
            // Auto-conecta utilizando a configuração padrão do manifesto thz.config.json
            thz.lang.db.ThzDatabaseBridge.inicializarPadrao();
            conn = nome != null ? CONEXOES.get(nome) : conexaoPadrao;
            if (conn == null) conn = conexaoPadrao;
        }
        if (conn == null) {
            throw new IllegalStateException("Nenhuma conexão de banco ativa. Use BANCO.conectar(url).");
        }
        return conn;
    }

    public static long executar(String sql, List<ValorThz> parametros) {
        return executarEm("padrao", sql, parametros);
    }

    public static long executarEm(String conexaoNome, String sql, List<ValorThz> parametros) {
        Connection conn = obterConexao(conexaoNome);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            vincularParametros(stmt, parametros);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao executar SQL: " + e.getMessage(), e);
        }
    }

    public static List<ValorThz.Registro> consultar(String sql, List<ValorThz> parametros) {
        return consultarEm("padrao", sql, parametros);
    }

    public static List<ValorThz.Registro> consultarEm(String conexaoNome, String sql, List<ValorThz> parametros) {
        Connection conn = obterConexao(conexaoNome);
        List<ValorThz.Registro> linhas = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            vincularParametros(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colunas = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, ValorThz> campos = new LinkedHashMap<>();
                    for (int i = 1; i <= colunas; i++) {
                        String nomeCol = meta.getColumnLabel(i);
                        Object val = rs.getObject(i);
                        campos.put(nomeCol, converterParaThz(val, meta.getColumnType(i)));
                    }
                    linhas.add(new ValorThz.Registro("LinhaSql", campos));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar SQL: " + e.getMessage(), e);
        }
        return linhas;
    }

    public static ValorThz consultarValor(String sql, List<ValorThz> parametros) {
        return consultarValorEm("padrao", sql, parametros);
    }

    public static ValorThz consultarValorEm(String conexaoNome, String sql, List<ValorThz> parametros) {
        Connection conn = obterConexao(conexaoNome);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            vincularParametros(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    Object val = rs.getObject(1);
                    return converterParaThz(val, meta.getColumnType(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar valor escalar SQL: " + e.getMessage(), e);
        }
        return ValorThz.NULO;
    }

    public static void iniciarTransacao() {
        iniciarTransacaoEm("padrao");
    }

    public static void iniciarTransacaoEm(String conexaoNome) {
        Connection conn = obterConexao(conexaoNome);
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao iniciar transação: " + e.getMessage(), e);
        }
    }

    public static void confirmarTransacao() {
        confirmarTransacaoEm("padrao");
    }

    public static void confirmarTransacaoEm(String conexaoNome) {
        Connection conn = obterConexao(conexaoNome);
        try {
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao confirmar transação: " + e.getMessage(), e);
        }
    }

    public static void cancelarTransacao() {
        cancelarTransacaoEm("padrao");
    }

    public static void cancelarTransacaoEm(String conexaoNome) {
        Connection conn = obterConexao(conexaoNome);
        try {
            conn.rollback();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cancelar transação: " + e.getMessage(), e);
        }
    }

    public static void executarScript(String sqlMultiplo) {
        executarScriptEm("padrao", sqlMultiplo);
    }

    public static void executarScriptEm(String conexaoNome, String sqlMultiplo) {
        Connection conn = obterConexao(conexaoNome);
        try (var stmt = conn.createStatement()) {
            for (String comando : sqlMultiplo.split(";")) {
                String limpo = comando.trim();
                if (!limpo.isEmpty()) {
                    stmt.execute(limpo);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao executar script SQL: " + e.getMessage(), e);
        }
    }

    public static String obterDriverNome(String conexaoNome) {
        try {
            Connection conn = obterConexao(conexaoNome);
            String url = conn.getMetaData().getURL();
            if (url != null) {
                if (url.startsWith("jdbc:sqlite:")) return "SQLITE";
                if (url.startsWith("jdbc:postgresql:")) return "POSTGRES";
                if (url.startsWith("jdbc:mysql:") || url.startsWith("jdbc:mariadb:")) return "MYSQL";
                if (url.startsWith("jdbc:oracle:")) return "ORACLE";
                if (url.startsWith("jdbc:sqlserver:")) return "SQLSERVER";
            }
            return conn.getMetaData().getDatabaseProductName().toUpperCase();
        } catch (Exception e) {
            return "DESCONHECIDO";
        }
    }

    private static void vincularParametros(PreparedStatement stmt, List<ValorThz> params) throws SQLException {
        if (params == null)
            return;
        for (int i = 0; i < params.size(); i++) {
            ValorThz p = params.get(i);
            int idx = i + 1;
            if (p instanceof ValorThz.Texto t) {
                stmt.setString(idx, t.valor());
            } else if (p instanceof ValorThz.Inteiro in) {
                stmt.setLong(idx, in.valor().longValue());
            } else if (p instanceof ValorThz.Decimal d) {
                stmt.setBigDecimal(idx, new BigDecimal(d.valor().valorEscalado, d.valor().escala));
            } else if (p instanceof ValorThz.Monetario m) {
                stmt.setBigDecimal(idx, new BigDecimal(m.valor().quantia.valorEscalado, m.valor().quantia.escala));
            } else if (p instanceof ValorThz.Logico l) {
                stmt.setBoolean(idx, l.valor());
            } else if (p instanceof ValorThz.Data dt) {
                stmt.setString(idx, dt.valor().formatar());
            } else if (p instanceof ValorThz.DataHora dh) {
                stmt.setString(idx, dh.valor().formatar());
            } else if (p instanceof ValorThz.Nulo) {
                stmt.setNull(idx, Types.NULL);
            } else {
                stmt.setString(idx, p != null ? p.formatar() : null);
            }
        }
    }

    private static ValorThz converterParaThz(Object obj, int sqlType) {
        if (obj == null)
            return ValorThz.NULO;
        if (obj instanceof Boolean b)
            return ValorThz.LOGICO(b);
        if (sqlType == Types.BOOLEAN || sqlType == Types.BIT) {
            if (obj instanceof Number n) return ValorThz.LOGICO(n.intValue() != 0);
            return ValorThz.LOGICO(Boolean.parseBoolean(obj.toString()));
        }
        if (obj instanceof String s)
            return ValorThz.TEXTO(s);
        if (obj instanceof Number n) {
            if (obj instanceof BigDecimal bd) {
                return ValorThz.DECIMAL(DecimalFixo.deTexto(bd.toPlainString(), bd.scale()));
            }
            if (obj instanceof Long || obj instanceof Integer || obj instanceof Short || obj instanceof Byte) {
                return ValorThz.INTEIRO(n.longValue());
            }
            BigDecimal bd = new BigDecimal(n.toString());
            return ValorThz.DECIMAL(DecimalFixo.deTexto(bd.toPlainString(), 4));
        }
        if (obj instanceof java.sql.Date d) {
            return ValorThz.DATA(DataThz.deTexto(d.toString()));
        }
        if (obj instanceof java.sql.Timestamp ts) {
            return ValorThz.DATA_HORA(DataHoraThz.deTexto(ts.toLocalDateTime().toString()));
        }
        return ValorThz.TEXTO(obj.toString());
    }

    public static synchronized void fecharTodas() {
        for (Connection c : CONEXOES.values()) {
            try {
                if (!c.isClosed())
                    c.close();
            } catch (SQLException ignored) {
            }
        }
        CONEXOES.clear();
        conexaoPadrao = null;
    }
}
