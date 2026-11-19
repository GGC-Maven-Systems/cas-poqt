package ph.com.guanzongroup.cas.poqt.model;

import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Category_Level2;
import org.guanzon.cas.parameter.model.Model_Department;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.poqt.status.POQuotationRequestStatus;

/**
 *
 * @author Arsiela
 */
public class Model_PO_Quotation_Request_Master extends Model {
    String psSearchDepartment = "";
    String psSearchBranch = "";
    String psSearchCategory = "";
    
    //All reference fields below are intentionally NOT constructed in initialize() - see their
    //accessors, which build them lazily on first access so opening this record never touches
    //those tables. (poCompany was previously eager-constructed here too but had no accessor
    //anywhere in this class - removed as a dead field.)
    Model_Branch poBranch;
    Model_Branch poDestination;
    Model_Industry poIndustry;
    Model_Department poDepartment;
    Model_Category poCategory;
    Model_Category_Level2 poCategory2;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

//            poEntity.updateObject("dModified", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));

            //assign default values
            poEntity.updateNull("dTransact");
            poEntity.updateNull("dExpPurch");
            poEntity.updateNull("dPrepared");
            poEntity.updateNull("dModified");
            poEntity.updateObject("nEntryNox", 0);
//            poEntity.updateString("cProcessd", "0");
            poEntity.updateString("cTranStat", POQuotationRequestStatus.OPEN);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logInitError(e);
            System.exit(1);
        }
    }

    /** Logs a failed initialize (the log wrapper is often not set on models). */
    private void logInitError(SQLException foException) {
        if (logwrapr != null) {
            logwrapr.severe(foException.getMessage());
        }
        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unable to initialize " + getTable(), foException);
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setIndustryId(String industryId) {
        return setValue("sIndstCdx", industryId);
    }

    public String getIndustryId() {
        return (String) getValue("sIndstCdx");
    }

    public JSONObject setCategoryCode(String categoryCode) {
        return setValue("sCategrCd", categoryCode);
    }

    public String getCategoryCode() {
        return (String) getValue("sCategrCd");
    }

    public JSONObject setCategoryLevel2(String categoryCode) {
        return setValue("sCategCd2", categoryCode);
    }

    public String getCategoryLevel2() {
        return (String) getValue("sCategCd2");
    }

    public JSONObject setBranchCode(String branchCode) {
        return setValue("sBranchCd", branchCode);
    }

    public String getBranchCode() {
        return (String) getValue("sBranchCd");
    }

    public JSONObject setDepartmentId(String departmentId) {
        return setValue("sDeptIDxx", departmentId);
    }

    public String getDepartmentId() {
        return (String) getValue("sDeptIDxx");
    }
    
    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    public JSONObject setDestination(String destination) {
        return setValue("sDestinat", destination);
    }

    public String getDestination() {
        return (String) getValue("sDestinat");
    }

    public JSONObject setReferenceNo(String referenceNo) {
        return setValue("sReferNox", referenceNo);
    }

    public String getReferenceNo() {
        return (String) getValue("sReferNox");
    }

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }
    
    public JSONObject setExpectedPurchaseDate(Date expectedPurchaseDate) {
        if(expectedPurchaseDate == null){
            try {
                poEntity.updateNull("dExpPurch");
                return null;
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        return setValue("dExpPurch", expectedPurchaseDate);
    }

    public Date getExpectedPurchaseDate() {
        return (Date) getValue("dExpPurch");
    }

    public JSONObject setEntryNo(Number entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public Number getEntryNo() {
        return (Number) getValue("nEntryNox");
    }

    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }
    
    public JSONObject isProcessed(boolean isProcessed) {
        return setValue("cProcessd", isProcessed ? "1" : "0");
    }

    public boolean isProcessed() {
        return ((String) getValue("cProcessd")).equals("1");
    }

    public JSONObject setPrepared(String preparedBy) {
        return setValue("sPrepared", preparedBy);
    }

    public String getPrepared() {
        return (String) getValue("sPrepared");
    }

    public JSONObject setPreparedDate(Date preparedDate) {
        return setValue("dPrepared", preparedDate);
    }

    public Date getPreparedDate() {
        return (Date) getValue("dPrepared");
    }

    public JSONObject setModifyingId(String modifiedBy) {
        return setValue("sModified", modifiedBy);
    }

    public String getModifyingId() {
        return (String) getValue("sModified");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    @Override
    public String getNextCode() {
//        return "";
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }

    //reference object models
    public Model_Branch Branch() throws GuanzonException, SQLException {
        if (poBranch == null) {
            poBranch = new ParamModels(poGRider).Branch();
        }
        return ModelReferences.load(poBranch, ModelReferences.text(getValue("sBranchCd")), "Branch", Model_Branch::getBranchCode);
    }

    public Model_Branch Destination() throws GuanzonException, SQLException {
        if (poDestination == null) {
            poDestination = new ParamModels(poGRider).Branch();
        }
        return ModelReferences.load(poDestination, ModelReferences.text(getValue("sDestinat")), "Branch", Model_Branch::getBranchCode);
    }

    public Model_Industry Industry() throws GuanzonException, SQLException {
        if (poIndustry == null) {
            poIndustry = new ParamModels(poGRider).Industry();
        }
        return ModelReferences.load(poIndustry, ModelReferences.text(getValue("sIndstCdx")), "Industry", Model_Industry::getIndustryId);
    }

    public Model_Category Category() throws GuanzonException, SQLException {
        if (poCategory == null) {
            poCategory = new ParamModels(poGRider).Category();
        }
        return ModelReferences.load(poCategory, ModelReferences.text(getValue("sCategrCd")), "Category", Model_Category::getCategoryId);
    }

    public Model_Category_Level2 Category2() throws GuanzonException, SQLException {
        if (poCategory2 == null) {
            poCategory2 = new ParamModels(poGRider).Category2();
        }
        return ModelReferences.load(poCategory2, ModelReferences.text(getValue("sCategCd2")), "Category_Level2", Model_Category_Level2::getCategoryId);
    }

    public Model_Department Department() throws GuanzonException, SQLException {
        if (poDepartment == null) {
            poDepartment = new ParamModels(poGRider).Department();
        }
        return ModelReferences.load(poDepartment, ModelReferences.text(getValue("sDeptIDxx")), "Department", Model_Department::getDepartmentId);
    }
//    
//    /* For Searching method*/
//    public Model_Branch SearchBranch() throws SQLException, GuanzonException {
//        if (!"".equals(psSearchBranch)) {
//            if (poBranch.getEditMode() == EditMode.READY
//                    && poBranch.getBranchCode().equals(psSearchBranch)) {
//                return poBranch;
//            } else {
//                poJSON = poBranch.openRecord(psSearchBranch);
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poBranch;
//                } else {
//                    poBranch.initialize();
//                    return poBranch;
//                }
//            }
//        } else {
//            poBranch.initialize();
//            return poBranch;
//        }
//    }
//    
//    public Model_Category_Level2 SearchCategory2() throws GuanzonException, SQLException {
//        if (!"".equals(psSearchCategory)) {
//            if (poCategory2.getEditMode() == EditMode.READY
//                    && poCategory2.getCategoryId().equals(psSearchCategory)) {
//                return poCategory2;
//            } else {
//                poJSON = poCategory2.openRecord(psSearchCategory);
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poCategory2;
//                } else {
//                    poCategory2.initialize();
//                    return poCategory2;
//                }
//            }
//        } else {
//            poCategory2.initialize();
//            return poCategory2;
//        }
//    }
}
