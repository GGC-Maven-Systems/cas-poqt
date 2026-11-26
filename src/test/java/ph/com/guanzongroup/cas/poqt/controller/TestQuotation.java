package ph.com.guanzongroup.cas.poqt.controller;

import static org.mockito.Mockito.when;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.model;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.ok;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.smart;

import java.util.ArrayList;
import java.util.List;
import javax.sql.rowset.CachedRowSet;
import org.guanzon.appdriver.agent.systables.Model_Transaction_Attachment;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Master;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Supplier;

/** POQuotation with the appdriver framework replaced (no database). */
class TestQuotation extends POQuotation {

    final List<String> calls = new ArrayList<>();
    JSONObject openResult = ok();
    CachedRowSet statusHistory;
    Object[] shownHistory;

    TestQuotation(GRiderCAS foGRider, Model_PO_Quotation_Master foMaster) {
        poGRider = foGRider;
        poMaster = foMaster;
        paDetail = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paPORequestSupplier = new ArrayList<>();
        paMasterList = new ArrayList<>();
        paAttachments = new ArrayList<>();
        SOURCE_CODE = "PQtn";
        pnEditMode = EditMode.READY;
        pbWthParent = true;
        psIndustryId = "09";
        psCategorCd = "0001";
        psCompanyId = "C001";
    }

    void mode(int fnMode) {
        pnEditMode = fnMode;
    }

    String browseSql() {
        return SQL_BROWSE;
    }

    int removedCount() {
        return paDetailRemoved.size();
    }

    /** A detail row; text columns start blank, the given values override. */
    Model_PO_Quotation_Detail line(Object... faValues) {
        Object[] laValues = new Object[faValues.length + 8];
        System.arraycopy(new Object[]{"sStockIDx", "", "sDescript", "", "sReplacID", "", "sReplacDs", ""}, 0, laValues, 0, 8);
        System.arraycopy(faValues, 0, laValues, 8, faValues.length);
        Model_PO_Quotation_Detail loRow = model(Model_PO_Quotation_Detail.class, laValues);
        paDetail.add(loRow);
        return loRow;
    }

    Model_PO_Quotation_Request_Supplier requestSupplier(Object... faValues) {
        Model_PO_Quotation_Request_Supplier loRow = model(Model_PO_Quotation_Request_Supplier.class, faValues);
        paPORequestSupplier.add(loRow);
        return loRow;
    }

    /** An attachment row (parameter edit mode and model values). */
    TransactionAttachment attachment(int fnEditMode, Object... faValues) {
        TransactionAttachment loRow = attachmentMock(fnEditMode, faValues);
        paAttachments.add(loRow);
        return loRow;
    }

    static TransactionAttachment attachmentMock(int fnEditMode, Object... faValues) {
        TransactionAttachment loRow = smart(TransactionAttachment.class);
        Model_Transaction_Attachment loModel = model(Model_Transaction_Attachment.class, faValues);
        QtKit.edit(loModel, fnEditMode);
        when(loRow.getModel()).thenReturn(loModel);
        when(loRow.getEditMode()).thenReturn(fnEditMode);
        return loRow;
    }

    @Override
    protected JSONObject initialize() {
        calls.add("initialize");
        return ok();
    }

    @Override
    protected JSONObject openTransaction(String fsTransNo) {
        calls.add("open " + fsTransNo);
        pnEditMode = EditMode.READY;
        return openResult;
    }

    @Override
    protected JSONObject newTransaction() {
        calls.add("new");
        pnEditMode = EditMode.ADDNEW;
        return ok();
    }

    @Override
    protected JSONObject updateTransaction() {
        calls.add("update");
        pnEditMode = EditMode.UPDATE;
        return ok();
    }

    @Override
    protected JSONObject saveTransaction() {
        calls.add("save");
        return ok();
    }

    @Override
    protected JSONObject addDetail() {
        calls.add("addDetail");
        paDetail.add(model(Model_PO_Quotation_Detail.class, "sStockIDx", "", "sDescript", "", "sReplacID", "", "sReplacDs", ""));
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
