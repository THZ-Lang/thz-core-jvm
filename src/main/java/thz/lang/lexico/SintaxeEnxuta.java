package thz.lang.lexico;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dessugariza a forma enxuta baseada em indentação para a gramática estrutural
 * já consumida pelo parser. A transformação mantém uma linha de saída por linha
 * de entrada para preservar os diagnósticos de linha da fonte original.
 */
public final class SintaxeEnxuta {
    private static final Pattern FUNCAO = Pattern.compile("(?i)^(funcao|função)\\s+(.+?)\\s*->\\s*([^:]+):$");
    private static final Pattern OPERACAO = Pattern.compile("(?i)^operacao\\s+(.+?)\\s*->\\s*([^:]+):$");
    private static final Pattern DECLARACAO_TIPADA = Pattern.compile("^([\\p{L}_][\\p{L}\\p{N}_]*)\\s*:\\s*([^:=]+?)\\s*:=\\s*(.+)$");
    private static final Pattern DECLARACAO_INFERIDA = Pattern.compile("^([\\p{L}_][\\p{L}\\p{N}_]*)\\s*:=\\s*(.+)$");
    private static final Pattern ATRIBUICAO = Pattern.compile("^([\\p{L}_][\\p{L}\\p{N}_]*(?:\\.[\\p{L}_][\\p{L}\\p{N}_]*)*)\\s*=\\s*(?!=)(.+)$");

    private record Bloco(int recuo, String tipo, String fechamento) {}

    public record DiagnosticoIndentacao(int linha, int coluna, String mensagem) {}

    private SintaxeEnxuta() {}

    public static boolean detectar(String fonte) {
        if (fonte == null || fonte.isBlank()) return false;
        // Se o código utiliza blocos estruturais com chaves fora de literais de texto,
        // trata-se do paradigma de chaves, dispensando a dessugarização por indentação.
        String semStrings = fonte.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "");
        if (semStrings.contains("{") || semStrings.contains("}")) return false;

