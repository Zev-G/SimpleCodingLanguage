package tmw.me.com.ide.codeEditor.languages;

import tmw.me.com.ide.Ide;
import tmw.me.com.ide.IdeSpecialParser;
import tmw.me.com.ide.codeEditor.highlighting.StyleSpansFactory;
import tmw.me.com.ide.codeEditor.texteditor.HighlightableTextEditor;
import tmw.me.com.ide.codeEditor.texteditor.IntegratedTextEditor;
import tmw.me.com.ide.codeEditor.visualcomponents.tooltip.EditorTooltip;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>This is the base class for all languages, the main subclass of this class is {@link SfsLanguage} but there is minimal support for some other languages.</p>
 */
public abstract class LanguageSupport {

    protected static final String NUMBER_PATTERN = "[0-9]+";
    protected static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";

    /**
     * The stylesheet which will be used by the {@link IntegratedTextEditor}, get this stylesheet with
     * getClass().getResource(String resource).toExternalForm().
     */
    protected final String styleSheet;
    /**
     * The languageName is just for display purposes. Try to make it something meaningful.
     */
    protected final String languageName;
    /**
     * If this is true then the subclass should Override {@link LanguageSupport#run(IntegratedTextEditor, Ide)}. If it isn't true then pressing the run button will do nothing if this is the selected tab.
     */
    protected boolean runnable = false;
    /**
     * If this is true then the subclass will have it's {@link LanguageSupport#getPossiblePieces(String, IntegratedTextEditor)} method called, and the result used to populate the popup.
     */
    protected boolean usingAutoComplete = false;
    /**
     * The language's style spans factory. This should be used to create custom styling for the language (other then the Regex-based styling which comes out-of-the-box with LanguageSupport).
     * If you do ever want to override the out-of-the-box Regex-based styling just use the styling of {@link PlainTextLanguage} and then do all your styling via this factory.
     */
    protected StyleSpansFactory<Collection<String>> customStyleSpansFactory = null;

    private LanguageSupplier<LanguageSupport> thisSupplier;

    /**
     * @param styleSheet   The language's style sheet, see {@link LanguageSupport#styleSheet}
     * @param languageName The language's name, see {@link LanguageSupport#languageName}
     */
    public LanguageSupport(String styleSheet, String languageName) {
        this.styleSheet = styleSheet;
        this.languageName = languageName;
    }

    public LanguageSupport(String languageName) {
        this(LanguageSupport.class.getResource("styles/plain.css").toExternalForm(), languageName);
    }

    /**
     * @return A pattern which is used for sectioning the highlighting, if you don't want your language to have highlighting you can always
     * make this return null.
     */
    public abstract Pattern generatePattern();

    /**
     * @return {@link LanguageSupport#styleSheet}
     */
    public String getStyleSheet() {
        return styleSheet;
    }

    /**
     * @return {@link LanguageSupport#languageName}
     */
    public String getLanguageName() {
        return languageName;
    }

    /**
     * @param matcher The matcher which the style class should be determined from, calling {@link Matcher#find()} will break highlighting.
     * @return A String which will be used for the pattern, currently this only supports a single String so you cannot return multiple style classes.
     * The returned style class should have a matching style in this Language's css file (defined in the constructor).
     */
    public abstract String styleClass(Matcher matcher);

    /**
     * @param line   The line which the user is typing on.
     * @param editor The relevant ITE
     * @return Return a list of {@link tmw.me.com.ide.IdeSpecialParser.PossiblePiecePackage}, these will be put into the autocomplete popup.
     */
    public ArrayList<IdeSpecialParser.PossiblePiecePackage> getPossiblePieces(String line, IntegratedTextEditor editor) {
        return null;
    }

    /**
     * This method should be used for adding any additional functionality this language needs onto the IntegratedTextEditor, for example the highlighting of variables in the {@link SfsLanguage} is added here.
     *
     * @param integratedTextEditor A reference to the {@link IntegratedTextEditor} which all functionality should be added onto.
     */
    public Behavior[] addBehaviour(IntegratedTextEditor integratedTextEditor) {
        return null;
    }

    public Behavior[] removeBehaviour(IntegratedTextEditor integratedTextEditor) {
        integratedTextEditor.getErrorLines().clear();
        return null;
    }

    /**
     * This method just insures that the run method on your LanguageSupport is only activated if it is runnable.
     */
    public final void runCalled(IntegratedTextEditor textEditor, Ide ide) {
        if (runnable)
            run(textEditor, ide);
    }

    /**
     * This method should only be overridden if {@link LanguageSupport#runnable} is true.
     *
     * @param textEditor A reference to the text editor this language is attached to.
     * @param ide        A reference to the Ide that is running the code.
     */
    public void run(IntegratedTextEditor textEditor, Ide ide) {
        if (ide != null) {
            ide.getRunConsole().getConsoleText().getChildren().clear();
            if (ide.getRunTabButton().getAccessibleText() == null || !ide.getRunTabButton().getAccessibleText().equals("ACTIVATED")) {
                ide.getRunTabButton().fire();
            }
        }
    }

    /**
     * This will be removed for a more modular version in the future, for now its a simple way of getting the right LanguageSupport for a file.
     */
    public static LanguageSupport getLanguageFromFile(File file) {
        if (file == null || !file.getName().contains(".")) {
            return new PlainTextLanguage();
        }
        String fileEnding = file.getName().split("\\.")[1];
        if (fileEnding.equals("java")) {
            return new JavaLanguage();
        } else if (fileEnding.equals("css")) {
            return new CssLanguage();
        } else {
            return new PlainTextLanguage();
        }
    }

    public boolean isUsingAutoComplete() {
        return usingAutoComplete;
    }

    public StyleSpansFactory<Collection<String>> getCustomStyleSpansFactory(HighlightableTextEditor editor) {
        return customStyleSpansFactory;
    }

    public LanguageSupplier<LanguageSupport> toSimpleSupplier() {
        if (thisSupplier == null) {
            thisSupplier = new LanguageSupplier<>() {
                @Override
                public LanguageSupport get() {
                    return LanguageSupport.this;
                }

                @Override
                public String getName() {
                    return getLanguageName();
                }
            };
        }
        return thisSupplier;
    }

    public LanguageSupplier<LanguageSupport> toSupplier() {
        if (thisSupplier == null) {
            thisSupplier = LanguageSupplier.fromLanguage(this);
        }
        return thisSupplier;
    }

    public boolean showingTooltip(EditorTooltip tooltip, int pos) {
        return false;
    }

}
