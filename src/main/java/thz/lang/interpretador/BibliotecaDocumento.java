package thz.lang.interpretador;



import java.util.Map;

/**
 * Funções de exportação de documentos da stdlib THZ-LANG.
 * Domínio: DOCUMENTO.*
 */
public final class BibliotecaDocumento {

    private BibliotecaDocumento() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        BibliotecaPadrao.registrarPublico(m, "DOCUMENTO.exportar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DOCUMENTO.exportar", args, 3, ctx);
            StdlibHelper.exigirClasse("DOCUMENTO.exportar", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("DOCUMENTO.exportar", args.get(1), "TEXTO", ctx);
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            String titulo = ((ValorThz.Texto) args.get(1)).valor();
            ValorThz dados = args.get(2);
            try {
                java.nio.file.Path resultado = thz.lang.documento.MotorDocumentos.exportar(java.nio.file.Path.of(caminho), titulo, dados);
                return ValorThz.TEXTO(resultado.toAbsolutePath().toString());
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao exportar documento: " + e.getMessage());
            }
        });
        BibliotecaPadrao.registrarPublico(m, "DOCUMENTO.exportarPdf", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DOCUMENTO.exportarPdf", args, 3, ctx);
            StdlibHelper.exigirClasse("DOCUMENTO.exportarPdf", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("DOCUMENTO.exportarPdf", args.get(1), "TEXTO", ctx);
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            String titulo = ((ValorThz.Texto) args.get(1)).valor();
            ValorThz dados = args.get(2);
            try {
                java.nio.file.Path resultado = thz.lang.documento.MotorDocumentos.exportarPdf(java.nio.file.Path.of(caminho), titulo, dados);
                return ValorThz.TEXTO(resultado.toAbsolutePath().toString());
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao exportar PDF: " + e.getMessage());
            }
        });
        BibliotecaPadrao.registrarPublico(m, "DOCUMENTO.exportarXlsx", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DOCUMENTO.exportarXlsx", args, 3, ctx);
            StdlibHelper.exigirClasse("DOCUMENTO.exportarXlsx", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("DOCUMENTO.exportarXlsx", args.get(1), "TEXTO", ctx);
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            String nomeAba = ((ValorThz.Texto) args.get(1)).valor();
            ValorThz dados = args.get(2);
            try {
                java.nio.file.Path resultado = thz.lang.documento.MotorDocumentos.exportarXlsx(java.nio.file.Path.of(caminho), nomeAba, dados);
                return ValorThz.TEXTO(resultado.toAbsolutePath().toString());
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao exportar planilha Excel (XLSX): " + e.getMessage());
            }
        });
        BibliotecaPadrao.registrarPublico(m, "DOCUMENTO.exportarDocx", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DOCUMENTO.exportarDocx", args, 3, ctx);
            StdlibHelper.exigirClasse("DOCUMENTO.exportarDocx", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("DOCUMENTO.exportarDocx", args.get(1), "TEXTO", ctx);
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            String titulo = ((ValorThz.Texto) args.get(1)).valor();
            ValorThz dados = args.get(2);
            try {
                java.nio.file.Path resultado = thz.lang.documento.MotorDocumentos.exportarDocx(java.nio.file.Path.of(caminho), titulo, dados);
                return ValorThz.TEXTO(resultado.toAbsolutePath().toString());
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao exportar documento Word (DOCX): " + e.getMessage());
            }
        });
    }
}
