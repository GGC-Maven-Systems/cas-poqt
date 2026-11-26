package ph.com.guanzongroup.cas.poqt.ui.utility;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.guanzon.appdriver.agent.ShowDialogFX;
import ph.com.guanzongroup.cas.poqt.utility.Suggestion;

/**
 * Applies an auto-suggest pick by running the field's own search (the
 * screen's F3) with {@link ShowDialogFX#preselect}: the existing Search
 * method takes the picked record without opening KwikSearch, and the screen
 * updates itself as after a normal search. When the search cannot find
 * exactly that record, KwikSearch opens with the picked text.
 */
public final class SuggestPick {

    private SuggestPick() {
    }

    /** Puts the name in the field (the code for code fields) and searches. */
    public static void pick(TextField foField, Suggestion foRow, boolean fbShowCode) {
        String lsText = fbShowCode || foRow.getName().isEmpty() ? foRow.getCode() : foRow.getName();
        ShowDialogFX.preselect(foRow.keyArray());
        try {
            foField.setText(lsText);
            foField.fireEvent(new KeyEvent(foField, foField, KeyEvent.KEY_PRESSED, "", "", KeyCode.F3,
                    false, false, false, false));
        } finally {
            ShowDialogFX.clearPreselect(); //in case the search stopped before the dialog
        }
    }
}
