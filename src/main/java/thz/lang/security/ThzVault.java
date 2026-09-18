package thz.lang.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * ThzVault — Cofre de arquivos criptografados com arquitetura inspirada no VeraCrypt.
 * Utiliza KDF Argon2id (RFC 9106), cifra autenticada AES-256-GCM, cabeçalho de integridade
 * e limpeza forçada de memória RAM (scrubbing).
 */
public final class ThzVault {

    private static final byte[] MAGIC = new byte[]{'T', 'H', 'Z', 'V', 'A', 'U', 'L', 'T'};
    private static final byte VERSAO_1 = 0x01;
    private static final byte KDF_ARGON2ID = 0x01;
    private static final byte CIFRA_AES_GCM_256 = 0x01;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int TAG_BIT_LEN = 128;

    private ThzVault() {}

    /**
     * Cifra e salva dados em um arquivo de cofre seguro (.thzvault).
     */
    public static void salvar(Path arquivo, byte[] dadosClaros, String senha) throws IOException {
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(iv);

        // Derivação de chave de 256 bits com Argon2id
        byte[] chaveMestra = ThzSecurity.derivarChaveArgon2(
                senha.toCharArray(),
                salt,
                ThzSecurity.ARGON2_DEFAULT_MEMORY_KB,
                ThzSecurity.ARGON2_DEFAULT_ITERATIONS,
                ThzSecurity.ARGON2_DEFAULT_PARALLELISM,
                32
        );

        byte[] payloadCifrado;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(chaveMestra, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BIT_LEN, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            payloadCifrado = cipher.doFinal(dadosClaros);
        } catch (Exception e) {
            Arrays.fill(chaveMestra, (byte) 0);
            throw new IOException("Falha ao criptografar cofre: " + e.getMessage(), e);
        } finally {
            Arrays.fill(chaveMestra, (byte) 0);
        }

        // Montagem do contêiner binário
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.write(MAGIC);
            dos.writeByte(VERSAO_1);
            dos.writeByte(KDF_ARGON2ID);
            dos.writeByte(CIFRA_AES_GCM_256);
            dos.write(salt);
            dos.write(iv);
            dos.writeInt(payloadCifrado.length);
            dos.write(payloadCifrado);
        }

        if (arquivo.getParent() != null) {
            Files.createDirectories(arquivo.getParent());
        }
        Files.write(arquivo, baos.toByteArray());
    }

    /**
     * Salva uma string em texto claro dentro de um cofre criptografado.
     */
    public static void salvarTexto(Path arquivo, String texto, String senha) throws IOException {
        byte[] dados = texto.getBytes(StandardCharsets.UTF_8);
        try {
            salvar(arquivo, dados, senha);
        } finally {
            Arrays.fill(dados, (byte) 0);
        }
    }

    /**
     * Lê e descriptografa um arquivo de cofre seguro (.thzvault).
     */
    public static byte[] ler(Path arquivo, String senha) throws IOException {
        byte[] conteudoArquivo = Files.readAllBytes(arquivo);
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(conteudoArquivo))) {
            byte[] magicLido = new byte[8];
            dis.readFully(magicLido);
            if (!Arrays.equals(MAGIC, magicLido)) {
                throw new IOException("Formato de cofre inválido (Magic Header incorreto).");
            }

            byte versao = dis.readByte();
            if (versao != VERSAO_1) {
                throw new IOException("Versão de cofre incompatível: " + versao);
            }

            byte kdf = dis.readByte();
            byte cifra = dis.readByte();
            if (kdf != KDF_ARGON2ID || cifra != CIFRA_AES_GCM_256) {
                throw new IOException("Algoritmo de criptografia do cofre não suportado.");
            }

            byte[] salt = new byte[SALT_LEN];
            byte[] iv = new byte[IV_LEN];
            dis.readFully(salt);
            dis.readFully(iv);

            int payloadLen = dis.readInt();
            byte[] payloadCifrado = new byte[payloadLen];
            dis.readFully(payloadCifrado);

            // Deriva chave mestra usando o salt gravado no cabeçalho
            byte[] chaveMestra = ThzSecurity.derivarChaveArgon2(
                    senha.toCharArray(),
                    salt,
                    ThzSecurity.ARGON2_DEFAULT_MEMORY_KB,
                    ThzSecurity.ARGON2_DEFAULT_ITERATIONS,
                    ThzSecurity.ARGON2_DEFAULT_PARALLELISM,
                    32
            );

            try {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                SecretKeySpec keySpec = new SecretKeySpec(chaveMestra, "AES");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BIT_LEN, iv);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
                return cipher.doFinal(payloadCifrado);
            } catch (Exception e) {
                throw new IOException("Senha incorreta ou integridade do cofre corrompida: " + e.getMessage(), e);
            } finally {
                Arrays.fill(chaveMestra, (byte) 0);
            }
        }
    }

    /**
     * Lê um cofre criptografado e retorna o conteúdo descriptografado como String UTF-8.
     */
    public static String lerTexto(Path arquivo, String senha) throws IOException {
        byte[] dadosClaros = ler(arquivo, senha);
        try {
            return new String(dadosClaros, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(dadosClaros, (byte) 0);
        }
    }
}
