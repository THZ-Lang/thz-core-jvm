package thz.lang.ui;

/**
 * Tema CSS para Vaadin Lumo (vaadin-* classes).
 */
public final class VaadinTemaCss implements TemaCss {

    public static final VaadinTemaCss INSTANCIA = new VaadinTemaCss();

    private VaadinTemaCss() {}

    @Override
    public String classe(ThzUiComponente.TipoUi tipo) {
        return switch (tipo) {
            case CONTAINER -> "vaadin-card-panel";
            case LINHA -> "vaadin-horizontal-layout";
            case COLUNA -> "vaadin-vertical-layout";
            case GRADE -> "vaadin-grid-layout";
            case CARD -> "vaadin-card";
            case PAINEL -> "vaadin-panel";
            case BOTAO -> "vaadin-button";
            case CAMPO_TEXTO, CAMPO_NUMERO, CAMPO_MOEDA, CAMPO_DATA -> "vaadin-field-wrapper";
            case SELECAO -> "vaadin-field-wrapper";
            case INTERRUPTOR, CHECKBOX -> "vaadin-toggle-wrapper";
            case METRICA_CARD -> "vaadin-metric-card";
            case EMBLEMA -> "vaadin-badge";
            case ALERTA -> "vaadin-alert";
            case TEXTO_RICO -> "vaadin-generic-block";
            case TABELA_DADOS -> "vaadin-grid-container";
            case DIVISOR -> "vaadin-divider";
            case ESPACO -> "vaadin-spacer";
            default -> "vaadin-generic-block";
        };
    }

    @Override
    public String classeBotao(String variante) {
        String theme;
        if ("primario".equalsIgnoreCase(variante) || "primary".equalsIgnoreCase(variante)) theme = "primary";
        else if ("sucesso".equalsIgnoreCase(variante) || "success".equalsIgnoreCase(variante)) theme = "success";
        else if ("perigo".equalsIgnoreCase(variante) || "danger".equalsIgnoreCase(variante) || "erro".equalsIgnoreCase(variante)) theme = "danger";
        else if ("aviso".equalsIgnoreCase(variante) || "warning".equalsIgnoreCase(variante)) theme = "warning";
        else if ("contorno".equalsIgnoreCase(variante) || "outline".equalsIgnoreCase(variante)) theme = "outline";
        else theme = "secondary";
        return "vaadin-button vaadin-button-" + theme;
    }

    @Override
    public String classeStatus(ThzUiComponente.TipoUi tipo, String status) {
        return switch (tipo) {
            case METRICA_CARD -> "vaadin-metric-card vaadin-metric-" + status;
            case EMBLEMA -> "vaadin-badge vaadin-badge-" + status;
            case ALERTA -> "vaadin-alert vaadin-alert-" + status;
            default -> "";
        };
    }

    @Override
    public String tag(ThzUiComponente.TipoUi tipo) {
        return switch (tipo) {
            case CARD -> "section";
            default -> "div";
        };
    }

    @Override
    public String prefixo() {
        return "vaadin";
    }
}
