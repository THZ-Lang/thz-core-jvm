package thz.lang.ui;

import java.util.*;

/**
 * ThzUiVaadinEmitter — Renderizador de interfaces declarativas THZ-UI para
 * Vaadin Flow e Vaadin Lumo Web Components oficiais.
 * <p>
 * Renderiza todos os componentes do ThzUiMaker com classes e tokens CSS
 * do tema oficial Vaadin Lumo Dark/Light, incluindo:
 * Layouts, Cards, KPI Métricas, Formulários, Tabelas, Alertas, Emblemas,
 * Botões com variantes, Campos monetários ISO 4217 e Divisores.
 */
public final class ThzUiVaadinEmitter {

    private static final TemaCss TEMA = VaadinTemaCss.INSTANCIA;

    private ThzUiVaadinEmitter() {}

    public static String renderizarPaginaVaadin(String titulo, ThzUiComponente raiz, boolean temaEscuro) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"pt-BR\" theme=\"").append(temaEscuro ? "dark" : "light").append("\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("  <title>").append(HtmlEscape.escapeHtml(titulo)).append(" — Vaadin UI</title>\n");
        sb.append("  <style>\n").append(gerarCssLumo(temaEscuro)).append("  </style>\n");
        sb.append("</head>\n<body>\n");
        sb.append("  <div class=\"vaadin-app-container\">\n");
        sb.append("    <header class=\"vaadin-app-header\">\n");
        sb.append("      <div class=\"vaadin-badge-logo\">THZ VAADIN ENGINE</div>\n");
        sb.append("      <h2 class=\"vaadin-title\">").append(HtmlEscape.escapeHtml(titulo)).append("</h2>\n");
        sb.append("    </header>\n\n");
        sb.append("    <main class=\"vaadin-main-content\">\n");
        renderizarComponente(raiz, sb, "      ");
        sb.append("    </main>\n");
        sb.append("    <footer class=\"vaadin-footer\">THZ-LANG Engine v3.0.0 · Vaadin Flow · Java 25 Virtual Threads</footer>\n");
        sb.append("    <div id=\"vaadin_notification_area\" class=\"vaadin-notification-container\"></div>\n");
        sb.append("  </div>\n");
        sb.append("  <script>\n").append(gerarJsVaadin()).append("  </script>\n");
        sb.append("</body>\n</html>");
        return sb.toString();
    }

    private static void renderizarComponente(ThzUiComponente c, StringBuilder sb, String indent) {
        if (c == null) return;
        String id = c.id();
        String rotulo = c.getPropriedade("rotulo", "");
        String placeholder = c.getPropriedade("placeholder", "");

        switch (c.tipo()) {
            case CONTAINER -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case LINHA -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case COLUNA -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case GRADE -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case CARD -> {
                String tituloCard = c.getPropriedade("titulo", "");
                sb.append(indent).append("<section id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                if (!tituloCard.isBlank()) {
                    sb.append(indent).append("  <div class=\"vaadin-card-header\"><h3>").append(HtmlEscape.escapeHtml(tituloCard)).append("</h3></div>\n");
                }
                sb.append(indent).append("  <div class=\"vaadin-card-body\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "    ");
                sb.append(indent).append("  </div>\n");
                sb.append(indent).append("</section>\n");
            }
            case PAINEL -> {
                String tituloP = c.getPropriedade("titulo", "");
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                if (!tituloP.isBlank()) sb.append(indent).append("  <div class=\"vaadin-panel-title\">").append(HtmlEscape.escapeHtml(tituloP)).append("</div>\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case BOTAO -> {
                String acao = c.eventos().getOrDefault("aoClicar", "");
                String variante = c.getPropriedade("variante", "primary");
                sb.append(indent).append("<button id=\"").append(id).append("\" class=\"").append(TEMA.classeBotao(variante))
                        .append("\" onclick=\"vaadinDespacharAcao('").append(HtmlEscape.escapeJs(acao)).append("', '").append(id).append("')\">")
                        .append(HtmlEscape.escapeHtml(rotulo)).append("</button>\n");
            }
            case CAMPO_TEXTO -> {
                String vinculo = c.getPropriedade("vinculo", id);
                String valor = c.getPropriedade("valor", "");
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"vaadin-field-label\">").append(HtmlEscape.escapeHtml(rotulo)).append("</label>\n");
                sb.append(indent).append("  <input type=\"text\" id=\"").append(id).append("\" data-vinculo=\"").append(HtmlEscape.escapeHtml(vinculo))
                        .append("\" class=\"vaadin-input\" placeholder=\"").append(HtmlEscape.escapeHtml(placeholder))
                        .append("\" value=\"").append(HtmlEscape.escapeHtml(valor))
                        .append("\" oninput=\"vaadinAtualizarVinculo('").append(HtmlEscape.escapeJs(vinculo)).append("', this.value)\"/>\n");
                sb.append(indent).append("</div>\n");
            }
            case CAMPO_NUMERO -> {
                String vinculo = c.getPropriedade("vinculo", id);
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"vaadin-field-label\">").append(HtmlEscape.escapeHtml(rotulo)).append("</label>\n");
                sb.append(indent).append("  <input type=\"number\" id=\"").append(id).append("\" data-vinculo=\"").append(HtmlEscape.escapeHtml(vinculo))
                        .append("\" class=\"vaadin-input\" placeholder=\"0\" step=\"any\"")
                        .append(" oninput=\"vaadinAtualizarVinculo('").append(HtmlEscape.escapeJs(vinculo)).append("', this.value)\"/>\n");
                sb.append(indent).append("</div>\n");
            }
            case CAMPO_MOEDA -> {
                String vinculo = c.getPropriedade("vinculo", id);
                String moeda = c.getPropriedade("moeda", "BRL");
                String valor = c.getPropriedade("valor", "");
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"vaadin-field-label\">").append(HtmlEscape.escapeHtml(rotulo)).append(" <span class=\"vaadin-badge-sm\">").append(moeda).append("</span></label>\n");
                sb.append(indent).append("  <div class=\"vaadin-input-group\"><span class=\"vaadin-input-prefix\">R$</span>")
                        .append("<input type=\"text\" id=\"").append(id).append("\" data-vinculo=\"").append(HtmlEscape.escapeHtml(vinculo))
                        .append("\" class=\"vaadin-input vaadin-input-currency\" placeholder=\"0,00\" value=\"").append(HtmlEscape.escapeHtml(valor))
                        .append("\" oninput=\"vaadinAtualizarVinculo('").append(HtmlEscape.escapeJs(vinculo)).append("', this.value)\"/></div>\n");
                sb.append(indent).append("</div>\n");
            }
            case CAMPO_DATA -> {
                String vinculo = c.getPropriedade("vinculo", id);
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"vaadin-field-label\">").append(HtmlEscape.escapeHtml(rotulo)).append("</label>\n");
                sb.append(indent).append("  <input type=\"date\" id=\"").append(id).append("\" data-vinculo=\"").append(HtmlEscape.escapeHtml(vinculo))
                        .append("\" class=\"vaadin-input\"")
                        .append(" oninput=\"vaadinAtualizarVinculo('").append(HtmlEscape.escapeJs(vinculo)).append("', this.value)\"/>\n");
                sb.append(indent).append("</div>\n");
            }
            case SELECAO -> {
                String vinculo = c.getPropriedade("vinculo", id);
                
                List<String> opcoes = c.getPropriedade("opcoes", List.of());
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"vaadin-field-label\">").append(HtmlEscape.escapeHtml(rotulo)).append("</label>\n");
                sb.append(indent).append("  <select id=\"").append(id).append("\" data-vinculo=\"").append(HtmlEscape.escapeHtml(vinculo))
                        .append("\" class=\"vaadin-select\" onchange=\"vaadinAtualizarVinculo('").append(HtmlEscape.escapeJs(vinculo)).append("', this.value)\">\n");
                sb.append(indent).append("    <option value=\"\">Selecione...</option>\n");
                for (String op : opcoes) {
                    sb.append(indent).append("    <option value=\"").append(HtmlEscape.escapeHtml(op)).append("\">").append(HtmlEscape.escapeHtml(op)).append("</option>\n");
                }
                sb.append(indent).append("  </select>\n");
                sb.append(indent).append("</div>\n");
            }
            case INTERRUPTOR, CHECKBOX -> {
                String vinculo = c.getPropriedade("vinculo", id);
                sb.append(indent).append("<label class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                sb.append(indent).append("  <input type=\"checkbox\" id=\"").append(id).append("\" class=\"vaadin-toggle-input\" data-vinculo=\"").append(HtmlEscape.escapeHtml(vinculo))
                        .append("\" onchange=\"vaadinAtualizarVinculo('").append(HtmlEscape.escapeJs(vinculo)).append("', this.checked)\"/>\n");
                sb.append(indent).append("  <span class=\"vaadin-toggle-slider\"></span>\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <span class=\"vaadin-toggle-label\">").append(HtmlEscape.escapeHtml(rotulo)).append("</span>\n");
                sb.append(indent).append("</label>\n");
            }
            case METRICA_CARD -> {
                String valor = c.getPropriedade("valor", "0");
                String tendencia = c.getPropriedade("tendencia", "");
                String status = c.getPropriedade("status", "info");
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classeStatus(c.tipo(), status)).append("\">\n");
                sb.append(indent).append("  <div class=\"vaadin-metric-label\">").append(HtmlEscape.escapeHtml(rotulo)).append("</div>\n");
                sb.append(indent).append("  <div class=\"vaadin-metric-value\">").append(HtmlEscape.escapeHtml(valor)).append("</div>\n");
                if (!tendencia.isBlank()) sb.append(indent).append("  <div class=\"vaadin-metric-trend\">").append(HtmlEscape.escapeHtml(tendencia)).append("</div>\n");
                sb.append(indent).append("</div>\n");
            }
            case EMBLEMA -> {
                String status = c.getPropriedade("status", "primario");
                sb.append(indent).append("<span id=\"").append(id).append("\" class=\"").append(TEMA.classeStatus(c.tipo(), status)).append("\">")
                        .append(HtmlEscape.escapeHtml(rotulo)).append("</span>\n");
            }
            case ALERTA -> {
                String texto = c.getPropriedade("texto", rotulo);
                String status = c.getPropriedade("tipoAlerta", "info");
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classeStatus(c.tipo(), status)).append("\">")
                        .append(HtmlEscape.escapeHtml(texto)).append("</div>\n");
            }
            case DIVISOR -> {
                sb.append(indent).append("<hr class=\"").append(TEMA.classe(c.tipo())).append("\"/>\n");
            }
            case ESPACO -> {
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\"></div>\n");
            }
            case TABELA_DADOS -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                sb.append(indent).append("  <table class=\"vaadin-grid\"><thead><tr>");
                
                List<String> colunas = c.getPropriedade("colunas", List.of("Coluna"));
                for (String col : colunas) sb.append("<th>").append(HtmlEscape.escapeHtml(col)).append("</th>");
                sb.append("</tr></thead><tbody id=\"").append(id).append("_body\"></tbody></table>\n");
                sb.append(indent).append("</div>\n");
            }
            default -> {
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
        }
    }

    private static String gerarCssLumo(boolean temaEscuro) {
        String base = """
            :root {
                --lumo-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                --lumo-border-radius-s: 6px;
                --lumo-border-radius-m: 8px;
                --lumo-border-radius-l: 12px;
                --lumo-space-xs: 4px; --lumo-space-s: 8px; --lumo-space-m: 16px; --lumo-space-l: 24px; --lumo-space-xl: 32px;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; font-family: var(--lumo-font-family); }
            """;

        String tema = temaEscuro ? """
                --lumo-base-color: #0f172a;
                --lumo-surface-color: #1e293b;
                --lumo-surface-alt: #334155;
                --lumo-primary-color: #3b82f6;
                --lumo-primary-hover: #2563eb;
                --lumo-success-color: #10b981;
                --lumo-success-bg: rgba(16,185,129,0.15);
                --lumo-error-color: #ef4444;
                --lumo-error-bg: rgba(239,68,68,0.12);
                --lumo-warning-color: #f59e0b;
                --lumo-warning-bg: rgba(245,158,11,0.12);
                --lumo-body-text-color: #f1f5f9;
                --lumo-secondary-text-color: #94a3b8;
                --lumo-tertiary-text: #64748b;
                --lumo-border-color: rgba(255,255,255,0.08);
                --lumo-border-hover: rgba(255,255,255,0.15);
                --lumo-input-bg: #0f172a;
                --lumo-shadow-m: 0 4px 16px rgba(0,0,0,0.4);
                --lumo-shadow-l: 0 8px 32px rgba(0,0,0,0.5);
            """ : """
                --lumo-base-color: #f8fafc;
                --lumo-surface-color: #ffffff;
                --lumo-surface-alt: #f1f5f9;
                --lumo-primary-color: #0284c7;
                --lumo-primary-hover: #0369a1;
                --lumo-success-color: #059669;
                --lumo-success-bg: rgba(5,150,105,0.1);
                --lumo-error-color: #dc2626;
                --lumo-error-bg: rgba(220,38,38,0.08);
                --lumo-warning-color: #d97706;
                --lumo-warning-bg: rgba(217,119,6,0.08);
                --lumo-body-text-color: #1e293b;
                --lumo-secondary-text-color: #64748b;
                --lumo-tertiary-text: #94a3b8;
                --lumo-border-color: #e2e8f0;
                --lumo-border-hover: #cbd5e1;
                --lumo-input-bg: #ffffff;
                --lumo-shadow-m: 0 4px 12px rgba(0,0,0,0.06);
                --lumo-shadow-l: 0 8px 24px rgba(0,0,0,0.08);
            """;

        return base + "    :root {\n" + tema + "    }\n" + """
            body { background: var(--lumo-base-color); color: var(--lumo-body-text-color); padding: 24px; line-height: 1.5; }
            .vaadin-app-container { max-width: 1200px; margin: 0 auto; display: flex; flex-direction: column; gap: 24px; }
            .vaadin-app-header { display: flex; align-items: center; justify-content: space-between; padding-bottom: 20px; border-bottom: 1px solid var(--lumo-border-color); }
            .vaadin-badge-logo { background: linear-gradient(135deg, #00b4d8, #0077b6); color: white; font-weight: 800; font-size: 0.7rem; padding: 6px 14px; border-radius: 9999px; letter-spacing: 0.12em; text-transform: uppercase; }
            .vaadin-title { font-size: 1.5rem; font-weight: 700; }
            .vaadin-footer { text-align: center; padding: 16px 0; font-size: 0.8rem; color: var(--lumo-tertiary-text); border-top: 1px solid var(--lumo-border-color); }

            /* Layouts */
            .vaadin-card-panel { display: flex; flex-direction: column; gap: 20px; width: 100%; }
            .vaadin-horizontal-layout { display: flex; flex-direction: row; gap: 16px; align-items: stretch; flex-wrap: wrap; }
            .vaadin-vertical-layout { display: flex; flex-direction: column; gap: 14px; }
            .vaadin-grid-layout { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; }

            /* Card */
            .vaadin-card { background: var(--lumo-surface-color); border: 1px solid var(--lumo-border-color); border-radius: var(--lumo-border-radius-l); overflow: hidden; box-shadow: var(--lumo-shadow-m); transition: box-shadow 0.2s; }
            .vaadin-card:hover { box-shadow: var(--lumo-shadow-l); }
            .vaadin-card-header { padding: 18px 24px 0; }
            .vaadin-card-header h3 { font-size: 1.1rem; font-weight: 700; margin: 0; }
            .vaadin-card-body { padding: 18px 24px 22px; display: flex; flex-direction: column; gap: 14px; }

            /* Panel */
            .vaadin-panel { background: var(--lumo-surface-alt); border-radius: var(--lumo-border-radius-m); padding: 16px; }
            .vaadin-panel-title { font-weight: 600; font-size: 0.9rem; color: var(--lumo-secondary-text-color); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 12px; }

            /* Buttons */
            .vaadin-button { display: inline-flex; align-items: center; justify-content: center; height: 40px; padding: 0 20px; font-size: 0.9rem; font-weight: 600; border-radius: var(--lumo-border-radius-m); cursor: pointer; border: none; transition: all 0.15s; gap: 8px; white-space: nowrap; }
            .vaadin-button:active { transform: scale(0.97); }
            .vaadin-button:disabled { opacity: 0.5; cursor: not-allowed; }
            .vaadin-button-primary { background: var(--lumo-primary-color); color: #fff; }
            .vaadin-button-primary:hover:not(:disabled) { background: var(--lumo-primary-hover); }
            .vaadin-button-secondary { background: var(--lumo-surface-alt); color: var(--lumo-body-text-color); border: 1px solid var(--lumo-border-color); }
            .vaadin-button-secondary:hover:not(:disabled) { border-color: var(--lumo-border-hover); }
            .vaadin-button-success { background: var(--lumo-success-color); color: #fff; }
            .vaadin-button-danger { background: var(--lumo-error-color); color: #fff; }
            .vaadin-button-warning { background: var(--lumo-warning-color); color: #fff; }
            .vaadin-button-outline { background: transparent; color: var(--lumo-primary-color); border: 2px solid var(--lumo-primary-color); }
            .vaadin-button-outline:hover { background: var(--lumo-primary-color); color: #fff; }

            /* Fields */
            .vaadin-field-wrapper { display: flex; flex-direction: column; gap: 6px; flex: 1; min-width: 180px; }
            .vaadin-field-label { font-size: 0.82rem; font-weight: 600; color: var(--lumo-secondary-text-color); }
            .vaadin-input { height: 40px; padding: 0 12px; background: var(--lumo-input-bg); border: 1px solid var(--lumo-border-color); border-radius: var(--lumo-border-radius-m); color: var(--lumo-body-text-color); font-size: 0.9rem; outline: none; transition: border-color 0.15s, box-shadow 0.15s; width: 100%; }
            .vaadin-input:focus { border-color: var(--lumo-primary-color); box-shadow: 0 0 0 2px rgba(59,130,246,0.25); }
            .vaadin-input-group { display: flex; align-items: stretch; }
            .vaadin-input-prefix { display: flex; align-items: center; padding: 0 10px; background: var(--lumo-surface-alt); border: 1px solid var(--lumo-border-color); border-right: none; border-radius: var(--lumo-border-radius-m) 0 0 var(--lumo-border-radius-m); font-size: 0.85rem; font-weight: 600; color: var(--lumo-secondary-text-color); }
            .vaadin-input-group .vaadin-input { border-radius: 0 var(--lumo-border-radius-m) var(--lumo-border-radius-m) 0; }
            .vaadin-select { height: 40px; padding: 0 12px; background: var(--lumo-input-bg); border: 1px solid var(--lumo-border-color); border-radius: var(--lumo-border-radius-m); color: var(--lumo-body-text-color); font-size: 0.9rem; width: 100%; cursor: pointer; }

            /* Toggle / Switch */
            .vaadin-toggle-wrapper { display: flex; align-items: center; gap: 10px; cursor: pointer; }
            .vaadin-toggle-input { display: none; }
            .vaadin-toggle-slider { width: 44px; height: 24px; background: var(--lumo-surface-alt); border-radius: 12px; position: relative; transition: background 0.2s; border: 1px solid var(--lumo-border-color); }
            .vaadin-toggle-slider::after { content: ''; position: absolute; top: 2px; left: 2px; width: 18px; height: 18px; background: white; border-radius: 50%; transition: transform 0.2s; }
            .vaadin-toggle-input:checked + .vaadin-toggle-slider { background: var(--lumo-primary-color); border-color: var(--lumo-primary-color); }
            .vaadin-toggle-input:checked + .vaadin-toggle-slider::after { transform: translateX(20px); }
            .vaadin-toggle-label { font-size: 0.9rem; color: var(--lumo-body-text-color); }

            /* Metric KPI Cards */
            .vaadin-metric-card { background: var(--lumo-surface-color); border: 1px solid var(--lumo-border-color); border-radius: var(--lumo-border-radius-l); padding: 20px 24px; flex: 1; min-width: 200px; }
            .vaadin-metric-label { font-size: 0.8rem; font-weight: 600; color: var(--lumo-secondary-text-color); text-transform: uppercase; letter-spacing: 0.06em; margin-bottom: 6px; }
            .vaadin-metric-value { font-size: 1.8rem; font-weight: 800; line-height: 1.2; }
            .vaadin-metric-trend { font-size: 0.82rem; margin-top: 6px; font-weight: 600; }
            .vaadin-metric-info .vaadin-metric-value { color: var(--lumo-primary-color); }
            .vaadin-metric-info .vaadin-metric-trend { color: var(--lumo-primary-color); }
            .vaadin-metric-sucesso .vaadin-metric-value, .vaadin-metric-success .vaadin-metric-value { color: var(--lumo-success-color); }
            .vaadin-metric-sucesso .vaadin-metric-trend, .vaadin-metric-success .vaadin-metric-trend { color: var(--lumo-success-color); }
            .vaadin-metric-erro .vaadin-metric-value, .vaadin-metric-danger .vaadin-metric-value { color: var(--lumo-error-color); }
            .vaadin-metric-erro .vaadin-metric-trend, .vaadin-metric-danger .vaadin-metric-trend { color: var(--lumo-error-color); }
            .vaadin-metric-aviso .vaadin-metric-value, .vaadin-metric-warning .vaadin-metric-value { color: var(--lumo-warning-color); }

            /* Badge */
            .vaadin-badge-sm { font-size: 0.7rem; padding: 2px 6px; border-radius: 4px; background: var(--lumo-surface-alt); }
            .vaadin-badge { display: inline-flex; align-items: center; padding: 4px 12px; border-radius: 9999px; font-size: 0.78rem; font-weight: 700; letter-spacing: 0.02em; }
            .vaadin-badge-primario, .vaadin-badge-primary { background: rgba(59,130,246,0.15); color: var(--lumo-primary-color); }
            .vaadin-badge-sucesso, .vaadin-badge-success { background: var(--lumo-success-bg); color: var(--lumo-success-color); }
            .vaadin-badge-erro, .vaadin-badge-danger { background: var(--lumo-error-bg); color: var(--lumo-error-color); }
            .vaadin-badge-aviso, .vaadin-badge-warning { background: var(--lumo-warning-bg); color: var(--lumo-warning-color); }

            /* Alerts */
            .vaadin-alert { padding: 14px 18px; border-radius: var(--lumo-border-radius-m); font-size: 0.9rem; line-height: 1.5; border-left: 4px solid; }
            .vaadin-alert-info { border-left-color: var(--lumo-primary-color); background: rgba(59,130,246,0.1); color: var(--lumo-body-text-color); }
            .vaadin-alert-success { border-left-color: var(--lumo-success-color); background: var(--lumo-success-bg); }
            .vaadin-alert-error { border-left-color: var(--lumo-error-color); background: var(--lumo-error-bg); }
            .vaadin-alert-warning { border-left-color: var(--lumo-warning-color); background: var(--lumo-warning-bg); }

            /* Divider & Spacer */
            .vaadin-divider { border: none; border-top: 1px solid var(--lumo-border-color); margin: 8px 0; }
            .vaadin-spacer { height: 16px; }

            /* Grid / Table */
            .vaadin-grid-container { overflow-x: auto; border-radius: var(--lumo-border-radius-m); border: 1px solid var(--lumo-border-color); }
            .vaadin-grid { width: 100%; border-collapse: collapse; font-size: 0.88rem; }
            .vaadin-grid th { background: var(--lumo-surface-alt); padding: 10px 14px; text-align: left; font-weight: 700; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--lumo-secondary-text-color); }
            .vaadin-grid td { padding: 10px 14px; border-top: 1px solid var(--lumo-border-color); }
            .vaadin-grid tbody tr:hover { background: var(--lumo-surface-alt); }

            /* Notifications */
            .vaadin-notification-container { position: fixed; bottom: 24px; right: 24px; z-index: 9999; display: flex; flex-direction: column; gap: 10px; max-width: 400px; }
            .vaadin-toast { background: var(--lumo-surface-color); color: var(--lumo-body-text-color); padding: 14px 20px; border-radius: var(--lumo-border-radius-m); box-shadow: var(--lumo-shadow-l); font-weight: 500; font-size: 0.9rem; animation: vaadinSlideIn 0.3s ease-out; border-left: 4px solid var(--lumo-success-color); }
            @keyframes vaadinSlideIn { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
        """;
    }

    private static String gerarJsVaadin() {
        return """
            window.vaadinEstado = {};
            function vaadinAtualizarVinculo(campo, valor) {
                window.vaadinEstado[campo] = valor;
            }
            function vaadinNotificar(msg, tipo = 'info') {
                const container = document.getElementById('vaadin_notification_area');
                if (!container) return;
                const toast = document.createElement('div');
                toast.className = 'vaadin-toast';
                if (tipo === 'erro') toast.style.borderLeftColor = '#ef4444';
                else if (tipo === 'aviso') toast.style.borderLeftColor = '#f59e0b';
                else toast.style.borderLeftColor = '#10b981';
                toast.textContent = msg;
                container.appendChild(toast);
                setTimeout(() => { toast.style.opacity = '0'; toast.style.transition = 'opacity 0.3s'; setTimeout(() => toast.remove(), 300); }, 4000);
            }
            async function vaadinDespacharAcao(acao, idBotao) {
                if (!acao) return;
                const btn = document.getElementById(idBotao);
                const orig = btn ? btn.textContent : '';
                if (btn) { btn.disabled = true; btn.textContent = 'Executando...'; }
                try {
                    let resp;
                    try {
                        resp = await fetch('/thz-bridge/rpc', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ canal: acao, payload: JSON.stringify({ estado: window.vaadinEstado }) })
                        });
                    } catch(e1) {
                        resp = await fetch('/api/rpc/invocar', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ acao, estado: window.vaadinEstado })
                        });
                    }
                    const res = await resp.json();
                    if (res && res.status === 'ok') {
                        vaadinNotificar('\\u2713 ' + (res.resultado || res.mensagem || 'Concluído com sucesso'), 'sucesso');
                    } else {
                        vaadinNotificar('\\u2717 ' + (res.erro || res.mensagem || 'Falha'), 'erro');
                    }
                } catch(e) {
                    vaadinNotificar('\\u2717 ' + e.message, 'erro');
                } finally {
                    if (btn) { btn.disabled = false; btn.textContent = orig; }
                }
            }
        """;
    }


}