package thz.lang.lexico;

import java.util.ArrayList;
import java.util.List;

public class ThzLexer {
    private final String input;
    private int pos = 0;
    private int line = 1;
    private int col = 1;
    private DialetoLinguagem dialeto = DialetoLinguagem.PT_BR;

    public ThzLexer(String input) {
        this.input = SintaxeEnxuta.normalizar(input);
        this.dialeto = detectarDialetoCabecalho(input);
    }

    public ThzLexer(String input, DialetoLinguagem dialeto) {
        this.input = SintaxeEnxuta.normalizar(input);
        this.dialeto = dialeto != null ? dialeto : detectarDialetoCabecalho(input);
    }

    public DialetoLinguagem getDialeto() {
        return dialeto;
    }

    /**
     * Inspeciona as primeiras linhas do código fonte para detectar diretivas de dialeto
     * como 'LINGUAGEM: pt-BR' ou 'LANGUAGE: en-US' (inclusive em comentários).
     */
    private static DialetoLinguagem detectarDialetoCabecalho(String src) {
        if (src == null || src.isEmpty()) return DialetoLinguagem.PT_BR;
        String[] linhas = src.split("\\R", 10);
        for (int i = 0; i < linhas.length; i++) {
            String l = linhas[i];
            String trim = l.trim();
            if (trim.startsWith("#")) {
                trim = trim.substring(1).trim();
            }
            if (trim.isEmpty()) continue;
            int col = l.indexOf(trim) + 1;
            int numLinha = i + 1;

            String upper = trim.toUpperCase(java.util.Locale.ROOT);
            if (upper.startsWith("LINGUAGEM:") || upper.startsWith("LANGUAGE:")
                    || upper.startsWith("DIALETO:") || upper.startsWith("DIALECT:")) {
                int idx = trim.indexOf(':');
                String valor = trim.substring(idx + 1).trim();
                return DialetoLinguagem.detectarComPosicao(valor, numLinha, col);
            }
            if (upper.startsWith("LANG ") || upper.startsWith("LANG:")) {
                int start = upper.startsWith("LANG:") ? 5 : 4;
                String valor = trim.substring(start).trim();
                return DialetoLinguagem.detectarComPosicao(valor, numLinha, col);
            }
        }
        return DialetoLinguagem.PT_BR;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        // Tolerância Dual-OS: descarta BOM UTF-8 (U+FEFF) no início do arquivo.
        if (pos < input.length() && input.charAt(pos) == '\uFEFF') advance();

        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\r') { advance(); continue; }
            if (c == '\n') { line++; col = 1; pos++; continue; }

            // Comentários de linha (# ...)
            if (c == '#') {
                while (pos < input.length() && input.charAt(pos) != '\n') advance();
                continue;
            }

            // Diretivas de Cabeçalho na raiz (ex: LINGUAGEM: pt-BR ou LANGUAGE: en-US)
            if (verificarDiretivaCabecalho()) {
                continue;
            }

            if (c == '"') { tokens.add(readString()); continue; }
            if (Character.isDigit(c)) { tokens.add(readNumber()); continue; }
            if (c == ':') {
                char nxt = pos + 1 < input.length() ? input.charAt(pos + 1) : 0;
                if (nxt == '=') {
                    tokens.add(make(TokenType.SETA_ATRIBUICAO, ":="));
                    advance(); advance(); continue;
                }
                tokens.add(make(TokenType.DOIS_PONTOS, ":")); advance(); continue;
            }
            if (c == '=') {
                char nxt = pos + 1 < input.length() ? input.charAt(pos + 1) : 0;
                if (nxt == '=') {
                    tokens.add(make(TokenType.OPERADOR_RELACIONAL, "=="));
                    advance(); advance(); continue;
                }
                tokens.add(make(TokenType.OPERADOR_RELACIONAL, "=")); advance(); continue;
            }
            if (c == '.') { tokens.add(make(TokenType.PONTO, ".")); advance(); continue; }
            if (c == ',') { tokens.add(make(TokenType.VIRGULA, ",")); advance(); continue; }
            if (c == '(') { tokens.add(make(TokenType.ABRE_PARENTESE, "(")); advance(); continue; }
            if (c == ')') { tokens.add(make(TokenType.FECHA_PARENTESE, ")")); advance(); continue; }
            if (c == '[') { tokens.add(make(TokenType.ABRE_COLCHETE, "[")); advance(); continue; }
            if (c == ']') { tokens.add(make(TokenType.FECHA_COLCHETE, "]")); advance(); continue; }
            if (c == '{') { tokens.add(make(TokenType.ABRE_CHAVE, "{")); advance(); continue; }
            if (c == '}') { tokens.add(make(TokenType.FECHA_CHAVE, "}")); advance(); continue; }
            if (c == '%') { tokens.add(make(TokenType.OPERADOR_ARITMETICO, "%")); advance(); continue; }
            if (c == '+') { tokens.add(make(TokenType.OPERADOR_ARITMETICO, "+")); advance(); continue; }
            if (c == '-') {
                char nxt = pos + 1 < input.length() ? input.charAt(pos + 1) : 0;
                if (nxt == '>') {
                    tokens.add(make(TokenType.SETA_CASO, "->"));
                    advance(); advance(); continue;
                }
                tokens.add(make(TokenType.OPERADOR_ARITMETICO, "-")); advance(); continue;
            }
            if (c == '!') {
                char nxt = pos + 1 < input.length() ? input.charAt(pos + 1) : 0;
                if (nxt == '=') {
                    tokens.add(make(TokenType.OPERADOR_RELACIONAL, "<>"));
                    advance(); advance(); continue;
                }
            }
            if (c == '*') { tokens.add(make(TokenType.OPERADOR_ARITMETICO, "*")); advance(); continue; }
            if (c == '/') { tokens.add(make(TokenType.OPERADOR_ARITMETICO, "/")); advance(); continue; }
            if (c == '<') {
                char nxt = pos + 1 < input.length() ? input.charAt(pos + 1) : 0;
                if (nxt == '-') { tokens.add(make(TokenType.SETA_ATRIBUICAO, "<-")); advance(); advance(); continue; }
                if (nxt == '=') { tokens.add(make(TokenType.OPERADOR_RELACIONAL, "<=")); advance(); advance(); continue; }
                if (nxt == '>') { tokens.add(make(TokenType.OPERADOR_RELACIONAL, "<>")); advance(); advance(); continue; }
                tokens.add(make(TokenType.OPERADOR_RELACIONAL, "<")); advance(); continue;
            }
            if (c == '>') {
                char nxt = pos + 1 < input.length() ? input.charAt(pos + 1) : 0;
                if (nxt == '=') { tokens.add(make(TokenType.OPERADOR_RELACIONAL, ">=")); advance(); advance(); continue; }
                tokens.add(make(TokenType.OPERADOR_RELACIONAL, ">")); advance(); continue;
            }
            if (Character.isLetter(c) || c == '_') {
                Token tok = readIdentifier();
                tokens.add(tok);
                if (tok.type() == TokenType.BLOCO_NATIVO_RUST) {
                    tokens.add(readRawNativeBlock());
                }
                continue;
            }
            throw new ErroLexico(line, col, "Caractere não reconhecido pela gramática THZ: '" + c + "'.");
        }
        tokens.add(new Token(TokenType.EOF, "", line, col));
        return tokens;
    }

    private Token readRawNativeBlock() {
        int startLine = line, startCol = col;
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            if (pos + 16 <= input.length()) {
                String sub = input.substring(pos, pos + 16);
                if (sub.equalsIgnoreCase("FIM_BLOCO_NATIVO") || sub.equalsIgnoreCase("END_NATIVE_BLOCK")) {
                    break;
                }
            }
            if (pos + 10 <= input.length()) {
                String sub = input.substring(pos, pos + 10);
                if (sub.equalsIgnoreCase("FIM_NATIVO") || sub.equalsIgnoreCase("END_NATIVE")) {
                    break;
                }
            }

            char c = input.charAt(pos);
            if (c == '\n') {
                line++;
                col = 1;
                pos++;
                sb.append('\n');
            } else {
                sb.append(c);
                pos++;
                col++;
            }
        }
        return new Token(TokenType.STRING_LITERAL, sb.toString().trim(), startLine, startCol);
    }

    private boolean verificarDiretivaCabecalho() {
        int tempPos = pos;
        StringBuilder sb = new StringBuilder();
        while (tempPos < input.length() && (Character.isLetter(input.charAt(tempPos)) || input.charAt(tempPos) == '_')) {
            sb.append(input.charAt(tempPos));
            tempPos++;
        }
        String palavra = sb.toString().toUpperCase(java.util.Locale.ROOT);
        if (palavra.equals("LINGUAGEM") || palavra.equals("LANGUAGE") || palavra.equals("DIALETO") || palavra.equals("DIALECT")) {
            while (tempPos < input.length() && (input.charAt(tempPos) == ' ' || input.charAt(tempPos) == '\t')) tempPos++;
            if (tempPos < input.length() && input.charAt(tempPos) == ':') {
                // Consome a linha inteira da diretiva
                while (pos < input.length() && input.charAt(pos) != '\n') {
                    advance();
                }
                return true;
            }
        }
        if (palavra.equals("LANG")) {
            while (tempPos < input.length() && (input.charAt(tempPos) == ' ' || input.charAt(tempPos) == '\t')) tempPos++;
            if (tempPos < input.length() && (input.charAt(tempPos) == ':' || Character.isLetter(input.charAt(tempPos)))) {
                // Consome a linha inteira da diretiva lang
                while (pos < input.length() && input.charAt(pos) != '\n') {
                    advance();
                }
                return true;
            }
        }
        return false;
    }

    private void advance() { pos++; col++; }

    private Token make(TokenType t, String v) { return new Token(t, v, line, col); }

    private Token readString() {
        int startLine = line, startCol = col;
        advance();
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && input.charAt(pos) != '"' && input.charAt(pos) != '\n') {
            if (input.charAt(pos) == '\\') {
                advance();
                if (pos >= input.length()) break;
                char esc = input.charAt(pos);
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'u' -> {
                        // Unicode escape hexadecimal de 4 dígitos (RFC 3629 / ISO 10646)
                        if (pos + 4 < input.length()) {
                            String hex = input.substring(pos + 1, pos + 5);
                            try {
                                int code = Integer.parseInt(hex, 16);
                                sb.append((char) code);
                                advance(); advance(); advance(); advance();
                            } catch (NumberFormatException ignored) {
                                sb.append("\\u");
                            }
                        } else {
                            sb.append("\\u");
                        }
                    }
                    default -> sb.append(esc);
                }
                advance();
            } else {
                sb.append(input.charAt(pos));
                advance();
            }
        }
        if (pos >= input.length() || input.charAt(pos) != '"') {
            throw new ErroLexico(startLine, startCol, "Literal de texto não terminado (esperada aspa dupla de fechamento).");
        }
        advance();
        return new Token(TokenType.STRING_LITERAL, sb.toString(), startLine, startCol);
    }

    private Token readNumber() {
        int startCol = col;
        StringBuilder num = new StringBuilder();
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (ch == '.' && (pos + 1 >= input.length() || !Character.isDigit(input.charAt(pos + 1)))) break;
            if (ch == '_' || ch == '.' || Character.isDigit(ch)) {
                if (ch != '_') num.append(ch);
                advance();
            } else break;
        }
        String s = num.toString();
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return new Token(TokenType.NUMERO_LITERAL, s, line, startCol);
    }

    private Token readIdentifier() {
        int startCol = col;
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (Character.isLetterOrDigit(ch) || ch == '_') { sb.append(ch); advance(); }
            else break;
        }
        String ident = sb.toString();

        // Checagem de pureza de dialeto estrito
        PalavrasReservadas.validarPurezaDialeto(ident, dialeto, line, startCol);

        TokenType t = PalavrasReservadas.tokenDe(ident, dialeto);
        if (t == null) t = TokenType.IDENTIFICADOR;
        return new Token(t, ident, line, startCol);
    }
}
