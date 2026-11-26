package ph.com.guanzongroup.cas.poqt.ui.utility;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Popup;
import javafx.util.Duration;
import ph.com.guanzongroup.cas.poqt.utility.Suggestion;

/**
 * Auto-suggest drop-down for a searchable text field.
 *
 * <p>
 * While the user types (at least {@link #MIN_CHARS} characters), a list of
 * matches appears under the field {@link #DELAY_MS} ms after the last
 * keystroke. Keys while the list is open: Up / Down move, Enter or a mouse
 * click picks, Esc closes, Tab closes it and moves on (nothing picked). While
 * it is closed the field works as before (Enter / F3 run the search dialog),
 * so scanned barcodes still go straight to the search. F3 / F4 always reach
 * the screen, list open or not, so the search dialog opens with the typed
 * text.</p>
 *
 * <p>
 * Text set by the program (showing a loaded record) never opens the list:
 * only typing does. The list is read on the JavaFX thread, after the delay, so
 * it never runs at the same time as the screen's own database work (the
 * queries are small: {@code LIMIT} 15).</p>
 *
 * <pre>
 * AutoSuggest.attach(tfSupplier, text -&gt; po().Suggestions().supplier(text), this::pickSupplier);
 * </pre>
 */
public final class AutoSuggest {

    /** Characters typed before the list appears. */
    public static final int MIN_CHARS = 2;
    /** Wait after the last keystroke, in milliseconds. */
    public static final int DELAY_MS = 300;

    private static final Logger LOG = Logger.getLogger(AutoSuggest.class.getName());
    private static final double ROW_HEIGHT = 26;
    private static final int VISIBLE_ROWS = 8;

    /** Reads the matches of the typed text. */
    @FunctionalInterface
    public interface Provider {

        List<Suggestion> find(String fsText) throws Exception;
    }

    private final TextField poField;
    private final Provider poProvider;
    private final Consumer<Suggestion> poOnPick;
    private final Popup poPopup = new Popup();
    private final ListView<Suggestion> poList = new ListView<>();
    private final PauseTransition poDelay = new PauseTransition(Duration.millis(DELAY_MS));
    private boolean pbTyped;

    private AutoSuggest(TextField foField, Provider foProvider, Consumer<Suggestion> foOnPick) {
        poField = foField;
        poProvider = foProvider;
        poOnPick = foOnPick;
    }

    /** Adds auto-suggest to a field; onPick receives the row the user chose. */
    public static AutoSuggest attach(TextField foField, Provider foProvider, Consumer<Suggestion> foOnPick) {
        AutoSuggest loSuggest = new AutoSuggest(foField, foProvider, foOnPick);
        loSuggest.init();
        return loSuggest;
    }

