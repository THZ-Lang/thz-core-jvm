package thz.lang.ui;

/**
 * Strategy para mapeamento de classes CSS por tipo de componente.
 * Permite trocar o tema (HTML Glassmorphism vs Vaadin Lumo) sem duplicar lógica.
 */
public interface TemaCss {

    /** Classe CSS para o tipo de componente informado. */
    String classe(ThzUiComponente.TipoUi tipo);

    /** Classe CSS para uma variante de botão. */
    String classeBotao(String variante);

    /** Classe CSS para status de métrica/emblema/alerta. */
    String classeStatus(ThzUiComponente.TipoUi tipo, String status);

    /** Tag HTML para o container raiz do componente. */
    String tag(ThzUiComponente.TipoUi tipo);

    /** Sufixo da classe de status (ex: "thz-metric-info" ou "vaadin-metric-info"). */
    String prefixo();
}
