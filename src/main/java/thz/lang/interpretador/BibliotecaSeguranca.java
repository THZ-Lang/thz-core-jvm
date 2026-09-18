package thz.lang.interpretador;



import java.util.Map;

/**
 * Funções de segurança da stdlib THZ-LANG.
 * Domínio: SEGURANCA.*
 */
public final class BibliotecaSeguranca {

    private BibliotecaSeguranca() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.sha256", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.sha256", args, 1, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.sha256", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.sha256(((ValorThz.Texto) args.get(0)).valor()));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.sha512", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.sha512", args, 1, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.sha512", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.sha512(((ValorThz.Texto) args.get(0)).valor()));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.hmacSha256", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.hmacSha256", args, 2, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.hmacSha256", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("SEGURANCA.hmacSha256", args.get(1), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.hmacSha256(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.criptografarAes", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.criptografarAes", args, 2, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.criptografarAes", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("SEGURANCA.criptografarAes", args.get(1), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.criptografarAes(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.descriptografarAes", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.descriptografarAes", args, 2, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.descriptografarAes", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("SEGURANCA.descriptografarAes", args.get(1), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.descriptografarAes(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.hashSenha", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.hashSenha", args, 1, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.hashSenha", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.hashSenha(((ValorThz.Texto) args.get(0)).valor()));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.verificarSenha", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.verificarSenha", args, 2, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.verificarSenha", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("SEGURANCA.verificarSenha", args.get(1), "TEXTO", ctx);
            return ValorThz.LOGICO(thz.lang.security.ThzSecurity.verificarSenha(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.argon2", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.argon2", args, 1, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.argon2", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.hashArgon2(((ValorThz.Texto) args.get(0)).valor()));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.verificarArgon2", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.verificarArgon2", args, 2, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.verificarArgon2", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("SEGURANCA.verificarArgon2", args.get(1), "TEXTO", ctx);
            return ValorThz.LOGICO(thz.lang.security.ThzSecurity.verificarArgon2(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.chacha20", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.chacha20", args, 2, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.chacha20", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("SEGURANCA.chacha20", args.get(1), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.criptografarChaCha20(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.descriptografarChaCha20", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.descriptografarChaCha20", args, 2, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.descriptografarChaCha20", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("SEGURANCA.descriptografarChaCha20", args.get(1), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.descriptografarChaCha20(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.cofreSalvar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.cofreSalvar", args, 3, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.cofreSalvar", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("SEGURANCA.cofreSalvar", args.get(1), "TEXTO", ctx);
            StdlibHelper.exigirClasse("SEGURANCA.cofreSalvar", args.get(2), "TEXTO", ctx);
            try {
                thz.lang.security.ThzVault.salvarTexto(
                        java.nio.file.Path.of(((ValorThz.Texto) args.get(0)).valor()),
                        ((ValorThz.Texto) args.get(1)).valor(),
                        ((ValorThz.Texto) args.get(2)).valor()
                );
                return ValorThz.LOGICO(true);
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao salvar cofre: " + e.getMessage());
            }
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.cofreLer", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("SEGURANCA.cofreLer", args, 2, ctx);
            StdlibHelper.exigirClasse("SEGURANCA.cofreLer", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("SEGURANCA.cofreLer", args.get(1), "TEXTO", ctx);
            try {
                String conteudo = thz.lang.security.ThzVault.lerTexto(
                        java.nio.file.Path.of(((ValorThz.Texto) args.get(0)).valor()),
                        ((ValorThz.Texto) args.get(1)).valor()
                );
                return ValorThz.TEXTO(conteudo);
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao ler cofre: " + e.getMessage());
            }
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.gerarToken", (args, ctx, interp) -> {
            int tamanho = args.isEmpty() ? 32 : ((ValorThz.Inteiro) args.get(0)).valor().intValue();
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.gerarToken(tamanho));
        });
        BibliotecaPadrao.registrarPublico(m, "SEGURANCA.uuid", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.gerarUuid());
        });
    }
}
