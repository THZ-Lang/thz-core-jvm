package thz.lang.documento;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import thz.lang.interpretador.ValorThz;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gerador de planilhas Excel (.xlsx) com estilos corporativos e auto-ajuste de colunas.
 */
public final class GeradorXlsx {

    private GeradorXlsx() {}

    public static Path gerar(Path destino, String nomePlanilha, ValorThz dados) throws Exception {
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            DataFormat dataFormat = workbook.createDataFormat();

            // Estilos
            CellStyle estiloHeader = criarEstiloCabecalho(workbook);
            CellStyle estiloTexto = criarEstiloTexto(workbook);
            CellStyle estiloZebraTexto = criarEstiloZebra(workbook, estiloTexto);
            CellStyle estiloDecimal = criarEstiloNumero(workbook, dataFormat.getFormat("#,##0.00"), HorizontalAlignment.RIGHT);
            CellStyle estiloZebraDecimal = criarEstiloZebra(workbook, estiloDecimal);
            CellStyle estiloInteiro = criarEstiloNumero(workbook, dataFormat.getFormat("#,##0"), HorizontalAlignment.RIGHT);
            CellStyle estiloZebraInteiro = criarEstiloZebra(workbook, estiloInteiro);
            CellStyle estiloCentralizado = criarEstiloAlinhado(workbook, HorizontalAlignment.CENTER);
            CellStyle estiloZebraCentralizado = criarEstiloZebra(workbook, estiloCentralizado);

            if (dados instanceof ValorThz.Registro reg) {
                renderizarRegistro(workbook, reg, nomePlanilha, estiloHeader, estiloTexto, estiloZebraTexto, estiloDecimal, estiloZebraDecimal, estiloInteiro, estiloZebraInteiro, estiloCentralizado, estiloZebraCentralizado);
            } else if (dados instanceof ValorThz.Fatia fatia) {
                String nomeAba = sanitizarNomeAba(nomePlanilha != null && !nomePlanilha.isBlank() ? nomePlanilha : "Dados");
                Sheet sheet = workbook.createSheet(nomeAba);
                renderizarFatiaEmSheet(sheet, fatia, estiloHeader, estiloTexto, estiloZebraTexto, estiloDecimal, estiloZebraDecimal, estiloInteiro, estiloZebraInteiro, estiloCentralizado, estiloZebraCentralizado);
            } else {
                Sheet sheet = workbook.createSheet("Resumo");
                Row r = sheet.createRow(0);
                r.createCell(0).setCellValue("Valor");
                r.getCell(0).setCellStyle(estiloHeader);
                Row r1 = sheet.createRow(1);
                r1.createCell(0).setCellValue(dados != null ? dados.formatar() : "NULO");
                r1.getCell(0).setCellStyle(estiloTexto);
                sheet.autoSizeColumn(0);
            }

            try (OutputStream out = new FileOutputStream(destino.toFile())) {
                workbook.write(out);
            }
        }

