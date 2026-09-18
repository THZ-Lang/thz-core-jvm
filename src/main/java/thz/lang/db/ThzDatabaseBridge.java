package thz.lang.db;

import thz.lang.config.ThzProjectConfig;
import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DecimalFixo;
import thz.lang.vetor.ThzVetorSimd;

import java.nio.file.Path;
import java.util.*;

/**
 * ThzDatabaseBridge — Intermediador Universal de Banco de Dados e Abstração ORM/JPA-like.
 * Suporta persistência automática de estruturas/registros, busca por ID, DDL dinâmica
 * e busca vetorial semântica KNN integrada ao motor SIMD do THZ-LANG.
 */
public final class ThzDatabaseBridge {

    static {
        inicializarPadrao();
    }

    private ThzDatabaseBridge() {}

    public static synchronized void inicializarPadrao() {
        if (ThzDb.temConexao("padrao")) {
            return;
        }
        var cfg = ThzProjectConfig.obterConfig().banco();
        String url = cfg.url();
        if (url == null || url.isBlank() || "auto".equalsIgnoreCase(url)) {
            url = "jdbc:sqlite:dados/app.db";
        }
        url = thz.lang.io.ThzLocalizadorRecursos.resolverUrlBancoSqlite(url, Path.of("."));
        try {
            ThzDb.conectar("padrao", url, cfg.usuario().isEmpty() ? null : cfg.usuario(), cfg.senha().isEmpty() ? null : cfg.senha());
        } catch (Exception e) {
            // Fallback para SQLite em memória se o caminho falhar
            try {
                ThzDb.conectar("padrao", "jdbc:sqlite::memory:", null, null);
            } catch (Exception ignored) {}
        }
    }

    public static String driverAtivo() {
        return ThzDb.obterDriverNome("padrao");
    }

    /**
     * Persistência estilo JPA (save/persist): executa INSERT ou UPDATE automaticamente
     * baseado na presença de chave primária 'id' ou 'codigo'.
     */
    public static ValorThz salvar(String tabela, ValorThz entidade) {
        Map<String, ValorThz> campos = extrairCampos(entidade);
        if (campos.isEmpty()) {
            throw new IllegalArgumentException("Entidade vazia não pode ser salva na tabela: " + tabela);
        }

        String campoId = campos.containsKey("id") ? "id" : campos.containsKey("codigo") ? "codigo" : campos.containsKey("idTransacao") ? "idTransacao" : null;
        ValorThz valorId = campoId != null ? campos.get(campoId) : null;

        boolean existe = false;
        if (valorId != null && !(valorId instanceof ValorThz.Nulo)) {
            try {
                var existente = buscarPorId(tabela, valorId);
                existe = existente != null && !(existente instanceof ValorThz.Nulo);
            } catch (Exception ignored) {}
        }

        if (existe && campoId != null) {
            // Executa UPDATE
            List<String> setClauses = new ArrayList<>();
            List<ValorThz> params = new ArrayList<>();
            for (var entry : campos.entrySet()) {
                if (!entry.getKey().equals(campoId)) {
                    setClauses.add(entry.getKey() + " = ?");
                    params.add(entry.getValue());
                }
            }
            params.add(valorId);
            String sql = "UPDATE " + tabela + " SET " + String.join(", ", setClauses) + " WHERE " + campoId + " = ?";
            ThzDb.executar(sql, params);
        } else {
            // Executa INSERT
            List<String> colunas = new ArrayList<>(campos.keySet());
            List<String> placeholders = colunas.stream().map(c -> "?").toList();
            List<ValorThz> params = new ArrayList<>(campos.values());
            String sql = "INSERT INTO " + tabela + " (" + String.join(", ", colunas) + ") VALUES (" + String.join(", ", placeholders) + ")";
            ThzDb.executar(sql, params);
        }

        return entidade;
    }

    /**
     * Busca por ID estilo JPA (find/getById).
     */
    public static ValorThz buscarPorId(String tabela, ValorThz id) {
        String colId = descobrirColunaId(tabela);
        String sql = "SELECT * FROM " + tabela + " WHERE " + colId + " = ? LIMIT 1";
        var linhas = ThzDb.consultar(sql, List.of(id));
        return !linhas.isEmpty() ? linhas.get(0) : ValorThz.NULO;
    }

