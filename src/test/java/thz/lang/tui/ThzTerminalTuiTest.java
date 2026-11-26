package thz.lang.tui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.interpretador.BibliotecaPadrao;
import thz.lang.interpretador.ValorThz;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ThzTerminalTuiTest {

    @Test
    @DisplayName("Deve desenhar janela com bordas unicode e titulo centralizado")
    void deveDesenharJanelaUnicode() {
        ThzTerminalTui tui = new ThzTerminalTui(40, 10);
        tui.desenharJanela(2, 1, 30, 6, "PAINEL", 36);

        String render = tui.renderizarString();
        assertNotNull(render);
        assertTrue(render.contains("┌"), "Deve conter canto superior esquerdo ┌");
        assertTrue(render.contains("┐"), "Deve conter canto superior direito ┐");
        assertTrue(render.contains("└"), "Deve conter canto inferior esquerdo └");
        assertTrue(render.contains("┘"), "Deve conter canto inferior direito ┘");
        assertTrue(render.contains("PAINEL"), "Deve conter o titulo da janela");
        assertTrue(render.contains("\033["), "Deve conter escape sequences ANSI de cor");
    }

    @Test
    @DisplayName("Deve desenhar barra de progresso com porcentagem e caracteres de preenchimento")
    void deveDesenharBarraProgresso() {
        ThzTerminalTui tui = new ThzTerminalTui(50, 5);
        tui.desenharBarraProgresso(2, 2, 20, 0.75, 32);

        String render = tui.renderizarString();
        assertTrue(render.contains("["), "Deve iniciar com colchete [");
        assertTrue(render.contains("]"), "Deve terminar com colchete ]");
        assertTrue(render.contains("█"), "Deve conter blocos preenchidos █");
        assertTrue(render.contains("░"), "Deve conter blocos vazios ░");
        assertTrue(render.contains("75%"), "Deve exibir porcentagem 75%");
    }

    @Test
    @DisplayName("Deve executar operacoes TUI atraves da BibliotecaPadrao")
    void deveExecutarViaBibliotecaPadrao() {
        ValorThz resCriar = BibliotecaPadrao.executar("TUI.criar", List.of(ValorThz.INTEIRO(60), ValorThz.INTEIRO(15)), null);
        assertTrue(resCriar instanceof ValorThz.Logico l && l.valor());

        ValorThz resJanela = BibliotecaPadrao.executar("TUI.janela", List.of(
                ValorThz.INTEIRO(1), ValorThz.INTEIRO(1),
                ValorThz.INTEIRO(40), ValorThz.INTEIRO(8),
                ValorThz.TEXTO("STATUS DO SISTEMA"), ValorThz.INTEIRO(32)
        ), null);
        assertTrue(resJanela instanceof ValorThz.Logico l && l.valor());

        ValorThz resRender = BibliotecaPadrao.executar("TUI.renderizar", List.of(), null);
        assertTrue(resRender instanceof ValorThz.Texto t);
        assertTrue(((ValorThz.Texto) resRender).valor().contains("STATUS DO SISTEMA"));
    }
}