    private void init() {
        poList.setCellFactory(lv -> new SuggestionCell());
        poList.setFocusTraversable(false);
        poList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                pick();
            }
        });
        poPopup.getContent().add(poList);
        poPopup.setAutoHide(true);
        poPopup.setAutoFix(true);

        poDelay.setOnFinished(e -> refresh());

        //runs before the screen's own key handler (setOnKeyPressed)
        poField.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        //the text changes before the key is released; a key that changed nothing (Ctrl+C) ends here
        poField.addEventFilter(KeyEvent.KEY_RELEASED, e -> pbTyped = false);
        poField.textProperty().addListener((o, ov, nv) -> onTextChanged(nv));
        poField.focusedProperty().addListener((o, ov, nv) -> {
            if (!nv) {
                close();
            }
        });
    }

    /** Hides the list and cancels a pending refresh. */
    public void close() {
        poDelay.stop();
        pbTyped = false;
        poPopup.hide();
    }

    /** Like {@link #close()}, but hides the list after the current event. */
    private void closeLater() {
        poDelay.stop();
        pbTyped = false;
        if (poPopup.isShowing()) {
            Platform.runLater(poPopup::hide);
        }
    }

    public boolean isShowing() {
        return poPopup.isShowing();
    }

    // =====================================================================
    // Keys and typing
    // =====================================================================
    private void onKeyPressed(KeyEvent foEvent) {
        KeyCode loKey = foEvent.getCode();
        if (poPopup.isShowing()) {
            switch (loKey) {
                case DOWN:
                    move(1);
                    foEvent.consume();
                    return;
                case UP:
                    move(-1);
                    foEvent.consume();
                    return;
                case PAGE_DOWN:
                    move(VISIBLE_ROWS);
                    foEvent.consume();
                    return;
                case PAGE_UP:
                    move(-VISIBLE_ROWS);
                    foEvent.consume();
                    return;
                case ENTER:
                    if (poList.getSelectionModel().getSelectedItem() != null) {
                        pick();
                        foEvent.consume();
                        return;
                    }
                    close();
                    return;
                case TAB:
                    //only Enter or a click picks; Tab moves on
                    closeLater();
                    return;
                case ESCAPE:
                    close();
                    foEvent.consume();
                    return;
                default:
                    break;
            }
        }

        switch (loKey) {
            case ENTER:
            case TAB:
            case F3:
            case F4:
                //the screen handles the key (search dialog / next field): no list afterwards.
                //The list is hidden after this key event, so hiding it never stops the key
                //from reaching the screen (F3 with typed text must open the search).
                closeLater();
                return;
            default:
                break;
        }
        if (!loKey.isNavigationKey() && !loKey.isFunctionKey() && !loKey.isModifierKey()
                && loKey != KeyCode.ESCAPE) {
            pbTyped = true; //letters, digits, Backspace, Delete, Ctrl+V, ...
        }
    }

    private void onTextChanged(String fsText) {
        if (!pbTyped || !poField.isFocused() || !poField.isEditable() || poField.isDisabled()) {
            return;
        }
        pbTyped = false;
        if (fsText == null || fsText.trim().length() < MIN_CHARS) {
            close();
            return;
        }
        poDelay.playFromStart();
    }

    // =====================================================================
    // List
    // =====================================================================
    private void refresh() {
        if (!poField.isFocused() || poField.getScene() == null || poField.getScene().getWindow() == null) {
            return;
        }
        String lsText = poField.getText() == null ? "" : poField.getText().trim();
        if (lsText.length() < MIN_CHARS) {
            poPopup.hide();
            return;
        }

        List<Suggestion> laRows;
        try {
            laRows = poProvider.find(lsText);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Auto-suggest failed for " + poField.getId(), ex);
            laRows = Collections.emptyList();
        }
        if (laRows == null || laRows.isEmpty()) {
            poPopup.hide();
            return;
        }

        poList.getItems().setAll(laRows);
        poList.getSelectionModel().select(0);
        poList.scrollTo(0);
        poList.setPrefWidth(Math.max(poField.getWidth(), 560));
        poList.setPrefHeight(Math.min(laRows.size(), VISIBLE_ROWS) * ROW_HEIGHT + 4);

        if (!poPopup.isShowing()) {
            Point2D loAt = poField.localToScreen(0, poField.getHeight());
            if (loAt == null) {
                return;
            }
            poPopup.show(poField, loAt.getX(), loAt.getY());
        }
    }

    private void move(int fnRows) {
        int lnSize = poList.getItems().size();
        if (lnSize == 0) {
            return;
        }
        int lnRow = poList.getSelectionModel().getSelectedIndex() + fnRows;
        lnRow = Math.max(0, Math.min(lnSize - 1, lnRow));
        poList.getSelectionModel().select(lnRow);
        poList.scrollTo(Math.max(0, lnRow - VISIBLE_ROWS / 2));
    }

    private void pick() {
        Suggestion loPicked = poList.getSelectionModel().getSelectedItem();
        close();
        if (loPicked != null) {
            poOnPick.accept(loPicked);
        }
    }

    /** Code, name, unit of measure (items) and (grey) detail on one line. */
    private static final class SuggestionCell extends ListCell<Suggestion> {

        private final Label poCode = new Label();
        private final Label poName = new Label();
        private final Label poUnit = new Label();
        private final Label poDetail = new Label();
        private final HBox poRow = new HBox(10, poCode, poName, poUnit, poDetail);

        SuggestionCell() {
            poCode.setMinWidth(110);
            poCode.setStyle("-fx-text-fill: #555555;");
            poDetail.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
            poUnit.setMinWidth(60);
            poUnit.setStyle("-fx-font-weight: bold;");
            poDetail.setMaxWidth(200);
            HBox.setHgrow(poName, Priority.ALWAYS);
            poName.setMaxWidth(Double.MAX_VALUE);
            poRow.setPadding(new Insets(1, 4, 1, 4));
        }

        @Override
        protected void updateItem(Suggestion foItem, boolean fbEmpty) {
            super.updateItem(foItem, fbEmpty);
            setText(null);
            if (fbEmpty || foItem == null) {
                setGraphic(null);
                return;
            }
            poCode.setText(foItem.getCode());
            poName.setText(foItem.getName());
            poUnit.setText(foItem.getUnit());
            poUnit.setVisible(!foItem.getUnit().isEmpty());
            poUnit.setManaged(!foItem.getUnit().isEmpty());
            poDetail.setText(foItem.getDetail());
            setGraphic(poRow);
        }
    }
}
