package thz.lang.brasil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/**
 * ThzInternalDatabase — Gerenciador do banco de dados interno protegido da linguagem (.thzdbi).
 * O formato .thzdbi é um banco SQLite físico com suporte a dados relacionais e vetoriais,
 * operado internamente pelo motor THZ para tabelas do Brasil (CEPs, IBGE, feriados) e caches.
 */
public final class ThzInternalDatabase {

    private static final String EXTENSAO_PADRAO = ".thzdbi";
    private static volatile Path caminhoDbInterno = null;
    private static volatile boolean inicializado = false;

    private ThzInternalDatabase() {}

    /**
     * Retorna o caminho do banco interno .thzdbi, criando a pasta e o schema se necessário.
     */
    public static synchronized Path obterCaminhoDb() {
        if (caminhoDbInterno == null) {
            Path raiz = Path.of(".").toAbsolutePath().normalize();
            Path dirInterno = raiz.resolve(".thz").resolve("internal");
            try {
                Files.createDirectories(dirInterno);
            } catch (IOException ignored) {}
            caminhoDbInterno = dirInterno.resolve("core" + EXTENSAO_PADRAO);
        }
        return caminhoDbInterno;
    }

    /**
     * Define um caminho alternativo para o banco .thzdbi (usado em testes ou isolamento).
     */
    public static synchronized void definirCaminhoDb(Path caminho) {
        caminhoDbInterno = caminho;
        inicializado = false;
        garantirSchema();
    }

    /**
     * Abre uma conexão JDBC com o banco .thzdbi interno.
     */
    public static Connection obterConexao() throws SQLException {
        Path p = obterCaminhoDb();
        String url = "jdbc:sqlite:" + p.toAbsolutePath().toString().replace("\\", "/");
        return DriverManager.getConnection(url);
    }

    private static synchronized void garantirSchema() {
        if (inicializado) return;
        inicializado = true;
        try (Connection conn = obterConexao(); Statement stmt = conn.createStatement()) {
            // Tabela de CEPs e Endereços do Brasil
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS thz_ceps (
                    cep TEXT PRIMARY KEY,
                    logradouro TEXT,
                    bairro TEXT,
                    cidade TEXT,
                    uf TEXT,
                    ibge TEXT,
                    ddd TEXT
                );
            """);

            // Tabela de Municípios IBGE
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS thz_municipios (
                    codigo_ibge TEXT PRIMARY KEY,
                    nome TEXT,
                    uf TEXT,
                    ddd TEXT
                );
            """);

            // Tabela de Cache Interno
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS thz_cache_interno (
                    chave TEXT PRIMARY KEY,
                    valor TEXT,
                    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Popula dados essenciais de capitais e referências se estiver vazio
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM thz_ceps")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    popularCepsIniciais(conn);
                }
            }

