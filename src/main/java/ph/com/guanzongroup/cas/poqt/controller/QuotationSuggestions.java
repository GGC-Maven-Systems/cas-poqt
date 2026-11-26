package ph.com.guanzongroup.cas.poqt.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.cas.client.account.AP_Client_Master;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.CategoryLevel2;
import org.guanzon.cas.parameter.ModelVariant;
import org.guanzon.cas.parameter.services.ParamControllers;
import ph.com.guanzongroup.cas.poqt.utility.Suggestion;

/**
 * Auto-suggest lists for the quotation screens: the top {@value #LIMIT}
 * matches of the typed text, read with the same tables and filters as the
 * controllers' Search methods.
 *
 * <p>
 * Never opens a dialog and never changes a transaction. Picking a row runs
 * the screen's own search with {@code ShowDialogFX.preselect(row keys)}, so
 * the record is applied by the existing Search method (same rules and
 * messages); when the search cannot find exactly that row, KwikSearch opens
 * as usual. Codes match from the start, names anywhere.</p>
 */
public class QuotationSuggestions {

    public static final int LIMIT = 15;

    private final GRiderCAS poGRider;
    private CategoryLevel2 poCategory;
    private Brand poBrand;
    private ModelVariant poModel;
    private Inventory poInventory;
    private AP_Client_Master poSupplier;

    public QuotationSuggestions(GRiderCAS foGRider) {
        poGRider = foGRider;
    }

    // =====================================================================
    // Simple tables
    // =====================================================================
    /** Branch (Destination, Branch): code sBranchCd. */
    public List<Suggestion> branch(String fsText) throws SQLException {
        return simple("SELECT sBranchCd xCode, sBranchNm xName, '' xDetail FROM Branch"
                + " WHERE cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND (" + starts("sBranchCd", fsText) + " OR " + contains("sBranchNm", fsText) + ")"
                + " ORDER BY sBranchNm");
    }

    /** Department: code sDeptIDxx. */
    public List<Suggestion> department(String fsText) throws SQLException {
        return simple("SELECT sDeptIDxx xCode, sDeptName xName, IFNULL(sDeptCode, '') xDetail FROM Department"
                + " WHERE cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND (" + starts("sDeptIDxx", fsText) + " OR " + contains("sDeptName", fsText)
                + " OR " + starts("sDeptCode", fsText) + ")"
                + " ORDER BY sDeptName");
    }

    /** Company: code sCompnyID. */
    public List<Suggestion> company(String fsText) throws SQLException {
        return simple("SELECT sCompnyID xCode, sCompnyNm xName, IFNULL(sCompnyCd, '') xDetail FROM Company"
                + " WHERE cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND (" + starts("sCompnyID", fsText) + " OR " + contains("sCompnyNm", fsText)
                + " OR " + starts("sCompnyCd", fsText) + ")"
                + " ORDER BY sCompnyNm");
    }

    /** Term: code sTermCode. */
    public List<Suggestion> term(String fsText) throws SQLException {
        return simple("SELECT sTermCode xCode, sDescript xName, '' xDetail FROM Term"
                + " WHERE cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND (" + starts("sTermCode", fsText) + " OR " + contains("sDescript", fsText) + ")"
                + " ORDER BY sDescript");
    }

    // =====================================================================
    // Browse SQL of the lookups (same as the Search methods)
    // =====================================================================
    /**
     * Category level 2 of an industry (Search Category).
     *
     * @param fbOrNoIndustry also categories without industry (the request's
     * search does; the quotation's does not)
     */
    public List<Suggestion> category(String fsText, String fsIndustryId, boolean fbOrNoIndustry)
            throws SQLException, GuanzonException {
        if (poCategory == null) {
            poCategory = new ParamControllers(poGRider, null).CategoryLevel2();
        }
        String lsIndustry = fbOrNoIndustry
                ? "(sIndstCdx = " + SQLUtil.toSQL(fsIndustryId) + " OR ISNULL(sIndstCdx))"
                : "sIndstCdx = " + SQLUtil.toSQL(fsIndustryId);
        String lsSQL = MiscUtil.addCondition(poCategory.getSQ_Browse(),
                "cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND " + lsIndustry
                + " AND (" + starts("sCategrCd", fsText) + " OR " + contains("sDescript", fsText) + ")");
        return rows(lsSQL + " ORDER BY sDescript", "sCategrCd", "sDescript", null, null);
    }

    /** Brand of an industry (the request searches industry "09"). */
    public List<Suggestion> brand(String fsText, String fsIndustryId) throws SQLException, GuanzonException {
        if (poBrand == null) {
            poBrand = new ParamControllers(poGRider, null).Brand();
        }
        String lsSQL = MiscUtil.addCondition(poBrand.getSQ_Browse(),
                "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND a.sIndstCdx = " + SQLUtil.toSQL(fsIndustryId)
                + " AND (" + starts("a.sBrandIDx", fsText) + " OR " + contains("a.sDescript", fsText) + ")");
        return rows(lsSQL + " ORDER BY a.sDescript", "sBrandIDx", "sDescript", null, null);
    }

