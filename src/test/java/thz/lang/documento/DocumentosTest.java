package thz.lang.documento;

import com.lowagie.text.pdf.PdfReader;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentosTest {

    private Path tempDir;

    @BeforeEach
    public void setup() throws Exception {
        tempDir = Files.createTempDirectory("thz-documentos-test-");
    }

    @AfterEach
    public void teardown() throws Exception {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignore) {}
                });
            }
        }
    }

    private ValorThz.Registro criarRegistroComplexoExemplo() {
        Map<String, ValorThz> campos = new LinkedHashMap<>();
        campos.put("titulo", ValorThz.TEXTO("Ficha Cadastral de Produto"));
        campos.put("sku", ValorThz.TEXTO("SWT-CORE-10G"));
        campos.put("nomeProduto", ValorThz.TEXTO("Switch Gerenciável L3"));
        campos.put("precoCusto", ValorThz.DECIMAL("3200.00"));
        campos.put("margemLucro", ValorThz.DECIMAL("35.00"));
        campos.put("controladoPorLote", ValorThz.LOGICO(true));

        List<ValorThz> lotes = new ArrayList<>();
        Map<String, ValorThz> l1 = new LinkedHashMap<>();
        l1.put("numeroLote", ValorThz.TEXTO("LOT-2026-A1"));
        l1.put("quantidade", ValorThz.INTEIRO(BigInteger.valueOf(50)));
        l1.put("deposito", ValorThz.TEXTO("Armazém SP"));
        lotes.add(new ValorThz.Registro("LoteEstoque", l1));

        Map<String, ValorThz> l2 = new LinkedHashMap<>();
        l2.put("numeroLote", ValorThz.TEXTO("LOT-2026-B2"));
        l2.put("quantidade", ValorThz.INTEIRO(BigInteger.valueOf(30)));
        l2.put("deposito", ValorThz.TEXTO("Filial Curitiba"));
        lotes.add(new ValorThz.Registro("LoteEstoque", l2));

        campos.put("lotesDisponiveis", new ValorThz.Fatia("LoteEstoque", lotes));
        return new ValorThz.Registro("FormularioProduto", campos);
    }

    @Test
    public void gerarPdfRegistroETabelaComSucesso() throws Exception {
        ValorThz.Registro reg = criarRegistroComplexoExemplo();
        Path outPdf = tempDir.resolve("produto.pdf");

        Path resultado = GeradorPdf.gerar(outPdf, "Catálogo de Produtos — THZ", reg);
        assertTrue(Files.exists(resultado), "Arquivo PDF deve ter sido gerado");
        assertTrue(Files.size(resultado) > 500, "Arquivo PDF não deve estar vazio");

        // Valida com PdfReader do OpenPDF
        try (InputStream in = new FileInputStream(resultado.toFile())) {
            PdfReader reader = new PdfReader(in);
            assertTrue(reader.getNumberOfPages() >= 1, "PDF deve conter pelo menos 1 página");
            reader.close();
        }
    }

    @Test
    public void gerarXlsxRegistroEAbasComSucesso() throws Exception {
        ValorThz.Registro reg = criarRegistroComplexoExemplo();
        Path outXlsx = tempDir.resolve("produto.xlsx");

        Path resultado = GeradorXlsx.gerar(outXlsx, "FichaProduto", reg);
        assertTrue(Files.exists(resultado), "Arquivo XLSX deve ter sido gerado");
        assertTrue(Files.size(resultado) > 1000, "Arquivo XLSX não deve estar vazio");

        // Valida com Apache POI
        try (InputStream in = new FileInputStream(resultado.toFile());
             Workbook wb = new XSSFWorkbook(in)) {
            assertEquals(2, wb.getNumberOfSheets(), "Deve conter aba principal e aba da fatia");
            Sheet sPrincipal = wb.getSheet("FichaProduto");
            assertNotNull(sPrincipal, "Aba principal deve existir");
            assertEquals("Propriedade", sPrincipal.getRow(0).getCell(0).getStringCellValue());

            Sheet sLotes = wb.getSheet("Lotes Disponiveis");
            assertNotNull(sLotes, "Aba de lotes deve existir");
            assertEquals("Numero Lote", sLotes.getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test
    public void gerarDocxRegistroETabelasComSucesso() throws Exception {
        ValorThz.Registro reg = criarRegistroComplexoExemplo();
        Path outDocx = tempDir.resolve("produto.docx");

        Path resultado = GeradorDocx.gerar(outDocx, "Catálogo de Produtos", reg);
        assertTrue(Files.exists(resultado), "Arquivo DOCX deve ter sido gerado");
        assertTrue(Files.size(resultado) > 1000, "Arquivo DOCX não deve estar vazio");

        // Valida com Apache POI XWPF
        try (InputStream in = new FileInputStream(resultado.toFile());
             XWPFDocument doc = new XWPFDocument(in)) {
            assertFalse(doc.getParagraphs().isEmpty(), "Documento deve conter parágrafos");
            List<XWPFTable> tables = doc.getTables();
            assertEquals(2, tables.size(), "Deve conter tabela de atributos e tabela de lotes");
        }
    }

    @Test
    public void fachadaMotorDocumentosDetectaExtensao() throws Exception {
        ValorThz.Registro reg = criarRegistroComplexoExemplo();

        Path p1 = MotorDocumentos.exportar(tempDir.resolve("doc1.pdf"), "Teste PDF", reg);
        assertTrue(Files.exists(p1));

        Path p2 = MotorDocumentos.exportar(tempDir.resolve("doc2.xlsx"), "Teste XLSX", reg);
        assertTrue(Files.exists(p2));

        Path p3 = MotorDocumentos.exportar(tempDir.resolve("doc3.docx"), "Teste DOCX", reg);
        assertTrue(Files.exists(p3));
    }

    @Test
    public void stdlibDocumentoExportaEmTempoDeExecucao() throws Exception {
        String pdfDest = tempDir.resolve("fatura.pdf").toString().replace("\\", "/");
        String xlsxDest = tempDir.resolve("fatura.xlsx").toString().replace("\\", "/");
        String docxDest = tempDir.resolve("fatura.docx").toString().replace("\\", "/");

        String codigo = """
                PROGRAMA TesteExportacaoDocumentos

                ESTRUTURA ItemFatura
                    descricao: TEXTO
                    quantidade: INTEIRO32
                    valorTotal: DECIMAL(10, 2)
                FIM_ESTRUTURA

                ESTRUTURA Fatura
                    numeroFatura: TEXTO
                    cliente: TEXTO
                    itens: FATIA[ItemFatura]
                FIM_ESTRUTURA

                PROCEDIMENTO Principal()
                INICIO
                    VARIAVEL i1 : ItemFatura <- CRIAR ItemFatura(descricao: "Servidor Rack 2U", quantidade: 2, valorTotal: 18000.00)
                    VARIAVEL i2 : ItemFatura <- CRIAR ItemFatura(descricao: "Licença Enterprise", quantidade: 1, valorTotal: 5000.00)
                    VARIAVEL fatura : Fatura <- CRIAR Fatura(numeroFatura: "FAT-2026-99", cliente: "Acme Corp", itens: [i1, i2])

                    VARIAVEL resPdf : TEXTO <- DOCUMENTO.exportarPdf("%s", "Fatura Corporativa Acme", fatura)
                    VARIAVEL resXlsx : TEXTO <- DOCUMENTO.exportarXlsx("%s", "Itens Faturados", fatura)
                    VARIAVEL resDocx : TEXTO <- DOCUMENTO.exportarDocx("%s", "Contrato de Fornecimento", fatura)
                FIM
                FIM_PROGRAMA
                """.formatted(pdfDest, xlsxDest, docxDest);

        ProgramaAst ast = new ThzParser(new ThzLexer(codigo).tokenize()).parse();
        InterpretadorThz interp = new InterpretadorThz(ast);
        interp.executarProcedimento("Principal", Map.<String, ValorThz>of());

        assertTrue(Files.exists(Path.of(pdfDest)), "PDF via Stdlib deve ter sido criado");
        assertTrue(Files.exists(Path.of(xlsxDest)), "XLSX via Stdlib deve ter sido criado");
        assertTrue(Files.exists(Path.of(docxDest)), "DOCX via Stdlib deve ter sido criado");
    }
}
