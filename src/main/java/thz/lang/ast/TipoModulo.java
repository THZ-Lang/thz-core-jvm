package thz.lang.ast;

/**
 * Arquétipos de Módulo do THZ-LANG.
 */
public enum TipoModulo {
    PROGRAMA("PROGRAMA"),
    PROGRAMA_VISUAL("PROGRAMA VISUAL"),
    PROGRAMA_NEGOCIO("PROGRAMA NEGOCIO"),
    PROGRAMA_ARQUITETURA("PROGRAMA ARQUITETURA"),
    BIBLIOTECA("BIBLIOTECA"),
    EXTENSAO("EXTENSAO"),
    FERRAMENTA("FERRAMENTA"),
    TESTE("TESTE"),
    TELA("TELA"),
    PIPELINE_DADOS("PIPELINE_DADOS");

    private final String descricao;

    TipoModulo(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    public boolean ehPrograma() {
        return this == PROGRAMA || this == PROGRAMA_VISUAL || this == PROGRAMA_NEGOCIO || this == PROGRAMA_ARQUITETURA || this == TELA || this == PIPELINE_DADOS;
    }

    public String terminadorPadrao() {
        return switch (this) {
            case PROGRAMA, PROGRAMA_VISUAL, PROGRAMA_NEGOCIO, PROGRAMA_ARQUITETURA -> "FIM_PROGRAMA";
            case BIBLIOTECA -> "FIM_BIBLIOTECA";
            case EXTENSAO -> "FIM_EXTENSAO";
            case FERRAMENTA -> "FIM_FERRAMENTA";
            case TESTE -> "FIM_TESTE";
            case TELA -> "FIM_TELA";
            case PIPELINE_DADOS -> "FIM_PIPELINE";
        };
    }
}
