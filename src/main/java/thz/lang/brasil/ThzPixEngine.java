package thz.lang.brasil;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Motor de PIX — payload EMVco / BR Code com CRC16 e validação de chaves.
 */
public final class ThzPixEngine {

    private ThzPixEngine() {}

    public static String gerarPixCopiaECola(String chave, String nomeRecebedor, String cidadeRecebedor, BigDecimal valor, String txId) {
        if (chave == null || chave.isBlank()) return "";
        String nome = sanitizarTextoEmv(nomeRecebedor != null && !nomeRecebedor.isBlank() ? nomeRecebedor : "RECEBEDOR", 25);
        String cidade = sanitizarTextoEmv(cidadeRecebedor != null && !cidadeRecebedor.isBlank() ? cidadeRecebedor : "SAO PAULO", 15);
        String tid = (txId != null && !txId.isBlank()) ? txId.replaceAll("[^A-Za-z0-9]", "") : "***";
        if (tid.length() > 25) tid = tid.substring(0, 25);

        String gui = formatarCampoEmv("00", "br.gov.bcb.pix");
        String chaveCampo = formatarCampoEmv("01", chave.trim());
        String merchantAccountInfo = formatarCampoEmv("26", gui + chaveCampo);

        StringBuilder payload = new StringBuilder();
        payload.append(formatarCampoEmv("00", "01"));
        payload.append(formatarCampoEmv("01", "12"));
        payload.append(merchantAccountInfo);
        payload.append(formatarCampoEmv("52", "0000"));
        payload.append(formatarCampoEmv("53", "986"));
        if (valor != null && valor.compareTo(BigDecimal.ZERO) > 0) {
            payload.append(formatarCampoEmv("54", valor.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString()));
        }
        payload.append(formatarCampoEmv("58", "BR"));
        payload.append(formatarCampoEmv("59", nome));
        payload.append(formatarCampoEmv("60", cidade));

        String campoTxId = formatarCampoEmv("05", tid);
        payload.append(formatarCampoEmv("62", campoTxId));

        payload.append("6304");
        String crc = calcularCrc16Ccitt(payload.toString());
        return payload.toString() + crc;
    }

    public static boolean validarChavePix(String chave, String tipo) {
        if (chave == null || chave.isBlank()) return false;
        String t = tipo != null ? tipo.trim().toUpperCase() : "AUTO";
        String limpo = chave.trim();

        return switch (t) {
            case "CPF" -> thz.lang.analytics.ThzDataQuality.validarCpf(limpo);
            case "CNPJ" -> thz.lang.analytics.ThzDataQuality.validarCnpj(limpo);
            case "EMAIL" -> Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$").matcher(limpo).matches();
            case "TELEFONE" -> limpo.matches("^\\+55\\d{10,11}$") || limpo.replaceAll("\\D", "").matches("^55\\d{10,11}$");
            case "ALEATORIA", "EVP" -> Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matcher(limpo).matches();
            default -> {
                yield thz.lang.analytics.ThzDataQuality.validarCpf(limpo)
                        || thz.lang.analytics.ThzDataQuality.validarCnpj(limpo)
                        || Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$").matcher(limpo).matches()
                        || limpo.replaceAll("\\D", "").length() >= 10
                        || Pattern.compile("^[0-9a-fA-F-]{36}$").matcher(limpo).matches();
            }
        };
    }

    static String formatarCampoEmv(String id, String valor) {
        int len = valor.getBytes(StandardCharsets.UTF_8).length;
        return id + String.format(Locale.US, "%02d", len) + valor;
    }

    static String sanitizarTextoEmv(String texto, int maxLen) {
        String limpo = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", "")
                .trim();
        if (limpo.length() > maxLen) limpo = limpo.substring(0, maxLen);
        return limpo;
    }

    static String calcularCrc16Ccitt(String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        int crc = 0xFFFF;
        int polinomio = 0x1021;
        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i)) & 1) == 1;
                boolean c15 = ((crc >> 15) & 1) == 1;
                crc <<= 1;
                if (c15 ^ bit) crc ^= polinomio;
            }
        }
        crc &= 0xFFFF;
        return String.format(Locale.US, "%04X", crc);
    }
}