    /**
     * Remoção por ID estilo JPA (deleteById).
     */
    public static boolean removerPorId(String tabela, ValorThz id) {
        String colId = descobrirColunaId(tabela);
        String sql = "DELETE FROM " + tabela + " WHERE " + colId + " = ?";
        long afetadas = ThzDb.executar(sql, List.of(id));
        return afetadas > 0;
    }

    private static String descobrirColunaId(String tabela) {
        try {
            var amostra = ThzDb.consultar("SELECT * FROM " + tabela + " LIMIT 1", List.of());
            if (!amostra.isEmpty()) {
                var campos = amostra.get(0).campos().keySet();
                for (String c : campos) {
                    if ("id".equalsIgnoreCase(c) || "codigo".equalsIgnoreCase(c) || "idTransacao".equalsIgnoreCase(c)) {
                        return c;
                    }
                }
                if (!campos.isEmpty()) return campos.iterator().next();
            }
        } catch (Exception ignored) {}
        return "id";
    }

    /**
     * Criação dinâmica DDL de tabelas (auto-migração).
     */
    public static boolean criarTabela(String tabela, Map<String, String> colunasDefinicao) {
        List<String> defs = new ArrayList<>();
        for (var entry : colunasDefinicao.entrySet()) {
            defs.add(entry.getKey() + " " + mapearTipoSql(entry.getValue()));
        }
        String sql = "CREATE TABLE IF NOT EXISTS " + tabela + " (" + String.join(", ", defs) + ")";
        ThzDb.executar(sql, List.of());
        return true;
    }

    /**
     * Consulta Semântica Vetorial KNN (K-Nearest Neighbors).
     * Realiza busca por similaridade de cosseno diretamente nas linhas do banco.
     */
    public static List<ValorThz.Registro> consultarVetorial(String tabela, String colunaVetor, float[] vetorConsulta, int limite) {
        String sql = "SELECT * FROM " + tabela;
        var todos = ThzDb.consultar(sql, List.of());

        record ItemScore(ValorThz.Registro registro, double similaridade) {}
        List<ItemScore> pontuados = new ArrayList<>();

        for (ValorThz.Registro reg : todos) {
            ValorThz valVetor = reg.campos().get(colunaVetor);
            if (valVetor != null) {
                float[] vetorItem = parsearVetor(valVetor.formatar());
                if (vetorItem.length > 0) {
                    double sim = ThzVetorSimd.similaridadeCosseno(vetorConsulta, vetorItem);
                    pontuados.add(new ItemScore(reg, sim));
                }
            }
        }

        pontuados.sort((a, b) -> Double.compare(b.similaridade(), a.similaridade()));

        List<ValorThz.Registro> resultado = new ArrayList<>();
        int max = Math.min(limite, pontuados.size());
        for (int i = 0; i < max; i++) {
            var item = pontuados.get(i);
            Map<String, ValorThz> camposComScore = new LinkedHashMap<>(item.registro().campos());
            camposComScore.put("_similaridade", ValorThz.DECIMAL(DecimalFixo.deTexto(String.format(Locale.US, "%.6f", item.similaridade()), 6)));
            resultado.add(new ValorThz.Registro(tabela + "_Item", camposComScore));
        }

        return resultado;
    }

    private static Map<String, ValorThz> extrairCampos(ValorThz valor) {
        if (valor instanceof ValorThz.Registro r) {
            return r.campos();
        }
        return Map.of();
    }

    private static String mapearTipoSql(String tipoThz) {
        String t = tipoThz.toUpperCase().trim();
        if (t.contains("INTEIRO") || t.contains("INT")) return "INTEGER";
        if (t.contains("DECIMAL") || t.contains("MONETARIO") || t.contains("NUMERO")) return "NUMERIC";
        if (t.contains("LOGICO") || t.contains("BOOL")) return "BOOLEAN";
        if (t.contains("DATA_HORA") || t.contains("TIMESTAMP")) return "TIMESTAMP";
        if (t.contains("DATA")) return "DATE";
        return "TEXT";
    }

    private static float[] parsearVetor(String str) {
        String limpo = str.replace("[", "").replace("]", "").trim();
        if (limpo.isEmpty()) return new float[0];
        String[] partes = limpo.split("[,;\\s]+");
        float[] v = new float[partes.length];
        for (int i = 0; i < partes.length; i++) {
            try { v[i] = Float.parseFloat(partes[i]); } catch (Exception e) { v[i] = 0f; }
        }
        return v;
    }
}
