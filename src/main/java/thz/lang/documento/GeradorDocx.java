package thz.lang.documento;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import thz.lang.interpretador.ValorThz;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gerador de documentos Word (.docx) corporativos com títulos, tabelas e estilização.
 */
public final class GeradorDocx {

    private static final String COR_TITULO = "18181B";    // Zinc 900
    private static final String COR_SUBTITULO = "71717A"; // Zinc 500
    private static final String COR_AZUL = "2563EB";      // Blue 600
    private static final String COR_FUNDO_HEADER = "27272A"; // Zinc 800
    private static final String COR_ZEBRA = "F4F4F5";     // Zinc 100
    private static final String COR_BORDA = "E4E4E7";     // Zinc 200

    private GeradorDocx() {}

    public static Path gerar(Path destino, String titulo, ValorThz dados) throws Exception {
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }

        try (XWPFDocument doc = new XWPFDocument()) {
            // Margens da página (1 polegada = 1440 twips)
            CTSectPr sectPr = doc.getDocument().getBody().addNewSectPr();
            CTPageMar pageMar = sectPr.addNewPgMar();
            pageMar.setLeft(BigInteger.valueOf(1080));
            pageMar.setRight(BigInteger.valueOf(1080));
            pageMar.setTop(BigInteger.valueOf(1080));
            pageMar.setBottom(BigInteger.valueOf(1080));

            // 1. Título do Documento
            XWPFParagraph pTitulo = doc.createParagraph();
            pTitulo.setSpacingAfter(60);
            XWPFRun rTitulo = pTitulo.createRun();
            rTitulo.setText(titulo != null && !titulo.isBlank() ? titulo : "Relatório Corporativo");
            rTitulo.setFontFamily("Segoe UI");
            rTitulo.setFontSize(18);
            rTitulo.setBold(true);
            rTitulo.setColor(COR_TITULO);

            // 2. Subtítulo e Metadados
            XWPFParagraph pSub = doc.createParagraph();
            pSub.setSpacingAfter(180);
            XWPFRun rSub = pSub.createRun();
            String dataEmissao = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            rSub.setText("THZ-LANG Engine v2.3 • Documento Corporativo Gerado em " + dataEmissao);
            rSub.setFontFamily("Segoe UI");
            rSub.setFontSize(9);
            rSub.setColor(COR_SUBTITULO);

            // 3. Conteúdo dos Dados
            if (dados instanceof ValorThz.Registro reg) {
                renderizarRegistro(doc, reg);
            } else if (dados instanceof ValorThz.Fatia fatia) {
                renderizarFatia(doc, "Tabela de Registros", fatia);
            } else {
                XWPFParagraph pVal = doc.createParagraph();
                XWPFRun rVal = pVal.createRun();
                rVal.setText(dados != null ? dados.formatar() : "NULO");
                rVal.setFontFamily("Segoe UI");
                rVal.setFontSize(11);
            }

            try (OutputStream out = new FileOutputStream(destino.toFile())) {
                doc.write(out);
            }
        }

