package thz.lang.lexico;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dessugariza a forma enxuta baseada em indentação e a sintaxe moderna (.NET 10 / Kotlin / Rust / TS)
 * para a gramática estrutural da AST da THZ-LANG.
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
        String semStrings = fonte.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "");
        if (semStrings.contains("{") || semStrings.contains("}")) return false;

        return fonte.lines().anyMatch(linha -> {
            String t = linha.strip();
            if (t.isEmpty()) return false;
            String baixo = t.toLowerCase(Locale.ROOT);
            boolean cabecalhoCanonico = Character.isLowerCase(t.codePointAt(0))
                    && (FUNCAO.matcher(t).matches()
                    || baixo.matches("^(programa|biblioteca|extensao|pipeline_dados|ferramenta|teste|tela|estrutura|regra|operacao|procedimento|se|senao|enquanto|para|metadados)\\b.*:$"));
            return t.contains(":=") || cabecalhoCanonico
                    || baixo.startsWith("val ") || baixo.startsWith("var ")
                    || baixo.startsWith("let ") || baixo.startsWith("record ")
                    || baixo.startsWith("print(") || baixo.startsWith("println(")
                    || (!possuiCabecalhoModulo(fonte) && !baixo.startsWith("#"));
        });
    }

    public static boolean possuiCabecalhoModulo(String fonte) {
        if (fonte == null || fonte.isBlank()) return false;
        return fonte.lines().anyMatch(l -> {
            String s = l.trim().toLowerCase(Locale.ROOT);
            return s.startsWith("programa ") || s.startsWith("program ")
                    || s.startsWith("biblioteca ") || s.startsWith("library ")
                    || s.startsWith("extensao ") || s.startsWith("extension ")
                    || s.startsWith("teste ") || s.startsWith("test ")
                    || s.startsWith("tela ") || s.startsWith("screen ")
                    || s.startsWith("ferramenta ") || s.startsWith("tool ")
                    || s.startsWith("pipeline_dados ");
        });
    }

    /**
     * Valida a estrutura visual da forma enxuta antes da dessugarização.
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

    public static String limparBom(String fonte) {
        if (fonte == null) return "";
        if (fonte.startsWith("\uFEFF")) {
            return fonte.substring(1);
        }
        return fonte;
    }

    public static String normalizarTopLevel(String fonte) {
        String limpa = limparBom(fonte);
        String normalizada = normalizar(limpa);
        if (!possuiCabecalhoModulo(normalizada)) {
            return encapsularTopLevel(normalizada);
        }
        return normalizada;
    }

    public static String normalizar(String fonteBruta) {
        String fonte = limparBom(fonteBruta);
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

    private static String encapsularTopLevel(String codigoConvertido) {
        List<String> definicoesModulo = new ArrayList<>();
        List<String> instrucoesPrincipal = new ArrayList<>();

        String[] linhas = codigoConvertido.split("\\R");
        boolean dentroDefinicao = false;

        for (String l : linhas) {
            String trimmed = l.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.startsWith("IMPORTAR ") || trimmed.startsWith("IMPORTAR_COPYBOOK ")) {
                definicoesModulo.add(l);
                continue;
            }
            if (trimmed.startsWith("ESTRUTURA ") || trimmed.startsWith("ENUMERACAO ") || trimmed.startsWith("FUNCAO ") || trimmed.startsWith("REGRA_NEGOCIO ")) {
                dentroDefinicao = true;
                definicoesModulo.add(l);
                if (trimmed.endsWith("FIM_ESTRUTURA") || trimmed.endsWith("FIM_ENUMERACAO") || trimmed.endsWith("FIM_FUNCAO") || trimmed.endsWith("FIM_REGRA_NEGOCIO")) {
                    dentroDefinicao = false;
                }
                continue;
            }
            if (dentroDefinicao) {
                definicoesModulo.add(l);
                if (trimmed.equals("FIM_ESTRUTURA") || trimmed.equals("FIM_ENUMERACAO") || trimmed.equals("FIM_FUNCAO") || trimmed.equals("FIM_REGRA_NEGOCIO") || trimmed.equals("FIM")) {
                    dentroDefinicao = false;
                }
                continue;
            }
            instrucoesPrincipal.add("    " + trimmed);
        }

        StringBuilder resultado = new StringBuilder("PROGRAMA ScriptAutoExec\n");
        for (String def : definicoesModulo) {
            resultado.append(def).append('\n');
        }
        if (!instrucoesPrincipal.isEmpty()) {
            resultado.append("PROCEDIMENTO Principal()\nINICIO\n");
            for (String inst : instrucoesPrincipal) {
                resultado.append(inst).append('\n');
            }
            resultado.append("FIM\n");
        }
        resultado.append("FIM_PROGRAMA\n");
        return resultado.toString();
    }

    private static String converterLinha(String textoOriginal, int recuo, Deque<Bloco> blocos) {
        if (textoOriginal.isBlank() || textoOriginal.startsWith("#")) return textoOriginal;

        // 1. Interpolação de strings: f"Olá {nome}" ou $"Olá {nome}"
        String texto = dessugarizarInterpolacao(textoOriginal);
        String baixo = texto.toLowerCase(Locale.ROOT);

        // 2. Compact records: record Nome(campo1: Tipo, campo2: Tipo)
        Matcher recordM = Pattern.compile("^(?i)(?:data\\s+)?record\\s+([\\p{L}_][\\p{L}\\p{N}_]*)\\s*\\((.+)\\)\\s*;?$").matcher(texto);
        if (recordM.matches()) {
            String nomeRecord = recordM.group(1);
            String camposRaw = recordM.group(2);
            StringBuilder sbRec = new StringBuilder("ESTRUTURA ").append(nomeRecord).append('\n');
            String[] campos = camposRaw.split(",");
            for (String c : campos) {
                String trimmed = c.trim();
                if (trimmed.contains(":")) {
                    String[] partes = trimmed.split(":", 2);
                    sbRec.append("    ").append(partes[0].trim()).append(" : ").append(tipoCanonico(partes[1].trim())).append('\n');
                } else {
                    sbRec.append("    ").append(trimmed).append(" : QUALQUER\n");
                }
            }
            sbRec.append("FIM_ESTRUTURA");
            return sbRec.toString();
        }

        // 3. val / var / let com tipagem explícita: val x: Int = 10 -> x: Int := 10
        Matcher valTipado = Pattern.compile("^(?:val|var|let(?:\\s+mut)?)\\s+([\\p{L}_][\\p{L}\\p{N}_]*)\\s*:\\s*([^=]+?)\\s*=\\s*(.+)$").matcher(texto);
        if (valTipado.matches()) {
            return valTipado.group(1) + ": " + tipoCanonico(valTipado.group(2)) + " := " + valTipado.group(3);
        }

        // 4. val / var / let inferido: val x = 10 -> x := 10
        Matcher valInferido = Pattern.compile("^(?:val|var|let(?:\\s+mut)?)\\s+([\\p{L}_][\\p{L}\\p{N}_]*)\\s*=\\s*(.+)$").matcher(texto);
        if (valInferido.matches()) {
            return valInferido.group(1) + " := " + valInferido.group(2);
        }

        // 5. print / println
        Matcher printCall = Pattern.compile("^(?i)print(?:ln)?\\s*\\((.*)\\)\\s*;?$").matcher(texto);
        if (printCall.matches()) {
            return "EXIBA " + printCall.group(1);
        }
        Matcher printStmt = Pattern.compile("^(?i)print(?:ln)?\\s+(.+)$").matcher(texto);
        if (printStmt.matches()) {
            return "EXIBA " + printStmt.group(1);
        }

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

    public static String dessugarizarInterpolacao(String texto) {
        if (texto == null || (!texto.contains("{") && !texto.startsWith("f\"") && !texto.startsWith("$\""))) return texto;
        Pattern p = Pattern.compile("(?:[fF\\$])?\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
        Matcher m = p.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String conteudo = m.group(1);
            if (conteudo.contains("{") && conteudo.contains("}")) {
                String exprSubstituida = expandirChavesInterpolacao(conteudo);
                m.appendReplacement(sb, Matcher.quoteReplacement(exprSubstituida));
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement("\"" + conteudo + "\""));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String expandirChavesInterpolacao(String conteudo) {
        Pattern pChaves = Pattern.compile("\\{([^}]+)\\}");
        Matcher m = pChaves.matcher(conteudo);
        List<String> partes = new ArrayList<>();
        int lastEnd = 0;
        while (m.find()) {
            if (m.start() > lastEnd) {
                String textoFixo = conteudo.substring(lastEnd, m.start());
                partes.add("\"" + textoFixo + "\"");
            }
            String expr = m.group(1).trim();
            partes.add(expr);
            lastEnd = m.end();
        }
        if (lastEnd < conteudo.length()) {
            String textoFixo = conteudo.substring(lastEnd);
            partes.add("\"" + textoFixo + "\"");
        }
        return String.join(" + ", partes);
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
                .replaceAll("(?i)\\bstring\\b", "TEXTO")
                .replaceAll("(?i)\\binteiro\\b", "INTEIRO")
                .replaceAll("(?i)\\bint\\b", "INTEIRO")
                .replaceAll("(?i)\\binteiro32\\b", "INTEIRO32")
                .replaceAll("(?i)\\blogico\\b", "LOGICO")
                .replaceAll("(?i)\\bbool\\b", "LOGICO")
                .replaceAll("(?i)\\bboolean\\b", "LOGICO")
                .replaceAll("(?i)\\bdecimal\\b", "DECIMAL")
                .replaceAll("(?i)\\bmonetario\\b", "MONETARIO")
                .replaceAll("(?i)\\bfatia\\b", "FATIA")
                .replaceAll("(?i)\\bresultado\\b", "RESULTADO");
    }
}
