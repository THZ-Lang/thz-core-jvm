package thz.lang.ui;

/**
 * Transpilador reverso de um schema ThzUiComponente para código canônico THZ-LANG (PROGRAMA VISUAL).
 */
public final class ThzUiCodeGenerator {

    private ThzUiCodeGenerator() {}

    public static String gerarCodigoThz(String nomePrograma, ThzUiComponente raiz) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROGRAMA VISUAL ").append(nomePrograma).append("\n\n");
        sb.append("METADADOS_ARQUITETURA\n");
        sb.append("    DOMINIO: \"InterfaceUsuario\"\n");
        sb.append("    CAMADA: \"Apresentacao\"\n");
        sb.append("    VERSAO: \"1.0.0\"\n");
        sb.append("    AUTOR: \"ThzUiMaker\"\n");
        sb.append("    SLO_LATENCIA_MAXIMA: \"16ms\"\n");
        sb.append("FIM_METADADOS\n\n");

        sb.append("PROCEDIMENTO MontarInterface()\n");
        sb.append("INICIO\n");
        sb.append("    # Interface gerada declarativamente pelo ThzUiMaker\n");
        gerarInstrucoesComponente(raiz, sb, "    ");
        sb.append("    TELA.exibir(\"").append(nomePrograma).append("\")\n");
        sb.append("FIM\n\n");
        sb.append("FIM_PROGRAMA\n");
        return sb.toString();
    }

    private static void gerarInstrucoesComponente(ThzUiComponente c, StringBuilder sb, String indent) {
        if (c == null) return;
        String id = c.id();
        String rotulo = c.getPropriedade("rotulo", "");

        switch (c.tipo()) {
            case CONTAINER, LINHA, COLUNA, GRADE -> {
                sb.append(indent).append("TELA.criarContainer(\"").append(id).append("\", \"").append(c.tipo()).append("\")\n");
                for (ThzUiComponente f : c.filhos()) {
                    gerarInstrucoesComponente(f, sb, indent);
                }
            }
            case CARD -> {
                String titulo = c.getPropriedade("titulo", rotulo);
                sb.append(indent).append("TELA.criarCard(\"").append(id).append("\", \"").append(escapeStr(titulo)).append("\")\n");
                for (ThzUiComponente f : c.filhos()) {
                    gerarInstrucoesComponente(f, sb, indent);
                }
            }
            case BOTAO -> {
                String acao = c.eventos().getOrDefault("aoClicar", "");
                sb.append(indent).append("TELA.adicionarBotao(\"").append(id).append("\", \"").append(escapeStr(rotulo)).append("\", \"").append(escapeStr(acao)).append("\")\n");
            }
            case CAMPO_TEXTO -> {
                String placeholder = c.getPropriedade("placeholder", "");
                sb.append(indent).append("TELA.adicionarCampoTexto(\"").append(id).append("\", \"").append(escapeStr(rotulo)).append("\", \"").append(escapeStr(placeholder)).append("\")\n");
            }
            case CAMPO_NUMERO -> {
                sb.append(indent).append("TELA.adicionarCampoNumero(\"").append(id).append("\", \"").append(escapeStr(rotulo)).append("\")\n");
            }
            case CAMPO_MOEDA -> {
                String moeda = c.getPropriedade("moeda", "BRL");
                sb.append(indent).append("TELA.adicionarCampoMoeda(\"").append(id).append("\", \"").append(escapeStr(rotulo)).append("\", \"").append(moeda).append("\")\n");
            }
            case SELECAO -> {
                sb.append(indent).append("TELA.adicionarSelecao(\"").append(id).append("\", \"").append(escapeStr(rotulo)).append("\")\n");
            }
            case INTERRUPTOR -> {
                sb.append(indent).append("TELA.adicionarInterruptor(\"").append(id).append("\", \"").append(escapeStr(rotulo)).append("\")\n");
            }
            case METRICA_CARD -> {
                String valor = c.getPropriedade("valor", "0");
                sb.append(indent).append("TELA.adicionarMetrica(\"").append(id).append("\", \"").append(escapeStr(rotulo)).append("\", \"").append(escapeStr(valor)).append("\")\n");
            }
            case ALERTA -> {
                String texto = c.getPropriedade("texto", rotulo);
                sb.append(indent).append("TELA.adicionarAlerta(\"").append(id).append("\", \"").append(escapeStr(texto)).append("\")\n");
            }
            default -> {
                sb.append(indent).append("# Componente: ").append(c.tipo()).append(" (").append(id).append(")\n");
            }
        }
    }

    private static String escapeStr(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
