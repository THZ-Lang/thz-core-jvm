package thz.lang.diagnosticos;

import java.util.List;

public final class Diagnosticos {
    private static final int LARGURA_NUMERO = 5;
    private Diagnosticos() {}

    public static String formatarErroComCaret(String fonte, DiagnosticoEntrada e) {
        String[] linhas = fonte.split("\\r?\\n", -1);
        int idx = Math.min(Math.max(e.linha() - 1, 0), Math.max(linhas.length - 1, 0));
        String conteudo = linhas.length > 0 ? linhas[idx] : "";
        int coluna = Math.max(1, e.coluna());
        String cabecalho = "[Erro][Linha " + e.linha() + ":" + coluna + "] " + e.mensagem();
        String prefixo = "       | ";
        String linhaFonte = prefixo.replace("|", "") + String.format("%" + LARGURA_NUMERO + "d", e.linha()) + " | " + conteudo;
        String caret = "         " + " ".repeat(LARGURA_NUMERO) + " | " + " ".repeat(coluna - 1) + "^";
        return cabecalho + "\n" + linhaFonte + "\n" + caret;
    }

    public static List<String> formatarDiagnosticos(String fonte, List<DiagnosticoEntrada> diags, String rotulo) {
        return diags.stream().map(d -> {
            String bloco = formatarErroComCaret(fonte, d);
            if (rotulo == null || rotulo.isEmpty()) return bloco;
            return bloco.replaceFirst("^\\[Erro\\]", "[Erro " + rotulo + "]");
        }).toList();
    }
}
