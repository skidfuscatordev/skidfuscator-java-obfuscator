package dev.skidfuscator.obfuscator.gui.autosave;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class AutoSaveDocumentListener implements DocumentListener {
    private final Runnable saveAction;

    public AutoSaveDocumentListener(Runnable saveAction) {
        this.saveAction = saveAction;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        saveAction.run();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        saveAction.run();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        saveAction.run();
    }
}
