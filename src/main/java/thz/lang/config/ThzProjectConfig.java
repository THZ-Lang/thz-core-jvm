package thz.lang.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ThzProjectConfig — Gerenciador do manifesto padrão do projeto (thz.config.json ou thz.json).
 * Centraliza metadados do projeto, drivers de banco de dados, mensageria, IA e governança.
 */
public final class ThzProjectConfig {

    private static final String ARQUIVO_CONFIG_PADRAO = "thz.config.json";
    private static final String ARQUIVO_CONFIG_ALT = "thz.json";

    private static volatile ProjetoConfig INSTANCIA = null;

    public record MetaProjeto(String nome, String versao, String autor, String dialeto, String descricao) {}
    public record BancoConfig(String driver, String url, String usuario, String senha, int poolMin, int poolMax, boolean autoMigracao, String vetorial) {}
    public record MensageriaConfig(String driver, String url, String host, int porta, String topicoPadrao, boolean autoCriarFilas) {}
    public record IaConfig(String motorEmbeddings, int dimensaoVetor, String armazenamentoVetorial) {}
    public record GovernancaConfig(boolean modoEstrito, String sloLatencia, List<String> conformidade) {}

    public record ProjetoConfig(
            MetaProjeto projeto,
            BancoConfig banco,
            MensageriaConfig mensageria,
            IaConfig ia,
            GovernancaConfig governanca,
            Path arquivoOrigem
    ) {}

    private ThzProjectConfig() {}

