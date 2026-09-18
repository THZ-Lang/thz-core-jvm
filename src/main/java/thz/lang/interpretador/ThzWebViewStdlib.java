package thz.lang.interpretador;



import java.util.Map;

/**
 * Funções de webview, UI, log, versão, config e nativo da stdlib THZ-LANG.
 * Domínio: WEBVIEW.*, UI.*, LOG.*, VERSAO.*, CONFIG.*, NATIVO.*
 */
public final class ThzWebViewStdlib {

    private ThzWebViewStdlib() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        // ---- WEBVIEW ----
        BibliotecaPadrao.registrarPublico(m, "WEBVIEW.iniciar", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("WEBVIEW.iniciar", args, 1, ctx);
            StdlibHelper.exigirClasse("WEBVIEW.iniciar", args.get(0), "TEXTO", ctx);
            String html = ((ValorThz.Texto) args.get(0)).valor();
            thz.lang.webview.ThzWebViewBridge.iniciar(html);
            return ValorThz.TEXTO(thz.lang.webview.ThzWebViewBridge.getUrl());
        });
        BibliotecaPadrao.registrarPublico(m, "WEBVIEW.emitir", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("WEBVIEW.emitir", args, 2, ctx);
            StdlibHelper.exigirClasse("WEBVIEW.emitir", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("WEBVIEW.emitir", args.get(1), "TEXTO", ctx);
            thz.lang.webview.ThzWebViewBridge.emitirParaJs(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            );
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "WEBVIEW.parar", (args, ctx, interp) -> {
            thz.lang.webview.ThzWebViewBridge.parar();
            return ValorThz.LOGICO(true);
        });

        // ---- UI (ThzUiMaker) ----
        BibliotecaPadrao.registrarPublico(m, "UI.temaPadrao", (args, ctx, interp) -> {
            return ValorThz.TEXTO("THZ Dark Glass");
        });
        BibliotecaPadrao.registrarPublico(m, "UI.renderizarHtml", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("UI.renderizarHtml", args, 2, ctx);
            StdlibHelper.exigirClasse("UI.renderizarHtml", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("UI.renderizarHtml", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String rotuloBotao = ((ValorThz.Texto) args.get(1)).valor();
            var tela = thz.lang.ui.ThzUiMaker.container("raiz", c -> {
                c.adicionar(thz.lang.ui.ThzUiMaker.card("card_principal", titulo, card -> {
                    card.adicionar(thz.lang.ui.ThzUiMaker.alerta("alerta_info", "info", "Tela construída com ThzUiMaker"));
                    card.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_acao", rotuloBotao, "ExecutarAcao"));
                }));
            });
            return ValorThz.TEXTO(tela.renderizarHtml(titulo, thz.lang.ui.ThzUiTema.escuroGlass()));
        });
        BibliotecaPadrao.registrarPublico(m, "UI.gerarCodigo", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("UI.gerarCodigo", args, 1, ctx);
            StdlibHelper.exigirClasse("UI.gerarCodigo", args.get(0), "TEXTO", ctx);
            String nome = ((ValorThz.Texto) args.get(0)).valor();
            var tela = thz.lang.ui.ThzUiMaker.container("raiz", c -> {
                c.adicionar(thz.lang.ui.ThzUiMaker.card("card_app", nome, card -> {
                    card.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_ok", "Confirmar", "ConfirmarAcao"));
                }));
            });
            return ValorThz.TEXTO(tela.gerarCodigoThz(nome));
        });

        // ---- LOG ----
        BibliotecaPadrao.registrarPublico(m, "LOG.info", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("LOG.info", args, 1, ctx);
            thz.lang.log.ThzLog.info(args.get(0).formatar());
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "LOG.aviso", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("LOG.aviso", args, 1, ctx);
            thz.lang.log.ThzLog.aviso(args.get(0).formatar());
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "LOG.erro", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("LOG.erro", args, 1, ctx);
            thz.lang.log.ThzLog.erro(args.get(0).formatar());
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "LOG.auditoria", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("LOG.auditoria", args, 3, ctx);
            thz.lang.log.ThzLog.auditoria(
                    args.get(0).formatar(),
                    args.get(1).formatar(),
                    args.get(2).formatar()
            );
            return ValorThz.LOGICO(true);
        });

        // ---- VERSAO ----
        BibliotecaPadrao.registrarPublico(m, "VERSAO.obter", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.version.ThzVersion.ATUAL.toString());
        });
        BibliotecaPadrao.registrarPublico(m, "VERSAO.satisfaz", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("VERSAO.satisfaz", args, 2, ctx);
            StdlibHelper.exigirClasse("VERSAO.satisfaz", args.get(0), "TEXTO", ctx);
            StdlibHelper.exigirClasse("VERSAO.satisfaz", args.get(1), "TEXTO", ctx);
            return ValorThz.LOGICO(thz.lang.version.ThzVersion.satisfaz(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });

        // ---- CONFIG ----
        BibliotecaPadrao.registrarPublico(m, "CONFIG.obter", (args, ctx, interp) -> {
            if (args.size() == 1) {
                StdlibHelper.exigirClasse("CONFIG.obter", args.get(0), "TEXTO", ctx);
                return ValorThz.TEXTO(thz.lang.config.ThzConfig.obter(((ValorThz.Texto) args.get(0)).valor()));
            } else if (args.size() == 2) {
                StdlibHelper.exigirClasse("CONFIG.obter", args.get(0), "TEXTO", ctx);
                StdlibHelper.exigirClasse("CONFIG.obter", args.get(1), "TEXTO", ctx);
                return ValorThz.TEXTO(thz.lang.config.ThzConfig.obter(((ValorThz.Texto) args.get(0)).valor(), ((ValorThz.Texto) args.get(1)).valor()));
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] CONFIG.obter exige 1 ou 2 argumentos.");
        });
        BibliotecaPadrao.registrarPublico(m, "CONFIG.carregarEnv", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                thz.lang.config.ThzConfig.carregarEnvPadrao();
            } else {
                StdlibHelper.exigirClasse("CONFIG.carregarEnv", args.get(0), "TEXTO", ctx);
                thz.lang.config.ThzConfig.carregarArquivoEnv(((ValorThz.Texto) args.get(0)).valor());
            }
            return ValorThz.LOGICO(true);
        });
        BibliotecaPadrao.registrarPublico(m, "CONFIG.projeto.nome", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.config.ThzProjectConfig.obterConfig().projeto().nome());
        });
        BibliotecaPadrao.registrarPublico(m, "CONFIG.projeto.versao", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.config.ThzProjectConfig.obterConfig().projeto().versao());
        });
        BibliotecaPadrao.registrarPublico(m, "CONFIG.projeto.autor", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.config.ThzProjectConfig.obterConfig().projeto().autor());
        });
        BibliotecaPadrao.registrarPublico(m, "CONFIG.projeto.dialeto", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.config.ThzProjectConfig.obterConfig().projeto().dialeto());
        });

        // ---- NATIVO / RUST INLINE BRIDGE ----
        BibliotecaPadrao.registrarPublico(m, "NATIVO.somar_rapido", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("NATIVO.somar_rapido", args, 2, ctx);
            return thz.lang.rust.ThzRustRunner.invocarFuncaoNativa("somar_rapido", args);
        });
        BibliotecaPadrao.registrarPublico(m, "NATIVO.calcular_hash_customizado", (args, ctx, interp) -> {
            StdlibHelper.exigirAridade("NATIVO.calcular_hash_customizado", args, 1, ctx);
            return thz.lang.rust.ThzRustRunner.invocarFuncaoNativa("calcular_hash_customizado", args);
        });
        BibliotecaPadrao.registrarPublico(m, "NATIVO.versao_rust", (args, ctx, interp) -> {
            return thz.lang.rust.ThzRustRunner.invocarFuncaoNativa("versao_rust", args);
        });
    }
}
