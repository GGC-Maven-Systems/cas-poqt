package ph.com.guanzongroup.cas.poqt.ui.utility;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import javafx.scene.control.TextFormatter;

/**
 * Accepts only text matching a pattern (copied from integsys).
 *
 * @author Arsiela
 */
public class TextFormaterUtil extends TextFormatter<String> {

    public TextFormaterUtil(Pattern pattern) {
        super(new TextUnaryOperator(pattern));
    }

    private static class TextUnaryOperator implements UnaryOperator<TextFormatter.Change> {

        private final Pattern pattern;

        TextUnaryOperator(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public TextFormatter.Change apply(TextFormatter.Change change) {
            return pattern.matcher(change.getControlNewText()).matches() ? change : null;
        }
    }
}
