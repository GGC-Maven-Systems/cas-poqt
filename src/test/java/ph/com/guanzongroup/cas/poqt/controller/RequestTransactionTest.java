package ph.com.guanzongroup.cas.poqt.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.err;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.json;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.model;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.ok;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.row;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.rows;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.iface.GValidator;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Master;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Supplier;
import ph.com.guanzongroup.cas.poqt.services.QuotationControllers;
import ph.com.guanzongroup.cas.poqt.services.QuotationModels;
import ph.com.guanzongroup.cas.poqt.status.POQuotationRequestStatus;
import ph.com.guanzongroup.cas.poqt.validator.QuotationValidators;

/** Procedures, lists, rows and saving of the quotation request. */
public class RequestTransactionTest extends RequestTestBase {

    /** QuotationModels giving fresh map models. */
    private static MockedConstruction<QuotationModels> models() {
        return mockConstruction(QuotationModels.class, (m, c) -> {
            when(m.POQuotationRequestMaster()).thenAnswer(i -> model(Model_PO_Quotation_Request_Master.class));
            when(m.POQuotationRequestDetails()).thenAnswer(i -> model(Model_PO_Quotation_Request_Detail.class));
            when(m.POQuotationRequestSupplier()).thenAnswer(i -> model(Model_PO_Quotation_Request_Supplier.class));
        });
    }

    private static MockedStatic<ShowDialogFX> browse(JSONObject foRow) {
        MockedStatic<ShowDialogFX> loDialog = mockStatic(ShowDialogFX.class);
        loDialog.when(() -> ShowDialogFX.Browse(any(GRiderCAS.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(foRow);
        return loDialog;
    }

    // =====================================================================
    // Procedures
    // =====================================================================
    @Test
    public void initTransaction() throws Exception {
        try (MockedConstruction<QuotationModels> loModels = models()) {
            assertEquals("success", req.InitTransaction().get("result"));
        }
        assertEquals("PQRq".length(), req.getSourceCode().length());
        assertTrue(req.calls.contains("initialize"));
        assertEquals(0, req.getDetailCount());
        assertEquals(0, req.getPOQuotationRequestSupplierCount());
    }

    @Test
    public void searchTransactionOpensPicked() throws Exception {
        req.setTransactionStatus("0");
        try (MockedConstruction<QuotationModels> loModels = models();
                MockedStatic<ShowDialogFX> loDialog = browse(json("sTransNox", "M00126000009"))) {
            assertEquals("success", req.searchTransaction().get("result"));
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class), contains("AND a.cTranStat = '0'"),
                    anyString(), anyString(), anyString(), anyString(), anyInt()));
        }
        assertTrue(req.calls.contains("open M00126000009"));
    }