    public static ProjetoConfig obterConfig() {
        if (INSTANCIA == null) {
            synchronized (ThzProjectConfig.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = carregarDoDiretorio(Path.of("."));
                }
            }
        }
        return INSTANCIA;
    }

    public static synchronized void recarregar(Path diretorio) {
        INSTANCIA = carregarDoDiretorio(diretorio);
    }

    public static ProjetoConfig carregarDoDiretorio(Path diretorio) {
        var opt = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(ARQUIVO_CONFIG_PADRAO, diretorio, List.of(".json"));
        if (opt.isEmpty()) {
            opt = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(ARQUIVO_CONFIG_ALT, diretorio, List.of(".json"));
        }

        if (opt.isPresent()) {
            Path p = opt.get();
            try {
                String json = Files.readString(p, StandardCharsets.UTF_8);
                ProjetoConfig cfg = parsearJson(json, p);
                aplicarConfiguracoesNoAmbiente(cfg);
                return cfg;
            } catch (IOException e) {
                System.err.println("[AVISO] Falha ao ler " + p + ": " + e.getMessage() + ". Utilizando configurações padrão.");
            }
        }

        ProjetoConfig padrao = criarPadrao(null);
        aplicarConfiguracoesNoAmbiente(padrao);
        return padrao;
    }

    public static ProjetoConfig parsearJson(String json, Path arquivoOrigem) {
        // Parser JSON leve e robusto sem dependências externas
        String nome = extrairString(json, "nome", "ProjetoTHZ");
        String versao = extrairString(json, "versao", "1.0.0");
        String autor = extrairString(json, "autor", "Autor THZ");
        String dialeto = extrairString(json, "dialeto", "pt-BR");
        String descricao = extrairString(json, "descricao", "");

        MetaProjeto meta = new MetaProjeto(nome, versao, autor, dialeto, descricao);

        // Bloco Banco
        String bancoDriver = extrairBlocoString(json, "banco", "driver", "auto");
        String bancoUrl = extrairBlocoString(json, "banco", "url", "jdbc:sqlite:dados/app.db");
        String bancoUsuario = extrairBlocoString(json, "banco", "usuario", "");
        String bancoSenha = extrairBlocoString(json, "banco", "senha", "");
        int poolMin = extrairBlocoInt(json, "banco", "poolMin", 2);
        int poolMax = extrairBlocoInt(json, "banco", "poolMax", 10);
        boolean autoMigracao = extrairBlocoBool(json, "banco", "autoMigracao", true);
        String bancoVetorial = extrairBlocoString(json, "banco", "vetorial", "embutido");

        BancoConfig banco = new BancoConfig(bancoDriver, bancoUrl, bancoUsuario, bancoSenha, poolMin, poolMax, autoMigracao, bancoVetorial);

        // Bloco Mensageria
        String msgDriver = extrairBlocoString(json, "mensageria", "driver", "auto");
        String msgUrl = extrairBlocoString(json, "mensageria", "url", "auto");
        String msgHost = extrairBlocoString(json, "mensageria", "host", "localhost");
        int msgPorta = extrairBlocoInt(json, "mensageria", "porta", 5672);
        String topicoPadrao = extrairBlocoString(json, "mensageria", "topicoPadrao", "eventos.sistema");
        boolean autoCriarFilas = extrairBlocoBool(json, "mensageria", "autoCriarFilas", true);

        MensageriaConfig msg = new MensageriaConfig(msgDriver, msgUrl, msgHost, msgPorta, topicoPadrao, autoCriarFilas);

        // Bloco IA
        String motorEmbeddings = extrairBlocoString(json, "ia", "motorEmbeddings", "local-fnv1a");
        int dimensaoVetor = extrairBlocoInt(json, "ia", "dimensaoVetor", 128);
        String armazenamentoVetorial = extrairBlocoString(json, "ia", "armazenamentoVetorial", "sqlite-vec");

        IaConfig ia = new IaConfig(motorEmbeddings, dimensaoVetor, armazenamentoVetorial);

        // Bloco Governança
        boolean modoEstrito = extrairBlocoBool(json, "governanca", "modoEstrito", false);
        String sloLatencia = extrairBlocoString(json, "governanca", "sloLatencia", "15ms");
        List<String> conformidades = extrairListaString(json, "conformidade", List.of("ISO-IEC-10967"));

        GovernancaConfig gov = new GovernancaConfig(modoEstrito, sloLatencia, conformidades);

        return new ProjetoConfig(meta, banco, msg, ia, gov, arquivoOrigem);
    }

    public static ProjetoConfig criarPadrao(Path destino) {
        MetaProjeto meta = new MetaProjeto("AppCorporativoThz", "1.0.0", "Engenharia de Software", "pt-BR", "Sistema Corporativo THZ-LANG");
        BancoConfig banco = new BancoConfig("auto", "jdbc:sqlite:dados/app.db", "", "", 2, 10, true, "embutido");
        MensageriaConfig msg = new MensageriaConfig("auto", "auto", "localhost", 5672, "eventos.sistema", true);
        IaConfig ia = new IaConfig("local-fnv1a", 128, "embutido");
        GovernancaConfig gov = new GovernancaConfig(false, "15ms", List.of("ISO-IEC-10967", "LGPD-Art7"));
        return new ProjetoConfig(meta, banco, msg, ia, gov, destino);
    }

    public static String gerarJsonModelo(ProjetoConfig cfg) {
        return """
        {
          "projeto": {
            "nome": "%s",
            "versao": "%s",
            "autor": "%s",
            "dialeto": "%s",
            "descricao": "%s"
          },
          "banco": {
            "driver": "%s",
            "url": "%s",
            "usuario": "%s",
            "senha": "%s",
            "poolMin": %d,
            "poolMax": %d,
            "autoMigracao": %s,
            "vetorial": "%s"
          },
          "mensageria": {
            "driver": "%s",
            "url": "%s",
            "host": "%s",
            "porta": %d,
            "topicoPadrao": "%s",
            "autoCriarFilas": %s
          },
          "ia": {
            "motorEmbeddings": "%s",
            "dimensaoVetor": %d,
            "armazenamentoVetorial": "%s"
          },
          "governanca": {
            "modoEstrito": %s,
            "sloLatencia": "%s",
            "conformidade": ["ISO-IEC-10967", "LGPD-Art7"]
          }
        }
        """.formatted(
                cfg.projeto.nome, cfg.projeto.versao, cfg.projeto.autor, cfg.projeto.dialeto, cfg.projeto.descricao,
                cfg.banco.driver, cfg.banco.url, cfg.banco.usuario, cfg.banco.senha, cfg.banco.poolMin, cfg.banco.poolMax, cfg.banco.autoMigracao, cfg.banco.vetorial,
                cfg.mensageria.driver, cfg.mensageria.url, cfg.mensageria.host, cfg.mensageria.porta, cfg.mensageria.topicoPadrao, cfg.mensageria.autoCriarFilas,
                cfg.ia.motorEmbeddings, cfg.ia.dimensaoVetor, cfg.ia.armazenamentoVetorial,
                cfg.governanca.modoEstrito, cfg.governanca.sloLatencia
        );
    }

    private static void aplicarConfiguracoesNoAmbiente(ProjetoConfig cfg) {
        ThzConfig.definir("projeto.nome", cfg.projeto.nome);
        ThzConfig.definir("projeto.versao", cfg.projeto.versao);
        ThzConfig.definir("projeto.autor", cfg.projeto.autor);
        ThzConfig.definir("projeto.dialeto", cfg.projeto.dialeto);
        ThzConfig.definir("banco.driver", cfg.banco.driver);
        ThzConfig.definir("banco.url", cfg.banco.url);
        ThzConfig.definir("mensageria.driver", cfg.mensageria.driver);
        ThzConfig.definir("mensageria.url", cfg.mensageria.url);
    }

    // --- Auxiliares de Parsing de JSON Leve ---
    private static String extrairString(String json, String chave, String padrao) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(chave) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : padrao;
    }

    private static String extrairBlocoString(String json, String bloco, String chave, String padrao) {
        Pattern blocoPat = Pattern.compile("\"" + Pattern.quote(bloco) + "\"\\s*:\\s*\\{([^\\}]*)\\}");
        Matcher bm = blocoPat.matcher(json);
        if (bm.find()) {
            return extrairString(bm.group(1), chave, padrao);
        }
        return extrairString(json, chave, padrao);
    }

    private static int extrairBlocoInt(String json, String bloco, String chave, int padrao) {
        Pattern blocoPat = Pattern.compile("\"" + Pattern.quote(bloco) + "\"\\s*:\\s*\\{([^\\}]*)\\}");
        Matcher bm = blocoPat.matcher(json);
        String conteudo = bm.find() ? bm.group(1) : json;
        Pattern p = Pattern.compile("\"" + Pattern.quote(chave) + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(conteudo);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return padrao;
    }

    private static boolean extrairBlocoBool(String json, String bloco, String chave, boolean padrao) {
        Pattern blocoPat = Pattern.compile("\"" + Pattern.quote(bloco) + "\"\\s*:\\s*\\{([^\\}]*)\\}");
        Matcher bm = blocoPat.matcher(json);
        String conteudo = bm.find() ? bm.group(1) : json;
        Pattern p = Pattern.compile("\"" + Pattern.quote(chave) + "\"\\s*:\\s*(true|false)");
        Matcher m = p.matcher(conteudo);
        if (m.find()) {
            return Boolean.parseBoolean(m.group(1));
        }
        return padrao;
    }

    private static List<String> extrairListaString(String json, String chave, List<String> padrao) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(chave) + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher m = p.matcher(json);
        if (m.find()) {
            String itensStr = m.group(1);
            List<String> itens = new ArrayList<>();
            Matcher im = Pattern.compile("\"([^\"]*)\"").matcher(itensStr);
            while (im.find()) {
                itens.add(im.group(1));
            }
            if (!itens.isEmpty()) return itens;
        }
        return padrao;
    }
}
