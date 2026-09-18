package thz.lang.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ThzUiMakerTest {

    @Test
    @DisplayName("ThzUiMaker deve compor árvore de componentes declarativos")
    void testComposicaoDeclarativa() {
        ThzUiComponente tela = ThzUiMaker.container("app_root", raiz -> {
            raiz.adicionar(ThzUiMaker.card("card_faturamento", "Gestão de Faturamento", card -> {
                card.adicionar(ThzUiMaker.metrica("kpi_receita", "Receita Mensal", "R$ 1.250.000,00", "+12%", "sucesso"));
                card.adicionar(ThzUiMaker.divisor());
                card.adicionar(ThzUiMaker.linha("linha_filtros", linha -> {
                    linha.adicionar(ThzUiMaker.campoTexto("txt_cliente", "Cliente", "Nome do cliente", "cliente"));
                    linha.adicionar(ThzUiMaker.campoMoeda("txt_valor", "Valor Mínimo", "BRL", "valorMinimo"));
                    linha.adicionar(ThzUiMaker.selecao("sel_status", "Status", List.of("TODOS", "APROVADO", "PENDENTE"), "status"));
                }));
                card.adicionar(ThzUiMaker.botao("btn_filtrar", "Filtrar Resultados", "AplicarFiltro"));
            }));
        }).construir();

        assertNotNull(tela);
        assertEquals("app_root", tela.id());
        assertEquals(ThzUiComponente.TipoUi.CONTAINER, tela.tipo());
        assertEquals(1, tela.filhos().size());

        ThzUiComponente card = tela.filhos().get(0);
        assertEquals(ThzUiComponente.TipoUi.CARD, card.tipo());
        assertEquals("Gestão de Faturamento", card.getPropriedade("titulo", ""));
        assertEquals(4, card.filhos().size());
    }

    @Test
    @DisplayName("ThzUiMaker deve renderizar página HTML5 completa com CSS Glassmorphism e JS Bridge")
    void testRenderizacaoHtml() {
        var maker = ThzUiMaker.container("painel_dashboard", c -> {
            c.adicionar(ThzUiMaker.alerta("alerta_aviso", "aviso", "Ambiente de homologação"));
            c.adicionar(ThzUiMaker.botao("btn_salvar", "Salvar Dados", "SalvarRegistro"));
        });

        String html = maker.renderizarHtml("Dashboard Corporativo", ThzUiTema.escuroGlass());

        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("Dashboard Corporativo"));
        assertTrue(html.contains("thz-btn-primario"));
        assertTrue(html.contains("thzDespacharAcao('SalvarRegistro', 'btn_salvar')"));
        assertTrue(html.contains("window.thzEstado"));
        assertTrue(html.contains("backdrop-filter: blur(12px)"));
    }

    @Test
    @DisplayName("ThzUiMaker deve gerar código canônico THZ-LANG (PROGRAMA VISUAL)")
    void testGeracaoCodigoThz() {
        var maker = ThzUiMaker.container("raiz", c -> {
            c.adicionar(ThzUiMaker.card("card_vendas", "Cadastro de Pedido", card -> {
                card.adicionar(ThzUiMaker.campoTexto("nome_cliente", "Cliente", "Insira o nome", "cliente"));
                card.adicionar(ThzUiMaker.campoMoeda("total_pedido", "Total", "BRL", "total"));
                card.adicionar(ThzUiMaker.botao("btn_enviar", "Enviar Pedido", "ProcessarPedido"));
            }));
        });

        String codigoThz = maker.gerarCodigoThz("CadastroPedidoVisual");

        assertNotNull(codigoThz);
        assertTrue(codigoThz.contains("PROGRAMA VISUAL CadastroPedidoVisual"));
        assertTrue(codigoThz.contains("METADADOS_ARQUITETURA"));
        assertTrue(codigoThz.contains("PROCEDIMENTO MontarInterface()"));
        assertTrue(codigoThz.contains("TELA.criarContainer"));
        assertTrue(codigoThz.contains("TELA.adicionarBotao"));
        assertTrue(codigoThz.contains("FIM_PROGRAMA"));
    }
}
