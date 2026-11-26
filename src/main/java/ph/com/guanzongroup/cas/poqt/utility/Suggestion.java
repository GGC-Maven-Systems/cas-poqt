package ph.com.guanzongroup.cas.poqt.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * One row of an auto-suggest list: the code and name the user sees, an
 * optional unit of measure (items), an optional detail, and the values that
 * identify the record in the screen's own search (see
 * {@code ShowDialogFX.preselect}).
 */
public final class Suggestion {

    private final String code;
    private final String name;
    private final String unit;
    private final String detail;
    private final List<String> keys;

    public Suggestion(String fsCode, String fsName, String fsUnit, String fsDetail, String... faKeys) {
        code = text(fsCode);
        name = text(fsName);
        unit = text(fsUnit);
        detail = text(fsDetail);
        List<String> laKeys = new ArrayList<>();
        for (String lsKey : faKeys == null || faKeys.length == 0 ? new String[]{code, name} : faKeys) {
            if (!text(lsKey).isEmpty()) {
                laKeys.add(text(lsKey));
            }
        }
        keys = Collections.unmodifiableList(laKeys);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /** Unit of measure (items); empty for other lists. */
    public String getUnit() {
        return unit;
    }

    public String getDetail() {
        return detail;
    }

    /** Values the record has in the search's columns (code, name, ...). */
    public List<String> getKeys() {
        return keys;
    }

    public String[] keyArray() {
        return keys.toArray(new String[0]);
    }

    @Override
    public String toString() {
        String lsText = code + "  " + name;
        if (!unit.isEmpty()) {
            lsText += "  " + unit;
        }
        return detail.isEmpty() ? lsText : lsText + "  (" + detail + ")";
    }

    @Override
    public boolean equals(Object foOther) {
        return foOther instanceof Suggestion && toString().equals(foOther.toString())
                && keys.equals(((Suggestion) foOther).keys);
    }

    @Override
    public int hashCode() {
        return Arrays.asList(code, name, unit, detail).hashCode();
    }

    private static String text(String fsValue) {
        return fsValue == null ? "" : fsValue.trim();
    }
}
