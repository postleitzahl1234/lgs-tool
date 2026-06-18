package de.lgstool.geo.expr;

/** A single lexical token. {@code number} is meaningful only for {@link TokenType#NUMBER}. */
record Token(TokenType type, String text, double number, int pos) {
}
