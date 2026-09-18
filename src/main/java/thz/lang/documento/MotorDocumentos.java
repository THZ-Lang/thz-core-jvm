package thz.lang.documento;

import thz.lang.interpretador.ValorThz;

import java.nio.file.Path;

/**
 * Fachada unificada de geração e exportação de documentos empresariais no THZ-LANG.
 */
public final class MotorDocumentos {

    private MotorDocumentos() {}

    /**
     * Exporta dados para o formato determinado pela extensão do arquivo de destino (.pdf, .xlsx, .docx).
     */
    public static Path exportar(Path destino, String titulo, ValorThz dados) throws Exception {
        String nome = destino.getFileName().toString().toLowerCase();
        if (nome.endsWith(".pdf")) {
            return exportarPdf(destino, titulo, dados);
        } else if (nome.endsWith(".xlsx") || nome.endsWith(".xls")) {
            return exportarXlsx(destino, titulo, dados);
        } else if (nome.endsWith(".docx") || nome.endsWith(".doc")) {
            return exportarDocx(destino, titulo, dados);
        } else {
            // Padrão: PDF
            return exportarPdf(destino, titulo, dados);
        }
    }

    public static Path exportarPdf(Path destino, String titulo, ValorThz dados) throws Exception {
        return GeradorPdf.gerar(destino, titulo, dados);
    }

    public static Path exportarXlsx(Path destino, String nomePlanilha, ValorThz dados) throws Exception {
        return GeradorXlsx.gerar(destino, nomePlanilha, dados);
    }

    public static Path exportarDocx(Path destino, String titulo, ValorThz dados) throws Exception {
        return GeradorDocx.gerar(destino, titulo, dados);
    }
}
