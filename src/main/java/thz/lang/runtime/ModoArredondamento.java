package thz.lang.runtime;

/**
 * Modos de arredondamento explícitos. Padrão da linguagem: bancário (half-even).
 */
public enum ModoArredondamento {
    /** Half-even: padrão contábil/financeiro, minimiza viés acumulado. */
    BANCARIO,
    /** Half-up: arredondamento escolar/comercial. */
    MEIA_CIMA,
    /** Truncamento em direção ao zero. */
    TRUNCAR
}