    @Test
    public void searchTransactionWithFilters() throws Exception {
        req.setTransactionStatus("012");
        try (MockedConstruction<QuotationModels> loModels = models();
                MockedStatic<ShowDialogFX> loDialog = browse(json("sTransNox", "M00126000009"))) {
            req.searchTransaction("Main", "Proc", "Paper", "2026-11-26", "0009");
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class),
                    contains("c.sBranchNm LIKE '%Main' AND d.sDeptName LIKE '%Proc' AND f.sDescript LIKE '%Paper'"
                            + " AND a.dTransact = '2026-11-26' AND a.sTransNox LIKE '%0009'"),
                    anyString(), anyString(), anyString(), anyString(), anyInt()));
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class), contains("a.cTranStat IN ('0', '1', '2')"),
                    anyString(), anyString(), anyString(), anyString(), anyInt()));
        }
    }

    @Test
    public void searchTransactionNothingPicked() throws Exception {
        try (MockedStatic<ShowDialogFX> loDialog = browse(null)) {
            assertEquals("No record loaded.", req.searchTransaction().get("message"));
            assertEquals("No record loaded.", req.searchTransaction(null, "", null, "1900-01-01", "").get("message"));
        }
        req.setTransactionStatus("1");
        try (MockedStatic<ShowDialogFX> loDialog = browse(null)) {
            assertEquals("No record loaded.", req.searchTransaction("", "", "", null, null).get("message"));
        }
    }

    // =====================================================================
    // Lists
    // =====================================================================
    @Test
    public void loadRequestList() throws Exception {
        req.setTransactionStatus("12");
        when(gr.executeQuery(anyString())).thenReturn(rows(
                row("sTransNox", "M00126000001", "dTransact", java.sql.Date.valueOf("2026-11-26")),
                row("sTransNox", "M00126000002", "dTransact", java.sql.Date.valueOf("2026-11-25"))));
        try (MockedConstruction<QuotationModels> loModels = models()) {
            JSONObject loResult = req.loadPOQuotationRequestList("Main", "Proc", "Paper",
                    java.sql.Date.valueOf("2026-11-26"), "0001", true);
            assertEquals("success", loResult.get("result"));
        }
        assertEquals(2, req.getPOQuotationRequestCount());
        assertEquals(EditMode.READY, req.POQuotationRequestList(1).getEditMode());
        verify(gr).executeQuery(contains("NOT IN (SELECT q.sSourceNo FROM PO_Quotation_Master q"));
        verify(gr).executeQuery(contains("a.cTranStat IN ('1', '2')"));
    }

    @Test
    public void loadRequestListEmptyAndErrors() throws Exception {
        req.setTransactionStatus("1");
        when(gr.executeQuery(anyString())).thenReturn(rows());
        assertEquals("success", req.loadPOQuotationRequestList(null, null, null, null, null, false).get("result"));
        assertEquals(0, req.getPOQuotationRequestCount());

        when(gr.executeQuery(anyString())).thenThrow(new SQLException("Connection lost"));
        assertEquals("Connection lost", req.loadPOQuotationRequestList("", "", "", null, "", false).get("message"));

        //a request that cannot be opened
        GRiderCAS loOther = mock(GRiderCAS.class);
        when(loOther.executeQuery(anyString())).thenReturn(rows(row("sTransNox", "X", "dTransact", null)));
        TestRequest loReq = new TestRequest(loOther, master);
        try (MockedConstruction<QuotationModels> loModels = mockConstruction(QuotationModels.class, (m, c) -> {
            Model_PO_Quotation_Request_Master loBad = model(Model_PO_Quotation_Request_Master.class);
            org.mockito.Mockito.doThrow(GuanzonException.class).when(loBad).openRecord(anyString());
            when(m.POQuotationRequestMaster()).thenReturn(loBad);
        })) {
            assertEquals("error", loReq.loadPOQuotationRequestList("", "", "", null, "", false).get("result"));
        }
    }

    @Test
    public void loadSupplierList() throws Exception {
        when(gr.executeQuery(anyString())).thenReturn(rows(row("sTransNox", TRANS_NO), row("sTransNox", TRANS_NO)));
        QtKit.edit(master, EditMode.UPDATE);
        try (MockedConstruction<QuotationModels> loModels = models()) {
            req.loadPOQuotationRequestSupplierList();
        }
        assertEquals(2, req.getPOQuotationRequestSupplierCount());
        assertEquals(EditMode.UPDATE, req.POQuotationRequestSupplierList(1).getEditMode());
        assertEquals(2, req.POQuotationRequestSupplierList().size());
    }

    // =====================================================================
    // Rows
    // =====================================================================
    @Test
    public void addDetail() throws Exception {
        assertEquals("success", req.AddDetail().get("result"));
        assertEquals("Last row has empty item.", req.AddDetail().get("message"));
        req.Detail(0).setDescription("Pen");
        req.AddDetail();
        assertEquals(2, req.getDetailCount());
    }

    @Test
    public void reloadDetailRemovesEmptyRowsAndKeepsBrand() throws Exception {
        req.line("sStockIDx", "S1", "sDescript", "Pen");
        Model_PO_Quotation_Request_Detail loEmpty = req.line("sStockIDx", "", "sDescript", "", "sBrandIDx", "B1", "sModelIDx", "M1");
        QtKit.edit(loEmpty, EditMode.ADDNEW);
        req.ReloadDetail();
        assertEquals(2, req.getDetailCount()); //empty row removed, a new last row added
        assertEquals("B1", req.Detail(1).getBrandId());
        assertEquals("M1", req.Detail(1).getModelId());

        TestRequest loEmptyReq = new TestRequest(gr, master);
        loEmptyReq.ReloadDetail();
        assertEquals(1, loEmptyReq.getDetailCount());
    }

    @Test
    public void reloadSupplierOnlyWhileEditing() throws Exception {
        req.supplier("sSupplier", "", "sCompnyID", "");
        req.ReloadSupplier();
        assertEquals(1, req.getPOQuotationRequestSupplierCount());

        req.mode(EditMode.UPDATE);
        Model_PO_Quotation_Request_Supplier loSaved = req.POQuotationRequestSupplierList(0);
        QtKit.edit(loSaved, EditMode.UPDATE);
        Model_PO_Quotation_Request_Supplier loNew = req.supplier("sSupplier", "", "sCompnyID", "");
        req.supplier("sSupplier", "S2", "sCompnyID", "C2");
        try (MockedConstruction<QuotationModels> loModels = models()) {
            req.ReloadSupplier();
        }
        assertFalse(loSaved.isReverse());
        assertFalse(req.POQuotationRequestSupplierList().contains(loNew));
        assertEquals(3, req.getPOQuotationRequestSupplierCount()); //a new empty last row

        TestRequest loEmpty = new TestRequest(gr, master);
        loEmpty.mode(EditMode.ADDNEW);
        try (MockedConstruction<QuotationModels> loModels = models()) {
            loEmpty.ReloadSupplier();
        }
        assertEquals(1, loEmpty.getPOQuotationRequestSupplierCount());
    }

    @Test
    public void addSupplier() throws Exception {
        try (MockedConstruction<QuotationModels> loModels = models()) {
            assertEquals("success", req.AddPOQuotationRequestSupplier().get("result"));
            assertEquals("Unable to add supplier.", req.AddPOQuotationRequestSupplier().get("message"));
            req.POQuotationRequestSupplierList(0).setSupplierId("S1");
            req.AddPOQuotationRequestSupplier();
        }
        assertEquals(2, req.getPOQuotationRequestSupplierCount());
    }

    @Test
    public void removeDetails() {
        req.line("sStockIDx", "S1", "sDescript", "Pen");
        Model_PO_Quotation_Request_Detail loSaved = req.line("sStockIDx", "S2", "sDescript", "Pad");
        QtKit.edit(loSaved, EditMode.UPDATE);
        req.removeDetails();
        assertEquals(1, req.getDetailCount());
        assertEquals(1, req.removedCount());
        assertEquals("-", loSaved.getValue("cReversex"));
    }

    @Test
    public void removeWithInvDetailsKeepsFreeText() {
        req.line("sStockIDx", "", "sDescript", "Free text");
        req.line("sStockIDx", "S1", "sDescript", "Pen");
        Model_PO_Quotation_Request_Detail loSaved = req.line("sStockIDx", "S2", "sDescript", "Pad");
        QtKit.edit(loSaved, EditMode.UPDATE);
        req.removeWithInvDetails();
        assertEquals(2, req.getDetailCount());
        assertEquals("Free text", req.Detail(0).getDescription());
        assertEquals("-", loSaved.getValue("cReversex"));
    }

    @Test
    public void resetAndAccessors() throws Exception {
        req.setSearchBranch("x");
        req.setSearchCompany("C");
        req.setSearchIndustry("I");
        req.setSearchDestination("D");
        assertEquals("C", req.getSearchCompany());
        assertEquals("I", req.getSearchIndustry());
        assertEquals("D", req.getSearchDestination());
        req.supplier();
        req.resetOthers();
        assertEquals(0, req.getPOQuotationRequestSupplierCount());
        assertEquals("", req.getSearchBranch());
        try (MockedConstruction<QuotationModels> loModels = models()) {
            req.resetMaster();
        }
        assertFalse(master == req.Master());
        assertSame(req.poDetail, req.getDetail());
    }

    // =====================================================================
    // Saving
    // =====================================================================
    private void savableLines() {
        req.line("sStockIDx", "S1", "sDescript", "Pen", "nQuantity", 2.0);
    }

    @Test
    public void willSaveNewRequest() throws Exception {
        QtKit.edit(master, EditMode.ADDNEW);
        savableLines();
        req.line("sStockIDx", "", "sDescript", "", "nQuantity", 0.0); //empty new row: dropped
        Model_PO_Quotation_Request_Detail loZero = req.line("sStockIDx", "S3", "sDescript", "Pad", "nQuantity", 0.0);
        QtKit.edit(loZero, EditMode.UPDATE); //saved row with no quantity: excluded
        req.supplier("sSupplier", "SUP1", "sCompnyID", "C1", "sTermCode", "T30");
        req.supplier("sSupplier", "", "sCompnyID", "");
        assertEquals("success", req.willSave().get("result"));
        assertEquals("NEXT0001", master.getTransactionNo());
        assertEquals("U001", master.getPrepared());
        assertEquals("enc:U001", master.getModifyingId());
        assertEquals(2, req.getDetailCount());
        assertEquals(1, req.removedCount());
        assertEquals(1, req.getPOQuotationRequestSupplierCount());
        assertEquals("NEXT0001", req.POQuotationRequestSupplierList(0).getTransactionNo());
        assertEquals(1, req.Detail(0).getEntryNo());
    }

    @Test
    public void willSaveNeedsQuantity() throws Exception {
        req.line("sStockIDx", "S1", "sDescript", "Pen", "nQuantity", 0.0);
        assertEquals("No transaction quantity to be save.", req.willSave().get("message"));
    }

    @Test
    public void willSaveSupplierRules() throws Exception {
        savableLines();
        Model_PO_Quotation_Request_Supplier loRow = req.supplier("sSupplier", "SUP1", "sCompnyID", "C1", "sTermCode", "");
        assertEquals("Supplier term at row 1 cannot be empty.", req.willSave().get("message"));
        loRow.setTerm("T30");
        loRow.setCompanyId("");
        assertEquals("Supplier company at row 1 cannot be empty.", req.willSave().get("message"));
    }

    @Test
    public void willSaveNoDetailLeft() throws Exception {
        Model_PO_Quotation_Request_Detail loRow = req.line("sStockIDx", "S1", "sDescript", "Pen", "nQuantity", 1.0);
        loRow.isReverse(true);
        QtKit.edit(loRow, EditMode.ADDNEW);
        //quantity is read again from the values: make the row go after the first check
        Model_PO_Quotation_Request_Detail loEmpty = req.line("sStockIDx", "", "sDescript", "");
        loRow.setValue("nQuantity", 1.0);
        loEmpty.setValue("nQuantity", "");
        req.Detail().remove(loRow);
        req.Detail().add(0, loRow);
        assertEquals("success", req.willSave().get("result"));
    }

    @Test
    public void willSaveConfirmedNeedsAuthority() throws Exception {
        master.setTransactionStatus(POQuotationRequestStatus.CONFIRMED);
        savableLines();
        try (MockedConstruction<ActionAuthManager> loAuth = mockConstruction(ActionAuthManager.class,
                (m, c) -> when(m.isAuthorized()).thenReturn(json("result", "true")))) {
            assertEquals("success", req.willSave().get("result"));
        }
        try (MockedConstruction<ActionAuthManager> loAuth = mockConstruction(ActionAuthManager.class,
                (m, c) -> when(m.isAuthorized()).thenReturn(json("result", "false", "warning", "no rights")));
                MockedStatic<ShowDialogFX> loDialog = mockStatic(ShowDialogFX.class);
                MockedStatic<org.guanzon.appdriver.agent.ShowMessageFX> loMsg = mockStatic(org.guanzon.appdriver.agent.ShowMessageFX.class)) {
            loDialog.when(() -> ShowDialogFX.getUserApproval(any(GRiderCAS.class))).thenReturn(err("cancelled"));
            assertEquals("error", req.willSave().get("result"));
        }
    }

    /** The saved copy of a returned request, as willSave reopens it. */
    private POQuotationRequest savedCopy(String fsStock, double fnQty, String fsDescription) throws Exception {
        POQuotationRequest loCopy = mock(POQuotationRequest.class);
        Model_PO_Quotation_Request_Master loMaster = model(Model_PO_Quotation_Request_Master.class,
                "sReferNox", "REF1", "sCategCd2", "0101", "sDestinat", "M001", "sRemarksx", "");
        when(loCopy.Master()).thenReturn(loMaster);
        Model_PO_Quotation_Request_Detail loRow = model(Model_PO_Quotation_Request_Detail.class,
                "sStockIDx", fsStock, "nQuantity", fnQty, "sDescript", fsDescription);
        when(loCopy.getDetailCount()).thenReturn(1);
        when(loCopy.Detail(0)).thenReturn(loRow);
        return loCopy;
    }

    @Test
    public void returnedRequestNeedsAChange() throws Exception {
        master.setTransactionStatus(POQuotationRequestStatus.RETURNED);
        savableLines();
        req.line("sStockIDx", "", "sDescript", ""); //the empty last row
        POQuotationRequest loCopy = savedCopy("S1", 2.0, "Pen");
        try (MockedConstruction<QuotationControllers> loCtl = mockConstruction(QuotationControllers.class,
                (m, c) -> when(m.POQuotationRequest()).thenReturn(loCopy))) {
            assertEquals("No update has been made.", req.willSave().get("message"));
        }
        //a changed quantity: saving goes on (authority checked for a returned request)
        POQuotationRequest loChanged = savedCopy("S1", 5.0, "Pen");
        try (MockedConstruction<QuotationControllers> loCtl = mockConstruction(QuotationControllers.class,
                (m, c) -> when(m.POQuotationRequest()).thenReturn(loChanged));
                MockedConstruction<ActionAuthManager> loAuth = mockConstruction(ActionAuthManager.class,
                        (m, c) -> when(m.isAuthorized()).thenReturn(json("result", "true")))) {
            assertEquals("success", req.willSave().get("result"));
        }
    }

    @Test
    public void saveRunsValidator() throws Exception {
        GValidator loValidator = mock(GValidator.class);
        when(loValidator.validate()).thenReturn(err("Invalid destination."));
        try (MockedStatic<QuotationValidators> loFactory = mockStatic(QuotationValidators.class)) {
            loFactory.when(() -> QuotationValidators.quotationRequest("09")).thenReturn(loValidator);
            assertEquals("Invalid destination.", req.save().get("message"));
        }
        verify(loValidator).setTransactionStatus(POQuotationRequestStatus.OPEN);
        verify(loValidator).setMaster(master);
    }

    @Test
    public void saveOthersSavesChangedSuppliers() throws Exception {
        Model_PO_Quotation_Request_Supplier loNew = req.supplier("sSupplier", "S1");
        Model_PO_Quotation_Request_Supplier loSame = req.supplier("sSupplier", "S2");
        QtKit.edit(loSame, EditMode.READY);
        assertEquals("success", req.saveOthers().get("result"));
        verify(loNew).saveRecord();
        assertNotNull(loNew.getModifiedDate());

        loNew.setValue("__saveResult", err("Duplicate entry"));
        assertEquals("Duplicate entry", req.saveOthers().get("message"));

        when(gr.getServerDate()).thenThrow(new SQLException("Connection lost"));
        assertEquals("success", req.saveOthers().get("result")); //logged only, as before
    }

    @Test
    public void initFieldsDefaults() throws Exception {
        assertEquals("success", req.initFields().get("result"));
        assertEquals("M001", master.getBranchCode());
        assertEquals("09", master.getIndustryId());
        assertEquals("0001", master.getCategoryCode());
        assertEquals(POQuotationRequestStatus.OPEN, master.getTransactionStatus());
        assertEquals("033", master.getDepartmentId());

        when(gr.getServerDate()).thenThrow(new SQLException("Connection lost"));
        assertEquals("error", req.initFields().get("result"));
    }

    @Test
    public void initSql() {
        req.initSQL();
        assertTrue(req.browseSql().contains("FROM PO_Quotation_Request_Master a"));
    }

    @Test
    public void statusNames() {
        assertEquals("Open", req.getStatus("0"));
        assertEquals("Confirmed", req.getStatus("1"));
        assertEquals("Approved", req.getStatus("2"));
        assertEquals("Cancelled", req.getStatus("3"));
        assertEquals("Void", req.getStatus("4"));
        assertEquals("Posted", req.getStatus("5"));
        assertEquals("Returned", req.getStatus("9"));
        assertEquals("Unknown", req.getStatus("7"));
        assertEquals("Unknown", req.getStatus(""));
    }

    @Test
    public void unusedDates() {
        assertEquals("2026-11-26", QuotationTransaction.xsDateShort(java.sql.Date.valueOf("2026-11-26")));
        assertEquals(java.time.LocalDate.of(2026, 11, 26), QuotationTransaction.strToDate("2026-11-26"));
        assertNotNull(new Date());
    }
}
