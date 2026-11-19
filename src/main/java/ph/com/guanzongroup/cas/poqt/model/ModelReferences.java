package ph.com.guanzongroup.cas.poqt.model;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;

/**
 * The lazy reference-record loading the PO models share.
 *
 * <p>
 * Every reference accessor of the quotation models
 * (Branch(), Supplier(), Term(), ...) did the same thing in its own copy;
 * they now call {@link #load}:</p>
 * <ol>
 * <li>empty id: the reference is cleared (initialize) and returned;</li>
 * <li>the reference already holds this id: returned as is (no query);</li>
 * <li>reference tables only (cacheTable not null): served from
 * {@link ReferenceCache} when another record loaded it already;</li>
 * <li>otherwise opened from the database; cached on success (reference
 * tables), cleared on failure.</li>
 * </ol>
 *
 * <p>
 * Reference tables (Branch, Industry, Category, Company, Term, Client, Brand,
 * Color, ...) are cached; transaction records (inventory, stock requests,
 * quotations) are not, so they are never served stale.</p>
 */
final class ModelReferences {

    private ModelReferences() {
    }

    /** Id of the record a reference model currently holds. */
    @FunctionalInterface
    interface IdOf<T> {

        String get(T foModel);
    }

    /** How a reference model is opened by its id (openRecord by default). */
    @FunctionalInterface
    interface Opener<T> {

        JSONObject open(T foModel, String fsId) throws SQLException, GuanzonException;
    }

    /** Loads a reference opened with openRecord(id). */
    static <T extends Model> T load(T foModel, String fsId, String fsCacheTable, IdOf<T> foIdOf)
            throws SQLException, GuanzonException {
        return load(foModel, fsId, fsCacheTable, foIdOf, (loModel, lsId) -> loModel.openRecord(lsId));
    }

    /**
     * Loads a reference model for fsId (see the class comment).
     *
     * @param fsCacheTable ReferenceCache table name; null for transaction
     * records (never cached)
     */
    static <T extends Model> T load(T foModel, String fsId, String fsCacheTable, IdOf<T> foIdOf, Opener<T> foOpen)
            throws SQLException, GuanzonException {
        if (fsId == null || fsId.isEmpty()) {
            foModel.initialize();
            return foModel;
        }
        if (foModel.getEditMode() == EditMode.READY && fsId.equals(foIdOf.get(foModel))) {
            return foModel;
        }
        if (fsCacheTable != null && ReferenceCache.tryLoad(fsCacheTable, fsId, foModel)) {
            return foModel;
        }

        JSONObject loJSON = foOpen.open(foModel, fsId);
        if ("success".equals(loJSON.get("result"))) {
            if (fsCacheTable != null) {
                ReferenceCache.store(fsCacheTable, fsId, foModel);
            }
        } else {
            foModel.initialize();
        }
        return foModel;
    }

    /** Text value of a column; empty for null. */
    static String text(Object foValue) {
        return foValue == null ? "" : (String) foValue;
    }

    /** The date part of a date-time (yyyy-MM-dd), as the PO default dates. */
    static String shortDate(Date fdValue) {
        return new SimpleDateFormat("yyyy-MM-dd").format(fdValue);
    }
}
