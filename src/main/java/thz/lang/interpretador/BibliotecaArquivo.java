package thz.lang.interpretador;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Funções de arquivo, diretório e snapshot da stdlib THZ-LANG.
 * Domínio: ARQUIVO.*, DIRETORIO.*, SNAPSHOT.*
 */
public final class BibliotecaArquivo {

    private BibliotecaArquivo() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        // ---- ARQUIVO ----
        BibliotecaPadrao.registrarPublico(m, "ARQUIVO.localizar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ARQUIVO.localizar", args, 1, ctx);
            StdlibHelper.exigirClasse("ARQUIVO.localizar", args.get(0), "TEXTO", ctx);
            String termo = ((ValorThz.Texto) args.get(0)).valor();
            var opt = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(termo, java.nio.file.Path.of("."), null);
            return opt.map(path -> ValorThz.TEXTO(path.toAbsolutePath().toString().replace("\\", "/"))).orElse(ValorThz.TEXTO(""));
        });
        BibliotecaPadrao.registrarPublico(m, "ARQUIVO.lerTexto", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ARQUIVO.lerTexto", args, 1, ctx);
            StdlibHelper.exigirClasse("ARQUIVO.lerTexto", args.get(0), "TEXTO", ctx);
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            if (!thz.lang.io.ThzIO.existe(caminho)) {
                var opt = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(caminho, java.nio.file.Path.of("."), null);
                if (opt.isPresent()) {
                    caminho = opt.get().toString();
                }
            }
            return ValorThz.TEXTO(thz.lang.io.ThzIO.lerTexto(caminho));
        });
        BibliotecaPadrao.registrarPublico(m, "ARQUIVO.escreverTexto", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ARQUIVO.escreverTexto", args, 2, ctx);
            StdlibHelper.exigirClasse("ARQUIVO.escreverTexto", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("ARQUIVO.escreverTexto", args.get(1), "TEXTO", ctx);
            thz.lang.io.ThzIO.escreverTexto(((ValorThz.Texto) args.get(0)).valor(), ((ValorThz.Texto) args.get(1)).valor());
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "ARQUIVO.anexarTexto", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ARQUIVO.anexarTexto", args, 2, ctx);
            StdlibHelper.exigirClasse("ARQUIVO.anexarTexto", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("ARQUIVO.anexarTexto", args.get(1), "TEXTO", ctx);
            thz.lang.io.ThzIO.anexarTexto(((ValorThz.Texto) args.get(0)).valor(), ((ValorThz.Texto) args.get(1)).valor());
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "ARQUIVO.existe", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ARQUIVO.existe", args, 1, ctx);
            StdlibHelper.exigirClasse("ARQUIVO.existe", args.get(0), "TEXTO", ctx);
            return ValorThz.LOGICO(thz.lang.io.ThzIO.existe(((ValorThz.Texto) args.get(0)).valor()));
        });
        BibliotecaPadrao.registrarPublico(m, "ARQUIVO.remover", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("ARQUIVO.remover", args, 1, ctx);
            StdlibHelper.exigirClasse("ARQUIVO.remover", args.get(0), "TEXTO", ctx);
            return ValorThz.LOGICO(thz.lang.io.ThzIO.remover(((ValorThz.Texto) args.get(0)).valor()));
        });

        // ---- DIRETORIO ----
        BibliotecaPadrao.registrarPublico(m, "DIRETORIO.listar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DIRETORIO.listar", args, 1, ctx);
            StdlibHelper.exigirClasse("DIRETORIO.listar", args.get(0), "TEXTO", ctx);
            List<String> itens = thz.lang.io.ThzIO.listarDiretorio(((ValorThz.Texto) args.get(0)).valor());
            List<ValorThz> res = new ArrayList<>();
            for (String item : itens) res.add(ValorThz.TEXTO(item));
            return new ValorThz.Fatia("TEXTO", res);
        });
        BibliotecaPadrao.registrarPublico(m, "DIRETORIO.criar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("DIRETORIO.criar", args, 1, ctx);
            StdlibHelper.exigirClasse("DIRETORIO.criar", args.get(0), "TEXTO", ctx);
            thz.lang.io.ThzIO.criarDiretorio(((ValorThz.Texto) args.get(0)).valor());
            return ValorThz.LOGICO(true);
        });

        // ---- SNAPSHOT ----
        BibliotecaPadrao.registrarPublico(m, "SNAPSHOT.criar", (args, ctx, interp) -> {
            java.nio.file.Path origem = args.isEmpty() ? java.nio.file.Path.of(".") : java.nio.file.Path.of(args.get(0).formatar());
            java.nio.file.Path destino = args.size() > 1 ? java.nio.file.Path.of(args.get(1).formatar()) : null;
            try {
                java.nio.file.Path criado = thz.lang.snapshot.ThzSnapshotEngine.criarSnapshot(origem, destino);
                return ValorThz.TEXTO(criado.toAbsolutePath().toString().replace("\\", "/"));
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] " + e.getMessage());
            }
        });
        BibliotecaPadrao.registrarPublico(m, "SNAPSHOT.restaurar", (args, ctx, interp) -> {
            java.nio.file.Path snap = args.isEmpty() ? null : java.nio.file.Path.of(args.get(0).formatar());
            java.nio.file.Path destino = args.size() > 1 ? java.nio.file.Path.of(args.get(1).formatar()) : java.nio.file.Path.of(".");
            try {
                boolean ok = thz.lang.snapshot.ThzSnapshotEngine.restaurarSnapshot(snap, destino);
                return ValorThz.LOGICO(ok);
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao restaurar snapshot: " + e.getMessage());
            }
        });
        BibliotecaPadrao.registrarPublico(m, "SNAPSHOT.tamanho", (args, ctx, interp) -> {
            return ValorThz.INTEIRO(thz.lang.snapshot.ThzSnapshotEngine.obterTamanhoSnapshot());
        });
        BibliotecaPadrao.registrarPublico(m, "SNAPSHOT.limpar", (args, ctx, interp) -> {
            return ValorThz.LOGICO(thz.lang.snapshot.ThzSnapshotEngine.limparSnapshot());
        });
        BibliotecaPadrao.registrarPublico(m, "SNAPSHOT.verificar", (args, ctx, interp) -> {
            java.nio.file.Path snap = args.isEmpty() ? null : java.nio.file.Path.of(args.get(0).formatar());
            return ValorThz.LOGICO(thz.lang.snapshot.ThzSnapshotEngine.verificarIntegridade(snap));
        });
    }
}
