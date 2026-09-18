package thz.lang.brasil;

import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DecimalFixo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ThzBrasilEngine — Fachada unificada para os motores de domínio brasileiro.
 * Delega para: ThzCepEngine, ThzPixEngine, ThzBoletoEngine,
 * ThzDocumentoEngine, ThzFeriadoEngine, ThzExtensoEngine.
 */
public final class ThzBrasilEngine {

    private ThzBrasilEngine() {}

    // ---- CEP/UF (delegação) ----

    public static String formatarCep(String cep) { return ThzCepEngine.formatarCep(cep); }
    public static boolean validarUf(String uf) { return ThzCepEngine.validarUf(uf); }
    public static String regiaoUf(String uf) { return ThzCepEngine.regiaoUf(uf); }
    public static ValorThz.Registro consultarCep(String cep) { return ThzCepEngine.consultarCep(cep); }
    public static String formatarEndereco(String logradouro, String numero, String complemento, String bairro, String cidade, String uf, String cep) {
        return ThzCepEngine.formatarEndereco(logradouro, numero, complemento, bairro, cidade, uf, cep);
    }

    // ---- PIX (delegação) ----

    public static String gerarPixCopiaECola(String chave, String nomeRecebedor, String cidadeRecebedor, BigDecimal valor, String txId) {
        return ThzPixEngine.gerarPixCopiaECola(chave, nomeRecebedor, cidadeRecebedor, valor, txId);
    }
    public static boolean validarChavePix(String chave, String tipo) { return ThzPixEngine.validarChavePix(chave, tipo); }

    // ---- Boleto (delegação) ----

    public static boolean validarLinhaDigitavel(String linha) { return ThzBoletoEngine.validarLinhaDigitavel(linha); }
    public static String linhaDigitavelParaCodigoBarras(String linha) { return ThzBoletoEngine.linhaDigitavelParaCodigoBarras(linha); }
    public static DecimalFixo extrairValorBoleto(String linha) { return ThzBoletoEngine.extrairValorBoleto(linha); }

    // ---- Documento (delegação) ----

    public static String formatarCpf(String cpf) { return ThzDocumentoEngine.formatarCpf(cpf); }
    public static String formatarCnpj(String cnpj) { return ThzDocumentoEngine.formatarCnpj(cnpj); }
    public static String formatarTelefone(String tel) { return ThzDocumentoEngine.formatarTelefone(tel); }
    public static boolean validarTituloEleitor(String titulo) { return ThzDocumentoEngine.validarTituloEleitor(titulo); }
    public static boolean validarCnh(String cnh) { return ThzDocumentoEngine.validarCnh(cnh); }
    public static boolean validarPis(String pis) { return ThzDocumentoEngine.validarPis(pis); }

    // ---- Feriado (delegação) ----

    public static boolean ehFeriadoNacional(LocalDate data) { return ThzFeriadoEngine.ehFeriadoNacional(data); }
    public static boolean ehDiaUtil(LocalDate data) { return ThzFeriadoEngine.ehDiaUtil(data); }
    public static LocalDate proximoDiaUtil(LocalDate data) { return ThzFeriadoEngine.proximoDiaUtil(data); }

    // ---- Extenso (delegação) ----

    public static String valorPorExtenso(BigDecimal valor) { return ThzExtensoEngine.valorPorExtenso(valor); }
}
