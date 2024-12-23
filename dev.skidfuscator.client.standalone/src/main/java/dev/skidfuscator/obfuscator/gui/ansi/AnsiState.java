package dev.skidfuscator.obfuscator.gui.ansi;

import java.awt.*;
import java.util.Stack;

public class AnsiState implements Cloneable {
    private Color foreground;
    private Color background;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikethrough;
    private Stack<AnsiState> stateStack;

    public AnsiState() {
        this.stateStack = new Stack<>();
    }

    public void pushState() {
        try {
            stateStack.push((AnsiState) this.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    public void popState() {
        if (!stateStack.isEmpty()) {
            AnsiState previousState = stateStack.pop();
            this.foreground = previousState.foreground;
            this.background = previousState.background;
            this.bold = previousState.bold;
            this.italic = previousState.italic;
            this.underline = previousState.underline;
            this.strikethrough = previousState.strikethrough;
        }
    }

    public void reset() {
        foreground = null;
        background = null;
        bold = false;
        italic = false;
        underline = false;
        strikethrough = false;
        stateStack.clear();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        AnsiState clone = (AnsiState) super.clone();
        clone.stateStack = new Stack<>();
        return clone;
    }

    // Getters and setters
    public Color getForeground() { return foreground; }
    public void setForeground(Color color) { this.foreground = color; }
    public Color getBackground() { return background; }
    public void setBackground(Color color) { this.background = color; }
    public boolean isBold() { return bold; }
    public void setBold(boolean bold) { this.bold = bold; }
    public boolean isItalic() { return italic; }
    public void setItalic(boolean italic) { this.italic = italic; }
    public boolean isUnderline() { return underline; }
    public void setUnderline(boolean underline) { this.underline = underline; }
    public boolean isStrikethrough() { return strikethrough; }
    public void setStrikethrough(boolean strikethrough) { this.strikethrough = strikethrough; }
}