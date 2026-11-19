package ph.com.guanzongroup.cas.poqt.validator;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.iface.GValidator;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Master;
import ph.com.guanzongroup.cas.poqt.status.POQuotationRequestStatus;
import ph.com.guanzongroup.cas.poqt.utility.Results;

/**
 * PO Quotation Request rules shared by all industries.
 *
 * <p>
 * cas-purchasing had one validator per industry (POQuotationRequest_MP, _MC,
 * _Vehicle, _Monarch, _Appliances, _LP); they were identical, so they are this
 * one class. An industry with different rules extends it and overrides a hook;
 * map it in {@link QuotationValidators}.</p>
 */
public class POQuotationRequestValidator implements GValidator {

    private static final String NULL_DATE = "1900-01-01";

    protected GRiderCAS poGRider;
    protected String psTranStat;
    protected Model_PO_Quotation_Request_Master poMaster;
    protected final List<Model_PO_Quotation_Request_Detail> paDetail = new ArrayList<>();

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
        poMaster = (Model_PO_Quotation_Request_Master) value;
    }

    @Override
    public void setDetail(ArrayList<Object> value) {
        paDetail.clear();
        for (Object loItem : value) {
            paDetail.add((Model_PO_Quotation_Request_Detail) loItem);
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
            return POQuotationRequestStatus.OPEN.equals(psTranStat) ? validateNew() : validateOther(psTranStat);
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            return Results.error(ex.getMessage());
        }
    }

    /** Saving an OPEN request. */
    protected JSONObject validateNew() throws SQLException {
        Date ldTransact = poMaster.getTransactionDate();
        if (ldTransact == null || NULL_DATE.equals(new SimpleDateFormat("yyyy-MM-dd").format(ldTransact))) {
            return Results.error("Invalid Transaction Date.");
        }
        if (poMaster.getIndustryId() == null) {
            return Results.error("Industry is not set.");
        }
        if (isEmpty(poMaster.getCategoryCode())) {
            return Results.error("Category Code is not set.");
        }
        if (isEmpty(poMaster.getBranchCode())) {
            return Results.error("Branch is not set.");
        }
        if (isEmpty(poMaster.getCategoryLevel2())) {
            return Results.error("Category is not set.");
        }
        if (isEmpty(poMaster.getDestination())) {
            return Results.error("Destination is not set.");
        }
        if (isEmpty(poMaster.getDepartmentId())) {
            return Results.error("Department is not set.");
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

    /** Rules for confirm, approve, post, cancel and void. Default: none. */
    protected JSONObject validateOther(String fsStatus) throws SQLException {
        return Results.success();
    }

    protected static boolean isEmpty(String fsValue) {
        return fsValue == null || fsValue.isEmpty();
    }
}
