package org.mapleir.dot4j.parse;

import static org.mapleir.dot4j.parse.Token.*;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

class Lexer {
    private static final Map<String, Integer> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("strict", Token.STRICT);
        KEYWORDS.put("graph", Token.GRAPH);
        KEYWORDS.put("digraph", Token.DIGRAPH);
        KEYWORDS.put("node", Token.NODE);
        KEYWORDS.put("edge", Token.EDGE);
        KEYWORDS.put("subgraph", Token.SUBGRAPH);
    }

    private static final char CH_EOF = (char) -1;
    private final PushbackReader in;
    private char ch;
    Position pos;

    public Lexer(Reader in, String name) throws IOException {
        this.in = new PushbackReader(in);
        pos = new Position(name);
        readChar();
    }

    public Token token() throws IOException {
        final Token sym = symbol();
        if (sym != null) {
            readChar();
            return sym;
        }
        return numeralOrIdent();
    }

    private Token symbol() throws IOException {
        switch (ch) {
            case CH_EOF:
                return new Token(EOF, ch);
            case ';':
                return new Token(SEMICOLON, ch);
            case ',':
                return new Token(COMMA, ch);
            case '{':
                return new Token(BRACE_OPEN, ch);
            case '}':
                return new Token(BRACE_CLOSE, ch);
            case '=':
                return new Token(EQUAL, ch);
            case '[':
                return new Token(BRACKET_OPEN, ch);
            case ']':
                return new Token(BRACKET_CLOSE, ch);
            case ':':
                return new Token(COLON, ch);
            case '-':
                final char next = readRawChar();
                if (next == '-') {
                    return new Token(MINUS_MINUS, "--");
                }
                if (next == '>') {
                    return new Token(ARROW, "->");
                }
                unread('-', next);
                return null;
            default:
                return null;
        }
    }

    private Token numeralOrIdent() throws IOException {
        if (ch == '-' || ch == '.' || (ch >= '0' && ch <= '9')) {
            return numeral();
        }
        return ident();
    }

    private Token numeral() throws IOException {
        final StringBuilder s = new StringBuilder();
        do {
            s.append(ch);
            readRawChar();
        } while (ch == '.' || (ch >= '0' && ch <= '9'));
        sync();
        return new Token(ID, SUB_NUMERAL, s.toString());
    }

    private Token ident() throws IOException {
        if (ch == '"') {
            return quotedIdent();
        }
        if (ch == '<') {
            return htmlIdent();
        }
        if (isIdentStart()) {
            return simpleIdent();
        }
        throw new ParserException(pos, "Found unexpected character '" + ch + "'");
    }

    private boolean isIdentStart() {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= 128 && ch <= 255) || ch == '_';
    }

    private Token quotedIdent() throws IOException {
        final StringBuilder s = new StringBuilder();
        readRawChar();
        while (ch != '"' && ch != CH_EOF) {
            s.append(ch);
            readRawChar();
            if (ch == '"' && s.charAt(s.length() - 1) == '\\') {
                s.replace(s.length() - 1, s.length(), "\"");
                readRawChar();
            }
            if (ch == '\n' && s.charAt(s.length() - 1) == '\\') {
                s.delete(s.length() - 1, s.length());
                readRawChar();
            }
        }
        readChar();
        return new Token(ID, SUB_QUOTED, s.toString());
    }

    private Token htmlIdent() throws IOException {
        final StringBuilder s = new StringBuilder();
        int level = 1;
        readRawChar();
        level = htmlLevel(level, ch);
        while ((ch != '>' || level > 0) && ch != CH_EOF) {
            s.append(ch);
            readRawChar();
            level = htmlLevel(level, ch);
        }
        readChar();
        return new Token(ID, SUB_HTML, s.toString());
    }

    private int htmlLevel(int level, char ch) {
        if (ch == '<') {
            return level + 1;
        }
        if (ch == '>') {
            return level - 1;
        }
        return level;
    }

    private Token simpleIdent() throws IOException {
        final StringBuilder s = new StringBuilder();
        do {
            s.append(ch);
            readRawChar();
        } while ((isIdentStart() || (ch >= '0' && ch <= '9')) && ch != CH_EOF);
        sync();
        final Integer key = KEYWORDS.get(s.toString().toLowerCase());
        return key == null ? new Token(ID, SUB_SIMPLE, s.toString()) : new Token(key, s.toString());
    }

    private void sync() throws IOException {
        if (ch <= ' ') {
            readChar();
        }
    }

    private char readChar() throws IOException {
        do {
            readRawChar();
            if (ch == '/') {
                readComment();
            } else if (ch == '\n') {
                pos.newLine();
                final char next = readRawChar();
                if (next == '#') {
                    do {
                        readRawChar();
                    } while (ch != '\n' && ch != CH_EOF);
                } else {
                    unread('\n', next);
                }
            }
        } while (ch <= ' ' && ch != CH_EOF);
        return ch;
    }

    private void readComment() throws IOException {
        final char next = readRawChar();
        if (next == '/') {
            do {
                readRawChar();
            } while (ch != '\n' && ch != CH_EOF);
        } else if (next == '*') {
            do {
                do {
                    readRawChar();
                } while (ch != '*' && ch != CH_EOF);
                readRawChar();
            } while (ch != '/' && ch != CH_EOF);
            readRawChar();
        } else {
            unread('/', next);
        }
    }

    private char readRawChar() throws IOException {
        pos.newChar();
        return ch = (char) in.read();
    }

    private void unread(char before, char next) throws IOException {
        ch = before;
        in.unread(next);
    }
}
