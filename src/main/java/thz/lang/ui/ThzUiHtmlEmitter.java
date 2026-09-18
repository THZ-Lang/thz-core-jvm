package thz.lang.ui;

import java.util.*;

/**
 * Renderizador de interfaces declarativas ThzUiMaker para HTML5 semântico,
 * CSS3 Moderno (Glassmorphism, Dark/Light tokens) e JavaScript integrado a window.thz.
 */
public final class ThzUiHtmlEmitter {

    private static final TemaCss TEMA = HtmlTemaCss.INSTANCIA;

    private ThzUiHtmlEmitter() {}

    public static String renderizarPaginaCompleta(String titulo, ThzUiComponente raiz, ThzUiTema tema) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("  <title>").append(HtmlEscape.escapeHtml(titulo)).append("</title>\n");
        sb.append("  <style>\n");
        sb.append(gerarCss(tema));
        sb.append("  </style>\n");
        sb.append("</head>\n<body>\n");
        sb.append("  <div class=\"thz-app-root\">\n");
        renderizarComponente(raiz, sb, "    ");
        sb.append("  </div>\n");
        sb.append("  <script>\n");
        sb.append(gerarJs());
        sb.append("  </script>\n");
        sb.append("</body>\n</html>");
        return sb.toString();
    }

    public static String renderizarFragmento(ThzUiComponente componente) {
        StringBuilder sb = new StringBuilder();
        renderizarComponente(componente, sb, "");
        return sb.toString();
    }

    private static void renderizarComponente(ThzUiComponente c, StringBuilder sb, String indent) {
        if (c == null) return;
        String id = c.id();
        String rotulo = c.getPropriedade("rotulo", "");
        String placeholder = c.getPropriedade("placeholder", "");
        String estiloInline = montarEstiloInline(c.estilos());

        switch (c.tipo()) {
            case CONTAINER -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case LINHA -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case COLUNA -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case GRADE -> {
                int colunas = c.getPropriedade("colunas", 2);
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\" style=\"grid-template-columns: repeat(").append(colunas).append(", 1fr);\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case CARD -> {
                String tituloCard = c.getPropriedade("titulo", "");
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                if (!tituloCard.isBlank()) {
                    sb.append(indent).append("  <div class=\"thz-card-header\"><h3>").append(HtmlEscape.escapeHtml(tituloCard)).append("</h3></div>\n");
                }
                sb.append(indent).append("  <div class=\"thz-card-body\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "    ");
                sb.append(indent).append("  </div>\n");
                sb.append(indent).append("</div>\n");
            }
            case PAINEL -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case BOTAO -> {
                String acao = c.eventos().getOrDefault("aoClicar", "");
                String variante = c.getPropriedade("variante", "primario");
                sb.append(indent).append("<button id=\"").append(id).append("\" class=\"").append(TEMA.classeBotao(variante)).append("\" onclick=\"thzDespacharAcao('").append(HtmlEscape.escapeJs(acao)).append("', '").append(id).append("')\"").append(estiloInline).append(">")
                        .append(HtmlEscape.escapeHtml(rotulo)).append("</button>\n");
            }
            case CAMPO_TEXTO -> {
                String vinculo = c.getPropriedade("vinculo", id);
                String valor = c.getPropriedade("valor", "");
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"thz-label\">").append(HtmlEscape.escapeHtml(rotulo)).append("</label>\n");
                sb.append(indent).append("  <input type=\"text\" id=\"").append(id).append("\" data-vinculo=\"").append(HtmlEscape.escapeHtml(vinculo)).append("\" class=\"thz-input\" placeholder=\"").append(HtmlEscape.escapeHtml(placeholder)).append("\" value=\"").append(HtmlEscape.escapeHtml(valor)).append("\" oninput=\"thzVinculoAtualizado('").append(HtmlEscape.escapeJs(vinculo)).append("', this.value)\"/>\n");
                sb.append(indent).append("</div>\n");
            }
            case CAMPO_NUMERO -> {
                String vinculo = c.getPropriedade("vinculo", id);
                Object valor = c.getPropriedade("valor", 0);
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"thz-label\">").append(HtmlEscape.escapeHtml(rotulo)).append("</label>\n");
                sb.append(indent).append("  <input type=\"number\" id=\"").append(id).append("\" data-vinculo=\"").append(HtmlEscape.escapeHtml(vinculo)).append("\" class=\"thz-input\" value=\"").append(valor).append("\" oninput=\"thzVinculoAtualizado('").append(HtmlEscape.escapeJs(vinculo)).append("', Number(this.value))\"/>\n");
                sb.append(indent).append("</div>\n");
            }
            case CAMPO_MOEDA -> {
                String vinculo = c.getPropriedade("vinculo", id);
                String moeda = c.getPropriedade("moeda", "BRL");
                String valor = c.getPropriedade("valor", "0.00");
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"thz-label\">").append(HtmlEscape.escapeHtml(rotulo)).append(" (").append(moeda).append(")</label>\n");
                sb.append(indent).append("  <input type=\"text\" id=\"").append(id).append("\" data-vinculo=\"").append(HtmlEscape.escapeHtml(vinculo)).append("\" class=\"thz-input thz-input-moeda\" placeholder=\"0,00\" value=\"").append(HtmlEscape.escapeHtml(valor)).append("\" oninput=\"thzVinculoAtualizado('").append(HtmlEscape.escapeJs(vinculo)).append("', this.value)\"/>\n");
                sb.append(indent).append("</div>\n");
            }
            case CAMPO_DATA -> {
                String vinculo = c.getPropriedade("vinculo", id);
                String valor = c.getPropriedade("valor", "");
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"thz-label\">").append(HtmlEscape.escapeHtml(rotulo)).append("</label>\n");
                sb.append(indent).append("  <input type=\"date\" id=\"").append(id).append("\" data-vinculo=\"").append(HtmlEscape.escapeHtml(vinculo)).append("\" class=\"thz-input\" value=\"").append(HtmlEscape.escapeHtml(valor)).append("\" onchange=\"thzVinculoAtualizado('").append(HtmlEscape.escapeJs(vinculo)).append("', this.value)\"/>\n");
                sb.append(indent).append("</div>\n");
            }
            case SELECAO -> {
                String vinculo = c.getPropriedade("vinculo", id);
                List<?> opcoes = c.getPropriedade("opcoes", List.of());
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"thz-label\">").append(HtmlEscape.escapeHtml(rotulo)).append("</label>\n");
                sb.append(indent).append("  <select id=\"").append(id).append("\" data-vinculo=\"").append(HtmlEscape.escapeHtml(vinculo)).append("\" class=\"thz-select\" onchange=\"thzVinculoAtualizado('").append(HtmlEscape.escapeJs(vinculo)).append("', this.value)\">\n");
                for (Object op : opcoes) {
                    sb.append(indent).append("    <option value=\"").append(HtmlEscape.escapeHtml(op.toString())).append("\">").append(HtmlEscape.escapeHtml(op.toString())).append("</option>\n");
                }
                sb.append(indent).append("  </select>\n");
                sb.append(indent).append("</div>\n");
            }
            case INTERRUPTOR -> {
                String vinculo = c.getPropriedade("vinculo", id);
                boolean ativo = Boolean.parseBoolean(String.valueOf(c.getPropriedade("valor", "false")));
                sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                sb.append(indent).append("  <label class=\"thz-switch\">\n");
                sb.append(indent).append("    <input type=\"checkbox\" id=\"").append(id).append("\" ").append(ativo ? "checked " : "").append("onchange=\"thzVinculoAtualizado('").append(HtmlEscape.escapeJs(vinculo)).append("', this.checked)\">\n");
                sb.append(indent).append("    <span class=\"thz-slider\"></span>\n");
                sb.append(indent).append("  </label>\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <span class=\"thz-switch-label\">").append(HtmlEscape.escapeHtml(rotulo)).append("</span>\n");
                sb.append(indent).append("</div>\n");
            }
            case METRICA_CARD -> {
                String valor = c.getPropriedade("valor", "0");
                String tendencia = c.getPropriedade("tendencia", "");
                String status = c.getPropriedade("status", "info");
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classeStatus(c.tipo(), status)).append("\"").append(estiloInline).append(">\n");
                sb.append(indent).append("  <div class=\"thz-metric-title\">").append(HtmlEscape.escapeHtml(rotulo)).append("</div>\n");
                sb.append(indent).append("  <div class=\"thz-metric-value\">").append(HtmlEscape.escapeHtml(valor)).append("</div>\n");
                if (!tendencia.isBlank()) sb.append(indent).append("  <div class=\"thz-metric-trend\">").append(HtmlEscape.escapeHtml(tendencia)).append("</div>\n");
                sb.append(indent).append("</div>\n");
            }
            case EMBLEMA -> {
                String status = c.getPropriedade("status", "primario");
                sb.append(indent).append("<span id=\"").append(id).append("\" class=\"").append(TEMA.classeStatus(c.tipo(), status)).append("\"").append(estiloInline).append(">").append(HtmlEscape.escapeHtml(rotulo)).append("</span>\n");
            }
            case ALERTA -> {
                String tipoAlerta = c.getPropriedade("tipoAlerta", "info");
                String texto = c.getPropriedade("texto", rotulo);
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classeStatus(c.tipo(), tipoAlerta)).append("\"").append(estiloInline).append(">")
                        .append(HtmlEscape.escapeHtml(texto)).append("</div>\n");
            }
            case TEXTO_RICO -> {
                String texto = c.getPropriedade("texto", rotulo);
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">").append(HtmlEscape.escapeHtml(texto)).append("</div>\n");
            }
            case TABELA_DADOS -> {
                String rotuloTabela = c.getPropriedade("rotulo", "Tabela");
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append(">\n");
                sb.append(indent).append("  <div class=\"thz-label\" style=\"margin-bottom:8px\">").append(HtmlEscape.escapeHtml(rotuloTabela)).append("</div>\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case DIVISOR -> sb.append(indent).append("<hr class=\"").append(TEMA.classe(c.tipo())).append("\"").append(estiloInline).append("/>\n");
            case ESPACO -> sb.append(indent).append("<div class=\"").append(TEMA.classe(c.tipo())).append("\" style=\"height: 16px;\"></div>\n");
            default -> sb.append(indent).append("<!-- Componente ").append(c.tipo()).append(" -->\n");
        }
    }

    private static String montarEstiloInline(Map<String, String> estilos) {
        if (estilos.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" style=\"");
        for (var e : estilos.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("; ");
        }
        sb.append("\"");
        return sb.toString();
    }

    private static String gerarCss(ThzUiTema t) {
        return """
            :root {
                --thz-bg: %s;
                --thz-bg-card: %s;
                --thz-text: %s;
                --thz-text-muted: %s;
                --thz-primary: %s;
                --thz-secondary: %s;
                --thz-success: %s;
                --thz-warning: %s;
                --thz-danger: %s;
                --thz-border: %s;
                --thz-radius: %dpx;
                --thz-font: %s;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; font-family: var(--thz-font); }
            body { background-color: var(--thz-bg); color: var(--thz-text); padding: 24px; }
            .thz-app-root { max-width: 1200px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px; }
            .thz-flex-row { display: flex; flex-direction: row; gap: 16px; align-items: center; }
            .thz-flex-col { display: flex; flex-direction: column; gap: 16px; }
            .thz-grid { display: grid; gap: 16px; }
            .thz-container { display: flex; flex-direction: column; gap: 16px; width: 100%%; }
            .thz-card {
                background: var(--thz-bg-card);
                border: 1px solid var(--thz-border);
                border-radius: var(--thz-radius);
                padding: 20px;
                backdrop-filter: blur(12px);
                -webkit-backdrop-filter: blur(12px);
                box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.3);
            }
            .thz-card-header { margin-bottom: 14px; border-bottom: 1px solid var(--thz-border); padding-bottom: 8px; }
            .thz-form-group { display: flex; flex-direction: column; gap: 6px; }
            .thz-label { font-size: 0.875rem; font-weight: 600; color: var(--thz-text-muted); }
            .thz-input, .thz-select {
                background: rgba(15, 23, 42, 0.6);
                border: 1px solid var(--thz-border);
                color: var(--thz-text);
                padding: 10px 14px;
                border-radius: var(--thz-radius);
                font-size: 0.95rem;
                outline: none;
                transition: border-color 0.2s, box-shadow 0.2s;
            }
            .thz-input:focus, .thz-select:focus { border-color: var(--thz-primary); box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.25); }
            .thz-btn {
                padding: 10px 20px;
                border-radius: var(--thz-radius);
                font-weight: 600;
                font-size: 0.95rem;
                border: none;
                cursor: pointer;
                transition: transform 0.1s, opacity 0.2s;
            }
            .thz-btn:active { transform: scale(0.98); }
            .thz-btn-primario { background: var(--thz-primary); color: white; }
            .thz-btn-secundario { background: var(--thz-secondary); color: white; }
            .thz-btn-sucesso { background: var(--thz-success); color: white; }
            .thz-btn-perigo { background: var(--thz-danger); color: white; }
            .thz-metric-card {
                background: var(--thz-bg-card);
                border: 1px solid var(--thz-border);
                border-radius: var(--thz-radius);
                padding: 18px;
                display: flex;
                flex-direction: column;
                gap: 4px;
            }
            .thz-metric-title { font-size: 0.85rem; color: var(--thz-text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
            .thz-metric-value { font-size: 1.8rem; font-weight: 700; color: var(--thz-text); }
            .thz-badge {
                display: inline-block;
                padding: 4px 10px;
                border-radius: 9999px;
                font-size: 0.75rem;
                font-weight: 700;
                text-transform: uppercase;
            }
            .thz-badge-primario { background: rgba(59, 130, 246, 0.2); color: #60a5fa; border: 1px solid rgba(59, 130, 246, 0.4); }
            .thz-badge-sucesso { background: rgba(16, 185, 129, 0.2); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.4); }
            .thz-badge-aviso { background: rgba(245, 158, 11, 0.2); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.4); }
            .thz-badge-perigo { background: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.4); }
            .thz-alert { padding: 12px 16px; border-radius: var(--thz-radius); font-size: 0.9rem; border-left: 4px solid var(--thz-primary); background: rgba(59, 130, 246, 0.1); }
            .thz-alert-sucesso { border-left-color: var(--thz-success); background: rgba(16,185,129,0.12); color: #a7f3d0; }
            .thz-alert-erro { border-left-color: var(--thz-danger); background: rgba(239,68,68,0.12); color: #fecaca; }
            .thz-alert-info { border-left-color: var(--thz-primary); background: rgba(59,130,246,0.1); }
            .thz-tabela-wrapper { border: 1px solid var(--thz-border); border-radius: var(--thz-radius); padding: 12px; background: rgba(255,255,255,0.02); }
            .thz-divider { border: 0; border-top: 1px solid var(--thz-border); margin: 8px 0; }
            .thz-switch-wrapper { display: flex; align-items: center; gap: 10px; }
            .thz-switch { position: relative; display: inline-block; width: 44px; height: 24px; }
            .thz-switch input { opacity: 0; width: 0; height: 0; }
            .thz-slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(255,255,255,0.2); transition: .3s; border-radius: 24px; }
            .thz-slider:before { position: absolute; content: ""; height: 18px; width: 18px; left: 3px; bottom: 3px; background-color: white; transition: .3s; border-radius: 50%%; }
            input:checked + .thz-slider { background-color: var(--thz-primary); }
            input:checked + .thz-slider:before { transform: translateX(20px); }
        """.formatted(
                t.corFundo(), t.corFundoCard(), t.corTexto(), t.corTextoSecundario(),
                t.corPrimaria(), t.corSecundaria(), t.corSucesso(), t.corAviso(), t.corErro(),
                t.corBorda(), t.raioBordaPx(), t.fonteFamilia()
        );
    }

    private static String gerarJs() {
        return """
            const _estadoInterno = {};
            // hidrata estado inicial a partir dos inputs existentes
            document.addEventListener('DOMContentLoaded', () => {
              document.querySelectorAll('[data-vinculo]').forEach(el => {
                const v = el.getAttribute('data-vinculo');
                if (v && el.value !== undefined) _estadoInterno[v] = el.type === 'checkbox' ? el.checked : el.value;
              });
            });
            window.thzEstado = new Proxy(_estadoInterno, {
                set(target, prop, val) {
                    target[prop] = val;
                    document.querySelectorAll('[data-vinculo="' + prop + '"], [data-thz-vinculo="' + prop + '"]').forEach(el => {
                        if ('value' in el && el.tagName !== 'DIV' && el.tagName !== 'SPAN') {
                            if (el.type === 'checkbox') el.checked = !!val;
                            else if (el.value !== String(val)) el.value = val;
                        } else {
                            el.textContent = val;
                        }
                    });
                    return true;
                }
            });
            function thzVinculoAtualizado(vinculo, valor) { window.thzEstado[vinculo] = valor; }
            function thzMostrarResultado(tipo, texto) {
              let el = document.getElementById('thz_resultado');
              if (!el) {
                el = document.createElement('div');
                el.id = 'thz_resultado';
                el.style.marginTop = '16px';
                document.querySelector('.thz-app-root')?.appendChild(el);
              }
              const cls = tipo === 'erro' ? 'thz-alert thz-alert-erro' : tipo === 'sucesso' ? 'thz-alert thz-alert-sucesso' : 'thz-alert thz-alert-info';
              el.className = cls;
              el.style.padding = '12px 16px';
              el.style.borderRadius = '8px';
              el.style.whiteSpace = 'pre-wrap';
              el.textContent = texto;
              el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
            async function thzDespacharAcao(acao, idComponente) {
                if (!acao) return;
                if (acao === '__thz_restaurar__') { location.reload(); return; }
                const btn = document.getElementById(idComponente);
                const orig = btn ? btn.textContent : '';
                if (btn) { btn.disabled = true; btn.textContent = 'Processando...'; }
                try {
                  if (window.thz && typeof window.thz.invocar === 'function') {
                    const resp = await window.thz.invocar(acao, { componenteId: idComponente, estado: window.thzEstado });
                    if (resp && resp.status === 'ok') {
                      thzMostrarResultado('sucesso', '✓ ' + (resp.resultado || resp.mensagem || 'Operação concluída.'));
                      window.dispatchEvent(new CustomEvent('thz:operacao_sucesso', { detail: resp }));
                    } else {
                      thzMostrarResultado('erro', '✗ ' + (resp.erro || resp.mensagem || JSON.stringify(resp)));
                    }
                  } else {
                    console.log('[THZ UI] Ação despachada:', acao, window.thzEstado);
                  }
                } catch(e) {
                  thzMostrarResultado('erro', '✗ Erro de comunicação: ' + (e.message || e));
                } finally {
                  if (btn) { btn.disabled = false; btn.textContent = orig; }
                }
            }
            // escuta eventos server->js
            if (window.thz) {
              window.thz.ouvir('operacao_sucesso', d => thzMostrarResultado('sucesso', '✓ ' + JSON.stringify(d)));
              window.thz.ouvir('operacao_erro', d => thzMostrarResultado('erro', '✗ ' + JSON.stringify(d)));
            }
        """;
    }


}
