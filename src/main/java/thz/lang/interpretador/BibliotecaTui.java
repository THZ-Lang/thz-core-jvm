package thz.lang.interpretador;

import thz.lang.ast.ExprAst;
import thz.lang.tui.ThzTerminalTui;

import java.util.List;
import java.util.Map;

/**
 * Funções nativas da stdlib para interfaces em modo texto (TUI).
 * Domínio: TUI.*
 */
public final class BibliotecaTui {

    private static ThzTerminalTui instanciaGlobal = new ThzTerminalTui(80, 24);

    private BibliotecaTui() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        BibliotecaPadrao.registrarPublico(m, "TUI.criar", (args, ctx) -> {
            int w = args.size() > 0 && args.get(0) instanceof ValorThz.Inteiro i ? i.valor().intValue() : 80;
            int h = args.size() > 1 && args.get(1) instanceof ValorThz.Inteiro i ? i.valor().intValue() : 24;
            instanciaGlobal = new ThzTerminalTui(w, h);
            return ValorThz.LOGICO(true);
        });

        BibliotecaPadrao.registrarPublico(m, "TUI.limpar", (args, ctx) -> {
            instanciaGlobal.limpar();
            return ValorThz.LOGICO(true);
        });

        BibliotecaPadrao.registrarPublico(m, "TUI.janela", (args, ctx) -> {
            exigirMinArgs("TUI.janela", args, 5, ctx);
            int x = ((ValorThz.Inteiro) args.get(0)).valor().intValue();
            int y = ((ValorThz.Inteiro) args.get(1)).valor().intValue();
            int w = ((ValorThz.Inteiro) args.get(2)).valor().intValue();
            int h = ((ValorThz.Inteiro) args.get(3)).valor().intValue();
            String titulo = ((ValorThz.Texto) args.get(4)).valor();
            int cor = args.size() > 5 && args.get(5) instanceof ValorThz.Inteiro ci ? ci.valor().intValue() : 36; // Ciano default
            instanciaGlobal.desenharJanela(x, y, w, h, titulo, cor);
            return ValorThz.LOGICO(true);
        });

        BibliotecaPadrao.registrarPublico(m, "TUI.texto", (args, ctx) -> {
            exigirMinArgs("TUI.texto", args, 3, ctx);
            int x = ((ValorThz.Inteiro) args.get(0)).valor().intValue();
            int y = ((ValorThz.Inteiro) args.get(1)).valor().intValue();
            String texto = ((ValorThz.Texto) args.get(2)).valor();
            int cor = args.size() > 3 && args.get(3) instanceof ValorThz.Inteiro ci ? ci.valor().intValue() : 37;
            boolean bold = args.size() > 4 && args.get(4) instanceof ValorThz.Logico l && l.valor();
            instanciaGlobal.desenharTexto(x, y, texto, cor, bold);
            return ValorThz.LOGICO(true);
        });

        BibliotecaPadrao.registrarPublico(m, "TUI.barraProgresso", (args, ctx) -> {
            exigirMinArgs("TUI.barraProgresso", args, 4, ctx);
            int x = ((ValorThz.Inteiro) args.get(0)).valor().intValue();
            int y = ((ValorThz.Inteiro) args.get(1)).valor().intValue();
            int w = ((ValorThz.Inteiro) args.get(2)).valor().intValue();
            double pct;
            if (args.get(3) instanceof ValorThz.Decimal d) {
                pct = Double.parseDouble(d.valor().formatar());
            } else if (args.get(3) instanceof ValorThz.Inteiro i) {
                pct = i.valor().doubleValue() / 100.0;
            } else {
                pct = 0.0;
            }
            int cor = args.size() > 4 && args.get(4) instanceof ValorThz.Inteiro ci ? ci.valor().intValue() : 32; // Verde default
            instanciaGlobal.desenharBarraProgresso(x, y, w, pct, cor);
            return ValorThz.LOGICO(true);
        });

        BibliotecaPadrao.registrarPublico(m, "TUI.renderizar", (args, ctx) -> {
            String out = instanciaGlobal.renderizarString();
            return ValorThz.TEXTO(out);
        });

        BibliotecaPadrao.registrarPublico(m, "TUI.exibir", (args, ctx) -> {
            String out = instanciaGlobal.renderizarString();
            System.out.println(out);
            return ValorThz.TEXTO(out);
        });
    }

    private static void exigirMinArgs(String nome, List<ValorThz> args, int min, ExprAst ctx) {
        if (args.size() < min) {
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Função '" + nome + "' exige no mínimo " + min + " argumento(s), recebidos " + args.size() + ".");
        }
    }

    public static ThzTerminalTui getInstanciaGlobal() {
        return instanciaGlobal;
    }
}
