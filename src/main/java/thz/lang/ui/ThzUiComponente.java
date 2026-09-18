package thz.lang.ui;

import java.util.*;

/**
 * Nó fundamental da árvore de componentes declarativa do ThzUiMaker.
 */
public record ThzUiComponente(
        String id,
        TipoUi tipo,
        Map<String, Object> propriedades,
        Map<String, String> estilos,
        List<ThzUiComponente> filhos,
        Map<String, String> eventos
) {
    public enum TipoUi {
        CONTAINER,
        LINHA,
        COLUNA,
        GRADE,
        CARD,
        PAINEL,
        BOTAO,
        CAMPO_TEXTO,
        CAMPO_NUMERO,
        CAMPO_MOEDA,
        CAMPO_DATA,
        SELECAO,
        INTERRUPTOR,
        CHECKBOX,
        RADIO,
        TABELA_DADOS,
        METRICA_CARD,
        GRAFICO,
        EMBLEMA,
        ALERTA,
        TEXTO_RICO,
        DIVISOR,
        ESPACO
    }

    public ThzUiComponente {
        propriedades = propriedades == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(propriedades));
        estilos = estilos == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(estilos));
        filhos = filhos == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(filhos));
        eventos = eventos == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(eventos));
    }

    @SuppressWarnings("unchecked")
    public <T> T getPropriedade(String chave, T padrao) {
        Object v = propriedades.get(chave);
        if (v == null) return padrao;
        return (T) v;
    }

    public String getEstilo(String chave, String padrao) {
        return estilos.getOrDefault(chave, padrao);
    }
}
