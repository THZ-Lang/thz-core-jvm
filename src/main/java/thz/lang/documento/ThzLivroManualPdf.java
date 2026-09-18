package thz.lang.documento;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDestination;
import com.lowagie.text.pdf.PdfOutline;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import thz.lang.lexico.DialetoLinguagem;
import thz.lang.version.ThzVersion;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Compilador Editorial de Livro-Manual em PDF.
 * Suporta compilação bilíngue completa (Português PT-BR e Inglês EN-US),
 * paginação real calculada em 2 passagens e renderização tipográfica profissional.
 */
public final class ThzLivroManualPdf {

    // Paleta Editorial Corporativa (Slate & Blue)
    private static final Color COR_CAPA_FUNDO = new Color(15, 23, 42);       // Slate 900
    private static final Color COR_ACENTO = new Color(37, 99, 235);          // Blue 600
    private static final Color COR_ACENTO_CLARO = new Color(96, 165, 250);   // Blue 400
    private static final Color COR_TEXTO = new Color(30, 41, 59);            // Slate 800
    private static final Color COR_TEXTO_MUTED = new Color(100, 116, 139);   // Slate 500
    private static final Color COR_FUNDO_CODIGO = new Color(241, 245, 249);  // Slate 100
    private static final Color COR_BORDA_CODIGO = new Color(203, 213, 225);  // Slate 300
    private static final Color COR_HEADER_TABELA = new Color(30, 41, 59);    // Slate 800
    private static final Color COR_ZEBRA_TABELA = new Color(248, 250, 252);   // Slate 50

