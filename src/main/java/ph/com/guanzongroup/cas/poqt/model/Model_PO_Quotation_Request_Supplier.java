package ph.com.guanzongroup.cas.poqt.model;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.client.model.Model_Client_Address;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.model.Model_Client_Mobile;
import org.guanzon.cas.client.services.ClientModels;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Term;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.poqt.services.QuotationModels;

/**
 *
 * @author Arsiela
 */
public class Model_PO_Quotation_Request_Supplier extends Model {
    
    String psBrandId = "";
    String psModelId = "";
    
    //reference objects
    //All reference fields below are intentionally NOT constructed in initialize() - see their
    //accessors, which build them lazily on first access so opening this record never touches
    //those tables. (poIndustry/poCategory were previously eager-constructed here too but their
    //accessors are commented out below - removed as dead fields.)
    Model_Company poCompany;
    Model_Term poTerm;
    Model_Client_Master poSupplier;
    Model_Client_Address poSupplierAddress;
    Model_Client_Mobile poSupplierMobile;

    Model_PO_Quotation_Request_Master poPOQuotationRequest;
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
            poEntity.updateNull("dModified");
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("cReversex", "+");
            poEntity.updateObject("cSendStat", "0");
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";

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
    
    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        if (getValue("nEntryNox") == null || "".equals(getValue("nEntryNox"))) {
            return 0;
        }
        return (int) getValue("nEntryNox");
    }

    public JSONObject setCompanyId(String companyId) {
        return setValue("sCompnyID", companyId);
    }

    public String getCompanyId() {
        return (String) getValue("sCompnyID");
    }

    public JSONObject setSupplierId(String supplier) {
        return setValue("sSupplier", supplier);
    }

    public String getSupplierId() {
        return (String) getValue("sSupplier");
    }

    public JSONObject setAddressId(String addressID) {
        return setValue("sAddrssID", addressID);
    }

    public String getAddressId() {
        return (String) getValue("sAddrssID");
    }

    public JSONObject setContactId(String contactID) {
        return setValue("sContctID", contactID);
    }

    public String getContactId() {
        return (String) getValue("sContctID");
    }

    public JSONObject setTerm(String termCode) {
        return setValue("sTermCode", termCode);
    }

    public String getTerm() {
        return (String) getValue("sTermCode");
    }
    
    public JSONObject isReverse(boolean isReverse) {
        return setValue("cReversex", isReverse ? "+" : "-");
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }
    
    public JSONObject isSent(boolean sent) {
        return setValue("cSendStat", sent ? "1" : "0");
    }

    public boolean isSent() {
        return ((String) getValue("cSendStat")).equals("1");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    @Override
    public String getNextCode() {
        return "";
    }

    //reference object models
//    public Model_Industry Industry() throws SQLException, GuanzonException {
//        if (!"".equals((String) getValue("sIndstCdx"))) {
//            if (poIndustry.getEditMode() == EditMode.READY
//                    && poIndustry.getIndustryId().equals((String) getValue("sIndstCdx"))) {
//                return poIndustry;
//            } else {
//                poJSON = poIndustry.openRecord((String) getValue("sIndstCdx"));
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poIndustry;
//                } else {
//                    poIndustry.initialize();
//                    return poIndustry;
//                }
//            }
//        } else {
//            poIndustry.initialize();
//            return poIndustry;
//        }
//    }

    public Model_Company Company() throws GuanzonException, SQLException {
        if (poCompany == null) {
            poCompany = new ParamModels(poGRider).Company();
        }
        return ModelReferences.load(poCompany, ModelReferences.text(getValue("sCompnyID")), "Company", Model_Company::getCompanyId);
    }
    
//    public Model_Category_Level2 Category2() throws GuanzonException, SQLException {
//        if (!"".equals((String) getValue("sCategrCd"))) {
//            if (poCategory.getEditMode() == EditMode.READY
//                    && poCategory.getCategoryId().equals((String) getValue("sCategrCd"))) {
//                return poCategory;
//            } else {
//                poJSON = poCategory.openRecord((String) getValue("sCategrCd"));
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poCategory;
//                } else {
//                    poCategory.initialize();
//                    return poCategory;
//                }
//            }
//        } else {
//            poCategory.initialize();
//            return poCategory;
//        }
//    }
    
    public Model_Term Term() throws GuanzonException, SQLException {
        if (poTerm == null) {
            poTerm = new ParamModels(poGRider).Term();
        }
        return ModelReferences.load(poTerm, ModelReferences.text(getValue("sTermCode")), "Term", Model_Term::getTermId);
    }

    public Model_Client_Master Supplier() throws GuanzonException, SQLException {
        if (poSupplier == null) {
            poSupplier = new ClientModels(poGRider).ClientMaster();
        }
        return ModelReferences.load(poSupplier, ModelReferences.text(getValue("sSupplier")), "Client_Master", Model_Client_Master::getClientId);
    }

    public Model_Client_Address Address() throws GuanzonException, SQLException {
        if (poSupplierAddress == null) {
            poSupplierAddress = new ClientModels(poGRider).ClientAddress();
        }
        return ModelReferences.load(poSupplierAddress, ModelReferences.text(getValue("sSupplier")), "Client_Address", Model_Client_Address::getClientId);
    }

    public Model_Client_Mobile Contact() throws GuanzonException, SQLException {
        if (poSupplierMobile == null) {
            poSupplierMobile = new ClientModels(poGRider).ClientMobile();
        }
        return ModelReferences.load(poSupplierMobile, ModelReferences.text(getValue("sContctID")), "Client_Mobile", Model_Client_Mobile::getClientId);
    }

    /** Not cached (transaction record). */
    public Model_PO_Quotation_Request_Master POQuotationRequestMaster() throws GuanzonException, SQLException {
        if (poPOQuotationRequest == null) {
            poPOQuotationRequest = new QuotationModels(poGRider).POQuotationRequestMaster();
        }
        return ModelReferences.load(poPOQuotationRequest, ModelReferences.text(getValue("sTransNox")), null, Model_PO_Quotation_Request_Master::getTransactionNo);
    }
    
    public JSONObject openRecord(String transactionNo, String supplierId, String companyId) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsSQL = MiscUtil.makeSelect(this);
        lsSQL = MiscUtil.addCondition(lsSQL, " sTransNox = " + SQLUtil.toSQL(transactionNo) 
                                        + " AND sSupplier = " + SQLUtil.toSQL(supplierId)
                                        + " AND sCompnyID = " + SQLUtil.toSQL(companyId));
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (loRS.next()) {
                for (int lnCtr = 1; lnCtr <= loRS.getMetaData().getColumnCount(); lnCtr++){
                    setValue(lnCtr, loRS.getObject(lnCtr)); 
                }
                MiscUtil.close(loRS);
                pnEditMode = EditMode.READY;
                poJSON = new JSONObject();
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record to load.");
            } 
        } catch (SQLException e) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } 
        return poJSON;
    }
    
    //end reference object models
}