            inicializado = true;
        } catch (SQLException e) {
            // Falha silenciosa em caso de restrição de I/O em ambientes efêmeros
        }
    }

    private static void popularCepsIniciais(Connection conn) throws SQLException {
        String sql = "INSERT OR IGNORE INTO thz_ceps (cep, logradouro, bairro, cidade, uf, ibge, ddd) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // São Paulo - SP
            inserirCep(ps, "01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP", "3550308", "11");
            inserirCep(ps, "01001000", "Praça da Sé", "Sé", "São Paulo", "SP", "3550308", "11");
            inserirCep(ps, "04571010", "Avenida Engenheiro Luís Carlos Berrini", "Cidade Monções", "São Paulo", "SP", "3550308", "11");
            inserirCep(ps, "13010001", "Rua Treze de Maio", "Centro", "Campinas", "SP", "3509502", "19");

            // Rio de Janeiro - RJ
            inserirCep(ps, "20040002", "Avenida Rio Branco", "Centro", "Rio de Janeiro", "RJ", "3304557", "21");
            inserirCep(ps, "22041001", "Avenida Atlântica", "Copacabana", "Rio de Janeiro", "RJ", "3304557", "21");

            // Brasília - DF
            inserirCep(ps, "70040010", "Praça dos Três Poderes", "Zona Cívico-Administrativa", "Brasília", "DF", "5300108", "61");
            inserirCep(ps, "70070010", "Setor de Autarquias Sul", "Asa Sul", "Brasília", "DF", "5300108", "61");

            // Belo Horizonte - MG
            inserirCep(ps, "30130100", "Avenida Afonso Pena", "Centro", "Belo Horizonte", "MG", "3106200", "31");

            // Curitiba - PR
            inserirCep(ps, "80020010", "Rua XV de Novembro", "Centro", "Curitiba", "PR", "4106902", "41");

            // Porto Alegre - RS
            inserirCep(ps, "90010150", "Rua dos Andradas", "Centro Histórico", "Porto Alegre", "RS", "4314902", "51");

            // Salvador - BA
            inserirCep(ps, "40020000", "Largo do Pelourinho", "Pelourinho", "Salvador", "BA", "2927408", "71");

            // Recife - PE
            inserirCep(ps, "50030230", "Avenida Rio Branco", "Recife", "Recife", "PE", "2611606", "81");

            // Fortaleza - CE
            inserirCep(ps, "60025000", "Praça do Ferreira", "Centro", "Fortaleza", "CE", "2304400", "85");

            // Manaus - AM
            inserirCep(ps, "69005070", "Avenida Eduardo Ribeiro", "Centro", "Manaus", "AM", "1302603", "92");

            ps.executeBatch();
        }
    }

    private static void inserirCep(PreparedStatement ps, String cep, String logr, String bairro, String cidade, String uf, String ibge, String ddd) throws SQLException {
        ps.setString(1, cep);
        ps.setString(2, logr);
        ps.setString(3, bairro);
        ps.setString(4, cidade);
        ps.setString(5, uf);
        ps.setString(6, ibge);
        ps.setString(7, ddd);
        ps.addBatch();
    }

    /**
     * Consulta informações de um CEP no banco interno .thzdbi.
     */
    public static Map<String, String> consultarCep(String cep) {
        if (cep == null) return Map.of();
        String limpo = cep.replaceAll("\\D", "");
        if (limpo.length() != 8) return Map.of();

        garantirSchema();
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement("SELECT cep, logradouro, bairro, cidade, uf, ibge, ddd FROM thz_ceps WHERE cep = ?")) {
            ps.setString(1, limpo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> dados = new LinkedHashMap<>();
                    dados.put("cep", formatarCep(rs.getString("cep")));
                    dados.put("logradouro", rs.getString("logradouro"));
                    dados.put("bairro", rs.getString("bairro"));
                    dados.put("cidade", rs.getString("cidade"));
                    dados.put("uf", rs.getString("uf"));
                    dados.put("ibge", rs.getString("ibge"));
                    dados.put("ddd", rs.getString("ddd"));
                    return dados;
                }
            }
        } catch (SQLException ignored) {}

        // Fallback por Faixa Canônica Estadual dos Correios caso não esteja cadastrado na tabela de cidades
        return inferirFaixaCep(limpo);
    }

    /**
     * Cadastra ou atualiza um CEP no banco offline .thzdbi.
     */
    public static boolean cadastrarCep(String cep, String logradouro, String bairro, String cidade, String uf, String ibge, String ddd) {
        if (cep == null) return false;
        String limpo = cep.replaceAll("\\D", "");
        if (limpo.length() != 8) return false;

        garantirSchema();
        String sql = "INSERT OR REPLACE INTO thz_ceps (cep, logradouro, bairro, cidade, uf, ibge, ddd) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = obterConexao(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, limpo);
            ps.setString(2, logradouro != null ? logradouro : "");
            ps.setString(3, bairro != null ? bairro : "");
            ps.setString(4, cidade != null ? cidade : "");
            ps.setString(5, uf != null ? uf.toUpperCase() : "");
            ps.setString(6, ibge != null ? ibge : "");
            ps.setString(7, ddd != null ? ddd : "");
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static String formatarCep(String c) {
        if (c == null || c.length() != 8) return c;
        return c.substring(0, 5) + "-" + c.substring(5);
    }

    private static Map<String, String> inferirFaixaCep(String cep8) {
        try {
            int num = Integer.parseInt(cep8);
            String uf;
            String regiao;
            if (num >= 1000000 && num <= 19999999) { uf = "SP"; regiao = "São Paulo (Estado)"; }
            else if (num >= 20000000 && num <= 28999999) { uf = "RJ"; regiao = "Rio de Janeiro (Estado)"; }
            else if (num >= 29000000 && num <= 29999999) { uf = "ES"; regiao = "Espírito Santo"; }
            else if (num >= 30000000 && num <= 39999999) { uf = "MG"; regiao = "Minas Gerais"; }
            else if (num >= 40000000 && num <= 48999999) { uf = "BA"; regiao = "Bahia"; }
            else if (num >= 49000000 && num <= 49999999) { uf = "SE"; regiao = "Sergipe"; }
            else if (num >= 50000000 && num <= 56999999) { uf = "PE"; regiao = "Pernambuco"; }
            else if (num >= 57000000 && num <= 57999999) { uf = "AL"; regiao = "Alagoas"; }
            else if (num >= 58000000 && num <= 58999999) { uf = "PB"; regiao = "Paraíba"; }
            else if (num >= 59000000 && num <= 59999999) { uf = "RN"; regiao = "Rio Grande do Norte"; }
            else if (num >= 60000000 && num <= 63999999) { uf = "CE"; regiao = "Ceará"; }
            else if (num >= 64000000 && num <= 64999999) { uf = "PI"; regiao = "Piauí"; }
            else if (num >= 65000000 && num <= 65999999) { uf = "MA"; regiao = "Maranhão"; }
            else if (num >= 66000000 && num <= 68899999) { uf = "PA"; regiao = "Pará"; }
            else if (num >= 68900000 && num <= 68999999) { uf = "AP"; regiao = "Amapá"; }
            else if (num >= 69000000 && num <= 69299999) { uf = "AM"; regiao = "Amazonas"; }
            else if (num >= 69300000 && num <= 69389999) { uf = "RR"; regiao = "Roraima"; }
            else if (num >= 69400000 && num <= 69899999) { uf = "AM"; regiao = "Amazonas (Interior)"; }
            else if (num >= 69900000 && num <= 69999999) { uf = "AC"; regiao = "Acre"; }
            else if (num >= 70000000 && num <= 72799999) { uf = "DF"; regiao = "Distrito Federal"; }
            else if (num >= 72800000 && num <= 76799999) { uf = "GO"; regiao = "Goiás"; }
            else if (num >= 76800000 && num <= 76999999) { uf = "RO"; regiao = "Rondônia"; }
            else if (num >= 77000000 && num <= 77999999) { uf = "TO"; regiao = "Tocantins"; }
            else if (num >= 78000000 && num <= 78899999) { uf = "MT"; regiao = "Mato Grosso"; }
            else if (num >= 79000000 && num <= 79999999) { uf = "MS"; regiao = "Mato Grosso do Sul"; }
            else if (num >= 80000000 && num <= 87999999) { uf = "PR"; regiao = "Paraná"; }
            else if (num >= 88000000 && num <= 89999999) { uf = "SC"; regiao = "Santa Catarina"; }
            else if (num >= 90000000 && num <= 99999999) { uf = "RS"; regiao = "Rio Grande do Sul"; }
            else { uf = "BR"; regiao = "Nacional"; }

            Map<String, String> dados = new LinkedHashMap<>();
            dados.put("cep", formatarCep(cep8));
            dados.put("logradouro", "");
            dados.put("bairro", "");
            dados.put("cidade", regiao);
            dados.put("uf", uf);
            dados.put("ibge", "");
            dados.put("ddd", "");
            return dados;
        } catch (Exception e) {
            return Map.of();
        }
    }
}
