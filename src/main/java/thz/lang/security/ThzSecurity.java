package thz.lang.security;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * ThzSecurity — Hashing criptográfico, criptografia autenticada (AES-256-GCM, ChaCha20-Poly1305),
 * derivação de chaves moderna Argon2id e PBKDF2, HMAC e tokens seguros.
 */
public final class ThzSecurity {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final int PBKDF2_ITERATIONS = 310000; // OWASP 2026

    // Parâmetros recomendados para Argon2id (RFC 9106)
    public static final int ARGON2_DEFAULT_MEMORY_KB = 65536; // 64 MB
    public static final int ARGON2_DEFAULT_ITERATIONS = 3;
    public static final int ARGON2_DEFAULT_PARALLELISM = 4;
    public static final int ARGON2_TAG_LENGTH = 32; // 256 bits

    private ThzSecurity() {}

    public static String sha256(String entrada) {
        return hash("SHA-256", entrada);
    }

    public static String sha512(String entrada) {
        return hash("SHA-512", entrada);
    }

    private static String hash(String algoritmo, String entrada) {
        try {
            MessageDigest md = MessageDigest.getInstance(algoritmo);
            byte[] digest = md.digest(entrada.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de hash não disponível: " + algoritmo, e);
        }
    }

    public static String hmacSha256(String texto, String chave) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(chave.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmac = mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmac);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao calcular HMAC-SHA256: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // AES-256-GCM
    // -------------------------------------------------------------------------
    public static String criptografarAes(String textoClaro, String chaveHex) {
        try {
            byte[] keyBytes = normalizarChave(chaveHex);
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(textoClaro.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Falha na criptografia AES-GCM: " + e.getMessage(), e);
        }
    }

    public static String descriptografarAes(String payloadBase64, String chaveHex) {
        try {
            byte[] keyBytes = normalizarChave(chaveHex);
            byte[] payload = Base64.getUrlDecoder().decode(payloadBase64);

            if (payload.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Payload cifrado inválido.");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[payload.length - GCM_IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(payload, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Falha na descriptografia AES-GCM: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // ChaCha20-Poly1305 (Cifra Autenticada de Alta Performance)
    // -------------------------------------------------------------------------
    public static String criptografarChaCha20(String textoClaro, String chaveHex) {
        try {
            byte[] keyBytes = normalizarChave(chaveHex);
            byte[] nonce = new byte[12];
            RANDOM.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "ChaCha20");
            IvParameterSpec ivSpec = new IvParameterSpec(nonce);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] cipherText = cipher.doFinal(textoClaro.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + cipherText.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(cipherText, 0, payload, nonce.length, cipherText.length);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Falha na criptografia ChaCha20-Poly1305: " + e.getMessage(), e);
        }
    }

    public static String descriptografarChaCha20(String payloadBase64, String chaveHex) {
        try {
            byte[] keyBytes = normalizarChave(chaveHex);
            byte[] payload = Base64.getUrlDecoder().decode(payloadBase64);

            if (payload.length < 12) {
                throw new IllegalArgumentException("Payload ChaCha20 inválido.");
            }

            byte[] nonce = new byte[12];
            byte[] cipherText = new byte[payload.length - 12];
            System.arraycopy(payload, 0, nonce, 0, 12);
            System.arraycopy(payload, 12, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "ChaCha20");
            IvParameterSpec ivSpec = new IvParameterSpec(nonce);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Falha na descriptografia ChaCha20-Poly1305: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Argon2id (Padrão Ouro de Derivação de Chaves e Senhas)
    // -------------------------------------------------------------------------
    public static String hashArgon2(String senha) {
        return hashArgon2(senha, ARGON2_DEFAULT_MEMORY_KB, ARGON2_DEFAULT_ITERATIONS, ARGON2_DEFAULT_PARALLELISM);
    }

    public static String hashArgon2(String senha, int memoriaKb, int iteracoes, int paralelismo) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = derivarChaveArgon2(senha.toCharArray(), salt, memoriaKb, iteracoes, paralelismo, ARGON2_TAG_LENGTH);
        
        String formato = String.format("$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s",
                memoriaKb, iteracoes, paralelismo,
                Base64.getEncoder().withoutPadding().encodeToString(salt),
                Base64.getEncoder().withoutPadding().encodeToString(hash));
        return formato;
    }

    public static boolean verificarArgon2(String senha, String hashCompleto) {
        try {
            if (hashCompleto == null || !hashCompleto.startsWith("$argon2id$")) {
                return false;
            }
            String[] partes = hashCompleto.split("\\$");
            // partes: ["", "argon2id", "v=19", "m=...,t=...,p=...", saltBase64, hashBase64]
            if (partes.length < 6) return false;

            String[] params = partes[3].split(",");
            int m = Integer.parseInt(params[0].substring(2));
            int t = Integer.parseInt(params[1].substring(2));
            int p = Integer.parseInt(params[2].substring(2));

            byte[] salt = Base64.getDecoder().decode(partes[4]);
            byte[] hashEsperado = Base64.getDecoder().decode(partes[5]);

            byte[] hashCalculado = derivarChaveArgon2(senha.toCharArray(), salt, m, t, p, hashEsperado.length);
            return MessageDigest.isEqual(hashEsperado, hashCalculado);
        } catch (Exception e) {
            return false;
        }
    }

    public static byte[] derivarChaveArgon2(char[] senha, byte[] salt, int memoriaKb, int iteracoes, int paralelismo, int tamanhoSaidaBytes) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(iteracoes)
                .withMemoryAsKB(memoriaKb)
                .withParallelism(paralelismo)
                .withSalt(salt);

        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(builder.build());

        byte[] senhaBytes = new byte[senha.length * 2];
        for (int i = 0; i < senha.length; i++) {
            senhaBytes[i * 2] = (byte) (senha[i] >> 8);
            senhaBytes[i * 2 + 1] = (byte) senha[i];
        }

        byte[] resultado = new byte[tamanhoSaidaBytes];
        gen.generateBytes(senhaBytes, resultado, 0, resultado.length);
        Arrays.fill(senhaBytes, (byte) 0);
        return resultado;
    }

    // -------------------------------------------------------------------------
    // PBKDF2 (Legado Compatível)
    // -------------------------------------------------------------------------
    public static String hashSenha(String senha) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(senha.toCharArray(), salt);
        return HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(hash);
    }

    public static boolean verificarSenha(String senha, String hashArmazenado) {
        String[] partes = hashArmazenado.split(":");
        if (partes.length != 2) return false;
        byte[] salt = HexFormat.of().parseHex(partes[0]);
        byte[] hashEsperado = HexFormat.of().parseHex(partes[1]);
        byte[] hashCalculado = pbkdf2(senha.toCharArray(), salt);
        return MessageDigest.isEqual(hashEsperado, hashCalculado);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Falha no PBKDF2: " + e.getMessage(), e);
        }
    }

    public static String gerarToken(int tamanhoBytes) {
        byte[] bytes = new byte[Math.max(16, tamanhoBytes)];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String gerarUuid() {
        return UUID.randomUUID().toString();
    }

    public static byte[] normalizarChave(String chave) {
        try {
            if (chave.length() == 64 && chave.matches("^[0-9a-fA-F]+$")) {
                return HexFormat.of().parseHex(chave);
            }
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(chave.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
