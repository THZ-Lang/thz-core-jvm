package thz.lang.lexico;

public record Token(TokenType type, String value, int line, int column) {}
