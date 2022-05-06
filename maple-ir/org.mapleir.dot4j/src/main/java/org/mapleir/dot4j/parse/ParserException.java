package org.mapleir.dot4j.parse;

public class ParserException extends RuntimeException {
    private final Position position;

    public ParserException(Position position, String message) {
        super(message);
        this.position = position;
    }

    @Override
    public String toString() {
        return position.toString() + " " + getMessage();
    }

    public Position getPosition() {
        return position;
    }
}
