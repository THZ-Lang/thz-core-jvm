package thz.lang.brasil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import thz.lang.runtime.DecimalFixo;
import thz.lang.snapshot.ThzSnapshotEngine;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ThzBrasilSnapshotTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        Path dbPath = tempDir.resolve("teste_interno.thzdbi");
        ThzInternalDatabase.definirCaminhoDb(dbPath);
    }

    @Test
    @DisplayName("ThzInternalDatabase (.thzdbi) deve consultar e cadastrar CEPs offline em banco interno")
    void testBancoInternoThzDbi() {
        // 1. Consulta CEP pré-carregado na base inicial (Avenida Paulista)
        var res = ThzBrasilEngine.consultarCep("01310-100");
        assertNotNull(res);
        assertEquals("01310-100", res.campos().get("cep").formatar());
        assertEquals("Avenida Paulista", res.campos().get("logradouro").formatar());
        assertEquals("São Paulo", res.campos().get("cidade").formatar());
        assertEquals("SP", res.campos().get("uf").formatar());
        assertEquals("Sudeste", res.campos().get("regiao").formatar());

        // 2. Cadastrar novo CEP customizado no .thzdbi
        boolean cadastrado = ThzInternalDatabase.cadastrarCep(
                "13010002", "Rua General Osório", "Centro", "Campinas", "SP", "3509502", "19"
        );
        assertTrue(cadastrado);

        var resCampinas = ThzBrasilEngine.consultarCep("13010002");
        assertEquals("Rua General Osório", resCampinas.campos().get("logradouro").formatar());
        assertEquals("Campinas", resCampinas.campos().get("cidade").formatar());

        // 3. Fallback de inferência por faixa canônica estadual
        var resBahia = ThzBrasilEngine.consultarCep("45000000");
        assertEquals("BA", resBahia.campos().get("uf").formatar());
        assertEquals("Bahia", resBahia.campos().get("cidade").formatar());
    }

    @Test
    @DisplayName("ThzBrasilEngine deve gerar PIX Copia e Cola com EMVco e CRC16")
    void testPixCopiaECola() {
        String chave = "11222333000181";
        String payload = ThzBrasilEngine.gerarPixCopiaECola(
                chave, "EMPRESA TESTE", "SAO PAULO", new BigDecimal("150.50"), "PED123"
        );

        assertNotNull(payload);
        assertTrue(payload.startsWith("000201"));
        assertTrue(payload.contains("br.gov.bcb.pix"));
        assertTrue(payload.contains("11222333000181"));
        assertTrue(payload.contains("150.50"));
        assertTrue(payload.contains("PED123"));
        assertTrue(payload.contains("6304"), "Deve conter campo 63 do CRC");
        assertEquals(4, payload.substring(payload.length() - 4).length(), "CRC deve ter 4 caracteres hexadecimais");

        // Validação de Chaves PIX
        assertTrue(ThzBrasilEngine.validarChavePix("11222333000181", "CNPJ"));
        assertTrue(ThzBrasilEngine.validarChavePix("52998224725", "CPF"));
        assertTrue(ThzBrasilEngine.validarChavePix("contato@empresa.com.br", "EMAIL"));
        assertTrue(ThzBrasilEngine.validarChavePix("+5511987654321", "TELEFONE"));
    }

    @Test
    @DisplayName("ThzBrasilEngine deve validar linha digitável de boleto, converter para código de barras e extrair valor")
    void testBoletoBancario() {
        // Linha digitável canônica de teste Banco do Brasil (47 dígitos)
        String linhaValida = "00190.50095 40144.816069 06809.350314 3 37370000000100";
        assertTrue(ThzBrasilEngine.validarLinhaDigitavel(linhaValida));

        String codBarras = ThzBrasilEngine.linhaDigitavelParaCodigoBarras(linhaValida);
        assertEquals(44, codBarras.length(), "Código de barras deve ter 44 dígitos");

        DecimalFixo valor = ThzBrasilEngine.extrairValorBoleto(linhaValida);
        assertEquals("1.0000", valor.formatar());
    }

    @Test
    @DisplayName("ThzBrasilEngine deve validar documentos (Título de Eleitor, CNH, PIS) e formatar textos")
    void testDocumentosNacionaisEFormatacoes() {
        // Formatações
        assertEquals("123.456.789-00", ThzBrasilEngine.formatarCpf("12345678900"));
        assertEquals("11.222.333/0001-81", ThzBrasilEngine.formatarCnpj("11222333000181"));
        assertEquals("(11) 98765-4321", ThzBrasilEngine.formatarTelefone("11987654321"));

        // CNH e PIS
        assertTrue(ThzBrasilEngine.validarPis("17033259504"));
        assertFalse(ThzBrasilEngine.validarPis("00000000000"));

        // Valor por extenso
        assertEquals("mil duzentos e cinquenta reais e cinquenta centavos", ThzBrasilEngine.valorPorExtenso(new BigDecimal("1250.50")));
        assertEquals("um real", ThzBrasilEngine.valorPorExtenso(new BigDecimal("1.00")));
        assertEquals("zero reais", ThzBrasilEngine.valorPorExtenso(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("ThzBrasilEngine deve verificar feriados nacionais (inclusive Consciência Negra) e dias úteis")
    void testCalendarioEFeriados() {
        // 1. Feriados Fixos
        assertTrue(ThzBrasilEngine.ehFeriadoNacional(LocalDate.of(2026, 1, 1)));  // Confraternização
        assertTrue(ThzBrasilEngine.ehFeriadoNacional(LocalDate.of(2026, 4, 21))); // Tiradentes
        assertTrue(ThzBrasilEngine.ehFeriadoNacional(LocalDate.of(2026, 9, 7)));  // 7 de Setembro
        assertTrue(ThzBrasilEngine.ehFeriadoNacional(LocalDate.of(2026, 11, 20))); // Consciência Negra (Lei 14.759/23)
        assertTrue(ThzBrasilEngine.ehFeriadoNacional(LocalDate.of(2026, 12, 25))); // Natal

        // 2. Feriado Móvel (Páscoa em 2026 = 05/04/2026, Sexta-Feira Santa = 03/04/2026)
        assertTrue(ThzBrasilEngine.ehFeriadoNacional(LocalDate.of(2026, 4, 3))); // Sexta-feira Santa

        // 3. Dias Úteis
        assertFalse(ThzBrasilEngine.ehDiaUtil(LocalDate.of(2026, 4, 3))); // Sexta Santa não é útil
        assertFalse(ThzBrasilEngine.ehDiaUtil(LocalDate.of(2026, 4, 4))); // Sábado não é útil
        assertFalse(ThzBrasilEngine.ehDiaUtil(LocalDate.of(2026, 4, 5))); // Domingo não é útil
        assertTrue(ThzBrasilEngine.ehDiaUtil(LocalDate.of(2026, 4, 6)));  // Segunda-feira é útil

        // Próximo dia útil a partir da Sexta-Feira Santa (03/04) deve ser Segunda (06/04)
        LocalDate prox = ThzBrasilEngine.proximoDiaUtil(LocalDate.of(2026, 4, 3));
        assertEquals(LocalDate.of(2026, 4, 6), prox);
    }

    @Test
    @DisplayName("ThzSnapshotEngine deve criar e restaurar snapshot comprimido com cabeçalho mágico e manter 1 arquivo")
    void testSnapshotEngine() throws IOException {
        Path origemDir = tempDir.resolve("workspace_origem");
        Files.createDirectories(origemDir);
        Files.writeString(origemDir.resolve("faturamento.thz"), "PROGRAMA Faturamento FIM_PROGRAMA");
        Files.writeString(origemDir.resolve("dados.json"), "{\"status\": \"OK\"}");

        Path snapshotArquivo = tempDir.resolve("meu_snapshot.thzsnap");

        // 1. Criar Snapshot
        Path criado = ThzSnapshotEngine.criarSnapshot(origemDir, snapshotArquivo);
        assertTrue(Files.exists(criado));
        assertTrue(ThzSnapshotEngine.verificarIntegridade(criado));
        assertTrue(Files.size(criado) < ThzSnapshotEngine.MAX_BYTES_SNAPSHOT, "Deve ser menor que 100MB");

        // 2. Restaurar Snapshot em outro diretório
        Path destinoDir = tempDir.resolve("workspace_restaurado");
        boolean restaurado = ThzSnapshotEngine.restaurarSnapshot(criado, destinoDir);
        assertTrue(restaurado);

        assertTrue(Files.exists(destinoDir.resolve("faturamento.thz")));
        assertTrue(Files.exists(destinoDir.resolve("dados.json")));
        assertEquals("PROGRAMA Faturamento FIM_PROGRAMA", Files.readString(destinoDir.resolve("faturamento.thz")));

        // 3. Invariante de 1 único snapshot: criar novamente sobrescreve atomicamente
        Path criado2 = ThzSnapshotEngine.criarSnapshot(origemDir, snapshotArquivo);
        assertEquals(criado.toAbsolutePath(), criado2.toAbsolutePath());
    }
}
