package de.lgstool.geo.expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns an input string into a list of {@link Token}s. Whitespace is skipped;
 * numbers may contain a single decimal point; identifiers start with a letter and
 * may contain letters, digits and underscores.
 */
final class Lexer {

    private final String src;
    private int pos;

    Lexer(String src) {
        this.src = src == null ? "" : src;
    }

    List<Token> tokens() {
        List<Token> out = new ArrayList<>();
        Token t;
        do {
            t = next();
            out.add(t);
        } while (t.type() != TokenType.EOF);
        return out;
    }

    private Token next() {
        skipWhitespace();
        if (pos >= src.length()) {
            return new Token(TokenType.EOF, "", 0, pos);
        }
        char c = src.charAt(pos);
        if (Character.isDigit(c) || (c == '.' && pos + 1 < src.length() && Character.isDigit(src.charAt(pos + 1)))) {
            return number();
        }
        if (Character.isLetter(c)) {
            return ident();
        }
        int at = pos;
        pos++;
        return switch (c) {
            case '+' -> new Token(TokenType.PLUS, "+", 0, at);
            case '-' -> new Token(TokenType.MINUS, "-", 0, at);
            case '*' -> new Token(TokenType.STAR, "*", 0, at);
            case '/' -> new Token(TokenType.SLASH, "/", 0, at);
            case '^' -> new Token(TokenType.CARET, "^", 0, at);
            case '(' -> new Token(TokenType.LPAREN, "(", 0, at);
            case ')' -> new Token(TokenType.RPAREN, ")", 0, at);
            case ',' -> new Token(TokenType.COMMA, ",", 0, at);
            case '=' -> new Token(TokenType.EQUALS, "=", 0, at);
            default -> throw new EvalException("Unerwartetes Zeichen '" + c + "'.");
        };
    }

    private Token number() {
        int start = pos;
        boolean dot = false;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isDigit(c)) {
                pos++;
            } else if (c == '.' && !dot) {
                dot = true;
                pos++;
            } else {
                break;
            }
        }
        String text = src.substring(start, pos);
        try {
            return new Token(TokenType.NUMBER, text, Double.parseDouble(text), start);
        } catch (NumberFormatException e) {
            throw new EvalException("Ungültige Zahl '" + text + "'.");
        }
    }

    private Token ident() {
        int start = pos;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') {
                pos++;
            } else {
                break;
            }
        }
        String text = src.substring(start, pos);
        return new Token(TokenType.IDENT, text, 0, start);
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }
}
