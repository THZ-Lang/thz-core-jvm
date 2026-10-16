package thz.lang.tui;

import java.util.Arrays;
import java.util.List;

/**
 * ThzTerminalTui — Motor de Terminal User Interface (TUI) para THZ-LANG.
 * Suporta buffer 2D de células, desenho de janelas Unicode UTF-8, barras de progresso,
 * tabelas tabulares e emissão de sequências ANSI VT100 otimizadas.
 */
public final class ThzTerminalTui {

    public record Celula(char caractere, int fgAnsi, int bgAnsi, boolean bold, boolean dim) {
        public static final Celula VAZIA = new Celula(' ', 0, 0, false, false);
    }

    private final int largura;
    private final int altura;
    private final Celula[][] buffer;

    public ThzTerminalTui(int largura, int altura) {
        this.largura = Math.max(10, largura);
        this.altura = Math.max(3, altura);
        this.buffer = new Celula[this.altura][this.largura];
        limpar();
    }

    public int largura() { return largura; }
    public int altura() { return altura; }

    public void limpar() {
        for (int y = 0; y < altura; y++) {
            Arrays.fill(buffer[y], Celula.VAZIA);
        }
    }

    public void definirCelula(int x, int y, char c, int fgAnsi, int bgAnsi, boolean bold, boolean dim) {
        if (x >= 0 && x < largura && y >= 0 && y < altura) {
            buffer[y][x] = new Celula(c, fgAnsi, bgAnsi, bold, dim);
        }
    }

    public void desenharTexto(int x, int y, String texto, int fgAnsi, boolean bold) {
        if (y < 0 || y >= altura || texto == null) return;
        char[] chars = texto.toCharArray();
        for (int i = 0; i < chars.length && (x + i) < largura; i++) {
            if (x + i >= 0) {
                definirCelula(x + i, y, chars[i], fgAnsi, 0, bold, false);
            }
        }
    }

    public void desenharJanela(int x, int y, int w, int h, String titulo, int corBordaAnsi) {
        int x2 = Math.min(x + w - 1, largura - 1);
        int y2 = Math.min(y + h - 1, altura - 1);
        if (x >= x2 || y >= y2) return;

        // Cantos
        definirCelula(x, y, '┌', corBordaAnsi, 0, false, false);
        definirCelula(x2, y, '┐', corBordaAnsi, 0, false, false);
        definirCelula(x, y2, '└', corBordaAnsi, 0, false, false);
        definirCelula(x2, y2, '┘', corBordaAnsi, 0, false, false);

        // Bordas horizontais
        for (int cx = x + 1; cx < x2; cx++) {
            definirCelula(cx, y, '─', corBordaAnsi, 0, false, false);
            definirCelula(cx, y2, '─', corBordaAnsi, 0, false, false);
        }
        // Bordas verticais
        for (int cy = y + 1; cy < y2; cy++) {
            definirCelula(x, cy, '│', corBordaAnsi, 0, false, false);
            definirCelula(x2, cy, '│', corBordaAnsi, 0, false, false);
        }

        // Título centralizado
        if (titulo != null && !titulo.isBlank()) {
            String t = " " + titulo.trim() + " ";
            int posInicio = x + Math.max(1, (w - t.length()) / 2);
            desenharTexto(posInicio, y, t, corBordaAnsi, true);
        }
    }

    public void desenharBarraProgresso(int x, int y, int w, double percentual, int corProgressoAnsi) {
        if (y < 0 || y >= altura || w <= 4) return;
        double pct = Math.max(0.0, Math.min(1.0, percentual));
        String rotulo = String.format(" %3.0f%% ", pct * 100);
        int areaBarras = w - 2;
        int preenchido = (int) Math.round(areaBarras * pct);

        definirCelula(x, y, '[', 37, 0, false, false);
        for (int i = 0; i < areaBarras && (x + 1 + i) < largura; i++) {
            if (i < preenchido) {
                definirCelula(x + 1 + i, y, '█', corProgressoAnsi, 0, true, false);
            } else {
                definirCelula(x + 1 + i, y, '░', 90, 0, false, true);
            }
        }
        definirCelula(x + 1 + areaBarras, y, ']', 37, 0, false, false);

        // Imprime porcentagem se houver espaço
        if (x + w + rotulo.length() <= largura) {
            desenharTexto(x + w, y, rotulo, 37, true);
        }
    }

    public void desenharTabela(int x, int y, List<String> cabecalhos, List<List<String>> linhas, int corCabecalho) {
        if (cabecalhos == null || cabecalhos.isEmpty() || y >= altura) return;
        int numCols = cabecalhos.size();
        int[] larguras = new int[numCols];

        for (int i = 0; i < numCols; i++) {
            larguras[i] = cabecalhos.get(i).length();
        }
        if (linhas != null) {
            for (List<String> linha : linhas) {
                for (int i = 0; i < Math.min(numCols, linha.size()); i++) {
                    larguras[i] = Math.max(larguras[i], linha.get(i).length());
                }
            }
        }

        // Desenha Cabeçalho
        int curX = x;
        for (int i = 0; i < numCols; i++) {
            desenharTexto(curX, y, cabecalhos.get(i), corCabecalho, true);
            curX += larguras[i] + 3;
        }

        // Linha divisória
        int larguraTotal = curX - x;
        if (y + 1 < altura) {
            for (int i = 0; i < larguraTotal && (x + i) < largura; i++) {
                definirCelula(x + i, y + 1, '─', 90, 0, false, false);
            }
        }

        // Linhas de dados
        if (linhas != null) {
            for (int r = 0; r < linhas.size(); r++) {
                int linhaY = y + 2 + r;
                if (linhaY >= altura) break;
                List<String> dados = linhas.get(r);
                curX = x;
                for (int i = 0; i < Math.min(numCols, dados.size()); i++) {
                    desenharTexto(curX, linhaY, dados.get(i), 37, false);
                    curX += larguras[i] + 3;
                }
            }
        }
    }

    public String renderizarString() {
        StringBuilder sb = new StringBuilder();
        int ultimoFg = 0;
        int ultimoBg = 0;
        boolean ultimoBold = false;

        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                Celula cel = buffer[y][x];
                if (cel.fgAnsi() != ultimoFg || cel.bgAnsi() != ultimoBg || cel.bold() != ultimoBold) {
                    if (cel.fgAnsi() == 0 && cel.bgAnsi() == 0 && !cel.bold()) {
                        sb.append("\033[0m");
                    } else {
                        sb.append("\033[0");
                        if (cel.bold()) sb.append(";1");
                        if (cel.dim()) sb.append(";2");
                        if (cel.fgAnsi() > 0) sb.append(";").append(cel.fgAnsi());
                        if (cel.bgAnsi() > 0) sb.append(";").append(cel.bgAnsi());
                        sb.append("m");
                    }
                    ultimoFg = cel.fgAnsi();
                    ultimoBg = cel.bgAnsi();
                    ultimoBold = cel.bold();
                }
                sb.append(cel.caractere());
            }
            if (ultimoFg != 0 || ultimoBg != 0 || ultimoBold) {
                sb.append("\033[0m");
                ultimoFg = 0;
                ultimoBg = 0;
                ultimoBold = false;
            }
            if (y + 1 < altura) sb.append('\n');
        }
        return sb.toString();
    }
}
