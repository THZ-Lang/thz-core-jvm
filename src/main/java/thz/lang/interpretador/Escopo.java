package thz.lang.interpretador;

import java.util.HashMap;
import java.util.Map;

/**
 * Escopos lexicamente encadeados — port exato de Escopo em interpretador.ts.
 */
public class Escopo {
    private final Map<String, ValorThz> variaveis = new HashMap<>();
    private final Escopo pai;

    public Escopo() {
        this.pai = null;
    }

    public Escopo(Escopo pai) {
        this.pai = pai;
    }

    public void definir(String nome, ValorThz valor) {
        variaveis.put(nome, valor);
    }

    /**
     * Atualiza a variável no escopo onde ela foi declarada (sem shadowing acidental).
     * @return true se encontrou e atualizou
     */
    public boolean atualizar(String nome, ValorThz valor) {
        Escopo atual = this;
        while (atual != null) {
            if (atual.variaveis.containsKey(nome)) {
                atual.variaveis.put(nome, valor);
                return true;
            }
            atual = atual.pai;
        }
        return false;
    }

    public ValorThz resolver(String nome) {
        Escopo atual = this;
        while (atual != null) {
            ValorThz encontrado = atual.variaveis.get(nome);
            if (encontrado != null) return encontrado;
            // Necessário distinguir entre ausente e NULO armazenado — verificamos containsKey
            if (atual.variaveis.containsKey(nome)) {
                return atual.variaveis.get(nome);
            }
            atual = atual.pai;
        }
        return null;
    }

    public Escopo getPai() {
        return pai;
    }

    public Map<String, ValorThz> getVariaveisLocais() {
        return Map.copyOf(variaveis);
    }

    public Map<String, ValorThz> getTodasVariaveis() {
        Map<String, ValorThz> todas = new HashMap<>();
        if (pai != null) {
            todas.putAll(pai.getTodasVariaveis());
        }
        todas.putAll(variaveis);
        return Map.copyOf(todas);
    }
}
