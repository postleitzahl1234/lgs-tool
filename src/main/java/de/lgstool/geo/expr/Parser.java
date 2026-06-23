package de.lgstool.geo.expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for a single expression. Precedence, lowest to highest:
 * {@code + -}, then {@code * /}, then unary {@code -}/{@code +}, then {@code ^}
 * (right-associative). A parenthesized list with commas is a {@link Expr.Tuple}
 * (a point/coordinate); without commas it is grouping.
 *
 * <p>The top-level {@code =} of an assignment is handled by the engine before this
 * parser runs, so a bare {@code =} here is a syntax error.
 */
public final class Parser {

    private final List<Token> tokens;
    private int pos;

    private Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /** Parses {@code src} as a complete expression, requiring all input to be consumed. */
    public static Expr parse(String src) {
        Parser p = new Parser(new Lexer(src).tokens());
        Expr e = p.expr();
        if (p.peek().type() != TokenType.EOF) {
            throw new EvalException("Unerwartetes Zeichen nach Ausdruck: '" + p.peek().text() + "'.");
        }
        return e;
    }

    private Expr expr() {
        return orExpr();
    }

    private Expr orExpr() {
        Expr left = andExpr();
        while (peek().type() == TokenType.PIPE) {
            advance();
            left = new Expr.Bin('|', left, andExpr());
        }
        return left;
    }

    private Expr andExpr() {
        Expr left = comparison();
        while (peek().type() == TokenType.AMP) {
            advance();
            left = new Expr.Bin('&', left, comparison());
        }
        return left;
    }

    /** A single (non-associative) comparison: {@code a < b}, {@code a == b}, … */
    private Expr comparison() {
        Expr left = additive();
        char op = switch (peek().type()) {
            case LT -> '<';
            case GT -> '>';
            case LE -> 'l';     // <=
            case GE -> 'g';     // >=
            case EQEQ -> 'e';   // ==
            case EQUALS -> 'e'; // a lone '=' inside an expression is equality (e.g. Solve(x^2 = 4));
                                // a top-level assignment '=' is consumed by the engine before parsing
            case NEQ -> 'n';    // !=
            default -> 0;
        };
        if (op != 0) {
            advance();
            return new Expr.Bin(op, left, additive());
        }
        return left;
    }

    private Expr additive() {
        Expr left = multiplicative();
        while (peek().type() == TokenType.PLUS || peek().type() == TokenType.MINUS) {
            char op = advance().type() == TokenType.PLUS ? '+' : '-';
            left = new Expr.Bin(op, left, multiplicative());
        }
        return left;
    }

    private Expr multiplicative() {
        Expr left = unary();
        while (peek().type() == TokenType.STAR || peek().type() == TokenType.SLASH) {
            char op = advance().type() == TokenType.STAR ? '*' : '/';
            left = new Expr.Bin(op, left, unary());
        }
        return left;
    }

    private Expr unary() {
        if (peek().type() == TokenType.MINUS) {
            advance();
            return new Expr.Unary('-', unary());
        }
        if (peek().type() == TokenType.PLUS) {
            advance();
            return unary();
        }
        return power();
    }

    private Expr power() {
        Expr base = primary();
        if (peek().type() == TokenType.CARET) {
            advance();
            return new Expr.Bin('^', base, unary());
        }
        return base;
    }

    private Expr primary() {
        Token t = peek();
        switch (t.type()) {
            case NUMBER -> {
                advance();
                Expr n = new Expr.Num(t.number(), t.text());
                // Implicit multiplication: a number immediately followed by an identifier or
                // a parenthesis (e.g. 4i, 2x, 3pi, 2(x+1)) means multiplication.
                if (peek().type() == TokenType.IDENT || peek().type() == TokenType.LPAREN) {
                    return new Expr.Bin('*', n, power());
                }
                return n;
            }
            case IDENT -> {
                advance();
                if (peek().type() == TokenType.LPAREN) {
                    return new Expr.Call(t.text(), argList());
                }
                return new Expr.Ref(t.text());
            }
            case LPAREN -> {
                return parenOrTuple();
            }
            case LBRACE -> {
                return braceList();
            }
            case STRING -> {
                advance();
                return new Expr.Str(t.text());
            }
            default -> throw new EvalException("Unerwartetes Zeichen: '" + t.text() + "'.");
        }
    }

    private Expr parenOrTuple() {
        expect(TokenType.LPAREN);
        List<Expr> parts = new ArrayList<>();
        parts.add(expr());
        while (peek().type() == TokenType.COMMA) {
            advance();
            parts.add(expr());
        }
        expect(TokenType.RPAREN);
        return parts.size() == 1 ? parts.get(0) : new Expr.Tuple(parts);
    }

    private Expr braceList() {
        expect(TokenType.LBRACE);
        List<Expr> items = new ArrayList<>();
        if (peek().type() != TokenType.RBRACE) {
            items.add(expr());
            while (peek().type() == TokenType.COMMA) {
                advance();
                items.add(expr());
            }
        }
        expect(TokenType.RBRACE);
        return new Expr.ListLit(items);
    }

    private List<Expr> argList() {
        expect(TokenType.LPAREN);
        List<Expr> args = new ArrayList<>();
        if (peek().type() != TokenType.RPAREN) {
            args.add(expr());
            while (peek().type() == TokenType.COMMA) {
                advance();
                args.add(expr());
            }
        }
        expect(TokenType.RPAREN);
        return args;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        return tokens.get(pos++);
    }

    private void expect(TokenType type) {
        if (peek().type() != type) {
            throw new EvalException("Erwarte '" + symbol(type) + "', fand '" + peek().text() + "'.");
        }
        advance();
    }

    private static String symbol(TokenType type) {
        return switch (type) {
            case LPAREN -> "(";
            case RPAREN -> ")";
            case COMMA -> ",";
            default -> type.name();
        };
    }
}