    /** Model variants of a brand (Search Model): model code, model name, variant. */
    public List<Suggestion> model(String fsText, String fsBrandId) throws SQLException, GuanzonException {
        List<Suggestion> laRows = new ArrayList<>();
        if (fsBrandId == null || fsBrandId.isEmpty()) {
            return laRows; //the search needs the brand first
        }
        if (poModel == null) {
            poModel = new ParamControllers(poGRider, null).ModelVariant();
        }
        String lsSQL = MiscUtil.addCondition(poModel.getSQ_Browse(),
                "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND b.sBrandIDx = " + SQLUtil.toSQL(fsBrandId)
                + " AND (" + starts("IFNULL(b.sModelCde, '')", fsText) + " OR " + contains("IFNULL(b.sDescript, '')", fsText)
                + " OR " + contains("a.sDescript", fsText) + ")");
        ResultSet loRS = poGRider.executeQuery(lsSQL + " ORDER BY xModelNme, a.sDescript LIMIT " + LIMIT);
        try {
            while (loRS.next()) {
                String lsCode = loRS.getString("xModelCde");
                String lsModel = loRS.getString("xModelNme");
                String lsVariant = loRS.getString("sDescript");
                laRows.add(new Suggestion(lsCode, lsModel, null, lsVariant, lsCode, lsModel, lsVariant));
            }
        } finally {
            MiscUtil.close(loRS);
        }
        return laRows;
    }

    /**
     * Items (Search Inventory of the request, or the replacement item of the
     * quotation): barcode, description, unit, brand / model.
     *
     * @param fsCategory2 empty: any
     * @param fsBrandId empty: any
     * @param fsModelId empty: any
     */
    public List<Suggestion> inventory(String fsText, String fsCategory1, String fsCategory2, String fsBrandId,
            String fsModelId) throws SQLException, GuanzonException {
        if (poInventory == null) {
            poInventory = new InvControllers(poGRider, null).Inventory();
        }
        String lsSQL = MiscUtil.addCondition(poInventory.getSQ_Browse(),
                "a.sCategCd1 = " + SQLUtil.toSQL(fsCategory1)
                + (isEmpty(fsCategory2) ? "" : " AND a.sCategCd2 = " + SQLUtil.toSQL(fsCategory2))
                + (isEmpty(fsBrandId) ? "" : " AND a.sBrandIDx = " + SQLUtil.toSQL(fsBrandId))
                + (isEmpty(fsModelId) ? "" : " AND a.sModelIDx = " + SQLUtil.toSQL(fsModelId))
                + " AND (" + starts("a.sBarCodex", fsText) + " OR " + contains("TRIM(a.sDescript)", fsText) + ")");
        List<Suggestion> laRows = new ArrayList<>();
        ResultSet loRS = poGRider.executeQuery(lsSQL + " GROUP BY a.sStockIDx ORDER BY a.sDescript LIMIT " + LIMIT);
        try {
            while (loRS.next()) {
                String lsBarcode = loRS.getString("sBarCodex");
                String lsDescription = loRS.getString("sDescript");
                laRows.add(new Suggestion(lsBarcode, lsDescription, loRS.getString("xMeasurNm"),
                        join(loRS.getString("xBrandNme"), loRS.getString("xModelNme")),
                        lsBarcode, lsDescription == null ? null : lsDescription.trim()));
            }
        } finally {
            MiscUtil.close(loRS);
        }
        return laRows;
    }

    /** Supplier (AP client): client ID and name. */
    public List<Suggestion> supplier(String fsText) throws SQLException, GuanzonException {
        if (poSupplier == null) {
            poSupplier = new ClientControllers(poGRider, null).APClientMaster();
        }
        String lsSQL = MiscUtil.addCondition(poSupplier.getSQ_Browse(),
                "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND (" + starts("a.sClientID", fsText) + " OR " + contains("IFNULL(b.sCompnyNm, '')", fsText) + ")");
        return rows(lsSQL + " ORDER BY b.sCompnyNm", "sClientID", "sCompnyNm", null, null);
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    private List<Suggestion> simple(String fsSQL) throws SQLException {
        return rows(fsSQL, "xCode", "xName", null, "xDetail");
    }

    private List<Suggestion> rows(String fsSQL, String fsCode, String fsName, String fsUnit, String fsDetail)
            throws SQLException {
        List<Suggestion> laRows = new ArrayList<>();
        ResultSet loRS = poGRider.executeQuery(fsSQL + " LIMIT " + LIMIT);
        try {
            while (loRS.next()) {
                laRows.add(new Suggestion(loRS.getString(fsCode), loRS.getString(fsName),
                        fsUnit == null ? null : loRS.getString(fsUnit),
                        fsDetail == null ? null : loRS.getString(fsDetail)));
            }
        } finally {
            MiscUtil.close(loRS);
        }
        return laRows;
    }

    static String starts(String fsColumn, String fsText) {
        return fsColumn + " LIKE " + SQLUtil.toSQL(escape(fsText) + "%");
    }

    static String contains(String fsColumn, String fsText) {
        return fsColumn + " LIKE " + SQLUtil.toSQL("%" + escape(fsText) + "%");
    }

    /** The typed text as LIKE text: % and _ are taken literally. */
    static String escape(String fsText) {
        String lsText = fsText == null ? "" : fsText.trim();
        return lsText.replace("%", "\\%").replace("_", "\\_");
    }

    private static String join(String fsBrand, String fsModel) {
        String lsBrand = fsBrand == null ? "" : fsBrand.trim();
        String lsModel = fsModel == null ? "" : fsModel.trim();
        return lsBrand.isEmpty() ? lsModel : lsModel.isEmpty() ? lsBrand : lsBrand + " / " + lsModel;
    }

    private static boolean isEmpty(String fsValue) {
        return fsValue == null || fsValue.isEmpty();
    }
}