        return destino;
    }

    private static void renderizarRegistro(XWPFDocument doc, ValorThz.Registro reg) {
        criarSecao(doc, "Ficha Cadastral: " + reg.nomeEstrutura());

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

        if (!camposSimples.isEmpty()) {
            XWPFTable table = doc.createTable(camposSimples.size() + 1, 2);
            formatarTabelaGeral(table);

            // Header
            XWPFTableRow rHeader = table.getRow(0);
            configurarCelulaHeader(rHeader.getCell(0), "Propriedade", 3200);
            configurarCelulaHeader(rHeader.getCell(1), "Valor", 6000);

            // Rows
            for (int i = 0; i < camposSimples.size(); i++) {
                var entry = camposSimples.get(i);
                XWPFTableRow row = table.getRow(i + 1);
                boolean zebra = (i % 2 != 0);

                configurarCelulaDados(row.getCell(0), formatarRotulo(entry.getKey()), true, zebra, 3200);
                String txt = entry.getValue() != null ? entry.getValue().formatar() : "—";
                configurarCelulaDados(row.getCell(1), txt, false, zebra, 6000);
            }

            doc.createParagraph().setSpacingAfter(120);
        }

        for (var fatiaEntry : fatias) {
            renderizarFatia(doc, formatarRotulo(fatiaEntry.getKey()), fatiaEntry.getValue());
        }
    }

    private static void renderizarFatia(XWPFDocument doc, String rotulo, ValorThz.Fatia fatia) {
        criarSecao(doc, rotulo);

        if (fatia.elementos().isEmpty()) {
            XWPFParagraph pVazio = doc.createParagraph();
            XWPFRun rVazio = pVazio.createRun();
            rVazio.setText("(Nenhum registro)");
            rVazio.setItalic(true);
            rVazio.setFontSize(10);
            rVazio.setColor(COR_SUBTITULO);
            return;
        }

        List<String> colunas = new ArrayList<>();
        if (fatia.elementos().get(0) instanceof ValorThz.Registro r1) {
            colunas.addAll(r1.campos().keySet());
        } else {
            colunas.add("Item");
        }

        int totalColunas = colunas.size();
        int larguraCol = Math.max(1200, 9200 / totalColunas);

        XWPFTable table = doc.createTable(fatia.elementos().size() + 1, totalColunas);
        formatarTabelaGeral(table);

        // Header
        XWPFTableRow rHeader = table.getRow(0);
        for (int c = 0; c < totalColunas; c++) {
            configurarCelulaHeader(rHeader.getCell(c), formatarRotulo(colunas.get(c)), larguraCol);
        }

        // Data Rows
        for (int rIdx = 0; rIdx < fatia.elementos().size(); rIdx++) {
            ValorThz item = fatia.elementos().get(rIdx);
            XWPFTableRow row = table.getRow(rIdx + 1);
            boolean zebra = (rIdx % 2 != 0);

            if (item instanceof ValorThz.Registro reg) {
                for (int c = 0; c < totalColunas; c++) {
                    ValorThz v = reg.campos().get(colunas.get(c));
                    configurarCelulaDados(row.getCell(c), v != null ? v.formatar() : "", false, zebra, larguraCol);
                }
            } else {
                configurarCelulaDados(row.getCell(0), item != null ? item.formatar() : "", false, zebra, larguraCol);
            }
        }

        doc.createParagraph().setSpacingAfter(120);
    }

    private static void criarSecao(XWPFDocument doc, String titulo) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120);
        p.setSpacingAfter(60);
        XWPFRun r = p.createRun();
        r.setText(titulo);
        r.setFontFamily("Segoe UI");
        r.setFontSize(13);
        r.setBold(true);
        r.setColor(COR_AZUL);
    }

    private static void formatarTabelaGeral(XWPFTable table) {
        table.setWidth("100%");
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) tblPr = table.getCTTbl().addNewTblPr();
        CTTblBorders borders = tblPr.addNewTblBorders();
        definirBordaTbl(borders.addNewTop());
        definirBordaTbl(borders.addNewBottom());
        definirBordaTbl(borders.addNewLeft());
        definirBordaTbl(borders.addNewRight());
        definirBordaTbl(borders.addNewInsideH());
        definirBordaTbl(borders.addNewInsideV());
    }

    private static void definirBordaTbl(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(4));
        border.setColor(COR_BORDA);
    }

    private static void configurarCelulaHeader(XWPFTableCell cell, String texto, int larguraTwips) {
        cell.setWidth(String.valueOf(larguraTwips));
        cell.setColor(COR_FUNDO_HEADER);
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

        XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        p.setSpacingBefore(40);
        p.setSpacingAfter(40);
        XWPFRun r = p.createRun();
        r.setText(texto);
        r.setFontFamily("Segoe UI");
        r.setFontSize(10);
        r.setBold(true);
        r.setColor("FFFFFF");
    }

    private static void configurarCelulaDados(XWPFTableCell cell, String texto, boolean negrito, boolean zebra, int larguraTwips) {
        cell.setWidth(String.valueOf(larguraTwips));
        if (zebra) {
            cell.setColor(COR_ZEBRA);
        }
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

        XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        p.setSpacingBefore(30);
        p.setSpacingAfter(30);
        XWPFRun r = p.createRun();
        r.setText(texto);
        r.setFontFamily("Segoe UI");
        r.setFontSize(9);
        r.setBold(negrito);
        r.setColor(COR_TITULO);
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
