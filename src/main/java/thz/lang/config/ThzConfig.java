package thz.lang.config;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ThzConfig — Gerenciador de configurações com suporte a .env, variáveis de
 * ambiente e properties.
 */
public final class ThzConfig {

    private static final Map<String, String> CONFIG_CACHE = new ConcurrentHashMap<>();

    static {
        // Carrega variáveis de ambiente padrão do SO
        System.getenv().forEach(CONFIG_CACHE::put);
        carregarEnvPadrao();
    }

    private ThzConfig() {
    }

    public static void carregarEnvPadrao() {
        carregarArquivoEnv(".env");
    }

    /**
     * Carrega um arquivo .env e adiciona suas variáveis ao cache de configuração.
     * 
     * @param caminho
     */
    public static void carregarArquivoEnv(String caminho) {
        Path p = Path.of(caminho);
        if (Files.exists(p) && !Files.isDirectory(p)) {
            try {
                for (String linha : Files.readAllLines(p)) {
                    String trim = linha.trim();
                    if (trim.isEmpty() || trim.startsWith("#"))
                        continue;
                    int eq = trim.indexOf('=');
                    if (eq > 0) {
                        String chave = trim.substring(0, eq).trim();
                        String valor = trim.substring(eq + 1).trim();
                        if ((valor.startsWith("\"") && valor.endsWith("\""))
                                || (valor.startsWith("'") && valor.endsWith("'"))) {
                            valor = valor.substring(1, valor.length() - 1);
                        }
                        CONFIG_CACHE.put(chave, valor);
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Carrega um conteúdo de properties e adiciona suas variáveis ao cache de
     * configuração.
     * 
     * @param conteudo
     */
    public static void carregarProperties(String conteudo) {
        Properties prop = new Properties();
        try {
            prop.load(new StringReader(conteudo));
            for (String key : prop.stringPropertyNames()) {
                CONFIG_CACHE.put(key, prop.getProperty(key));
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Define o valor de uma variável de configuração.
     * 
     * @param chave
     * @param valor
     */
    public static void definir(String chave, String valor) {
        if (chave != null && valor != null) {
            CONFIG_CACHE.put(chave, valor);
        }
    }

    /**
     * Obtém o valor de uma variável de configuração, retornando um valor padrão
     * caso não esteja definida.
     * 
     * @param chave
     * @param padrao
     * @return
     */
    public static String obter(String chave, String padrao) {
        String val = CONFIG_CACHE.get(chave);
        if (val != null)
            return val;
        val = System.getProperty(chave);
        return val != null ? val : padrao;
    }

    public static String obter(String chave) {
        return obter(chave, "");
    }

    /**
     * Obtém o valor de uma variável de configuração como inteiro, retornando um
     * valor padrão caso não esteja definida ou não seja um número válido.
     * 
     * @param chave
     * @param padrao
     * @return
     */
    public static long obterInteiro(String chave, long padrao) {
        String val = obter(chave, null);
        if (val == null || val.isBlank())
            return padrao;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    /**
     * Obtém o valor de uma variável de configuração como booleano, retornando um
     * valor padrão caso não esteja definida ou não seja um valor lógico válido.
     * 
     * @param chave
     * @param padrao
     * @return
     */
    public static boolean obterLogico(String chave, boolean padrao) {
        String val = obter(chave, null);
        if (val == null || val.isBlank())
            return padrao;
        String limpo = val.trim().toLowerCase();
        return limpo.equals("true") || limpo.equals("verdadeiro") || limpo.equals("1") || limpo.equals("sim");
    }

    /**
     * Obtém todas as variáveis de configuração carregadas, retornando um mapa
     * imutável.
     * 
     * @return
     */
    public static Map<String, String> obterTodos() {
        return Collections.unmodifiableMap(new HashMap<>(CONFIG_CACHE));
    }
}
