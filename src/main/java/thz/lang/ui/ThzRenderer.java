package thz.lang.ui;

import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;

/**
 * Contrato de renderização desacoplado — permite múltiplas implementações
 * (Swing, WebView, Headless, PDF) sem o thz-core depender de thz-gui.
 *
 * Implementações:
 * - {@code ThzFormWebRenderer} (core): HTML5 + CSS Glassmorphism + JS Bridge
 * - {@code ThzUiSwingRenderer} (gui): Swing JComponent nativo
 * - {@code RenderizadorFormularioSwing} (gui): Swing JFrame para formulários
 */
public interface ThzRenderer {

    /**
     * Renderiza um formulário THZ (Registro + operação alvo) e retorna mensagem de status.
     * Implementações não devem lançar exceção para o usuário final; devem retornar string amigável.
     */
    String renderizarFormulario(ValorThz.Registro registro, String operacaoAlvo, InterpretadorThz interpretador);

    /**
     * Renderiza um componente UI individual a partir de uma árvore ThzUiComponente.
     * Retorna o conteúdo renderizado (HTML, Swing XML, etc.) como string.
     * Implementações opcionais podem retornar {@code null} se não suportarem componentes soltos.
     */
    default String renderizarComponente(thz.lang.ui.ThzUiComponente componente) {
        return null;
    }

    /**
     * Indica se esta implementação suporta renderização interativa (com state proxy / two-way binding).
     * Webview e Swing suportam; headless/PDF não.
     */
    default boolean suportaInteratividade() {
        return false;
    }

    /**
     * Indica se esta implementação suporta exportação para formato estático (PDF, imagem, etc.).
     */
    default boolean suportaExportacao() {
        return false;
    }
}
