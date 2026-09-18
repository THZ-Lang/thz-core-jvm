package thz.lang.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do ThzUiVaadinEmitter — renderizador de interfaces
 * declarativas THZ-UI para componentes Vaadin Flow com tema Lumo.
 */
class ThzUiVaadinEmitterTest {

    @Test
    void deveRenderizarPaginaVaadinComTemaDarkEComponentesOficiais() {
        var maker = ThzUiMaker.container("raiz", c -> {
            c.adicionar(ThzUiMaker.card("card_principal", "Portal Financeiro", card -> {
                card.adicionar(ThzUiMaker.alerta("alerta_info", "info",
                        "Aplicação servida via Vaadin Flow Engine"));
                card.adicionar(ThzUiMaker.botao("btn_exportar", "Exportar Relatório", "ExportarRelatorio"));
                card.adicionar(ThzUiMaker.botao("btn_dashboard", "Atualizar Dashboard", "AtualizarDashboard"));
            }));
        });

        String html = maker.renderizarVaadin("Portal Financeiro", true);

        assertNotNull(html, "HTML Vaadin não deve ser nulo");
        assertFalse(html.isBlank(), "HTML Vaadin não deve estar vazio");

        // Estrutura HTML5 básica
        assertTrue(html.contains("<!DOCTYPE html>"), "Deve conter doctype HTML5");
        assertTrue(html.contains("theme=\"dark\""), "Deve aplicar tema dark do Vaadin Lumo");
        assertTrue(html.contains("Portal Financeiro"), "Deve conter o título do programa");

        // Tokens CSS oficiais do Vaadin Lumo
        assertTrue(html.contains("--lumo-primary-color"), "Deve conter token Lumo --lumo-primary-color");
        assertTrue(html.contains("--lumo-body-text-color"), "Deve conter token Lumo --lumo-body-text-color");
        assertTrue(html.contains("--lumo-border-radius"), "Deve conter token Lumo --lumo-border-radius");

        // Badge identificador do Vaadin
        assertTrue(html.contains("THZ VAADIN ENGINE"), "Deve conter badge identificador do Vaadin");

        // Componentes renderizados
        assertTrue(html.contains("vaadin-card"), "Deve conter classe vaadin-card");
        assertTrue(html.contains("vaadin-button"), "Deve conter classe vaadin-button");
        assertTrue(html.contains("vaadin-alert"), "Deve conter classe vaadin-alert");

        // Botões com ação RPC
        assertTrue(html.contains("ExportarRelatorio"), "Deve conter ação RPC ExportarRelatorio");
        assertTrue(html.contains("AtualizarDashboard"), "Deve conter ação RPC AtualizarDashboard");
        assertTrue(html.contains("vaadinDespacharAcao"), "Deve conter função JS de despacho RPC Vaadin");

        // Container de notificações Vaadin
        assertTrue(html.contains("vaadin_notification_area"), "Deve conter container de notificações");

        System.out.println("[VAADIN TEST] HTML Vaadin renderizado com sucesso (" + html.length() + " bytes)");
    }

    @Test
    void deveRenderizarPaginaVaadinComTemaLight() {
        var maker = ThzUiMaker.container("raiz", c -> {
            c.adicionar(ThzUiMaker.alerta("alerta_teste", "success", "Teste tema claro"));
        });

        String html = maker.renderizarVaadin("Teste Light", false);

        assertNotNull(html);
        assertTrue(html.contains("theme=\"light\""), "Deve aplicar tema light do Vaadin Lumo");
        assertTrue(html.contains("Teste Light"), "Deve conter título");
        assertTrue(html.contains("#f8fafc"), "Deve conter cor de fundo do tema claro");

        System.out.println("[VAADIN TEST] Tema Light renderizado com sucesso (" + html.length() + " bytes)");
    }

    @Test
    void deveRenderizarCamposDeEntradaVaadin() {
        var maker = ThzUiMaker.container("raiz", c -> {
            c.adicionar(ThzUiMaker.card("card_form", "Formulário", card -> {
                card.adicionar(ThzUiMaker.campoTexto("campo_nome", "Nome Completo", "Digite seu nome", "nome_cliente"));
                card.adicionar(ThzUiMaker.campoMoeda("campo_valor", "Valor Total", "BRL", "valor_total"));
            }));
        });

        String html = maker.renderizarVaadin("Formulário Vaadin", true);

        assertNotNull(html);
        assertTrue(html.contains("vaadin-input"), "Deve conter classe vaadin-input");
        assertTrue(html.contains("vaadin-field-label"), "Deve conter classe vaadin-field-label");
        assertTrue(html.contains("Nome Completo"), "Deve conter rótulo do campo nome");
        assertTrue(html.contains("nome_cliente"), "Deve conter vínculo de dados nome_cliente");
        assertTrue(html.contains("BRL"), "Deve conter moeda BRL no campo monetário");
        assertTrue(html.contains("vaadinAtualizarVinculo"), "Deve conter função JS de vínculo de dados");

        System.out.println("[VAADIN TEST] Campos de entrada renderizados com sucesso (" + html.length() + " bytes)");
    }
}
