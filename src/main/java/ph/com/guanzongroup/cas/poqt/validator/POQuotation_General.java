package ph.com.guanzongroup.cas.poqt.validator;

import java.sql.SQLException;
import org.guanzon.appdriver.agent.MatrixAuthManager;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.SQLUtil;
import org.json.simple.JSONObject;

/**
 * PO Quotation rules for General Inventory and Los Pedritos (cas-purchasing's
 * POQuotation_LP): no quotation request needed, and confirming asks for the
 * PURCHASE ORDER MATRIX approval when the total requires it.
 */
public class POQuotation_General extends POQuotationValidator {

    @Override
    protected boolean isSourceRequired() {
        return false;
    }

    /**
     * Adds the matrix approval request for the quotation total. The result
     * carries the matrix (from processAuth) and is always "success"; the
     * controller asks for the approval.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected JSONObject validateConfirmed() throws SQLException, GuanzonException {
        MatrixAuthManager loMatrix = new MatrixAuthManager(poGRider, SOURCE_CD, poMaster.getTransactionNo());

        String lsTotal = poMaster.getTransactionTotal().toString();
        String lsRemarks = poMaster.getBranchCode()
                + "/" + poMaster.getTransactionNo()
                + ";" + SQLUtil.dateFormat(poMaster.getTransactionDate(), "yyyy-MM-dd")
                + ";" + lsTotal
                + ";" + poMaster.getRemarks();

        String lsAuthCode = loMatrix.getAuthType("PURCHASE ORDER MATRIX", lsTotal, "");
        if (!lsAuthCode.isEmpty()) {
            loMatrix.addAuthRequest(lsAuthCode, "", "", lsRemarks);
        }

        JSONObject loJSON = loMatrix.hasAuthRequest() ? loMatrix.processAuth() : new JSONObject();
        loJSON.put("result", "success");
        return loJSON;
    }
}
