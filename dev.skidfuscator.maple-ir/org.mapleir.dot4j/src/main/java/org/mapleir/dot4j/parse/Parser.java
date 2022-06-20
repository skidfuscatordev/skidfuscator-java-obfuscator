package org.mapleir.dot4j.parse;

import static org.mapleir.dot4j.model.Factory.*;
import static org.mapleir.dot4j.parse.Token.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.builtin.ComplexLabel;
import org.mapleir.dot4j.model.*;

public final class Parser {
    private final Lexer lexer;
    private Token token;

    public static DotGraph read(File file) throws IOException {
        return read(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), file.getName());
    }

    public static DotGraph read(InputStream is) throws IOException {
        return read(new InputStreamReader(is, StandardCharsets.UTF_8), "<input stream>");
    }

    public static DotGraph read(String dot) throws IOException {
        return read(new StringReader(dot), "<string>");
    }

    public static DotGraph read(Reader dot, String name) throws IOException {
        return new Parser(new Lexer(dot, name)).parse();
    }

    private Parser(Lexer lexer) throws IOException {
        this.lexer = lexer;
        nextToken();
    }

    private DotGraph parse() {
        return Context.use(ctx -> {
            final DotGraph graph = graph();
            if (token.type == STRICT) {
                graph.setStrict(true);
                nextToken();
            }
            if (token.type == DIGRAPH) {
                graph.setDirected(true);
            } else if (token.type != GRAPH) {
                fail("'graph' or 'digraph' expected");
            }
            nextToken();
            if (token.type == ID) {
                graph.setName(label(token).toString());
                nextToken();
            }
            statementList(graph);
            assertToken(EOF);
            return graph;
        });
    }

    private ComplexLabel label(Token token) {
        return token.subtype == SUB_HTML ? ComplexLabel.html(token.value) : ComplexLabel.of(token.value);
    }

    private void statementList(DotGraph graph) throws IOException {
        assertToken(BRACE_OPEN);
        while (statement(graph)) {
            if (token.type == SEMICOLON) {
                nextToken();
            }
        }
        assertToken(BRACE_CLOSE);
    }

    private boolean statement(DotGraph graph) throws IOException {
        final Token base = token;
        switch (base.type) {
            case ID:
                nextToken();
                if (token.type == EQUAL) {
                    applyMutableAttributes(graph.getGraphAttr(), Arrays.asList(base, nextToken(ID)));
                    nextToken();
                } else {
                    final PortNode nodeId = nodeId(base);
                    if (token.type == MINUS_MINUS || token.type == ARROW) {
                        edgeStatement(graph, nodeId);
                    } else {
                        nodeStatement(graph, nodeId);
                    }
                }
                return true;
            case SUBGRAPH:
            case BRACE_OPEN:
                final DotGraph sub = subgraph(graph.isDirected());
                if (token.type == MINUS_MINUS || token.type == ARROW) {
                    edgeStatement(graph, sub);
                } else {
                    graph.addSource(sub);
                }
                return true;
            case GRAPH:
            case NODE:
            case EDGE:
                attributeStatement(graph);
                return true;
            default:
                return false;
        }
    }

    private DotGraph subgraph(boolean directed) {
        return Context.use(ctx -> {
            final DotGraph sub = graph().setDirected(directed);
            if (token.type == SUBGRAPH) {
                nextToken();
                if (token.type == ID) {
                    sub.setName(label(token).toString());
                    nextToken();
                }
            }
            statementList(sub);
            return sub;
        });
    }

    private void edgeStatement(DotGraph graph, Source<? extends Source<?>> linkSource)
            throws IOException {
        final List<Source<? extends Source<?>>> points = new ArrayList<>();
        points.add(linkSource);
        do {
            if (graph.isDirected() && token.type == MINUS_MINUS) {
                fail("-- used in digraph. Use -> instead.");
            }
            if (!graph.isDirected() && token.type == ARROW) {
                fail("-> used in graph. Use -- instead.");
            }
            nextToken();
            if (token.type == ID) {
                final Token id = token;
                nextToken();
                points.add(nodeId(id));
            } else if (token.type == SUBGRAPH || token.type == BRACE_OPEN) {
                points.add(subgraph(graph.isDirected()));
            }
        } while (token.type == MINUS_MINUS || token.type == ARROW);
        final List<Token> attrs = (token.type == BRACKET_OPEN) ? attributeList() : Collections.emptyList();
        for (int i = 0; i < points.size() - 1; i++) {
            final Source<? extends Source<?>> from = points.get(i);
            final Target to = (Target) points.get(i + 1);
            graph.addSource(from.addEdge(applyAttributes(Edge.between(from, to), attrs)));
        }
    }

    private Compass compass(String name) {
        return Compass.of(name).orElseThrow(() ->
                new ParserException(lexer.pos, "Invalid compass value '" + name + "'"));
    }

    private void nodeStatement(DotGraph graph, PortNode nodeId) throws IOException {
        Node node = node(nodeId.getNode().getName()); // TODO ignore port and compass?
        if (token.type == BRACKET_OPEN) {
            applyMutableAttributes(node, attributeList());
        }
        graph.addSource(node);
    }

    private PortNode nodeId(Token base) throws IOException {
        final PortNode node = new PortNode().setNode(node(label(base)));
        if (token.type == COLON) {
            final String second = nextToken(ID).value;
            nextToken();
            if (token.type == COLON) {
                node.setRecord(second).setCompass(compass(nextToken(ID).value));
                nextToken();
            } else {
                if (Compass.of(second).isPresent()) {
                    node.setCompass(compass(second));
                } else {
                    node.setRecord(second);
                }
            }
        }
        return node;
    }

    private void attributeStatement(DotGraph graph) throws IOException {
        final Attributed<?> target = attributes(graph, token);
        nextToken();
        applyMutableAttributes(target, attributeList());
    }

    private void applyMutableAttributes(Attributed<?> attributed, List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i += 2) {
            final String key = tokens.get(i).value;
            final Token value = tokens.get(i + 1);
            if ("label".equals(key) || "xlabel".equals(key) || "headlabel".equals(key) || "taillabel".equals(key)) {
                attributed.with(key, label(value));
            } else {
                attributed.with(key, value.value);
            }
        }
    }

    private <T extends Attributed<T>> T applyAttributes(T attributed, List<Token> tokens) {
        T res = attributed;
        for (int i = 0; i < tokens.size(); i += 2) {
            res = res.with(tokens.get(i).value, tokens.get(i + 1).value);
        }
        return res;
    }

    private Attributed<?> attributes(DotGraph graph, Token token) {
        switch (token.type) {
            case GRAPH:
                return graph.getGraphAttr();
            case NODE:
                return Context.get().nodeAttrs();
            case EDGE:
                return Context.get().edgeAttrs();
            default:
                return null;
        }
    }

    private List<Token> attributeList() throws IOException {
        final List<Token> res = new ArrayList<>();
        do {
            assertToken(BRACKET_OPEN);
            if (token.type == ID) {
                res.addAll(attrListElement());
            }
            assertToken(BRACKET_CLOSE);
        } while (token.type == BRACKET_OPEN);
        return res;
    }

    private List<Token> attrListElement() throws IOException {
        final List<Token> res = new ArrayList<>();
        do {
            res.add(token);
            nextToken(EQUAL);
            res.add(nextToken(ID));
            nextToken();
            if (token.type == SEMICOLON || token.type == COMMA) {
                nextToken();
            }
        } while (token.type == ID);
        return res;
    }

    private Token nextToken() throws IOException {
        return token = lexer.token();
    }

    private Token nextToken(int type) throws IOException {
        nextToken();
        checkToken(type);
        return token;
    }

    private Token assertToken(int type) throws IOException {
        checkToken(type);
        return nextToken();
    }

    private void checkToken(int type) {
        if (token.type != type) {
            fail("'" + Token.desc(type) + "' expected");
        }
    }

    private void fail(String msg) {
        throw new ParserException(lexer.pos, msg);
    }
}