    // Fontes
    private static final Font FONT_CAPA_SUPER = new Font(Font.HELVETICA, 10, Font.BOLD, COR_ACENTO_CLARO);
    private static final Font FONT_CAPA_TITULO = new Font(Font.HELVETICA, 24, Font.BOLD, Color.WHITE);
    private static final Font FONT_CAPA_SUB = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(226, 232, 240));
    private static final Font FONT_CAPA_CORPO = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, new Color(148, 163, 184));
    private static final Font FONT_CAPA_META = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, new Color(148, 163, 184));

    private static final Font FONT_TOC_TITULO = new Font(Font.HELVETICA, 16, Font.BOLD, COR_ACENTO);
    private static final Font FONT_TOC_SECAO = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(15, 23, 42));
    private static final Font FONT_TOC_ITEM = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, COR_TEXTO);
    private static final Font FONT_TOC_PAGINA = new Font(Font.HELVETICA, 9.5f, Font.BOLD, COR_ACENTO);

    private static final Font FONT_PARTE_TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, COR_ACENTO);

    private static final Font FONT_H1 = new Font(Font.HELVETICA, 15, Font.BOLD, COR_ACENTO);
    private static final Font FONT_H2 = new Font(Font.HELVETICA, 12.5f, Font.BOLD, new Color(15, 23, 42));
    private static final Font FONT_H3 = new Font(Font.HELVETICA, 10.5f, Font.BOLD, new Color(51, 65, 85));
    private static final Font FONT_H4 = new Font(Font.HELVETICA, 9.5f, Font.BOLD, new Color(71, 85, 105));

    private static final Font FONT_CORPO = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, COR_TEXTO);
    private static final Font FONT_CORPO_BOLD = new Font(Font.HELVETICA, 9.5f, Font.BOLD, COR_TEXTO);
    private static final Font FONT_CORPO_ITALIC = new Font(Font.HELVETICA, 9.5f, Font.ITALIC, COR_TEXTO);
    private static final Font FONT_CORPO_CODE = new Font(Font.COURIER, 8.5f, Font.NORMAL, new Color(15, 23, 42));
    private static final Font FONT_CORPO_LINK = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, COR_ACENTO);

    private static final Font FONT_CODIGO_BLOCO = new Font(Font.COURIER, 8.5f, Font.NORMAL, new Color(15, 23, 42));
    private static final Font FONT_CODIGO_LANG = new Font(Font.HELVETICA, 7.5f, Font.BOLD, Color.WHITE);

    private static final Font FONT_TABELA_HEAD = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font FONT_TABELA_ROW = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, COR_TEXTO);

    private static final Font FONT_CALLOUT_HEAD = new Font(Font.HELVETICA, 9, Font.BOLD, COR_TEXTO);
    private static final Font FONT_RODAPE = new Font(Font.HELVETICA, 8, Font.NORMAL, COR_TEXTO_MUTED);

    public record CapituloManual(String id, String titulo, String parte, Path arquivo, int ordem) {
        public String nomeArquivo() {
            return arquivo != null ? arquivo.getFileName().toString() : "";
        }
    }

    public static class IndiceCapitulo {
        public CapituloManual capitulo;
        public int paginaInicio = 1;

        public IndiceCapitulo(CapituloManual capitulo) {
            this.capitulo = capitulo;
        }
    }

    /**
     * Controlador de cabeçalho e rodapé em páginas do manual.
     */
    private static class EventosManualPdf extends PdfPageEventHelper {
        private String tituloAtual = "";
        private boolean suprimirCabecalhoRodape = false;
        private final DialetoLinguagem dialeto;

        public EventosManualPdf(DialetoLinguagem dialeto) {
            this.dialeto = dialeto;
        }

        public void setTituloAtual(String tituloAtual) {
            this.tituloAtual = tituloAtual;
        }

        public void setSuprimir(boolean suprimir) {
            this.suprimirCabecalhoRodape = suprimir;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (suprimirCabecalhoRodape || writer.getPageNumber() == 1) return;

            PdfContentByte cb = writer.getDirectContent();
            Rectangle page = document.getPageSize();
            boolean en = (dialeto == DialetoLinguagem.EN_US);

            // Cabeçalho Superior
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase((en ? "THZ-LANG Engine • " : "Motor THZ-LANG • ") + tituloAtual, FONT_RODAPE),
                    document.leftMargin(), page.getTop() - document.topMargin() + 10, 0);

            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(en ? "Official Engineering Manual" : "Manual Oficial de Engenharia", FONT_RODAPE),
                    page.getRight() - document.rightMargin(), page.getTop() - document.topMargin() + 10, 0);

            cb.setColorStroke(COR_BORDA_CODIGO);
            cb.setLineWidth(0.5f);
            cb.moveTo(document.leftMargin(), page.getTop() - document.topMargin() + 5);
            cb.lineTo(page.getRight() - document.rightMargin(), page.getTop() - document.topMargin() + 5);
            cb.stroke();

            // Rodapé Inferior
            String txtRodape = en
                    ? "Version v" + ThzVersion.ATUAL + " • ISO/IEC 10967 & RFC 4122 Standard"
                    : "Versão v" + ThzVersion.ATUAL + " • Conformidade ISO/IEC 10967 & RFC 4122";

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(txtRodape, FONT_RODAPE),
                    document.leftMargin(), document.bottomMargin() - 15, 0);

            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(String.valueOf(writer.getPageNumber()), FONT_RODAPE),
                    page.getRight() - document.rightMargin(), document.bottomMargin() - 15, 0);

            cb.moveTo(document.leftMargin(), document.bottomMargin() - 5);
            cb.lineTo(page.getRight() - document.rightMargin(), document.bottomMargin() - 5);
            cb.stroke();
        }
    }

    /**
     * Gera todos os manuais (Português, Inglês e padrão).
     */
    public static List<Path> gerarTodosManuais(Path raizWorkspace, Path dirDestino) throws Exception {
        if (!Files.exists(dirDestino)) {
            Files.createDirectories(dirDestino);
        }
        Path pdfPt = dirDestino.resolve("MANUAL_THZ_LANG_PT.pdf");
        Path pdfEn = dirDestino.resolve("MANUAL_THZ_LANG_EN.pdf");
        Path pdfPadrao = dirDestino.resolve("MANUAL_THZ_LANG.pdf");

        gerarLivroManual(raizWorkspace, pdfPt, DialetoLinguagem.PT_BR);
        gerarLivroManual(raizWorkspace, pdfEn, DialetoLinguagem.EN_US);

        // Copia a versão primária
        Files.copy(pdfPt, pdfPadrao, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        return List.of(pdfPt, pdfEn, pdfPadrao);
    }

    public static Path gerarLivroManual(Path raizWorkspace, Path destinoPdf) throws Exception {
        return gerarLivroManual(raizWorkspace, destinoPdf, DialetoLinguagem.PT_BR);
    }

    /**
     * Compila todos os arquivos .md em um Livro-Manual encadernado em PDF.
     */
    public static Path gerarLivroManual(Path raizWorkspace, Path destinoPdf, DialetoLinguagem dialeto) throws Exception {
        if (destinoPdf.getParent() != null) {
            Files.createDirectories(destinoPdf.getParent());
        }

        List<CapituloManual> capitulos = descobrirCapitulosDinamicamente(raizWorkspace, dialeto);
        List<IndiceCapitulo> indices = new ArrayList<>();
        for (CapituloManual c : capitulos) {
            indices.add(new IndiceCapitulo(c));
        }

        // Passagem 1: Renderiza em memória para calcular o número exato de páginas de cada capítulo
        int paginasSumarioEstimadas = estimarPaginasSumario(capitulos);
        executarRenderizacao(raizWorkspace, capitulos, indices, paginasSumarioEstimadas, null, dialeto);

        // Passagem 2: Renderiza para o arquivo final com os números de página reais gravados no sumário
        try (OutputStream out = new FileOutputStream(destinoPdf.toFile())) {
            executarRenderizacao(raizWorkspace, capitulos, indices, paginasSumarioEstimadas, out, dialeto);
        }

        return destinoPdf;
    }

    private static int estimarPaginasSumario(List<CapituloManual> capitulos) {
        int linhas = capitulos.size() * 2 + 10;
        return Math.max(1, (int) Math.ceil(linhas / 35.0));
    }

    private static void executarRenderizacao(Path raiz, List<CapituloManual> capitulos,
                                             List<IndiceCapitulo> indices,
                                             int paginasSumario,
                                             OutputStream outReal,
                                             DialetoLinguagem dialeto) throws Exception {
        OutputStream out = outReal != null ? outReal : new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 38, 38, 42, 42);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        EventosManualPdf eventos = new EventosManualPdf(dialeto);
        writer.setPageEvent(eventos);

        doc.open();

        // 1. Capa Oficial
        eventos.setSuprimir(true);
        renderizarCapa(doc, dialeto);
        eventos.setSuprimir(false);
        doc.newPage();

        // 2. Sumário Inteligente
        eventos.setSuprimir(true);
        renderizarSumarioComPaginasReais(doc, indices, paginasSumario, dialeto);
        eventos.setSuprimir(false);
        doc.newPage();

        // 3. Renderização Sequencial dos Capítulos
        String parteAtual = "";
        PdfOutline raizOutline = writer.getRootOutline();

        for (int i = 0; i < capitulos.size(); i++) {
            CapituloManual cap = capitulos.get(i);
            IndiceCapitulo idx = indices.get(i);

            if (!cap.parte().equals(parteAtual)) {
                parteAtual = cap.parte();
                eventos.setSuprimir(true);
                renderizarDivisoriaParte(doc, parteAtual, dialeto);
                eventos.setSuprimir(false);
                doc.newPage();
            }

            // Grava a página de início do capítulo para o sumário real
            idx.paginaInicio = writer.getPageNumber();

            eventos.setTituloAtual(cap.titulo());

            // Outline / Bookmark no leitor de PDF
            if (raizOutline != null) {
                new PdfOutline(raizOutline,
                        new PdfDestination(PdfDestination.FITH, 0),
                        cap.titulo(), true);
            }

            renderizarCapituloMarkdown(doc, cap, dialeto);
            doc.newPage();
        }

        doc.close();
    }

    public static List<CapituloManual> descobrirCapitulos(Path raiz) {
        return descobrirCapitulosDinamicamente(raiz, DialetoLinguagem.PT_BR);
    }

    /**
     * Descobre e organiza dinamicamente todos os documentos .md do projeto.
     */
    public static List<CapituloManual> descobrirCapitulosDinamicamente(Path raiz, DialetoLinguagem dialeto) {
        List<CapituloManual> lista = new ArrayList<>();
        Path docs = raiz.resolve("docs");
        boolean en = (dialeto == DialetoLinguagem.EN_US);

        String p1 = en ? "Part I: Overview & Foundations" : "Parte I: Visão Geral e Fundamentos";
        String p2 = en ? "Part II: Language Manual & Specification" : "Parte II: Manual da Linguagem & Especificação";
        String p3 = en ? "Part III: Engineering, Quality & Standards" : "Parte III: Engenharia, Qualidade & Normas";
        String p4 = en ? "Part IV: Architecture, Compilation & Performance" : "Parte IV: Arquitetura, Compilação & Performance";
        String p5 = en ? "Part V: Tooling, IDE & Ecosystem" : "Parte V: Ferramentas, IDE & Ecossistema";
        String p6 = en ? "Part VI: Architectural Decision Records (ADRs)" : "Parte VI: Decisões Arquiteturais (ADRs)";
        String p7 = en ? "Part VII: Supplementary Documentation" : "Parte VII: Documentação Complementar";

        // 1. Arquivos da Raiz & Fundamentos
        adicionarDocumento(lista, raiz, "README.md", "README.md", p1, 10, dialeto);
        adicionarDocumento(lista, raiz, "CONTRIBUTING.md", "CONTRIBUTING.md", p1, 12, dialeto);
        adicionarDocumento(lista, raiz, "PROJECT.md", "PROJECT.md", p1, 14, dialeto);
        adicionarDocumento(lista, raiz, "GLOSSARIO_LINGUAGEM_UBIQUA.md", "UBIQUITOUS_LANGUAGE_GLOSSARY.md", p1, 16, dialeto);

        // 2. Linguagem e Especificação
        adicionarDocumento(lista, raiz, "MANUAL_LINGUAGEM.md", "MANUAL_LANGUAGE.md", p2, 20, dialeto);
        adicionarDocumento(lista, raiz, "GRAMATICA.md", "GRAMMAR.md", p2, 22, dialeto);
        adicionarDocumento(lista, raiz, "EXEMPLOS_E_PADROES.md", "EXAMPLES_AND_PATTERNS.md", p2, 24, dialeto);

        // 3. Engenharia, Normas e Qualidade
        adicionarDocumento(lista, raiz, "CONFORMIDADE_E_NORMAS.md", "COMPLIANCE_AND_STANDARDS.md", p3, 30, dialeto);
        adicionarDocumento(lista, raiz, "DIRETRIZES_QUALIDADE.md", "QUALITY_GUIDELINES.md", p3, 32, dialeto);
        adicionarDocumento(lista, raiz, "TESTES_E_BENCHMARKS.md", "TESTS_AND_BENCHMARKS.md", p3, 34, dialeto);

        // 4. Arquitetura, Compilação e Performance
        adicionarDocumento(lista, raiz, "ARQUITETURA_COMPILACAO_NATIVA.md", "NATIVE_COMPILATION_ARCHITECTURE.md", p4, 40, dialeto);
        adicionarDocumento(lista, raiz, "RUNTIME_NATIVO.md", "NATIVE_RUNTIME.md", p4, 42, dialeto);
        adicionarDocumento(lista, raiz, "SELF_HOSTING.md", "SELF_HOSTING.md", p4, 44, dialeto);
        adicionarDocumento(lista, raiz, "PIPELINE_DADOS.md", "DATA_PIPELINE.md", p4, 46, dialeto);
        adicionarDocumento(lista, raiz, "GUIA_PERFORMANCE.md", "PERFORMANCE_GUIDE.md", p4, 48, dialeto);

        // 5. Tooling, IDE e Ecossistema
        adicionarDocumento(lista, raiz, "CLI_E_TOOLING.md", "CLI_AND_TOOLING.md", p5, 50, dialeto);
        adicionarDocumento(lista, raiz, "TELA_THZUI.md", "THZUI_SCREEN.md", p5, 52, dialeto);
        adicionarDocumento(lista, raiz, "LSP_VSCODE.md", "LSP_VSCODE.md", p5, 54, dialeto);
        adicionarDocumento(lista, raiz, "API_REST.md", "API_REST.md", p5, 56, dialeto);
        adicionarDocumento(lista, raiz, "DEPLOYMENT.md", "DEPLOYMENT.md", p5, 58, dialeto);
        adicionarDocumento(lista, raiz, "INTELLIJ_SETUP.md", "INTELLIJ_SETUP.md", p5, 60, dialeto);
        adicionarDocumento(lista, raiz, "TROUBLESHOOTING.md", "TROUBLESHOOTING.md", p5, 62, dialeto);

        // 6. Architectural Decision Records (ADRs)
        Path adrDir = (dialeto == DialetoLinguagem.EN_US && Files.isDirectory(docs.resolve("en").resolve("ADRs")))
                ? docs.resolve("en").resolve("ADRs")
                : docs.resolve("ADRs");
        if (Files.isDirectory(adrDir)) {
            try (Stream<Path> stream = Files.list(adrDir)) {
                List<Path> adrs = stream
                        .filter(p -> p.toString().endsWith(".md") && !p.getFileName().toString().equalsIgnoreCase("README.md"))
                        .sorted()
                        .toList();
                int adrOrder = 70;
                for (Path p : adrs) {
                    adicionarDocumentoSimples(lista, p, p6, adrOrder++);
                }
            } catch (Exception ignored) {}
        }

        // 7. Descoberta de outros arquivos .md soltos em docs/ que não foram catalogados
        if (Files.isDirectory(docs)) {
            if (dialeto == DialetoLinguagem.EN_US) {
                Path docsEn = docs.resolve("en");
                if (Files.isDirectory(docsEn)) {
                    try (Stream<Path> stream = Files.walk(docsEn, 2)) {
                        List<Path> extras = stream
                                .filter(p -> p.toString().endsWith(".md")
                                        && !p.toString().contains("ADRs")
                                        && !p.getFileName().toString().equalsIgnoreCase("README.md"))
                                .toList();
                        for (Path extra : extras) {
                            boolean jaExiste = lista.stream().anyMatch(c -> c.arquivo().getFileName().equals(extra.getFileName()));
                            if (!jaExiste) {
                                adicionarDocumentoSimples(lista, extra, p7, 80);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } else {
                try (Stream<Path> stream = Files.walk(docs, 2)) {
                    List<Path> extras = stream
                            .filter(p -> p.toString().endsWith(".md")
                                    && !p.toString().contains("ADRs")
                                    && !p.toString().contains(java.io.File.separator + "en" + java.io.File.separator)
                                    && !p.toString().endsWith(java.io.File.separator + "en")
                                    && !p.getFileName().toString().equalsIgnoreCase("README.md"))
                            .toList();
                    for (Path extra : extras) {
                        boolean jaExiste = lista.stream().anyMatch(c -> c.arquivo().getFileName().equals(extra.getFileName()));
                        if (!jaExiste) {
                            adicionarDocumentoSimples(lista, extra, p7, 80);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        lista.sort(Comparator.comparingInt(CapituloManual::ordem));
        return lista;
    }

    private static void adicionarDocumento(List<CapituloManual> lista, Path raiz, String nomePt, String nomeEn, String parte, int ordem, DialetoLinguagem dialeto) {
        Path arqFinal = null;
        if (dialeto == DialetoLinguagem.EN_US) {
            Path docEn = raiz.resolve("docs").resolve("en").resolve(nomeEn);
            if (Files.exists(docEn)) {
                arqFinal = docEn;
            } else {
                Path docEnSame = raiz.resolve("docs").resolve("en").resolve(nomePt);
                if (Files.exists(docEnSame)) {
                    arqFinal = docEnSame;
                }
            }
        }
        if (arqFinal == null) {
            Path docPt = raiz.resolve("docs").resolve(nomePt);
            if (Files.exists(docPt)) {
                arqFinal = docPt;
            } else {
                Path docRaiz = raiz.resolve(nomePt);
                if (Files.exists(docRaiz)) {
                    arqFinal = docRaiz;
                }
            }
        }
        if (arqFinal != null && Files.exists(arqFinal)) {
            adicionarDocumentoSimples(lista, arqFinal, parte, ordem);
        }
    }

    private static void adicionarDocumentoSimples(List<CapituloManual> lista, Path arq, String parte, int ordem) {
        if (!Files.exists(arq)) return;
        String titulo = extrairTituloMarkdown(arq);
        String id = arq.getFileName().toString().replace(".md", "");
        lista.add(new CapituloManual(id, titulo, parte, arq, ordem));
    }

    private static String extrairTituloMarkdown(Path arq) {
        try {
            List<String> lines = Files.readAllLines(arq);
            for (String l : lines) {
                String trim = l.trim();
                if (trim.startsWith("# ")) {
                    return limparSintaxeMarkdown(trim.substring(2).trim());
                }
            }
        } catch (Exception ignored) {}

        String nome = arq.getFileName().toString().replace(".md", "");
        return formatarNomeLegivel(nome);
    }

    private static String formatarNomeLegivel(String raw) {
        return raw.replace("_", " ").replace("-", " ");
    }

    // =========================================================================
    // RENDERIZADORES DE ESTRUTURA
    // =========================================================================

    private static void renderizarCapa(Document doc, DialetoLinguagem dialeto) throws Exception {
        PdfPTable tabelaCapa = new PdfPTable(1);
        tabelaCapa.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COR_CAPA_FUNDO);
        cell.setPadding(35);
        cell.setMinimumHeight(670);
        cell.setBorder(Rectangle.NO_BORDER);

        boolean en = (dialeto == DialetoLinguagem.EN_US);

        String tag = en ? "OFFICIAL ENGINEERING & ARCHITECTURE MANUAL" : "MANUAL CORPORATIVO DE ENGENHARIA & ARQUITETURA";
        Paragraph pTag = new Paragraph(tag, FONT_CAPA_SUPER);
        pTag.setSpacingAfter(15);
        cell.addElement(pTag);

        Paragraph pTitulo = new Paragraph(en ? "THZ-LANG ENGINE\n& ECOSYSTEM" : "THZ-LANG ENGINE\n& ECOSSISTEMA", FONT_CAPA_TITULO);
        pTitulo.setLeading(28);
        pTitulo.setSpacingAfter(15);
        cell.addElement(pTitulo);

        String sub = en
                ? "Formal Language Specification, Domain-Driven Governance (DDD),\nLiving Architecture, and High-Performance Processing Engine."
                : "Especificação Formal da Linguagem, Governança DDD (G4),\nArquitetura Viva e Processamento de Dados de Alta Performance.";

        Paragraph pSub = new Paragraph(sub, FONT_CAPA_SUB);
        pSub.setLeading(16);
        pSub.setSpacingAfter(25);
        cell.addElement(pSub);

        // Barra decorativa azul
        PdfPTable barra = new PdfPTable(1);
        barra.setWidthPercentage(100);
        PdfPCell bCell = new PdfPCell();
        bCell.setBackgroundColor(COR_ACENTO);
        bCell.setFixedHeight(3.5f);
        bCell.setBorder(Rectangle.NO_BORDER);
        barra.addCell(bCell);
        barra.setSpacingAfter(30);
        cell.addElement(barra);

        String destaques = en
                ? "• Dual Execution Paradigm: JVM High Performance (Java 25) & Native AOT Compilation (LLVM Clang / GraalVM)\n" +
                  "• Exact Financial & Tax Arithmetic ISO/IEC 10967 (Zero Floating-Point Representation Errors)\n" +
                  "• SIMD Vectorized Data Pipeline with Ephemeral Memory Arena O(1) Allocation\n" +
                  "• Strict Design by Contract (REQUIRES / ENSURES) with Automated Architecture Auditing\n" +
                  "• Universal Cross-Platform Swing Desktop IDE with Native LSP4J & Official VS Code Extension"
                : "• Arquitetura Dual: Runtime JVM (Java 25) & Compilação Nativa AOT (LLVM Clang / GraalVM)\n" +
                  "• Aritmética Financeira e Fiscal Exata ISO/IEC 10967 (Zero Erros de Ponto Flutuante)\n" +
                  "• Pipeline de Processamento com SIMD Vetorizado e Alocação Epêmera em Arena O(1)\n" +
                  "• Design by Contract Estrito (EXIGE / GARANTE) com Auditoria Automática de Requisitos\n" +
                  "• Desktop IDE Swing Multiplataforma, Servidor LSP4J e Extensão Oficial VS Code";

        Paragraph pDestaques = new Paragraph(destaques, FONT_CAPA_CORPO);
        pDestaques.setLeading(16);
        pDestaques.setSpacingAfter(60);
        cell.addElement(pDestaques);

        String meta = en
                ? "Version: v" + ThzVersion.ATUAL + " • Build Date: " +
                  LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
                  "\n© 2026 THZ-LANG Project. All rights reserved."
                : "Versão: v" + ThzVersion.ATUAL + " • Compilação: " +
                  LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                  "\n© 2026 Projeto THZ-LANG. Todos os direitos reservados.";

        Paragraph pMeta = new Paragraph(meta, FONT_CAPA_META);
        pMeta.setLeading(13);
        cell.addElement(pMeta);

        tabelaCapa.addCell(cell);
        doc.add(tabelaCapa);
    }

    private static void renderizarSumarioComPaginasReais(Document doc, List<IndiceCapitulo> indices, int offsetPaginas, DialetoLinguagem dialeto) throws Exception {
        boolean en = (dialeto == DialetoLinguagem.EN_US);
        Paragraph pTitulo = new Paragraph(en ? "Table of Contents" : "Sumário do Livro-Manual", FONT_TOC_TITULO);
        pTitulo.setSpacingAfter(15);
        doc.add(pTitulo);

        String parteAtual = "";
        int numeroCap = 1;

        for (IndiceCapitulo idx : indices) {
            CapituloManual cap = idx.capitulo;

            if (!cap.parte().equals(parteAtual)) {
                parteAtual = cap.parte();
                Paragraph pParte = new Paragraph(parteAtual, FONT_TOC_SECAO);
                pParte.setSpacingBefore(12);
                pParte.setSpacingAfter(6);
                doc.add(pParte);
            }

            PdfPTable itemTable = new PdfPTable(new float[]{85f, 15f});
            itemTable.setWidthPercentage(100);
            itemTable.setSpacingAfter(3);

            String numPrefix = String.format("%02d. ", numeroCap++);
            String label = numPrefix + cap.titulo();

            PdfPCell cellTexto = new PdfPCell(new Phrase(label, FONT_TOC_ITEM));
            cellTexto.setBorder(Rectangle.NO_BORDER);
            cellTexto.setPadding(2);

            int pagReal = idx.paginaInicio > 0 ? idx.paginaInicio : 1;
            PdfPCell cellPag = new PdfPCell(new Phrase(String.valueOf(pagReal), FONT_TOC_PAGINA));
            cellPag.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellPag.setBorder(Rectangle.NO_BORDER);
            cellPag.setPadding(2);

            itemTable.addCell(cellTexto);
            itemTable.addCell(cellPag);

            doc.add(itemTable);
        }
    }

    private static void renderizarDivisoriaParte(Document doc, String parte, DialetoLinguagem dialeto) throws Exception {
        boolean en = (dialeto == DialetoLinguagem.EN_US);
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(248, 250, 252));
        cell.setBorderColor(COR_ACENTO);
        cell.setBorderWidth(2);
        cell.setPadding(35);

        Paragraph pTag = new Paragraph(en ? "MANUAL SECTION" : "SEÇÃO DO MANUAL", FONT_CAPA_SUPER);
        pTag.setAlignment(Element.ALIGN_CENTER);
        pTag.setSpacingAfter(10);
        cell.addElement(pTag);

        Paragraph p = new Paragraph(parte, FONT_PARTE_TITULO);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);

        table.addCell(cell);
        doc.add(table);
    }

    // =========================================================================
    // PARSER SINTÁTICO DE MARKDOWN
    // =========================================================================

    private static void renderizarCapituloMarkdown(Document doc, CapituloManual cap, DialetoLinguagem dialeto) throws Exception {
        String conteudo = Files.readString(cap.arquivo());
        boolean en = (dialeto == DialetoLinguagem.EN_US);

        Paragraph pHeader = new Paragraph(cap.titulo(), FONT_H1);
        pHeader.setSpacingAfter(3);
        doc.add(pHeader);

        String subLabel = en ? "Source File: " : "Arquivo Fonte: ";
        Paragraph pSub = new Paragraph(subLabel + cap.nomeArquivo(), FONT_RODAPE);
        pSub.setSpacingAfter(12);
        doc.add(pSub);

        try (BufferedReader reader = new BufferedReader(new StringReader(conteudo))) {
            String line;
            boolean emBlocoCodigo = false;
            String langCodigo = "TEXT";
            StringBuilder bufferCodigo = new StringBuilder();
            List<String> bufferTabela = new ArrayList<>();
            List<String> bufferCallout = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                String trim = line.trim();

                // 1. Bloco de Código (```)
                if (trim.startsWith("```")) {
                    if (emBlocoCodigo) {
                        renderizarBlocoCodigo(doc, bufferCodigo.toString(), langCodigo);
                        bufferCodigo.setLength(0);
                        emBlocoCodigo = false;
                    } else {
                        emBlocoCodigo = true;
                        String l = trim.substring(3).trim();
                        if (l.isEmpty()) {
                            langCodigo = en ? "CODE" : "CÓDIGO";
                        } else if (!en && (l.equalsIgnoreCase("TEXT") || l.equalsIgnoreCase("TXT"))) {
                            langCodigo = "TEXTO";
                        } else if (!en && l.equalsIgnoreCase("CODE")) {
                            langCodigo = "CÓDIGO";
                        } else {
                            langCodigo = l.toUpperCase();
                        }
                    }
                    continue;
                }

                if (emBlocoCodigo) {
                    bufferCodigo.append(line).append("\n");
                    continue;
                }

                // 2. Tabelas (| col | col |)
                if (trim.startsWith("|") && trim.endsWith("|")) {
                    bufferTabela.add(trim);
                    continue;
                } else if (!bufferTabela.isEmpty()) {
                    renderizarTabela(doc, bufferTabela);
                    bufferTabela.clear();
                }

                // 3. Callouts / Blockquotes (> texto)
                if (trim.startsWith(">")) {
                    bufferCallout.add(trim.substring(1).trim());
                    continue;
                } else if (!bufferCallout.isEmpty()) {
                    renderizarCalloutAgrupado(doc, bufferCallout, en);
                    bufferCallout.clear();
                }

                // 4. Linhas em Branco
                if (trim.isEmpty()) {
                    doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 3)));
                    continue;
                }

                // 5. Linha Horizontal (--- ou ***)
                if (trim.matches("^[-*_]{3,}$")) {
                    renderizarLinhaDivisoria(doc);
                    continue;
                }

                // 6. Cabeçalhos (#, ##, ###, ####)
                if (trim.startsWith("#### ")) {
                    Paragraph h4 = new Paragraph(processarInline(trim.substring(5).trim(), FONT_H4));
                    h4.setSpacingBefore(6);
                    h4.setSpacingAfter(3);
                    doc.add(h4);
                } else if (trim.startsWith("### ")) {
                    Paragraph h3 = new Paragraph(processarInline(trim.substring(4).trim(), FONT_H3));
                    h3.setSpacingBefore(8);
                    h3.setSpacingAfter(4);
                    doc.add(h3);
                } else if (trim.startsWith("## ")) {
                    Paragraph h2 = new Paragraph(processarInline(trim.substring(3).trim(), FONT_H2));
                    h2.setSpacingBefore(12);
                    h2.setSpacingAfter(5);
                    doc.add(h2);
                } else if (trim.startsWith("# ")) {
                    String h1Text = trim.substring(2).trim();
                    if (!h1Text.equalsIgnoreCase(cap.titulo())) {
                        Paragraph h1 = new Paragraph(processarInline(h1Text, FONT_H2));
                        h1.setSpacingBefore(10);
                        h1.setSpacingAfter(4);
                        doc.add(h1);
                    }
                } else if (trim.startsWith("- ") || trim.startsWith("* ")) {
                    Paragraph pItem = new Paragraph();
                    pItem.add(new Chunk("•  ", FONT_CORPO_BOLD));
                    pItem.add(processarInline(trim.substring(2).trim(), FONT_CORPO));
                    pItem.setIndentationLeft(14);
                    pItem.setSpacingAfter(2);
                    doc.add(pItem);
                } else if (trim.matches("^\\d+\\.\\s+.*")) {
                    int idxPonto = trim.indexOf('.');
                    String num = trim.substring(0, idxPonto + 1);
                    String texto = trim.substring(idxPonto + 1).trim();

                    Paragraph pItem = new Paragraph();
                    pItem.add(new Chunk(num + "  ", FONT_CORPO_BOLD));
                    pItem.add(processarInline(texto, FONT_CORPO));
                    pItem.setIndentationLeft(14);
                    pItem.setSpacingAfter(2);
                    doc.add(pItem);
                } else {
                    Paragraph p = new Paragraph(processarInline(trim, FONT_CORPO));
                    p.setLeading(13.5f);
                    p.setSpacingAfter(4.5f);
                    doc.add(p);
                }
            }

            if (emBlocoCodigo && bufferCodigo.length() > 0) {
                renderizarBlocoCodigo(doc, bufferCodigo.toString(), langCodigo);
            }
            if (!bufferTabela.isEmpty()) {
                renderizarTabela(doc, bufferTabela);
            }
            if (!bufferCallout.isEmpty()) {
                renderizarCalloutAgrupado(doc, bufferCallout, en);
            }
        }
    }

    private static void renderizarLinhaDivisoria(Document doc) throws Exception {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(6);

        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(0.75f);
        cell.setBackgroundColor(COR_BORDA_CODIGO);
        cell.setBorder(Rectangle.NO_BORDER);

        table.addCell(cell);
        doc.add(table);
    }

    private static void renderizarBlocoCodigo(Document doc, String codigo, String lang) throws Exception {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(8);

        PdfPCell headerCell = new PdfPCell(new Phrase(" " + lang, FONT_CODIGO_LANG));
        headerCell.setBackgroundColor(COR_HEADER_TABELA);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setPadding(3);
        table.addCell(headerCell);

        PdfPCell bodyCell = new PdfPCell();
        bodyCell.setBackgroundColor(COR_FUNDO_CODIGO);
        bodyCell.setBorderColor(COR_BORDA_CODIGO);
        bodyCell.setBorderWidth(1);
        bodyCell.setPadding(7);

        Paragraph p = new Paragraph(codigo.stripTrailing(), FONT_CODIGO_BLOCO);
        p.setLeading(11.5f);
        bodyCell.addElement(p);

        table.addCell(bodyCell);
        doc.add(table);
    }

    private static void renderizarCalloutAgrupado(Document doc, List<String> linhas, boolean en) throws Exception {
        if (linhas.isEmpty()) return;

        Color corFundo = new Color(239, 246, 255); // Blue 50
        Color corBorda = COR_ACENTO;
        String rotulo = en ? "NOTE" : "NOTA INFORMATIVA";

        String primeiraLinha = linhas.get(0);
        int inicioLinhas = 0;

        if (primeiraLinha.startsWith("[!NOTE]") || primeiraLinha.startsWith("[!NOTA]")) {
            rotulo = en ? "NOTE" : "NOTA INFORMATIVA";
            corFundo = new Color(239, 246, 255); // Blue 50
            corBorda = COR_ACENTO;
            inicioLinhas = 1;
        } else if (primeiraLinha.startsWith("[!TIP]") || primeiraLinha.startsWith("[!DICA]")) {
            rotulo = en ? "ARCHITECTURE TIP" : "DICA DE ARQUITETURA";
            corFundo = new Color(240, 253, 244); // Green 50
            corBorda = new Color(34, 197, 94);
            inicioLinhas = 1;
        } else if (primeiraLinha.startsWith("[!IMPORTANT]") || primeiraLinha.startsWith("[!IMPORTANTE]")) {
            rotulo = en ? "IMPORTANT DIRECTIVE" : "DIRETRIZ IMPORTANTE";
            corFundo = new Color(250, 245, 255); // Purple 50
            corBorda = new Color(168, 85, 247);
            inicioLinhas = 1;
        } else if (primeiraLinha.startsWith("[!WARNING]") || primeiraLinha.startsWith("[!AVISO]") || primeiraLinha.startsWith("[!ATENCAO]") || primeiraLinha.startsWith("[!ATENÇÃO]")) {
            rotulo = en ? "WARNING" : "ATENÇÃO / AVISO";
            corFundo = new Color(254, 252, 232); // Yellow 50
            corBorda = new Color(234, 179, 8);
            inicioLinhas = 1;
        } else if (primeiraLinha.startsWith("[!CAUTION]") || primeiraLinha.startsWith("[!CUIDADO]")) {
            rotulo = en ? "CRITICAL CAUTION" : "CUIDADO CRÍTICO";
            corFundo = new Color(254, 242, 242); // Red 50
            corBorda = new Color(239, 68, 68);
            inicioLinhas = 1;
        }

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(7);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(corFundo);
        cell.setBorderColor(corBorda);
        cell.setBorderWidthLeft(3.5f);
        cell.setBorderWidthTop(0.5f);
        cell.setBorderWidthRight(0.5f);
        cell.setBorderWidthBottom(0.5f);
        cell.setPadding(8);

        Paragraph pRotulo = new Paragraph(rotulo, FONT_CALLOUT_HEAD);
        pRotulo.setSpacingAfter(3);
        cell.addElement(pRotulo);

        for (int i = inicioLinhas; i < linhas.size(); i++) {
            String l = linhas.get(i);
            if (!l.isEmpty()) {
                Paragraph pCorpo = new Paragraph(processarInline(l, FONT_CORPO));
                pCorpo.setLeading(13);
                pCorpo.setSpacingAfter(2);
                cell.addElement(pCorpo);
            }
        }

        table.addCell(cell);
        doc.add(table);
    }

    private static void renderizarTabela(Document doc, List<String> linhas) throws Exception {
        if (linhas.isEmpty()) return;

        List<String[]> grid = new ArrayList<>();
        for (String l : linhas) {
            if (l.contains("---")) continue;
            String[] cols = l.split("\\|");
            List<String> validas = new ArrayList<>();
            for (String c : cols) {
                String t = c.trim();
                if (!t.isEmpty() || cols.length > 2) {
                    validas.add(t);
                }
            }
            if (!validas.isEmpty()) {
                grid.add(validas.toArray(new String[0]));
            }
        }

        if (grid.isEmpty()) return;
        int numCols = grid.get(0).length;
        PdfPTable table = new PdfPTable(numCols);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(8);

        for (String col : grid.get(0)) {
            PdfPCell cell = new PdfPCell(new Phrase(limparSintaxeMarkdown(col), FONT_TABELA_HEAD));
            cell.setBackgroundColor(COR_HEADER_TABELA);
            cell.setPadding(5);
            cell.setBorderColor(COR_BORDA_CODIGO);
            table.addCell(cell);
        }

        for (int r = 1; r < grid.size(); r++) {
            String[] row = grid.get(r);
            Color bg = (r % 2 == 0) ? Color.WHITE : COR_ZEBRA_TABELA;
            for (int c = 0; c < numCols; c++) {
                String val = c < row.length ? row[c] : "";
                PdfPCell cell = new PdfPCell(processarInline(val, FONT_TABELA_ROW));
                cell.setBackgroundColor(bg);
                cell.setPadding(4.5f);
                cell.setBorderColor(COR_BORDA_CODIGO);
                table.addCell(cell);
            }
        }

        doc.add(table);
    }

    public static Phrase processarInline(String markdown, Font fonteBase) {
        Phrase phrase = new Phrase();
        if (markdown == null || markdown.isEmpty()) return phrase;

        String texto = markdown
                .replaceAll("\\[!\\[[^\\]]*\\]\\([^\\)]*\\)\\]\\([^\\)]*\\)", "")
                .replaceAll("!\\[[^\\]]*\\]\\([^\\)]*\\)", "")
                .replaceAll("<[^>]*>", "");

        Pattern pattern = Pattern.compile("(\\[([^\\]]+)\\]\\(([^\\)]+)\\))|(`([^`]+)`)|(\\*\\*([^*]+)\\*\\*)|(\\*([^*]+)\\*)");
        Matcher matcher = pattern.matcher(texto);

        int lastIndex = 0;
        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                String normal = texto.substring(lastIndex, matcher.start());
                phrase.add(new Chunk(normal, fonteBase));
            }

            if (matcher.group(1) != null) {
                String linkTexto = matcher.group(2);
                String linkUrl = matcher.group(3);
                Chunk chunkLink = new Chunk(linkTexto, FONT_CORPO_LINK);
                if (linkUrl.startsWith("http://") || linkUrl.startsWith("https://")) {
                    chunkLink.setAnchor(linkUrl);
                }
                phrase.add(chunkLink);
            } else if (matcher.group(4) != null) {
                String code = matcher.group(5);
                Chunk chunkCode = new Chunk(code, FONT_CORPO_CODE);
                chunkCode.setBackground(COR_FUNDO_CODIGO, 1.5f, 1.5f, 1.5f, 1.5f);
                phrase.add(chunkCode);
            } else if (matcher.group(6) != null) {
                String bold = matcher.group(7);
                phrase.add(new Chunk(bold, FONT_CORPO_BOLD));
            } else if (matcher.group(8) != null) {
                String italic = matcher.group(9);
                phrase.add(new Chunk(italic, FONT_CORPO_ITALIC));
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < texto.length()) {
            phrase.add(new Chunk(texto.substring(lastIndex), fonteBase));
        }

        return phrase;
    }

    public static String limparSintaxeMarkdown(String md) {
        if (md == null) return "";
        return md
                .replaceAll("\\[!\\[[^\\]]*\\]\\([^\\)]*\\)\\]\\([^\\)]*\\)", "")
                .replaceAll("!\\[[^\\]]*\\]\\([^\\)]*\\)", "")
                .replaceAll("\\[([^\\]]+)\\]\\([^\\)]*\\)", "$1")
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("\\*([^*]+)\\*", "$1")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("<[^>]*>", "")
                .trim();
    }
}
