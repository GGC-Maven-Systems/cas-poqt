package ph.com.guanzongroup.cas.poqt.ui.utility;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Enter on a search field searches, like F3, when the user typed something
 * in it; on a field still showing its loaded record (or left blank) Enter
 * just moves on, as Tab does. (cas-po's PO screens work the same way.)
 *
 * <p>
 * The screen's own F3 handling does the search: Enter is turned into an F3
 * key press on the field, so nothing in the screen's search code changes.
 * Text the program puts in the field (a loaded record) is not "typed".</p>
 */
public final class EnterSearch {

    private static final String TYPED_KEY = "cas.typed";

    private EnterSearch() {
    }

    public static void on(TextField... faFields) {
        for (TextField loField : faFields) {
            if (loField != null) {
                attach(loField);
            }
        }
    }

    /** True while the field holds text the user typed and has not searched yet. */
    public static boolean isTyped(TextField foField) {
        return Boolean.TRUE.equals(foField.getProperties().get(TYPED_KEY));
    }

    private static void attach(TextField foField) {
        final boolean[] lbKeyDown = {false};
        foField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isConsumed()) {
                return; //the auto-suggest list took the key (Enter picks a row)
            }
            KeyCode loKey = e.getCode();
            if (loKey == KeyCode.ENTER && !e.isControlDown() && !e.isAltDown()) {
                String lsText = foField.getText() == null ? "" : foField.getText().trim();
                if (isTyped(foField) && !lsText.isEmpty() && foField.isEditable() && !foField.isDisabled()) {
                    e.consume();
                    foField.getProperties().put(TYPED_KEY, Boolean.FALSE);
                    foField.fireEvent(new KeyEvent(foField, foField, KeyEvent.KEY_PRESSED, "", "", KeyCode.F3,
                            false, false, false, false));
                }
                return;
            }
            if (loKey == KeyCode.F3 || loKey == KeyCode.F4) {
                foField.getProperties().put(TYPED_KEY, Boolean.FALSE); //the screen searches now
                return;
            }
            if (!loKey.isNavigationKey() && !loKey.isFunctionKey() && !loKey.isModifierKey()
                    && loKey != KeyCode.TAB && loKey != KeyCode.ESCAPE) {
                lbKeyDown[0] = true; //letters, digits, Backspace, Delete, Ctrl+V, ...
            }
        });
        foField.addEventFilter(KeyEvent.KEY_RELEASED, e -> lbKeyDown[0] = false);
        foField.textProperty().addListener((o, ov, nv) ->
                //typing makes it "typed"; text set by the program (loading a record) does not
                foField.getProperties().put(TYPED_KEY, lbKeyDown[0] && foField.isFocused()));
    }
}
