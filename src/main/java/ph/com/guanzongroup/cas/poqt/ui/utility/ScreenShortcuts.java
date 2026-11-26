package ph.com.guanzongroup.cas.poqt.ui.utility;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.guanzon.appdriver.base.CommonUtils;

/**
 * Keyboard shortcuts for a screen's buttons, and a guard against closing the
 * window with unsaved changes.
 *
 * <p>
 * A shortcut works like clicking its button, and only while the button is
 * shown and enabled on the screen (so Ctrl+S does nothing when Save is
 * hidden). The keys are caught on the screen's root pane, so a screen that sits
 * in a dashboard tab only reacts while it has the focus, and a key a field
 * already handled (Esc closing the suggestion list, for example) does not
 * reach it. The button's tooltip shows the shortcut.</p>
 *
 * <pre>
 * ScreenShortcuts.on(AnchorMain)
 *         .add(btnSave, "Shortcut+S")
 *         .add(btnCancel, "Esc");
 * </pre>
 */
public final class ScreenShortcuts {

    private final Map<KeyCombination, Button> poKeys = new LinkedHashMap<>();

    private ScreenShortcuts(Node foRoot) {
        foRoot.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);
    }

    /** Shortcuts caught on this root pane. */
    public static ScreenShortcuts on(Node foRoot) {
        return new ScreenShortcuts(foRoot);
    }

    /**
     * @param fsKeys a key combination such as "Shortcut+S", "Esc", "F10"
     * ("Shortcut" is Ctrl on Windows)
     */
    public ScreenShortcuts add(Button foButton, String fsKeys) {
        KeyCombination loKeys = KeyCombination.valueOf(fsKeys);
        poKeys.put(loKeys, foButton);
        String lsText = foButton.getText() == null || foButton.getText().isEmpty() ? "" : foButton.getText() + " ";
        foButton.setTooltip(new Tooltip(lsText + "(" + loKeys.getDisplayText() + ")"));
        return this;
    }

    private void onKeyPressed(KeyEvent foEvent) {
        for (Map.Entry<KeyCombination, Button> loKey : poKeys.entrySet()) {
            if (loKey.getKey().match(foEvent) && isUsable(loKey.getValue())) {
                foEvent.consume();
                loKey.getValue().fire();
                return;
            }
        }
    }

    /** Shown (with all its parents) and enabled. */
    static boolean isUsable(Node foNode) {
        if (foNode.isDisabled() || foNode.getScene() == null) {
            return false;
        }
        return isShowing(foNode);
    }

    /** The node and all its parents are visible (a hidden dashboard tab is not). */
    static boolean isShowing(Node foNode) {
        for (Node loNode = foNode; loNode != null; loNode = loNode.getParent()) {
            if (!loNode.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Makes these text areas move on like the other fields:
     * <ul>
     * <li>Enter and Tab go to the next field, Shift+Tab to the previous one
     * (a text area otherwise types a line break / tab and keeps the focus);</li>
     * <li>Ctrl+Enter types a line break;</li>
     * <li>Ctrl+Tab still works as before.</li>
     * </ul>
     */
    public static void tabOutOf(TextArea... faAreas) {
        for (TextArea loArea : faAreas) {
            loArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    e.consume();
                    if (e.isControlDown()) {
                        if (loArea.isEditable() && !loArea.isDisabled()) {
                            loArea.replaceSelection("\n"); //line break
                        }
                    } else {
                        CommonUtils.SetNextFocus(loArea);
                    }
                } else if (e.getCode() == KeyCode.TAB && !e.isControlDown()) {
                    e.consume();
                    if (e.isShiftDown()) {
                        CommonUtils.SetPreviousFocus(loArea);
                    } else {
                        CommonUtils.SetNextFocus(loArea);
                    }
                }
            });
        }
    }

    /**
     * Asks before the window closes while there are unsaved changes; closing
     * stops when the user answers No. Uses an event filter, so the window's own
     * close handler (a dashboard's, for example) is kept.
     *
     * @param foRoot the screen's root pane (the guard is skipped while it is hidden)
     * @param foHasChanges whether the screen has unsaved changes
     * @param foConfirm asks the user; true closes
     */
    public static void guardClose(Parent foRoot, BooleanSupplier foHasChanges, BooleanSupplier foConfirm) {
        foRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o, oldWindow, newWindow) -> addCloseFilter(newWindow, foRoot, foHasChanges, foConfirm));
                addCloseFilter(newScene.getWindow(), foRoot, foHasChanges, foConfirm);
            }
        });
    }

    private static void addCloseFilter(Window foWindow, Parent foRoot, BooleanSupplier foHasChanges, BooleanSupplier foConfirm) {
        if (foWindow == null || foWindow.getProperties().containsKey(foRoot)) {
            return;
        }
        foWindow.getProperties().put(foRoot, Boolean.TRUE); //once per window
        foWindow.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
            if (foRoot.getScene() != null && foRoot.getScene().getWindow() == foWindow
                    && isShowing(foRoot) && foHasChanges.getAsBoolean() && !foConfirm.getAsBoolean()) {
                e.consume();
            }
        });
    }
}
