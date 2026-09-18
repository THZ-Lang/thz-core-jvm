package thz.lang.ui;

/**
 * Tema CSS para HTML5 Glassmorphism (thz-* classes).
 */
public final class HtmlTemaCss implements TemaCss {

    public static final HtmlTemaCss INSTANCIA = new HtmlTemaCss();

    private HtmlTemaCss() {}

    @Override
    public String classe(ThzUiComponente.TipoUi tipo) {
        return switch (tipo) {
            case CONTAINER -> "thz-container";
            case LINHA -> "thz-flex-row";
            case COLUNA -> "thz-flex-col";
            case GRADE -> "thz-grid";
            case CARD -> "thz-card";
            case PAINEL -> "thz-painel";
            case BOTAO -> "thz-btn";
            case CAMPO_TEXTO, CAMPO_NUMERO, CAMPO_MOEDA, CAMPO_DATA -> "thz-form-group";
            case SELECAO -> "thz-form-group";
            case INTERRUPTOR, CHECKBOX -> "thz-switch-wrapper";
            case METRICA_CARD -> "thz-metric-card";
            case EMBLEMA -> "thz-badge";
            case ALERTA -> "thz-alert";
            case TEXTO_RICO -> "thz-text";
            case TABELA_DADOS -> "thz-tabela-wrapper";
            case DIVISOR -> "thz-divider";
            case ESPACO -> "thz-spacer";
            default -> "thz-generic";
        };
    }

    @Override
    public String classeBotao(String variante) {
        return "thz-btn thz-btn-" + variante;
    }

    @Override
    public String classeStatus(ThzUiComponente.TipoUi tipo, String status) {
        return switch (tipo) {
            case METRICA_CARD -> "thz-metric-card thz-metric-" + status;
            case EMBLEMA -> "thz-badge thz-badge-" + status;
            case ALERTA -> "thz-alert thz-alert-" + status;
            default -> "";
        };
    }

    @Override
    public String tag(ThzUiComponente.TipoUi tipo) {
        return "div";
    }

    @Override
    public String prefixo() {
        return "thz";
    }
}