        return destino;
    }

    private static void renderizarRegistro(XSSFWorkbook wb, ValorThz.Registro reg, String nomePlanilha,
                                          CellStyle header, CellStyle txt, CellStyle zTxt,
                                          CellStyle dec, CellStyle zDec, CellStyle num, CellStyle zNum,
                                          CellStyle center, CellStyle zCenter) {
        String nomePrincipal = sanitizarNomeAba(nomePlanilha != null && !nomePlanilha.isBlank() ? nomePlanilha : reg.nomeEstrutura());
        Sheet sheetPrincipal = wb.createSheet(nomePrincipal);

        List<Map.Entry<String, ValorThz>> atributos = new ArrayList<>();
        List<Map.Entry<String, ValorThz.Fatia>> fatias = new ArrayList<>();

        for (Map.Entry<String, ValorThz> entry : reg.campos().entrySet()) {
            if ("titulo".equalsIgnoreCase(entry.getKey())) continue;
            if (entry.getValue() instanceof ValorThz.Fatia f) {
                fatias.add(Map.entry(entry.getKey(), f));
            } else {
                atributos.add(entry);
            }
        }

        // Cabeçalho da Ficha
        Row rHeader = sheetPrincipal.createRow(0);
        Cell cH1 = rHeader.createCell(0);
        cH1.setCellValue("Propriedade");
        cH1.setCellStyle(header);
        Cell cH2 = rHeader.createCell(1);
        cH2.setCellValue("Valor");
        cH2.setCellStyle(header);

        int rowNum = 1;
        for (var entry : atributos) {
            Row r = sheetPrincipal.createRow(rowNum++);
            boolean zebra = (rowNum % 2 == 0);

            Cell c1 = r.createCell(0);
            c1.setCellValue(formatarRotulo(entry.getKey()));
            c1.setCellStyle(zebra ? zTxt : txt);

            Cell c2 = r.createCell(1);
            preencherValorCelula(c2, entry.getValue(), zebra, txt, zTxt, dec, zDec, num, zNum, center, zCenter);
        }

        sheetPrincipal.autoSizeColumn(0);
        sheetPrincipal.autoSizeColumn(1);

        // Abas adicionais para cada fatia
        for (var fEntry : fatias) {
            String abaNome = sanitizarNomeAba(formatarRotulo(fEntry.getKey()));
            Sheet fSheet = wb.createSheet(abaNome);
            renderizarFatiaEmSheet(fSheet, fEntry.getValue(), header, txt, zTxt, dec, zDec, num, zNum, center, zCenter);
        }
    }

    private static void renderizarFatiaEmSheet(Sheet sheet, ValorThz.Fatia fatia,
                                               CellStyle header, CellStyle txt, CellStyle zTxt,
                                               CellStyle dec, CellStyle zDec, CellStyle num, CellStyle zNum,
                                               CellStyle center, CellStyle zCenter) {
        if (fatia.elementos().isEmpty()) {
            Row r = sheet.createRow(0);
            r.createCell(0).setCellValue("(Nenhum registro)");
            r.getCell(0).setCellStyle(txt);
            sheet.autoSizeColumn(0);
            return;
        }

        List<String> colunas = new ArrayList<>();
        if (fatia.elementos().get(0) instanceof ValorThz.Registro r1) {
            colunas.addAll(r1.campos().keySet());
        } else {
            colunas.add("Item");
        }

        // Header Row
        Row headerRow = sheet.createRow(0);
        for (int c = 0; c < colunas.size(); c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(formatarRotulo(colunas.get(c)));
            cell.setCellStyle(header);
        }

        // Data Rows
        int rowIdx = 1;
        for (ValorThz item : fatia.elementos()) {
            Row row = sheet.createRow(rowIdx++);
            boolean zebra = (rowIdx % 2 == 0);

            if (item instanceof ValorThz.Registro rItem) {
                for (int c = 0; c < colunas.size(); c++) {
                    Cell cell = row.createCell(c);
                    ValorThz v = rItem.campos().get(colunas.get(c));
                    preencherValorCelula(cell, v, zebra, txt, zTxt, dec, zDec, num, zNum, center, zCenter);
                }
            } else {
                Cell cell = row.createCell(0);
                preencherValorCelula(cell, item, zebra, txt, zTxt, dec, zDec, num, zNum, center, zCenter);
            }
        }

        for (int c = 0; c < colunas.size(); c++) {
            sheet.autoSizeColumn(c);
            // Dá um respiro extra de largura
            sheet.setColumnWidth(c, Math.min(sheet.getColumnWidth(c) + 1200, 20000));
        }
    }

    private static void preencherValorCelula(Cell cell, ValorThz v, boolean zebra,
                                            CellStyle txt, CellStyle zTxt,
                                            CellStyle dec, CellStyle zDec,
                                            CellStyle num, CellStyle zNum,
                                            CellStyle center, CellStyle zCenter) {
        if (v == null) {
            cell.setCellValue("");
            cell.setCellStyle(zebra ? zTxt : txt);
            return;
        }

        if (v instanceof ValorThz.Inteiro i) {
            cell.setCellValue(i.valor().doubleValue());
            cell.setCellStyle(zebra ? zNum : num);
        } else if (v instanceof ValorThz.Decimal d) {
            try {
                cell.setCellValue(Double.parseDouble(d.valor().formatar()));
                cell.setCellStyle(zebra ? zDec : dec);
            } catch (Exception e) {
                cell.setCellValue(d.formatar());
                cell.setCellStyle(zebra ? zTxt : txt);
            }
        } else if (v instanceof ValorThz.Logico l) {
            cell.setCellValue(l.valor() ? "SIM" : "NÃO");
            cell.setCellStyle(zebra ? zCenter : center);
        } else {
            cell.setCellValue(v.formatar());
            cell.setCellStyle(zebra ? zTxt : txt);
        }
    }

    private static CellStyle criarEstiloCabecalho(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 37, (byte) 99, (byte) 235}, null)); // Blue 600
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        definirBordas(style, IndexedColors.GREY_40_PERCENT.getIndex());
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle criarEstiloTexto(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        definirBordas(style, IndexedColors.GREY_25_PERCENT.getIndex());
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle criarEstiloAlinhado(XSSFWorkbook wb, HorizontalAlignment align) {
        CellStyle style = criarEstiloTexto(wb);
        style.setAlignment(align);
        return style;
    }

    private static CellStyle criarEstiloNumero(XSSFWorkbook wb, short format, HorizontalAlignment align) {
        CellStyle style = criarEstiloTexto(wb);
        style.setDataFormat(format);
        style.setAlignment(align);
        return style;
    }

    private static CellStyle criarEstiloZebra(XSSFWorkbook wb, CellStyle base) {
        XSSFCellStyle z = wb.createCellStyle();
        z.cloneStyleFrom(base);
        z.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 244, (byte) 244, (byte) 245}, null)); // Zinc 100
        z.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return z;
    }

    private static void definirBordas(CellStyle style, short color) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(color);
        style.setBottomBorderColor(color);
        style.setLeftBorderColor(color);
        style.setRightBorderColor(color);
    }

    private static String sanitizarNomeAba(String nome) {
        if (nome == null || nome.isBlank()) return "Planilha1";
        String s = nome.replaceAll("[\\\\/*?\\[\\]:]", "_").trim();
        return s.length() > 31 ? s.substring(0, 31) : s;
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
