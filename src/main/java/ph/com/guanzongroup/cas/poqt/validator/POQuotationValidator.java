package ph.com.guanzongroup.cas.poqt.validator;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.inv.InvTransCons;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Master;
import ph.com.guanzongroup.cas.poqt.status.POQuotationStatus;
import ph.com.guanzongroup.cas.poqt.utility.Results;

/**
 * PO Quotation rules shared by all industries.
 *
 * <p>
 * cas-purchasing had one validator per industry (POQuotation_MP, _MC,
 * _Vehicle, _Monarch, _Appliances, _LP); all were the same except LP, so they
 * are this one class. An industry with different rules extends it and
 * overrides a hook (see {@link POQuotation_General}); map it in
 * {@link QuotationValidators}.</p>
 */
public class POQuotationValidator implements GValidator {

    protected static final String SOURCE_CD = InvTransCons.PURCHASE_ORDER_QUOTATION;
    private static final String NULL_DATE = "1900-01-01";

    protected GRiderCAS poGRider;
    protected String psTranStat;
    protected Model_PO_Quotation_Master poMaster;
    protected final List<Model_PO_Quotation_Detail> paDetail = new ArrayList<>();

    @Override
    public void setApplicationDriver(Object applicationDriver) {
        poGRider = (GRiderCAS) applicationDriver;
    }

    @Override
    public void setTransactionStatus(String transactionStatus) {
        psTranStat = transactionStatus;
    }

    @Override
    public void setMaster(Object value) {
        poMaster = (Model_PO_Quotation_Master) value;
    }

    @Override
    public void setDetail(ArrayList<Object> value) {
        paDetail.clear();
        for (Object loItem : value) {
            paDetail.add((Model_PO_Quotation_Detail) loItem);
        }
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final JSONObject validate() {
        JSONObject loJSON = QuotationStatusRules.validateStatusChange(poMaster.getTransactionStatus(), psTranStat);
        if (!Results.isSuccess(loJSON)) {
            return loJSON;
        }
        try {
            switch (psTranStat) {
                case POQuotationStatus.OPEN:
                    return validateNew();
                case POQuotationStatus.CONFIRMED:
                    return validateConfirmed();
                default:
                    return Results.success();
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            return Results.error(ex.getMessage());
        }
    }

    // =====================================================================
    // Saving an OPEN quotation
    // =====================================================================
    protected JSONObject validateNew() throws SQLException {
        Date ldTransact = poMaster.getTransactionDate();
        Date ldValidity = poMaster.getValidityDate();
        Date ldReference = poMaster.getReferenceDate();

        if (ldTransact == null || NULL_DATE.equals(shortDate(ldTransact))) {
            return Results.error("Invalid Transaction Date.");
        }
        if (ldValidity == null || NULL_DATE.equals(shortDate(ldValidity))) {
            return Results.error("Invalid Validity Date.");
        }
        if (ldReference == null || NULL_DATE.equals(shortDate(ldReference))) {
            return Results.error("Invalid Reference Date.");
        }
        if (poMaster.getIndustryId() == null) {
            return Results.error("Industry is not set.");
        }
        if (isEmpty(poMaster.getCompanyId())) {
            return Results.error("Company is not set.");
        }
        if (isEmpty(poMaster.getCategoryCode())) {
            return Results.error("Category Code is not set.");
        }
        if (isEmpty(poMaster.getBranchCode())) {
            return Results.error("Branch is not set.");
        }
        if (isSourceRequired()) {
            if (isEmpty(poMaster.getSourceNo())) {
                return Results.error("Source No is not set.");
            }
            if (isEmpty(poMaster.getSourceCode())) {
                return Results.error("Source Code is not set.");
            }
        }
        if (isEmpty(poMaster.getSupplierId())) {
            return Results.error("Supplier is not set.");
        }
        if (isEmpty(poMaster.getTerm())) {
            return Results.error("Term is not set.");
        }
        if (poMaster.getGrossAmount() <= 0.0000) {
            return Results.error("Invalid transaction total.");
        }
        if (poMaster.getTransactionTotal() <= 0.0000) {
            return Results.error("Invalid net total.");
        }
        if (isEmpty(poMaster.getPrepared())) {
            return Results.error("Prepared by is not set.");
        }
        if (isEmpty(poMaster.getModifyingId())) {
            return Results.error("Modified by is not set.");
        }
        if (isEmpty(poMaster.getTransactionStatus())) {
            return Results.error("Transaction Status is not set.");
        }
        return Results.success();
    }

    /** Whether the quotation must come from a quotation request. Default: yes. */
    protected boolean isSourceRequired() {
        return true;
    }

    // =====================================================================
    // Confirming
    // =====================================================================
    /** Confirm rules. Default: none. */
    protected JSONObject validateConfirmed() throws SQLException, GuanzonException {
        return Results.success();
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    protected static boolean isEmpty(String fsValue) {
        return fsValue == null || fsValue.isEmpty();
    }

    protected static String shortDate(Date fdValue) {
        return new SimpleDateFormat("yyyy-MM-dd").format(fdValue);
    }
}
