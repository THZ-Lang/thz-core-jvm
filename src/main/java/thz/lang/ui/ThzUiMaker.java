package thz.lang.ui;

import java.util.*;
import java.util.function.Consumer;

/**
 * Fluent API Builder para criação, estilização e composição declarativa de interfaces no THZ-LANG.
 */
public final class ThzUiMaker {

    private final String id;
    private final ThzUiComponente.TipoUi tipo;
    private final Map<String, Object> propriedades = new LinkedHashMap<>();
    private final Map<String, String> estilos = new LinkedHashMap<>();
    private final List<ThzUiComponente> filhos = new ArrayList<>();
    private final Map<String, String> eventos = new LinkedHashMap<>();

    private ThzUiMaker(String id, ThzUiComponente.TipoUi tipo) {
        this.id = id;
        this.tipo = tipo;
    }

    // ---- Builders Estáticos ----

    public static ThzUiMaker novo(String id, ThzUiComponente.TipoUi tipo) {
        return new ThzUiMaker(id, tipo);
    }

    public static ThzUiMaker container(String id, Consumer<ThzUiMaker> builder) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.CONTAINER);
        if (builder != null) builder.accept(m);
        return m;
    }

    public static ThzUiMaker linha(String id, Consumer<ThzUiMaker> builder) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.LINHA);
        if (builder != null) builder.accept(m);
        return m;
    }

    public static ThzUiMaker coluna(String id, Consumer<ThzUiMaker> builder) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.COLUNA);
        if (builder != null) builder.accept(m);
        return m;
    }

    public static ThzUiMaker grade(String id, int colunas, Consumer<ThzUiMaker> builder) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.GRADE);
        m.propriedades.put("colunas", colunas);
        if (builder != null) builder.accept(m);
        return m;
    }

    public static ThzUiMaker card(String id, String titulo, Consumer<ThzUiMaker> builder) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.CARD);
        m.propriedades.put("titulo", titulo);
        if (builder != null) builder.accept(m);
        return m;
    }

    public static ThzUiMaker botao(String id, String rotulo, String acao) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.BOTAO);
        m.propriedades.put("rotulo", rotulo);
        m.propriedades.put("variante", "primario");
        if (acao != null) m.eventos.put("aoClicar", acao);
        return m;
    }

    public static ThzUiMaker campoTexto(String id, String rotulo, String placeholder, String vinculo) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.CAMPO_TEXTO);
        m.propriedades.put("rotulo", rotulo);
        m.propriedades.put("placeholder", placeholder);
        m.propriedades.put("vinculo", vinculo != null ? vinculo : id);
        return m;
    }

    public static ThzUiMaker campoNumero(String id, String rotulo, String vinculo) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.CAMPO_NUMERO);
        m.propriedades.put("rotulo", rotulo);
        m.propriedades.put("vinculo", vinculo != null ? vinculo : id);
        return m;
    }

    public static ThzUiMaker campoMoeda(String id, String rotulo, String moeda, String vinculo) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.CAMPO_MOEDA);
        m.propriedades.put("rotulo", rotulo);
        m.propriedades.put("moeda", moeda != null ? moeda : "BRL");
        m.propriedades.put("vinculo", vinculo != null ? vinculo : id);
        return m;
    }

    public static ThzUiMaker campoData(String id, String rotulo, String vinculo) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.CAMPO_DATA);
        m.propriedades.put("rotulo", rotulo);
        m.propriedades.put("vinculo", vinculo != null ? vinculo : id);
        return m;
    }

    public static ThzUiMaker selecao(String id, String rotulo, List<String> opcoes, String vinculo) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.SELECAO);
        m.propriedades.put("rotulo", rotulo);
        m.propriedades.put("opcoes", opcoes);
        m.propriedades.put("vinculo", vinculo != null ? vinculo : id);
        return m;
    }

    public static ThzUiMaker interruptor(String id, String rotulo, String vinculo) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.INTERRUPTOR);
        m.propriedades.put("rotulo", rotulo);
        m.propriedades.put("vinculo", vinculo != null ? vinculo : id);
        return m;
    }

    public static ThzUiMaker metrica(String id, String rotulo, String valor, String tendencia, String status) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.METRICA_CARD);
        m.propriedades.put("rotulo", rotulo);
        m.propriedades.put("valor", valor);
        m.propriedades.put("tendencia", tendencia != null ? tendencia : "");
        m.propriedades.put("status", status != null ? status : "info");
        return m;
    }

    public static ThzUiMaker emblema(String id, String rotulo, String status) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.EMBLEMA);
        m.propriedades.put("rotulo", rotulo);
        m.propriedades.put("status", status != null ? status : "primario");
        return m;
    }

    public static ThzUiMaker alerta(String id, String tipoAlerta, String texto) {
        ThzUiMaker m = new ThzUiMaker(id, ThzUiComponente.TipoUi.ALERTA);
        m.propriedades.put("tipoAlerta", tipoAlerta);
        m.propriedades.put("texto", texto);
        return m;
    }

    public static ThzUiMaker divisor() {
        return new ThzUiMaker("div_" + UUID.randomUUID().toString().substring(0, 8), ThzUiComponente.TipoUi.DIVISOR);
    }

    public static ThzUiMaker espaco() {
        return new ThzUiMaker("spc_" + UUID.randomUUID().toString().substring(0, 8), ThzUiComponente.TipoUi.ESPACO);
    }

    // ---- Métodos de Configuração Fluent ----

    public ThzUiMaker comPropriedade(String chave, Object valor) {
        this.propriedades.put(chave, valor);
        return this;
    }

    public ThzUiMaker comEstilo(String propriedadeCss, String valor) {
        this.estilos.put(propriedadeCss, valor);
        return this;
    }

    public ThzUiMaker comEvento(String evento, String acao) {
        this.eventos.put(evento, acao);
        return this;
    }

    public ThzUiMaker adicionar(ThzUiMaker filho) {
        if (filho != null) {
            this.filhos.add(filho.construir());
        }
        return this;
    }

    public ThzUiMaker adicionar(ThzUiComponente filho) {
        if (filho != null) {
            this.filhos.add(filho);
        }
        return this;
    }

    public ThzUiComponente construir() {
        return new ThzUiComponente(id, tipo, propriedades, estilos, filhos, eventos);
    }

    // ---- Renderizadores e Geradores de Código ----

    public String renderizarHtml(String titulo, ThzUiTema tema) {
        return ThzUiHtmlEmitter.renderizarPaginaCompleta(titulo, construir(), tema != null ? tema : ThzUiTema.escuroGlass());
    }

    public String renderizarVaadin(String titulo, boolean temaEscuro) {
        return ThzUiVaadinEmitter.renderizarPaginaVaadin(titulo, construir(), temaEscuro);
    }

    public String gerarCodigoThz(String nomePrograma) {
        return ThzUiCodeGenerator.gerarCodigoThz(nomePrograma, construir());
    }
}
