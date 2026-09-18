package thz.lang.documento;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import thz.lang.interpretador.ValorThz;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gerador de documentos PDF corporativos e relatórios a partir de dados THZ-LANG.
 */
public final class GeradorPdf {

    private static final Color COR_TITULO = new Color(24, 24, 27);      // Zinc 900
    private static final Color COR_SUBTITULO = new Color(113, 113, 122); // Zinc 500
    private static final Color COR_AZUL = new Color(37, 99, 235);        // Blue 600
    private static final Color COR_FUNDO_CABECALHO = new Color(39, 39, 42); // Zinc 800
    private static final Color COR_FUNDO_ZEBRA = new Color(244, 244, 245);  // Zinc 100
    private static final Color COR_BORDA = new Color(228, 228, 231);        // Zinc 200

    private static final Font FONT_TITULO = new Font(Font.HELVETICA, 16, Font.BOLD, COR_TITULO);
    private static final Font FONT_SUBTITULO = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_SUBTITULO);
    private static final Font FONT_SECAO = new Font(Font.HELVETICA, 12, Font.BOLD, COR_AZUL);
    private static final Font FONT_CABECALHO_TABELA = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font FONT_CELULA = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_TITULO);
    private static final Font FONT_CELULA_NEGRITO = new Font(Font.HELVETICA, 9, Font.BOLD, COR_TITULO);
    private static final Font FONT_RODAPE = new Font(Font.HELVETICA, 8, Font.ITALIC, COR_SUBTITULO);

    private GeradorPdf() {}

    /**
     * Evento de página para adicionar rodapé corporativo com data/hora e numeração.
     */
    private static class EventosRodapePdf extends PdfPageEventHelper {
        private final String dataEmissao = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle page = document.getPageSize();

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("THZ-LANG Engine • Documento Corporativo Gerado em " + dataEmissao, FONT_RODAPE),
                    document.leftMargin(), document.bottomMargin() - 15, 0);

            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), FONT_RODAPE),
                    page.getRight() - document.rightMargin(), document.bottomMargin() - 15, 0);
        }
    }

    public static Path gerar(Path destino, String titulo, ValorThz dados) throws Exception {
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }

        try (OutputStream out = new FileOutputStream(destino.toFile())) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new EventosRodapePdf());

            doc.open();

            // 1. Cabeçalho Principal
            Paragraph pTitulo = new Paragraph(titulo, FONT_TITULO);
            pTitulo.setSpacingAfter(2);
            doc.add(pTitulo);

            Paragraph pSub = new Paragraph("Relatório de Sistema e Governança • THZ-LANG v" + thz.lang.version.ThzVersion.ATUAL, FONT_SUBTITULO);
            pSub.setSpacingAfter(12);
            doc.add(pSub);

            // Linha divisória azul
            PdfPTable linhaDivisoria = new PdfPTable(1);
            linhaDivisoria.setWidthPercentage(100);
            PdfPCell cellLinha = new PdfPCell();
            cellLinha.setBackgroundColor(COR_AZUL);
            cellLinha.setFixedHeight(2f);
            cellLinha.setBorder(Rectangle.NO_BORDER);
            linhaDivisoria.addCell(cellLinha);
            linhaDivisoria.setSpacingAfter(14);
            doc.add(linhaDivisoria);

            // 2. Conteúdo dos Dados
            if (dados instanceof ValorThz.Registro reg) {
                renderizarRegistro(doc, reg);
            } else if (dados instanceof ValorThz.Fatia fatia) {
                renderizarFatia(doc, "Tabela de Registros", fatia);
            } else {
                Paragraph pValor = new Paragraph(String.valueOf(dados != null ? dados.formatar() : "NULO"), FONT_CELULA);
                doc.add(pValor);
            }

            doc.close();
        }

        return destino;
    }

    private static void renderizarRegistro(Document doc, ValorThz.Registro reg) throws Exception {
        Paragraph secReg = new Paragraph("Ficha Cadastral: " + reg.nomeEstrutura(), FONT_SECAO);
        secReg.setSpacingAfter(8);
        doc.add(secReg);

        // Separa campos simples de fatias (tabelas)
        List<Map.Entry<String, ValorThz>> camposSimples = new ArrayList<>();
        List<Map.Entry<String, ValorThz.Fatia>> fatias = new ArrayList<>();

        for (Map.Entry<String, ValorThz> entry : reg.campos().entrySet()) {
            if ("titulo".equalsIgnoreCase(entry.getKey())) continue;
            if (entry.getValue() instanceof ValorThz.Fatia f) {
                fatias.add(Map.entry(entry.getKey(), f));
            } else {
                camposSimples.add(entry);
            }
        }

        // Tabela de Atributos do Registro (Chave -> Valor)
        if (!camposSimples.isEmpty()) {
            PdfPTable table = new PdfPTable(new float[]{30f, 70f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(14);

            for (int i = 0; i < camposSimples.size(); i++) {
                var entry = camposSimples.get(i);
                Color bg = (i % 2 == 0) ? Color.WHITE : COR_FUNDO_ZEBRA;

                PdfPCell cNome = new PdfPCell(new Phrase(formatarRotulo(entry.getKey()), FONT_CELULA_NEGRITO));
                cNome.setBackgroundColor(bg);
                cNome.setPadding(6);
                cNome.setBorderColor(COR_BORDA);

                String valorTxt = entry.getValue() != null ? entry.getValue().formatar() : "—";
                PdfPCell cVal = new PdfPCell(new Phrase(valorTxt, FONT_CELULA));
                cVal.setBackgroundColor(bg);
                cVal.setPadding(6);
                cVal.setBorderColor(COR_BORDA);

                table.addCell(cNome);
                table.addCell(cVal);
            }

            doc.add(table);
        }

        // Fatias como tabelas detalhadas
        for (var fatiaEntry : fatias) {
            renderizarFatia(doc, formatarRotulo(fatiaEntry.getKey()), fatiaEntry.getValue());
        }
    }

    private static void renderizarFatia(Document doc, String rotulo, ValorThz.Fatia fatia) throws Exception {
        Paragraph secFatia = new Paragraph(rotulo, FONT_SECAO);
        secFatia.setSpacingBefore(8);
        secFatia.setSpacingAfter(6);
        doc.add(secFatia);

        if (fatia.elementos().isEmpty()) {
            Paragraph pVazio = new Paragraph("(Nenhum registro encontrado)", FONT_SUBTITULO);
            pVazio.setSpacingAfter(10);
            doc.add(pVazio);
            return;
        }

        // Extrai colunas
        List<String> colunas = new ArrayList<>();
        if (fatia.elementos().get(0) instanceof ValorThz.Registro r1) {
            colunas.addAll(r1.campos().keySet());
        } else {
            colunas.add("Item");
        }

        PdfPTable table = new PdfPTable(colunas.size());
        table.setWidthPercentage(100);
        table.setSpacingAfter(14);

        // Cabeçalhos
        for (String col : colunas) {
            PdfPCell cHeader = new PdfPCell(new Phrase(formatarRotulo(col), FONT_CABECALHO_TABELA));
            cHeader.setBackgroundColor(COR_FUNDO_CABECALHO);
            cHeader.setPadding(6);
            cHeader.setBorderColor(COR_FUNDO_CABECALHO);
            table.addCell(cHeader);
        }

        // Linhas de dados
        for (int rowIdx = 0; rowIdx < fatia.elementos().size(); rowIdx++) {
            ValorThz item = fatia.elementos().get(rowIdx);
            Color bg = (rowIdx % 2 == 0) ? Color.WHITE : COR_FUNDO_ZEBRA;

            if (item instanceof ValorThz.Registro reg) {
                for (String col : colunas) {
                    ValorThz v = reg.campos().get(col);
                    String txt = v != null ? v.formatar() : "";
                    PdfPCell cell = new PdfPCell(new Phrase(txt, FONT_CELULA));
                    cell.setBackgroundColor(bg);
                    cell.setPadding(5);
                    cell.setBorderColor(COR_BORDA);
                    table.addCell(cell);
                }
            } else {
                PdfPCell cell = new PdfPCell(new Phrase(item != null ? item.formatar() : "", FONT_CELULA));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorderColor(COR_BORDA);
                table.addCell(cell);
            }
        }

        doc.add(table);
    }

    private static String formatarRotulo(String camelOrSnake) {
        if (camelOrSnake == null || camelOrSnake.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        char[] chars = camelOrSnake.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '_') {
                sb.append(' ');
            } else if (Character.isUpperCase(c) && i > 0 && Character.isLowerCase(chars[i - 1])) {
                sb.append(' ').append(c);
            } else if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