        return fonte.lines().anyMatch(linha -> {
            String t = linha.strip();
            if (t.isEmpty()) return false;
            String baixo = t.toLowerCase(Locale.ROOT);
            boolean cabecalhoCanonico = Character.isLowerCase(t.codePointAt(0))
                    && (FUNCAO.matcher(t).matches()
                    || baixo.matches("^(programa|biblioteca|extensao|pipeline_dados|ferramenta|teste|tela|estrutura|regra|operacao|procedimento|se|senao|enquanto|para|metadados)\\b.*:$"));
            return t.contains(":=") || cabecalhoCanonico;
        });
    }

    /**
     * Valida a estrutura visual da forma enxuta antes da dessugarização. Isso
     * evita que um recuo acidental gere uma AST válida, porém diferente da
     * intenção de quem escreveu o código.
     */
    public static List<DiagnosticoIndentacao> validarIndentacao(String fonte) {
        if (fonte == null || fonte.isBlank() || !possuiCabecalhoEnxuto(fonte)) return List.of();

        List<DiagnosticoIndentacao> diagnosticos = new ArrayList<>();
        String[] linhas = fonte.split("\\R", -1);
        int recuoAnterior = -1;
        int linhaAnterior = -1;
        boolean anteriorAbreBloco = false;

        for (int i = 0; i < linhas.length; i++) {
            String linha = linhas[i];
            String conteudo = linha.stripLeading();
            if (conteudo.isBlank() || conteudo.startsWith("#")) continue;

            String prefixo = linha.substring(0, linha.length() - conteudo.length());
            if (prefixo.indexOf('\t') >= 0) {
                diagnosticos.add(new DiagnosticoIndentacao(i + 1, 1,
                        "[Erro Sintático][Linha " + (i + 1) + ":1] A sintaxe enxuta exige espaços; substitua a tabulação por 4 espaços."));
            }

            int recuo = 0;
            for (int p = 0; p < prefixo.length(); p++) recuo += prefixo.charAt(p) == '\t' ? 4 : 1;
            if (recuo % 4 != 0) {
                diagnosticos.add(new DiagnosticoIndentacao(i + 1, 1,
                        "[Erro Sintático][Linha " + (i + 1) + ":1] Recuo inválido: use múltiplos de 4 espaços."));
            }

            if (recuoAnterior < 0 && recuo != 0) {
                diagnosticos.add(new DiagnosticoIndentacao(i + 1, 1,
                        "[Erro Sintático][Linha " + (i + 1) + ":1] A declaração do módulo deve começar na coluna 1."));
            } else if (recuoAnterior >= 0 && anteriorAbreBloco && recuo <= recuoAnterior) {
                diagnosticos.add(new DiagnosticoIndentacao(i + 1, 1,
                        "[Erro Sintático][Linha " + (i + 1) + ":1] O bloco aberto na linha " + linhaAnterior + " precisa de conteúdo indentado."));
            } else if (recuoAnterior >= 0 && anteriorAbreBloco && recuo != recuoAnterior + 4) {
                diagnosticos.add(new DiagnosticoIndentacao(i + 1, 1,
                        "[Erro Sintático][Linha " + (i + 1) + ":1] Use exatamente 4 espaços para entrar no bloco aberto na linha " + linhaAnterior + "."));
            } else if (recuoAnterior >= 0 && !anteriorAbreBloco && recuo > recuoAnterior) {
                diagnosticos.add(new DiagnosticoIndentacao(i + 1, 1,
                        "[Erro Sintático][Linha " + (i + 1) + ":1] Recuo inesperado: a linha anterior não abre um bloco com ':'."));
            }

            recuoAnterior = recuo;
            linhaAnterior = i + 1;
            anteriorAbreBloco = conteudo.stripTrailing().endsWith(":");
        }
        return List.copyOf(diagnosticos);
    }

    private static boolean possuiCabecalhoEnxuto(String fonte) {
        return fonte.lines().anyMatch(linha -> {
            String texto = linha.strip();
            if (texto.isEmpty() || !Character.isLowerCase(texto.codePointAt(0))) return false;
            String baixo = texto.toLowerCase(Locale.ROOT);
            return FUNCAO.matcher(texto).matches()
                    || baixo.matches("^(programa|biblioteca|extensao|pipeline_dados|ferramenta|teste|tela|estrutura|enumeracao|regra|regra_negocio|operacao|procedimento|se|senao|senão|enquanto|para|metadados|metadados_arquitetura|tente|capture|escolha|caso|usar_bloco_memoria|vetorizar_para)\\b.*:$");
        });
    }

    public static String normalizar(String fonte) {
        if (!detectar(fonte)) return fonte;

        String[] linhas = fonte.split("\\R", -1);
        StringBuilder saida = new StringBuilder(fonte.length() + 128);
        Deque<Bloco> blocos = new ArrayDeque<>();

        for (int i = 0; i < linhas.length; i++) {
            String original = linhas[i];
            String texto = original.stripLeading();
            int recuo = original.length() - texto.length();
            String baixo = texto.toLowerCase(Locale.ROOT);
            boolean vazia = texto.isBlank() || texto.startsWith("#");
            boolean senao = baixo.equals("senao:") || baixo.equals("senão:");
            boolean continuacao = senao || baixo.startsWith("capture ");

            StringBuilder prefixo = new StringBuilder();
            if (!vazia) {
                while (!blocos.isEmpty() && recuo <= blocos.peek().recuo()
                        && !(continuacao && ((blocos.peek().tipo().equals("SE") && senao)
                        || (blocos.peek().tipo().equals("TENTE") && baixo.startsWith("capture ")))
                        && recuo == blocos.peek().recuo())) {
                    prefixo.append(blocos.pop().fechamento()).append(' ');
                }
            }

            String convertida = converterLinha(texto, recuo, blocos);
            saida.append(" ".repeat(Math.max(0, recuo))).append(prefixo).append(convertida);
            if (i + 1 < linhas.length) saida.append('\n');
        }

        if (!blocos.isEmpty()) {
            saida.append('\n');
            while (!blocos.isEmpty()) saida.append(blocos.pop().fechamento()).append(' ');
        }
        return saida.toString();
    }

    private static String converterLinha(String texto, int recuo, Deque<Bloco> blocos) {
        if (texto.isBlank() || texto.startsWith("#")) return texto;
        String baixo = texto.toLowerCase(Locale.ROOT);

        Matcher funcao = FUNCAO.matcher(texto);
        if (funcao.matches()) {
            blocos.push(new Bloco(recuo, "FUNCAO", "FIM_FUNCAO"));
            return "FUNCAO " + funcao.group(2) + ": " + tipoCanonico(funcao.group(3));
        }
        Matcher operacao = OPERACAO.matcher(texto);
        if (operacao.matches()) {
            blocos.push(new Bloco(recuo, "OPERACAO", "FIM"));
            return "OPERACAO " + operacao.group(1) + ": " + tipoCanonico(operacao.group(2)) + " INICIO";
        }

        if (baixo.startsWith("programa ") && texto.endsWith(":")) return abrir(texto, recuo, blocos, "programa", "PROGRAMA", "FIM_PROGRAMA");
        if (baixo.startsWith("biblioteca ") && texto.endsWith(":")) return abrir(texto, recuo, blocos, "biblioteca", "BIBLIOTECA", "FIM_BIBLIOTECA");
        if (baixo.startsWith("extensao ") && texto.endsWith(":")) return abrir(texto, recuo, blocos, "extensao", "EXTENSAO", "FIM_EXTENSAO");
        if (baixo.startsWith("pipeline_dados ") && texto.endsWith(":")) return abrir(texto, recuo, blocos, "pipeline_dados", "PIPELINE_DADOS", "FIM_PIPELINE");
        if (baixo.startsWith("ferramenta ") && texto.endsWith(":")) return abrir(texto, recuo, blocos, "ferramenta", "FERRAMENTA", "FIM_FERRAMENTA");
        if (baixo.startsWith("teste ") && texto.endsWith(":")) return abrir(texto, recuo, blocos, "teste", "TESTE", "FIM_TESTE");
        if (baixo.startsWith("tela ") && texto.endsWith(":")) return abrir(texto, recuo, blocos, "tela", "TELA", "FIM_TELA");
        if (baixo.equals("metadados:") || baixo.equals("metadados_arquitetura:")) {
            blocos.push(new Bloco(recuo, "METADADOS", "FIM_METADADOS"));
            return "METADADOS_ARQUITETURA";
        }
        if (baixo.startsWith("estrutura ") && texto.endsWith(":")) return abrir(texto, recuo, blocos, "estrutura", "ESTRUTURA", "FIM_ESTRUTURA");
        if (baixo.startsWith("enumeracao ") && texto.endsWith(":")) return abrir(texto, recuo, blocos, "enumeracao", "ENUMERACAO", "FIM_ENUMERACAO");
        if ((baixo.startsWith("regra ") || baixo.startsWith("regra_negocio ")) && texto.endsWith(":")) {
            String palavra = baixo.startsWith("regra_negocio ") ? "regra_negocio" : "regra";
            return abrir(texto, recuo, blocos, palavra, "REGRA_NEGOCIO", "FIM_REGRA_NEGOCIO");
        }
        if (baixo.startsWith("procedimento ") && texto.endsWith(":")) {
            blocos.push(new Bloco(recuo, "PROCEDIMENTO", "FIM"));
            return "PROCEDIMENTO " + semCabecalho(texto, "procedimento") + " INICIO";
        }
        if (baixo.startsWith("se ") && texto.endsWith(":")) {
            blocos.push(new Bloco(recuo, "SE", "FIM_SE"));
            return "SE " + semCabecalho(texto, "se");
        }
        if (baixo.equals("senao:") || baixo.equals("senão:")) return "SENAO";
        if (baixo.startsWith("enquanto ") && texto.endsWith(":")) {
            blocos.push(new Bloco(recuo, "ENQUANTO", "FIM_ENQUANTO"));
            return "ENQUANTO " + semCabecalho(texto, "enquanto");
        }
        if (baixo.startsWith("para ") && texto.endsWith(":")) {
            blocos.push(new Bloco(recuo, "PARA", "FIM_PARA"));
            return "PARA " + semCabecalho(texto, "para").replaceAll("(?i)\\bde\\b", "DE").replaceAll("(?i)\\bate\\b", "ATE");
        }
        if (baixo.equals("tente:")) {
            blocos.push(new Bloco(recuo, "TENTE", "FIM_TENTE"));
            return "TENTE";
        }
        if (baixo.startsWith("capture ") && texto.endsWith(":")) return "CAPTURE " + semCabecalho(texto, "capture");
        if (baixo.startsWith("escolha ") && texto.endsWith(":")) {
            blocos.push(new Bloco(recuo, "ESCOLHA", "FIM_ESCOLHA"));
            return "ESCOLHA " + semCabecalho(texto, "escolha");
        }
        if (baixo.startsWith("caso ") && texto.endsWith(":")) return "CASO " + semCabecalho(texto, "caso") + " ->";
        if (baixo.startsWith("usar_bloco_memoria ") && texto.endsWith(":")) {
            blocos.push(new Bloco(recuo, "MEMORIA", "FIM_BLOCO_MEMORIA"));
            return "USAR_BLOCO_MEMORIA " + semCabecalho(texto, "usar_bloco_memoria");
        }
        if (baixo.startsWith("vetorizar_para ") && texto.endsWith(":")) {
            blocos.push(new Bloco(recuo, "VETORIZAR", "FIM_VETORIZAR"));
            return "VETORIZAR_PARA " + semCabecalho(texto, "vetorizar_para")
                    .replaceAll("(?i)\\bem\\b", "EM").replaceAll("(?i)\\bpasso_simd\\b", "PASSO_SIMD");
        }

        Matcher tipada = DECLARACAO_TIPADA.matcher(texto);
        if (tipada.matches()) return tipada.group(1) + ": " + tipoCanonico(tipada.group(2)) + " := " + tipada.group(3);
        Matcher inferida = DECLARACAO_INFERIDA.matcher(texto);
        if (inferida.matches()) return inferida.group(1) + " := " + inferida.group(2);
        Matcher atribuicao = ATRIBUICAO.matcher(texto);
        if (atribuicao.matches()) return atribuicao.group(1) + " <- " + atribuicao.group(2).stripLeading();

        if (baixo.startsWith("retorne ")) return "RETORNE " + texto.substring(texto.indexOf(' ') + 1);
        if (baixo.equals("retorne")) return "RETORNE";
        if (baixo.startsWith("exiba ")) return "EXIBA " + texto.substring(texto.indexOf(' ') + 1);
        if (baixo.startsWith("exige ")) return "EXIGE " + texto.substring(texto.indexOf(' ') + 1);
        if (baixo.startsWith("garante ")) return "GARANTE " + texto.substring(texto.indexOf(' ') + 1);
        if (baixo.startsWith("invariante ")) return "INVARIANTE " + texto.substring(texto.indexOf(' ') + 1);
        return texto;
    }

    private static String abrir(String texto, int recuo, Deque<Bloco> blocos, String palavra, String canonica, String fechamento) {
        blocos.push(new Bloco(recuo, canonica, fechamento));
        return canonica + " " + semCabecalho(texto, palavra);
    }

    private static String semCabecalho(String texto, String palavra) {
        return texto.substring(palavra.length()).strip().replaceFirst(":$", "").strip();
    }

    private static String tipoCanonico(String tipo) {
        String t = tipo.strip();
        return t.replaceAll("(?i)\\btexto\\b", "TEXTO")
                .replaceAll("(?i)\\binteiro\\b", "INTEIRO")
                .replaceAll("(?i)\\binteiro32\\b", "INTEIRO32")
                .replaceAll("(?i)\\blogico\\b", "LOGICO")
                .replaceAll("(?i)\\bdecimal\\b", "DECIMAL")
                .replaceAll("(?i)\\bmonetario\\b", "MONETARIO")
                .replaceAll("(?i)\\bfatia\\b", "FATIA")
                .replaceAll("(?i)\\bresultado\\b", "RESULTADO");
    }
}
