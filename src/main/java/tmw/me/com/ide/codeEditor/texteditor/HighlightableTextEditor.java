package tmw.me.com.ide.codeEditor.texteditor;

import javafx.application.Platform;
import javafx.scene.text.TextFlow;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyledSegment;
import tmw.me.com.ide.codeEditor.highlighting.Highlighter;
import tmw.me.com.ide.codeEditor.highlighting.LanguageSupportStyleSpansFactory;
import tmw.me.com.ide.codeEditor.highlighting.StyleSpansFactory;
import tmw.me.com.ide.codeEditor.languages.LanguageSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

public abstract class HighlightableTextEditor extends LanguageControlledTextEditor {

    private Highlighter highlighter = new Highlighter(this, new LanguageSupportStyleSpansFactory(this));
    private boolean highlightOnCaretMove = false;

    private HighlightingThread stylingThread;

    public HighlightableTextEditor() {
        super();
        init();
    }

    public HighlightableTextEditor(LanguageSupport support) {
        super(support);
        init();
    }

    private void init() {
        this.plainTextChanges().filter(ch -> !ch.getInserted().equals(ch.getRemoved())).subscribe(plainTextChange -> highlight());

        caretPositionProperty().addListener(((observable, oldValue, newValue) -> highlight()));
    }

    public TextExt copySegment(StyledSegment<String, Collection<String>> segment) {
        TextExt text = new TextExt(segment.getSegment());
        text.getStyleClass().addAll(segment.getStyle());
        text.getStyleClass().add("apply-font");
        return text;
    }

    public TextFlow copyLine(int line) {
        TextFlow textFlow = new TextFlow();
        Paragraph<Collection<String>, String, Collection<String>> par = getParagraph(line);
        for (StyledSegment<String, Collection<String>> segment : par.getStyledSegments()) {
            textFlow.getChildren().add(copySegment(segment));
        }
        textFlow.getStylesheets().addAll(STYLE_SHEET, getLanguage().getStyleSheet());
        return textFlow;
    }

    public void setHighlighter(Highlighter highlighter) {
        this.highlighter = highlighter;
        highlight();
    }

    public ArrayList<StyleSpansFactory<Collection<String>>> getFactories() {
        return highlighter.getFactories();
    }

    public Highlighter getHighlighter() {
        return highlighter;
    }

    /**
     * Computes and applies the highlighting on a separate thread.
     */
    public void highlight() {
        if (stylingThread != null && stylingThread.isAlive()) {
            stylingThread.interrupt();
        }
        stylingThread = new HighlightingThread(this);

        stylingThread.setText(this.getText());
        stylingThread.start();
    }

    public abstract void onHighlight();

    public void setHighlightOnCaretMove(boolean b) {
        this.highlightOnCaretMove = b;
    }

    public static class HighlightingThread extends Thread {

        private String text;
        private final HighlightableTextEditor editor;

        public HighlightingThread(HighlightableTextEditor editor) {
            super();
            this.editor = editor;
            this.setDaemon(true);
        }

        @Override
        public void run() {
            computeHighlighting();
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        private void computeHighlighting() {
            if (editor.languageSupport.get() != null) {
                Pattern pattern = editor.languageSupport.get().generatePattern();
                if (pattern != null) {
                    StyleSpans<Collection<String>> styleSpans = editor.getHighlighter().createStyleSpans();
                    if (!isInterrupted()) {
                        Platform.runLater(() -> {
                            if (!isInterrupted()) {
                                try {
                                    editor.setStyleSpans(0, styleSpans);
                                } catch (IndexOutOfBoundsException ignored) {
                                }
                            }
                        });
                    }
                }
            }
        }

    }

    public static void controlFrom(HighlightableTextEditor from, HighlightableTextEditor to) {
        linkITEs(from, to);
        to.setEditable(false);
        from.caretPositionProperty().addListener((observableValue, integer, t1) -> Platform.runLater(() -> {
            if (t1 == from.getCaretPosition() && to.getText().length() >= t1)
                to.displaceCaret(t1);
        }));
        from.languageSupportProperty().addListener((observableValue, support, t1) -> to.setLanguageSupport(t1.toSupplier().get()));
        from.selectionProperty().addListener((observableValue, indexRange, t1) -> to.selectRange(t1.getStart(), t1.getEnd()));
    }


}
