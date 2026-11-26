package ph.com.guanzongroup.cas.poqt.ui.utility;

import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

/**
 * Marks searchable text fields: a magnifier at the right of the field
 * (style class {@code search-field} in StyleSheet.css) and a tooltip, in
 * place of the "Press F3: Search" prompt text (same as cas-po).
 *
 * <p>
 * The old prompt is kept in the field's properties ({@link #PROMPT_KEY}), so
 * {@code JFXUtil.getTextFieldsIDWithPrompt} (the Search button) still finds
 * the field.</p>
 */
public final class SearchFields {

    public static final String STYLE_CLASS = "search-field";
    public static final String TOOLTIP = "Enter or F3: search";
    public static final String PROMPT_KEY = "cas.searchPrompt";

    private SearchFields() {
    }

    public static void mark(TextField... faFields) {
        for (TextField loField : faFields) {
            if (loField == null) {
                continue;
            }
            String lsPrompt = loField.getPromptText();
            if (lsPrompt != null && lsPrompt.contains("F3")) {
                loField.getProperties().put(PROMPT_KEY, lsPrompt);
                loField.setPromptText("");
            } else if (!loField.getProperties().containsKey(PROMPT_KEY)) {
                loField.getProperties().put(PROMPT_KEY, "Press F3: Search");
            }
            if (!loField.getStyleClass().contains(STYLE_CLASS)) {
                loField.getStyleClass().add(STYLE_CLASS);
            }
            if (loField.getTooltip() == null) {
                loField.setTooltip(new Tooltip(TOOLTIP));
            }
        }
    }
}
