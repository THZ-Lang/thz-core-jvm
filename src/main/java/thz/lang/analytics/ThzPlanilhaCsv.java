package thz.lang.analytics;

import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DecimalFixo;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * ThzPlanilhaCsv — Motor de Manipulação Tabular, Interoperabilidade CSV/Excel e PROCV (VLOOKUP).
 * Suporta leitura/escrita de CSV com tratamento de aspas, busca vertical estilo PROCV/INDEX-MATCH
 * e geração de tabelas dinâmicas pivotadas (Pivot Table).
 */
public final class ThzPlanilhaCsv {

    private ThzPlanilhaCsv() {}

    /**
     * Lê um arquivo CSV/TSV e retorna uma lista de registros tipados (FATIA[REGISTRO]).
     */
    public static List<ValorThz.Registro> lerCsv(Path caminho, String separador) throws IOException {
        if (!Files.exists(caminho)) {
            var opt = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(caminho.toString(), Path.of("."), List.of(".csv", ".tsv", ".txt"));
            if (opt.isPresent()) caminho = opt.get();
        }

        List<ValorThz.Registro> linhas = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(caminho, StandardCharsets.UTF_8)) {
            String cabecalhoLinha = br.readLine();
            if (cabecalhoLinha == null || cabecalhoLinha.isBlank()) return List.of();

            // Detecta separador automaticamente se for 'auto'
            char sep = (separador == null || "auto".equalsIgnoreCase(separador) || separador.isEmpty())
                    ? detectarSeparador(cabecalhoLinha)
                    : separador.charAt(0);

            List<String> cabecalhos = parsearLinhaCsv(cabecalhoLinha, sep);

            String linhaStr;
            while ((linhaStr = br.readLine()) != null) {
                if (linhaStr.isBlank()) continue;
                List<String> valores = parsearLinhaCsv(linhaStr, sep);
                Map<String, ValorThz> campos = new LinkedHashMap<>();
                for (int i = 0; i < cabecalhos.size(); i++) {
                    String col = cabecalhos.get(i).trim();
                    String val = (i < valores.size()) ? valores.get(i).trim() : "";
                    campos.put(col, inferirValorThz(val));
                }
                linhas.add(new ValorThz.Registro("LinhaPlanilha", campos));
            }
        }
        return linhas;
    }

    /**
     * Grava uma lista de registros em arquivo CSV com formatação compatível com Excel (UTF-8).
     */
    public static boolean escreverCsv(Path destino, List<ValorThz.Registro> linhas, String separador) throws IOException {
        if (linhas == null || linhas.isEmpty()) return false;
        if (destino.getParent() != null) Files.createDirectories(destino.getParent());

        char sep = (separador != null && !separador.isEmpty()) ? separador.charAt(0) : ';';

        // Coleta todas as colunas do primeiro registro
        List<String> colunas = new ArrayList<>(linhas.get(0).campos().keySet());

        StringBuilder sb = new StringBuilder();
        // Cabeçalho
        for (int i = 0; i < colunas.size(); i++) {
            sb.append(escaparCsv(colunas.get(i), sep));
            if (i < colunas.size() - 1) sb.append(sep);
        }
        sb.append("\n");

        // Linhas de dados
        for (ValorThz.Registro reg : linhas) {
            for (int i = 0; i < colunas.size(); i++) {
                ValorThz v = reg.campos().get(colunas.get(i));
                String valStr = v != null ? v.formatar() : "";
                sb.append(escaparCsv(valStr, sep));
                if (i < colunas.size() - 1) sb.append(sep);
            }
            sb.append("\n");
        }

        Files.writeString(destino, sb.toString(), StandardCharsets.UTF_8);
        return true;
    }

    /**
     * PROCV / VLOOKUP: Localiza a primeira ocorrência de 'valorBusca' no 'campoBusca'
     * e retorna o conteúdo do 'campoRetorno'.
     */
    public static ValorThz procv(List<ValorThz.Registro> tabela, String campoBusca, String valorBusca, String campoRetorno) {
        if (tabela == null || tabela.isEmpty() || campoBusca == null || campoRetorno == null) {
            return ValorThz.NULO;
        }

        for (ValorThz.Registro reg : tabela) {
            ValorThz v = reg.campos().get(campoBusca);
            if (v != null && v.formatar().equalsIgnoreCase(valorBusca)) {
                ValorThz ret = reg.campos().get(campoRetorno);
                return ret != null ? ret : ValorThz.NULO;
            }
        }
        return ValorThz.NULO;
    }

    /**
     * Tabela Dinâmica / Pivot Table: Agrupa linhas por 'campoLinha' e 'campoColuna', agregando 'campoValor' com SUM/AVG/COUNT/MAX/MIN.
     */
    public static List<ValorThz.Registro> pivotar(List<ValorThz.Registro> tabela, String campoLinha, String campoColuna, String campoValor, String operacao) {
        if (tabela == null || tabela.isEmpty()) return List.of();

        String op = operacao != null ? operacao.toUpperCase() : "SUM";
        Set<String> colunasDinamicas = new TreeSet<>();
        Map<String, Map<String, List<BigDecimal>>> matriz = new LinkedHashMap<>();

        for (ValorThz.Registro reg : tabela) {
            ValorThz vl = reg.campos().get(campoLinha);
            ValorThz vc = reg.campos().get(campoColuna);
            ValorThz vv = reg.campos().get(campoValor);

            String rotuloLinha = vl != null ? vl.formatar() : "Sem Categoria";
            String rotuloColuna = vc != null ? vc.formatar() : "Geral";
            BigDecimal numValor = extrairNumero(vv);

            colunasDinamicas.add(rotuloColuna);

            matriz.computeIfAbsent(rotuloLinha, k -> new HashMap<>())
                  .computeIfAbsent(rotuloColuna, k -> new ArrayList<>())
                  .add(numValor);
        }

        List<ValorThz.Registro> resultado = new ArrayList<>();
        for (var entryLinha : matriz.entrySet()) {
            Map<String, ValorThz> camposRegistro = new LinkedHashMap<>();
            camposRegistro.put(campoLinha, ValorThz.TEXTO(entryLinha.getKey()));

            BigDecimal totalLinha = BigDecimal.ZERO;
            for (String col : colunasDinamicas) {
                List<BigDecimal> listaValores = entryLinha.getValue().getOrDefault(col, List.of());
                BigDecimal ag = calcularAgregacao(listaValores, op);
                camposRegistro.put(col, ValorThz.DECIMAL(DecimalFixo.deTexto(ag.setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4)));
                totalLinha = totalLinha.add(ag);
            }
            camposRegistro.put("_Total", ValorThz.DECIMAL(DecimalFixo.deTexto(totalLinha.setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4)));
            resultado.add(new ValorThz.Registro("Pivot_Linha", camposRegistro));
        }

        return resultado;
    }

    private static BigDecimal calcularAgregacao(List<BigDecimal> valores, String op) {
        if (valores.isEmpty()) return BigDecimal.ZERO;
        return switch (op) {
            case "COUNT", "CONTAGEM" -> BigDecimal.valueOf(valores.size());
            case "MAX", "MAXIMO" -> valores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            case "MIN", "MINIMO" -> valores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            case "AVG", "MEDIA" -> {
                BigDecimal soma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                yield soma.divide(BigDecimal.valueOf(valores.size()), 4, RoundingMode.HALF_EVEN);
            }
            default -> valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        };
    }

    private static BigDecimal extrairNumero(ValorThz v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof ValorThz.Decimal d) return new BigDecimal(d.valor().formatar());
        if (v instanceof ValorThz.Inteiro in) return new BigDecimal(in.valor());
        return ThzDataQuality.parsearDecimalPtBr(v.formatar()).paraBigDecimal();
    }

    private static ValorThz inferirValorThz(String texto) {
        if (texto == null || texto.isEmpty()) return ValorThz.TEXTO("");
        if ("VERDADEIRO".equalsIgnoreCase(texto) || "TRUE".equalsIgnoreCase(texto)) return ValorThz.LOGICO(true);
        if ("FALSO".equalsIgnoreCase(texto) || "FALSE".equalsIgnoreCase(texto)) return ValorThz.LOGICO(false);
        if (texto.matches("^-?\\d+$")) {
            try { return ValorThz.INTEIRO(new java.math.BigInteger(texto)); } catch (Exception ignored) {}
        }
        if (texto.matches("^-?\\d+\\.\\d+$")) {
            return ValorThz.DECIMAL(DecimalFixo.deTexto(texto, 4));
        }
        return ValorThz.TEXTO(texto);
    }

    private static char detectarSeparador(String linha) {
        int pontoEVirgulas = contarOcorrencias(linha, ';');
        int virgulas = contarOcorrencias(linha, ',');
        int tabs = contarOcorrencias(linha, '\t');
        if (pontoEVirgulas >= virgulas && pontoEVirgulas >= tabs) return ';';
        if (tabs > virgulas) return '\t';
        return ',';
    }

    private static int contarOcorrencias(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) count++;
        }
        return count;
    }

    private static List<String> parsearLinhaCsv(String linha, char sep) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean dentroAspas = false;

        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '"') {
                if (dentroAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // pula aspa escapada
                } else {
                    dentroAspas = !dentroAspas;
                }
            } else if (c == sep && !dentroAspas) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }

    private static String escaparCsv(String val, char sep) {
        if (val == null) return "";
        if (val.contains(String.valueOf(sep)) || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
