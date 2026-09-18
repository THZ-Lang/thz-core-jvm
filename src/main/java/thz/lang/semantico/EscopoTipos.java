package thz.lang.semantico;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tabela de símbolos hierárquica para resolução de tipos em escopos léxicos durante a análise semântica.
 */
public final class EscopoTipos {
    private final Map<String, TipoThz> simbolos = new LinkedHashMap<>();
    private final EscopoTipos pai;

    public EscopoTipos() {
        this.pai = null;
    }

    public EscopoTipos(EscopoTipos pai) {
        this.pai = pai;
    }

    public void definir(String nome, TipoThz tipo, int linha, int coluna, List<ErroSemantico> erros) {
        if (simbolos.containsKey(nome)) {
            erros.add(new ErroSemantico(linha, coluna, "Redeclaração de '" + nome + "' no mesmo escopo."));
            return;
        }
        simbolos.put(nome, tipo);
    }

    public TipoThz resolver(String nome) {
        EscopoTipos atual = this;
        while (atual != null) {
            TipoThz t = atual.simbolos.get(nome);
            if (t != null) return t;
            atual = atual.pai;
        }
        return null;
    }
}
