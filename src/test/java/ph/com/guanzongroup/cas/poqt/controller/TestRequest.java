package ph.com.guanzongroup.cas.poqt.controller;

import static ph.com.guanzongroup.cas.poqt.controller.QtKit.model;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.ok;

import java.util.ArrayList;
import java.util.List;
import javax.sql.rowset.CachedRowSet;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Master;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Supplier;

/** POQuotationRequest with the appdriver framework replaced (no database). */
class TestRequest extends POQuotationRequest {

    final List<String> calls = new ArrayList<>();
    JSONObject openResult = ok();
    JSONObject newResult = ok();
    JSONObject updateResult = ok();
    JSONObject saveResult = ok();
    CachedRowSet statusHistory;
    Object[] shownHistory;

    TestRequest(GRiderCAS foGRider, Model_PO_Quotation_Request_Master foMaster) {
        poGRider = foGRider;
        poMaster = foMaster;
        paDetail = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paSuppliers = new ArrayList<>();
        paSuppliersRemoved = new ArrayList<>();
        paMasterList = new ArrayList<>();
        SOURCE_CODE = "PQRq";
        pnEditMode = EditMode.READY;
        pbWthParent = true;
        psIndustryId = "09";
        psCategorCd = "0001";
        psCompanyId = "C001";
    }

    void mode(int fnMode) {
        pnEditMode = fnMode;
    }

    void parent(boolean fbParent) {
        pbWthParent = fbParent;
    }

    Model_PO_Quotation_Request_Detail line(Object... faValues) {
        Model_PO_Quotation_Request_Detail loRow = model(Model_PO_Quotation_Request_Detail.class, faValues);
        //brand and model are fields of the detail model, not columns
        for (int lnCtr = 0; lnCtr < faValues.length; lnCtr += 2) {
            if ("sBrandIDx".equals(faValues[lnCtr])) {
                loRow.setBrandId((String) faValues[lnCtr + 1]);
            } else if ("sModelIDx".equals(faValues[lnCtr])) {
                loRow.setModelId((String) faValues[lnCtr + 1]);
            }
        }
        paDetail.add(loRow);
        return loRow;
    }

    Model_PO_Quotation_Request_Supplier supplier(Object... faValues) {
        Model_PO_Quotation_Request_Supplier loRow = model(Model_PO_Quotation_Request_Supplier.class, faValues);
        paSuppliers.add(loRow);
        return loRow;
    }

    String browseSql() {
        return SQL_BROWSE;
    }

    int removedCount() {
        return paDetailRemoved.size();
    }

    @Override
    protected JSONObject initialize() {
        calls.add("initialize");
        return ok();
    }

    @Override
    protected JSONObject newTransaction() {
        calls.add("new");
        pnEditMode = EditMode.ADDNEW;
        return newResult;
    }

    @Override
    protected JSONObject openTransaction(String fsTransNo) {
        calls.add("open " + fsTransNo);
        pnEditMode = EditMode.READY;
        return openResult;
    }

    @Override
    protected JSONObject updateTransaction() {
        calls.add("update");
        pnEditMode = EditMode.UPDATE;
        return updateResult;
    }

    @Override
    protected JSONObject saveTransaction() {
        calls.add("save");
        return saveResult;
    }

    @Override
    protected JSONObject addDetail() {
        calls.add("addDetail");
        paDetail.add(model(Model_PO_Quotation_Request_Detail.class, "sStockIDx", "", "sDescript", ""));
        return ok();
    }

    @Override
    protected JSONObject deleteDetail(int fnRow) {
        calls.add("deleteDetail " + fnRow);
        paDetail.remove(fnRow);
        return ok();
    }

    @Override
    protected CachedRowSet getStatusHistory() {
        return statusHistory;
    }

    @Override
    protected void showStatusHistoryUI(String fsTitle, String fsTransNo, String fsEntryBy, String fsEntryDate, CachedRowSet foRows) {
        shownHistory = new Object[]{fsTitle, fsTransNo, fsEntryBy, fsEntryDate, foRows};
    }
}
