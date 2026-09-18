package thz.lang.analytics;

import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DecimalFixo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ThzDataQuality — Motor de Sanitização, Higienização e Tratamento de Caos de Dados Empresariais.
 * Resolve problemas comuns do mundo corporativo:
 * - Parsing robusto de moedas e decimais em padrão brasileiro ("R$ 1.250.500,75" ou "(450,20)")
 * - Normalização de datas híbridas ("DD/MM/YYYY", "YYYY-MM-DD", "DD.MM.YY")
 * - Validação matemática de CPF e CNPJ da Receita Federal
 * - Mascaramento de dados sensíveis (LGPD Art. 7 / GDPR)
 * - Deduplicação por chaves compostas e imputação de valores nulos
 */
public final class ThzDataQuality {

    private ThzDataQuality() {}

    /**
     * Sanitiza strings removendo caracteres de controle, normalizando espaços e quebras.
     */
    public static String sanitizarTexto(String texto) {
        if (texto == null) return "";
        // Remove caracteres de controle ASCII exceto \n
        String limpo = texto.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
        // Normaliza múltiplos espaços consecutivos
        limpo = limpo.replaceAll("[ \\t]+", " ").trim();
        return limpo;
    }

    /**
     * Converte representações caóticas de moeda e decimais (PT-BR ou Internacional) em DecimalFixo exato.
     * Exemplos aceitos: "R$ 1.250.450,75", "(350,00)", "- 1500.50", "1250,50", "  10.000  "
     */
    public static DecimalFixo parsearDecimalPtBr(String texto) {
        if (texto == null || texto.isBlank()) {
            return DecimalFixo.ZERO;
        }

        String t = texto.trim().toUpperCase();
        boolean negativo = false;

        // Detecta formato contábil com parênteses: (1.500,00) -> negativo
        if (t.startsWith("(") && t.endsWith(")")) {
            negativo = true;
            t = t.substring(1, t.length() - 1).trim();
        } else if (t.startsWith("-")) {
            negativo = true;
            t = t.substring(1).trim();
        }

        // Remove prefixos de moeda
        t = t.replaceAll("^(R\\$|US\\$|EUR|BRL|USD|JPY|GBP|\\$)\\s*", "");

        // Remove espaços internos
        t = t.replaceAll("\\s+", "");

        // Se contiver vírgula como separador decimal (ex: 1.250.500,75)
        if (t.contains(",")) {
            // Remove pontos de milhar e substitui vírgula por ponto
            t = t.replace(".", "").replace(",", ".");
        } else if (t.contains(".")) {
            // Se contiver apenas ponto, verifica se é milhar ou decimal
            int ultimoPonto = t.lastIndexOf('.');
            int casasAposPonto = t.length() - 1 - ultimoPonto;
            if (casasAposPonto == 3 && t.indexOf('.') != ultimoPonto) {
                // Múltiplos pontos com 3 casas: provavelmente pontos de milhar
                t = t.replace(".", "");
            }
        }

        // Filtra apenas dígitos e ponto decimal
        t = t.replaceAll("[^0-9.]", "");
        if (t.isEmpty() || t.equals(".")) return DecimalFixo.ZERO;

        try {
            BigDecimal bd = new BigDecimal(t);
            if (negativo) bd = bd.negate();
            return DecimalFixo.deTexto(bd.setScale(4, RoundingMode.HALF_EVEN).toPlainString(), 4);
        } catch (Exception e) {
            return DecimalFixo.ZERO;
        }
    }

    /**
     * Normaliza formatos caóticos de data para o padrão canônico ISO "YYYY-MM-DD".
     * Exemplos aceitos: "25/08/2026", "25-08-2026", "2026/08/25", "25.08.2026", "25/08/26"
     */
    public static String parsearDataPtBr(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String limpo = texto.trim().split("[ T]")[0]; // remove hora se houver

        // 1. Padrão ISO YYYY-MM-DD
        Matcher mIso = Pattern.compile("^(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})$").matcher(limpo);
        if (mIso.matches()) {
            int a = Integer.parseInt(mIso.group(1));
            int m = Integer.parseInt(mIso.group(2));
            int d = Integer.parseInt(mIso.group(3));
            return String.format(Locale.US, "%04d-%02d-%02d", a, m, d);
        }

        // 2. Padrão Brasileiro DD/MM/YYYY ou DD-MM-YYYY ou DD.MM.YYYY
        Matcher mBr = Pattern.compile("^(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2,4})$").matcher(limpo);
        if (mBr.matches()) {
            int d = Integer.parseInt(mBr.group(1));
            int m = Integer.parseInt(mBr.group(2));
            int a = Integer.parseInt(mBr.group(3));
            if (a < 100) a += (a < 50 ? 2000 : 1900); // 26 -> 2026
            return String.format(Locale.US, "%04d-%02d-%02d", a, m, d);
        }

        return limpo;
    }

