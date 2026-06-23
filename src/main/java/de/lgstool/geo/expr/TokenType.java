package de.lgstool.geo.expr;

/** Lexical token kinds for the geometry expression language. */
enum TokenType {
    NUMBER,
    IDENT,
    STRING,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    CARET,
    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    COMMA,
    EQUALS,
    LT,
    GT,
    LE,
    GE,
    EQEQ,
    NEQ,
    AMP,
    PIPE,
    EOF
}
