package ph.com.guanzongroup.cas.poqt.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.MatrixAuthChecker;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * What {@link POQuotationRequest} and {@link POQuotation} share.
 *
 * <p>
 * In cas-purchasing both controllers had their own copy of all of this, and
 * each status procedure (Confirm, Approve, Disapprove, Void, Cancel, Post,
 * Return) repeated the same matrix-authorization code. They now call
 * {@link #changeStatus}.</p>
 *
 * <ul>
 * <li>New / Open / Update / Save with the user-authority check
 * ({@link #seekApproval()});</li>
 * <li>the status-change template ({@link #changeStatus});</li>
 * <li>the list filters (branch, department, supplier, category) and the
 * industry / company / category of the screen;</li>
 * <li>small helpers (detail counts, dates, system user name).</li>
 * </ul>
 */
public abstract class QuotationTransaction extends Transaction {

    protected String psIndustryId = "";
    protected String psCompanyId = "";
    protected String psCategorCd = "";

    protected String psSearchBranch;
    protected String psSearchDepartment;
    protected String psSearchSupplier;
    protected String psSearchCategory;

    /** Detail rows removed while editing. */
    protected List<Model> paDetailRemoved;

    // =====================================================================
    // Procedures
    // =====================================================================
    public JSONObject NewTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        if (!pbWthParent) {
            poJSON = seekApproval();
            if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        return newTransaction();
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        if (!pbWthParent) {
            poJSON = seekApproval();
            if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        //clear the previous record
        resetMaster();
        resetOthers();
        Detail().clear();
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() throws SQLException, GuanzonException {
        if (!pbWthParent) {
            poJSON = seekApproval();
            if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        return updateTransaction();
    }

    /** Clears the master record (before opening another one). */
    public abstract void resetMaster();

    /** Clears the lists and filters of the previous record. */
    public abstract void resetOthers();

    /**
     * Checks the current user's authority for the purchasing module and, if
     * needed, asks an approving officer.
     */
    @SuppressWarnings("unchecked")
    public JSONObject seekApproval() throws SQLException, GuanzonException {
        //"cas-purchasing" is the module key in the user rights, not the Maven project
        ActionAuthManager loAuth = new ActionAuthManager(poGRider, "cas-purchasing");
        poJSON = loAuth.isAuthorized();

        if (!((String) poJSON.get("result")).equalsIgnoreCase("true")) {
            ShowMessageFX.Warning((String) poJSON.get("warning"), "Authorization Required", null);
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
            int lnUserLevl = Integer.parseInt(poJSON.get("nUserLevl").toString());
            poJSON = loAuth.isAuthorized(lsUserIDxx, lnUserLevl);
            if (!((String) poJSON.get("result")).equalsIgnoreCase("true")) {
                ShowMessageFX.Warning((String) poJSON.get("warning"), "Authorization Required", null);
                poJSON.put("result", "error");
                poJSON.put("message", "User is not an authorized approving officer..");
                return poJSON;
            }
            setApproving(lsUserIDxx);
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    // =====================================================================
    // Status-change template
    // =====================================================================
    /**
     * Changes the status of the loaded record (the body every status
     * procedure had):
     * <ol>
     * <li>a record must be loaded;</li>
     * <li>without a parent: the validator runs (isEntryOkay); when it returns
     * a matrix, the authorization matrix is checked (current user, or an
     * approving officer). Not fully approved yet: the pending letter
     * (A = 1, B = 2, ...) is saved and "matrix" is returned (Return: "success").
     * No matrix: the user's authority is checked (seekApproval);</li>
     * <li>the new status is saved and the matrix request is posted.</li>
     * </ol>
     *
     * @param fbPassParent pass pbWthParent to statusChange (Approve and Post of
     * the quotation did)
     * @param fbReturn Return: a pending matrix gives "success", and the matrix
     * request is not posted
     */
    @SuppressWarnings("unchecked")
    protected JSONObject changeStatus(String fsStatus, String fsRemarks, String fsEvent, String fsSuccessMessage,
            boolean fbPassParent, boolean fbReturn)
            throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        String lsStatus = fsStatus;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }
        String lsTransNo = (String) poMaster.getValue("sTransNox");

        MatrixAuthChecker loCheck = null;
        if (!pbWthParent) {
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            JSONArray laMatrix = (JSONArray) poJSON.get("matrix");
            if (laMatrix != null) {
                loCheck = new MatrixAuthChecker(poGRider, SOURCE_CODE, lsTransNo);
                poJSON = loCheck.loadAuth();
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                if (!loCheck.isAuthOkay()) {
                    if (!loCheck.isAllowSys()) {
                        JSONObject loMatrix = (JSONObject) laMatrix.get(0);
                        String lsAuthType = (String) loMatrix.get("sAuthType");

                        //is the current user authorized to approve this?
                        poJSON = loCheck.authTrans(lsAuthType, poGRider.getUserID());
                        if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                            //ask an approving officer
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if ("error".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //the approving officer's authority (cas-purchasing checked the current user again)
                            poJSON = loCheck.authTrans(lsAuthType, lsUserIDxx);
                            if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    if (!loCheck.isAuthOkay()) {
                        //not approved by everyone yet: save the pending status
                        poGRider.beginTrans("UPDATE STATUS", fsEvent, SOURCE_CODE, lsTransNo);
                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), lsTransNo, fsRemarks, lsStatus, false);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }
                        poGRider.commitTrans();

                        poJSON.put("result", fbReturn ? "success" : "matrix");
                        return poJSON;
                    }
                }
            } else {
                poJSON = seekApproval();
                if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }

        poJSON = fbPassParent
                ? statusChange(poMaster.getTable(), lsTransNo, fsRemarks, lsStatus, false, pbWthParent)
                : statusChange(poMaster.getTable(), lsTransNo, fsRemarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (loCheck != null && !fbReturn) {
            loCheck.postAuth();
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", fsSuccessMessage);
        return poJSON;
    }

    // =====================================================================
    // List filters and screen context
    // =====================================================================
    public void setSearchBranch(String searchBranch) {
        psSearchBranch = searchBranch;
    }

    public String getSearchBranch() {
        return psSearchBranch;
    }

    public void setSearchDepartment(String searchDepartment) {
        psSearchDepartment = searchDepartment;
    }

    public String getSearchDepartment() {
        return psSearchDepartment;
    }

    public void setSearchSupplier(String searchSupplier) {
        psSearchSupplier = searchSupplier;
    }

    public String getSearchSupplier() {
        return psSearchSupplier;
    }

    public void setSearchCategory(String searchCategory) {
        psSearchCategory = searchCategory;
    }

    public String getSearchCategory() {
        return psSearchCategory;
    }

    public void setIndustryId(String industryId) {
        psIndustryId = industryId;
    }

    public void setCompanyId(String companyId) {
        psCompanyId = companyId;
    }

    public void setCategoryId(String categoryId) {
        psCategorCd = categoryId;
    }

    // =====================================================================
    // Framework callbacks and helpers
    // =====================================================================
    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public void saveComplete() {
        System.out.println("Transaction saved successfully.");
    }

    public int getDetailCount() {
        if (paDetail == null) {
            paDetail = new ArrayList<>();
        }
        return paDetail.size();
    }

    protected int getDetailRemovedCount() {
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }
        return paDetailRemoved.size();
    }

    /** Name of a system user (xxxSysUser to Client_Master); empty when not found. */
    @SuppressWarnings("unchecked")
    public String getSysUser(String fsId) throws SQLException, GuanzonException {
        String lsSQL = MiscUtil.addCondition(" SELECT b.sCompnyNm from xxxSysUser a "
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployNo ",
                " a.sUserIDxx =  " + SQLUtil.toSQL(fsId));
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            return loRS.next() ? loRS.getString("sCompnyNm") : "";
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
            return "";
        } finally {
            MiscUtil.close(loRS);
        }
    }

    protected static String xsDateShort(Date fdValue) {
        return new SimpleDateFormat("yyyy-MM-dd").format(fdValue);
    }

    protected static LocalDate strToDate(String val) {
        return LocalDate.parse(val, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