    /**
     * Validação matemática oficial de CPF (11 dígitos, cálculo dos 2 dígitos verificadores).
     */
    public static boolean validarCpf(String cpf) {
        if (cpf == null) return false;
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() != 11) return false;

        // Rejeita sequências de dígitos repetidos conhecidas (ex: 111.111.111-11)
        if (digitos.matches("(\\d)\\1{10}")) return false;

        int soma1 = 0;
        for (int i = 0; i < 9; i++) {
            soma1 += (digitos.charAt(i) - '0') * (10 - i);
        }
        int r1 = 11 - (soma1 % 11);
        int d1 = (r1 >= 10) ? 0 : r1;
        if (d1 != (digitos.charAt(9) - '0')) return false;

        int soma2 = 0;
        for (int i = 0; i < 10; i++) {
            soma2 += (digitos.charAt(i) - '0') * (11 - i);
        }
        int r2 = 11 - (soma2 % 11);
        int d2 = (r2 >= 10) ? 0 : r2;
        return d2 == (digitos.charAt(10) - '0');
    }

    /**
     * Validação matemática oficial de CNPJ (14 dígitos, algoritmo de módulo 11).
     */
    public static boolean validarCnpj(String cnpj) {
        if (cnpj == null) return false;
        String digitos = cnpj.replaceAll("\\D", "");
        if (digitos.length() != 14) return false;

        if (digitos.matches("(\\d)\\1{13}")) return false;

        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma1 = 0;
        for (int i = 0; i < 12; i++) {
            soma1 += (digitos.charAt(i) - '0') * pesos1[i];
        }
        int r1 = soma1 % 11;
        int d1 = (r1 < 2) ? 0 : (11 - r1);
        if (d1 != (digitos.charAt(12) - '0')) return false;

        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma2 = 0;
        for (int i = 0; i < 13; i++) {
            soma2 += (digitos.charAt(i) - '0') * pesos2[i];
        }
        int r2 = soma2 % 11;
        int d2 = (r2 < 2) ? 0 : (11 - r2);
        return d2 == (digitos.charAt(13) - '0');
    }

    /**
     * Mascara dados sensíveis para conformidade com a LGPD (Art. 7) / GDPR.
     * Exemplo: "123.456.789-00" -> "***.456.789-**"
     */
    public static String mascararDadoSensivel(String texto, int manterInicio, int manterFim) {
        if (texto == null) return "";
        int len = texto.length();
        if (len <= (manterInicio + manterFim)) return "***";

        String inicio = texto.substring(0, manterInicio);
        String fim = texto.substring(len - manterFim);
        String mascara = "*".repeat(len - manterInicio - manterFim);
        return inicio + mascara + fim;
    }

    /**
     * Remove linhas duplicadas de uma lista de registros baseado em um conjunto de campos chave.
     */
    public static List<ValorThz.Registro> removerDuplicatas(List<ValorThz.Registro> linhas, List<String> camposChave) {
        if (linhas == null || linhas.isEmpty()) return List.of();
        Set<String> chavesVistas = new HashSet<>();
        List<ValorThz.Registro> desduplicados = new ArrayList<>();

        for (ValorThz.Registro reg : linhas) {
            StringBuilder sb = new StringBuilder();
            if (camposChave != null && !camposChave.isEmpty()) {
                for (String k : camposChave) {
                    ValorThz v = reg.campos().get(k);
                    sb.append(v != null ? v.formatar() : "null").append("|");
                }
            } else {
                reg.campos().forEach((k, v) -> sb.append(k).append(":").append(v.formatar()).append(";"));
            }
            String chave = sb.toString();
            if (!chavesVistas.contains(chave)) {
                chavesVistas.add(chave);
                desduplicados.add(reg);
            }
        }
        return desduplicados;
    }

    /**
     * Imputa valor padrão para campos com valor nulo ou vazio em uma lista de registros.
     */
    public static List<ValorThz.Registro> imputarNulos(List<ValorThz.Registro> linhas, String campo, ValorThz valorPadrao) {
        if (linhas == null || linhas.isEmpty()) return List.of();
        List<ValorThz.Registro> corrigidos = new ArrayList<>();

        for (ValorThz.Registro reg : linhas) {
            Map<String, ValorThz> novosCampos = new LinkedHashMap<>(reg.campos());
            ValorThz atual = novosCampos.get(campo);
            if (atual == null || atual instanceof ValorThz.Nulo || atual.formatar().isBlank()) {
                novosCampos.put(campo, valorPadrao);
            }
            corrigidos.add(new ValorThz.Registro(reg.nomeEstrutura(), novosCampos));
        }
        return corrigidos;
    }
}
