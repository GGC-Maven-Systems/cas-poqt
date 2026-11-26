package ph.com.guanzongroup.cas.poqt.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
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
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.smart;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.base.WebFile;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.appdriver.token.RequestAccess;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Master;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Master;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Supplier;
import ph.com.guanzongroup.cas.poqt.services.QuotationControllers;
import ph.com.guanzongroup.cas.poqt.services.QuotationModels;
import ph.com.guanzongroup.cas.poqt.status.POQuotationStatus;
import ph.com.guanzongroup.cas.poqt.validator.QuotationValidators;

/** Lists, populating from a request, saving, uploads and history of the quotation. */
public class QuotationSaveTest extends QuotationTestBase {

    private Path poDir;
    private String psOldTemp;
    private String psOldToken;

    @Before
    public void setUpFolders() throws Exception {
        poDir = Files.createTempDirectory("qtsave");
        psOldTemp = System.getProperty("sys.default.path.temp.attachments");
        psOldToken = System.getProperty("sys.default.access.token");
        System.setProperty("sys.default.path.temp.attachments", poDir.toString());
        System.setProperty("sys.default.access.token", poDir.resolve("token.json").toString());
        resetToken();
    }

    @After
    public void restore() throws Exception {
        restore("sys.default.path.temp.attachments", psOldTemp);
        restore("sys.default.access.token", psOldToken);
        resetToken();
    }

    private static void restore(String fsKey, String fsValue) {
        if (fsValue == null) {
            System.clearProperty(fsKey);
        } else {
            System.setProperty(fsKey, fsValue);
        }
    }

    private static void resetToken() throws Exception {
        Field loField = POQuotation.class.getDeclaredField("token");
        loField.setAccessible(true);
        loField.set(null, null);
    }

    private void writeToken(Date fdCreated) throws Exception {
        String lsCreated = new SimpleDateFormat(SQLUtil.FORMAT_TIMESTAMP).format(fdCreated);
        Files.write(poDir.resolve("token.json"),
                ("{\"created\":\"" + lsCreated + "\",\"parent\":\"P\",\"access_key\":\"KEY\"}").getBytes());
    }

    // =====================================================================
    // Lists
    // =====================================================================
    private static MockedConstruction<QuotationModels> models() {
        return mockConstruction(QuotationModels.class, (m, c) -> {
            when(m.POQuotationMaster()).thenAnswer(i -> model(Model_PO_Quotation_Master.class));
            when(m.POQuotationRequestSupplier()).thenAnswer(i -> model(Model_PO_Quotation_Request_Supplier.class));
        });
    }

    @Test
    public void loadQuotationList() throws Exception {
        qt.setTransactionStatus("12");
        when(gr.executeQuery(anyString())).thenReturn(rows(
                row("sTransNox", "Q1", "dTransact", java.sql.Date.valueOf("2026-11-26"), "SpplierNm", "Acme"),
                row("sTransNox", "Q2", "dTransact", java.sql.Date.valueOf("2026-11-25"), "SpplierNm", "Beta")));
        try (MockedConstruction<QuotationModels> loModels = models()) {
            assertEquals("success", qt.loadPOQuotationList("Main", "Proc", "Acme", "Paper", "RQ", true).get("result"));
        }
        assertEquals(2, qt.getPOQuotationCount());
        verify(gr).executeQuery(contains("c.sBranchNm LIKE '%Main' AND h.sDeptName LIKE '%Proc' AND f.sDescript LIKE '%Paper'"
                + " AND i.sCompnyNm LIKE '%Acme' AND a.sSourceNo LIKE '%RQ'"));
        verify(gr).executeQuery(contains("PO_Detail.sSourceCd = 'PQtn'"));
        verify(gr).executeQuery(contains("a.cTranStat IN ('1', '2') ORDER BY a.dTransact DESC"));

        qt.setTransactionStatus("1");
        when(gr.executeQuery(anyString())).thenReturn(rows());
        assertEquals("success", qt.loadPOQuotationList(null, "", null, "", null, false).get("result"));
        assertEquals(0, qt.getPOQuotationCount());
    }

    @Test
    public void loadQuotationListErrors() throws Exception {
        qt.computeDiscount(0); //poJSON is set before a list is loaded
        when(gr.executeQuery(anyString())).thenThrow(new SQLException("offline"));
        assertEquals("offline", qt.loadPOQuotationList("", "", "", "", "", false).get("message"));

        org.mockito.Mockito.doReturn(rows(row("sTransNox", "Q1", "dTransact", null, "SpplierNm", "A"))).when(gr).executeQuery(anyString());
        try (MockedConstruction<QuotationModels> loModels = mockConstruction(QuotationModels.class, (m, c) -> {
            Model_PO_Quotation_Master loMaster = mock(Model_PO_Quotation_Master.class);
            doThrow(new GuanzonException(1, "bad")).when(loMaster).openRecord(anyString());
            when(m.POQuotationMaster()).thenReturn(loMaster);
        })) {
            assertEquals("error", qt.loadPOQuotationList("", "", "", "", "", false).get("result"));
        }
    }

    @Test
    public void loadRequestSupplierList() throws Exception {
        when(gr.executeQuery(anyString())).thenReturn(rows(
                row("sTransNox", "RQ1", "dTransact", java.sql.Date.valueOf("2026-11-26"), "SpplierNm", "Acme",
                        "sSupplier", "SUP1", "sCompnyID", "C1")));
        try (MockedConstruction<QuotationModels> loModels = models()) {
            assertEquals("success", qt.loadPOQuotationRequestSupplierList("Gu", "Main", "Proc", "Acme", "Paper").get("result"));
            assertEquals(1, qt.getPOQuotationRequestSupplierCount());
            verify(gr).executeQuery(contains("i.sCompnyNm LIKE '%Gu' AND d.sBranchNm LIKE '%Main' AND e.sDeptName LIKE '%Proc'"
                    + " AND h.sCompnyNm LIKE '%Acme' AND g.sDescript LIKE '%Paper'"));
            when(gr.executeQuery(anyString())).thenReturn(rows());
            qt.loadPOQuotationRequestSupplierList(null, null, null, null, null);
            assertEquals(0, qt.getPOQuotationRequestSupplierCount());
        }
    }

    @Test
    public void loadRequestSupplierListErrors() throws Exception {
        qt.computeDiscount(0); //poJSON is set before a list is loaded
        when(gr.executeQuery(anyString())).thenThrow(new SQLException("offline"));
        assertEquals("offline", qt.loadPOQuotationRequestSupplierList("", "", "", "", "").get("message"));

        org.mockito.Mockito.doReturn(rows(row("sTransNox", "RQ1", "dTransact", null, "SpplierNm", "A",
                "sSupplier", "S", "sCompnyID", "C"))).when(gr).executeQuery(anyString());
        try (MockedConstruction<QuotationModels> loModels = mockConstruction(QuotationModels.class, (m, c) -> {
            Model_PO_Quotation_Request_Supplier loRow = mock(Model_PO_Quotation_Request_Supplier.class);
            doThrow(new GuanzonException(1, "bad")).when(loRow).openRecord(anyString(), anyString(), anyString());
            when(m.POQuotationRequestSupplier()).thenReturn(loRow);
        })) {
            assertEquals("error", qt.loadPOQuotationRequestSupplierList("", "", "", "", "").get("result"));
        }
    }

    // =====================================================================
    // Populate from an approved request
    // =====================================================================
    private POQuotationRequest request(JSONObject foOpen) throws Exception {
        POQuotationRequest loRequest = smart(POQuotationRequest.class);
        when(loRequest.OpenTransaction(anyString())).thenReturn(foOpen);
        when(loRequest.getSourceCode()).thenReturn("PQRq");
        when(loRequest.Master()).thenReturn(model(Model_PO_Quotation_Request_Master.class, "sBranchCd", "B2"));
        when(loRequest.getDetailCount()).thenReturn(2);
        Model_PO_Quotation_Request_Detail loIncluded = model(Model_PO_Quotation_Request_Detail.class, "sStockIDx", "S1", "sDescript", "Pen");
        Model_PO_Quotation_Request_Detail loExcluded = model(Model_PO_Quotation_Request_Detail.class, "sStockIDx", "S2", "sDescript", "Ink", "cReversex", "-");
        when(loRequest.Detail(0)).thenReturn(loIncluded);
        when(loRequest.Detail(1)).thenReturn(loExcluded);
        return loRequest;
    }

    @Test
    public void populateFromRequest() throws Exception {
        qt.line();
        qt.requestSupplier("sTransNox", "RQ1", "sSupplier", "SUP1", "sCompnyID", "C1", "sAddrssID", "AD1",
                "sContctID", "CT1", "sTermCode", "T30");
        when(gr.executeQuery(anyString())).thenReturn(rows());
        POQuotationRequest loRequest = request(ok());
        try (MockedConstruction<QuotationControllers> loCtl = mockConstruction(QuotationControllers.class,
                (m, c) -> when(m.POQuotationRequest()).thenReturn(loRequest))) {
            //AddDetail leaves an empty result behind (as in cas-purchasing)
            assertFalse("error".equals(qt.populatePOQuotation(0).get("result")));
        }
        assertEquals("SUP1", master.getSupplierId());
        assertEquals("RQ1", master.getSourceNo());
        assertEquals("PQRq", master.getSourceCode());
        assertEquals("B2", master.getBranchCode());
        assertEquals("T30", master.getTerm());
        assertEquals("Pen", qt.Detail(0).getDescription());
        assertEquals(2, qt.getDetailCount());
    }

    @Test
    public void populateRefusals() throws Exception {
        qt.line("sStockIDx", "S9", "sDescript", "Other");
        qt.requestSupplier("sTransNox", "RQ1", "sSupplier", "SUP1", "sCompnyID", "C1");
        assertEquals("PO Quotation detail cannot be mixed with non quotation request.", qt.populatePOQuotation(0).get("message"));

        master.setSourceNo("RQ0");
        when(gr.executeQuery(anyString())).thenReturn(rows(row("sTransNox", "Q7")));
        assertTrue(((String) qt.populatePOQuotation(0).get("message")).contains("<Q7>"));

        when(gr.executeQuery(anyString())).thenReturn(rows());
        POQuotationRequest loRequest = request(err("Not found"));
        try (MockedConstruction<QuotationControllers> loCtl = mockConstruction(QuotationControllers.class,
                (m, c) -> when(m.POQuotationRequest()).thenReturn(loRequest))) {
            assertEquals("Not found", qt.populatePOQuotation(0).get("message"));
            when(loRequest.OpenTransaction(anyString())).thenThrow(new GuanzonException(1, "broken"));
            assertEquals("error", qt.populatePOQuotation(0).get("result"));
        }

        when(gr.executeQuery(anyString())).thenThrow(new SQLException("offline"));
        assertEquals("offline", qt.populatePOQuotation(0).get("message"));
    }

    // =====================================================================
    // willSave
    // =====================================================================
    private Model_PO_Quotation_Detail savable() {
        return qt.line("sStockIDx", "S1", "sDescript", "Pen", "nQuantity", 2.0, "nUnitPrce", 10.0);
    }

    @Test
    public void willSaveNewRecord() throws Exception {
        QtKit.edit(master, EditMode.ADDNEW);
        master.setValue("sReferNox", null);
        savable();
        qt.line(); //blank new row is dropped
        assertEquals("success", qt.willSave().get("result"));
        assertEquals("NEXT0001", master.getTransactionNo());
        assertEquals("enc:U001", master.getPrepared());
        assertEquals("", master.getReferenceNo());
        assertEquals(1, qt.getDetailCount());
        assertEquals("NEXT0001", qt.Detail(0).getTransactionNo());
        assertEquals(20.0, master.getGrossAmount(), 0.0001);
    }

    @Test
    public void willSaveExistingQuotationForRequest() throws Exception {
        master.setSourceNo("RQ1");
        savable();
        when(gr.executeQuery(anyString())).thenReturn(rows(row("sTransNox", "Q7")));
        assertTrue(((String) qt.willSave().get("message")).contains("<Q7>"));
    }

    @Test
    public void willSaveNeedsQuantity() throws Exception {
        qt.line("sDescript", "Pen");
        assertEquals("No transaction quantity to be save.", qt.willSave().get("message"));

        qt.Detail().clear();
        qt.line("sDescript", "", "nQuantity", 1.0);
        assertEquals("No transaction detail to be save.", qt.willSave().get("message"));
    }

    @Test
    public void willSaveRowRules() throws Exception {
        savable();
        Model_PO_Quotation_Detail loSaved = qt.line("sDescript", "Ink");
        QtKit.edit(loSaved, EditMode.READY);
        Model_PO_Quotation_Detail loSingle = qt.line("sDescript", "Marker");
        Model_PO_Quotation_Detail loDouble = qt.line("sDescript", "Pen");
        assertEquals("success", qt.willSave().get("result"));
        assertFalse(loSaved.isReverse());
        assertFalse(loSingle.isReverse());
        assertFalse(qt.Detail().contains(loDouble));

        Model_PO_Quotation_Detail loBlankSaved = qt.line("sDescript", "");
        QtKit.edit(loBlankSaved, EditMode.READY);
        assertTrue(((String) qt.willSave().get("message")).startsWith("Request description cannot be empty."));
    }

    @Test
    public void willSaveReplacementRule() throws Exception {
        master.setSourceNo("RQ1");
        when(gr.executeQuery(anyString())).thenReturn(rows());
        qt.line("sStockIDx", "S1", "sDescript", "Pen", "sReplacID", "S2", "sReplacDs", "Ballpen", "nQuantity", 1.0);
        qt.line("sStockIDx", "S1", "sDescript", "Pen", "nQuantity", 1.0);
        assertTrue(((String) qt.willSave().get("message")).contains("Replacement Description for row 2 must not be empty."));
    }

    @Test
    public void willSaveConfirmedNeedsApprover() throws Exception {
        master.setTransactionStatus(POQuotationStatus.CONFIRMED);
        when(gr.getUserLevel()).thenReturn(UserRight.ENCODER);
        savable();
        try (MockedStatic<ShowDialogFX> loDialog = mockStatic(ShowDialogFX.class)) {
            loDialog.when(() -> ShowDialogFX.getUserApproval(any(GRiderCAS.class))).thenReturn(err("Cancelled"));
            assertEquals("Cancelled", qt.willSave().get("message"));

            loDialog.when(() -> ShowDialogFX.getUserApproval(any(GRiderCAS.class)))
                    .thenReturn(json("result", "success", "sUserIDxx", "U2", "nUserLevl", String.valueOf(UserRight.ENCODER)));
            assertEquals("User is not an authorized approving officer.", qt.willSave().get("message"));

            loDialog.when(() -> ShowDialogFX.getUserApproval(any(GRiderCAS.class)))
                    .thenReturn(json("result", "success", "sUserIDxx", "U2", "nUserLevl", String.valueOf(UserRight.SUPERVISOR)));
            assertEquals("success", qt.willSave().get("result"));
        }
    }

    /** A copy of the saved record, as the returned-quotation check opens it. */
    private POQuotation savedCopy(double fnQuantity) throws Exception {
        POQuotation loRecord = smart(POQuotation.class);
        Model_PO_Quotation_Master loMaster = model(Model_PO_Quotation_Master.class, "sReferNox", "", "sSupplier", "",
                "sTermCode", "", "sRemarksx", "", "cVATAdded", "0");
        when(loRecord.Master()).thenReturn(loMaster);
        when(loRecord.getDetailCount()).thenReturn(1);
        Model_PO_Quotation_Detail loDetail = model(Model_PO_Quotation_Detail.class, "sStockIDx", "S1", "sDescript", "Pen",
                "sReplacID", "", "sReplacDs", "", "nQuantity", fnQuantity);
        when(loRecord.Detail(0)).thenReturn(loDetail);
        return loRecord;
    }

    @Test
    public void willSaveReturnedNeedsChanges() throws Exception {
        master.setTransactionStatus(POQuotationStatus.RETURNED);
        savable();
        qt.line();
        POQuotation loSame = savedCopy(2.0);
        try (MockedConstruction<QuotationControllers> loCtl = mockConstruction(QuotationControllers.class,
                (m, c) -> when(m.POQuotation()).thenReturn(loSame))) {
            assertEquals("No update has been made.", qt.willSave().get("message"));
        }
        POQuotation loChanged = savedCopy(5.0);
        try (MockedConstruction<QuotationControllers> loCtl = mockConstruction(QuotationControllers.class,
                (m, c) -> when(m.POQuotation()).thenReturn(loChanged))) {
            assertEquals("success", qt.willSave().get("result"));
        }
    }

    @Test
    public void willSaveRenamesAndUploadsAttachments() throws Exception {
        savable();
        Files.write(poDir.resolve("a.png"), "image".getBytes());
        writeToken(new Date());
        TransactionAttachment loAttachment = qt.attachment(EditMode.ADDNEW, "sFileName", "a.png", "cSendStat", "0",
                "sMD5Hashx", "");
        //a.png is taken in the database, a_1.png is free
        when(gr.executeQuery(anyString())).thenReturn(rows(row("sFileName", "a.png")), rows());
        try (MockedConstruction<SysTableContollers> loTables = QuotationRowsTest.attachments();
                MockedStatic<WebFile> loWeb = mockStatic(WebFile.class)) {
            loWeb.when(() -> WebFile.UploadFile(any(), anyString(), anyString(), any(), any(), any(), any(), any(), any(), anyString()))
                    .thenReturn(ok());
            assertEquals("success", qt.willSave().get("result"));
            loWeb.verify(() -> WebFile.UploadFile(org.mockito.ArgumentMatchers.eq("KEY"), anyString(), anyString(),
                    org.mockito.ArgumentMatchers.eq("a_1.png"), any(), any(), any(), any(), any(), anyString()));
        }
        assertEquals("a_1.png", loAttachment.getModel().getFileName());
        assertTrue(Files.exists(poDir.resolve("a_1.png")));
        assertEquals("1", loAttachment.getModel().getSendStatus());
        assertEquals(TRANS_NO, loAttachment.getModel().getSourceNo());
        assertNotNull(loAttachment.getModel().getMD5Hash());
    }

    @Test
    public void willSaveStopsOnUploadError() throws Exception {
        savable();
        Files.write(poDir.resolve("b"), "doc".getBytes());
        writeToken(new Date());
        qt.attachment(EditMode.READY, "sFileName", "b", "cSendStat", "0", "sMD5Hashx", "h");
        try (MockedStatic<WebFile> loWeb = mockStatic(WebFile.class)) {
            loWeb.when(() -> WebFile.UploadFile(any(), anyString(), anyString(), any(), any(), any(), any(), any(), any(), anyString()))
                    .thenReturn(err("down"));
            assertTrue(((String) qt.willSave().get("message")).startsWith("System error while uploading file b."));
        }
    }

    @Test
    public void willSaveCopiesNameWithoutExtension() throws Exception {
        savable();
        Files.write(poDir.resolve("readme"), "x".getBytes());
        TransactionAttachment loAttachment = qt.attachment(EditMode.ADDNEW, "sFileName", "readme", "cSendStat", "1");
        when(gr.executeQuery(anyString())).thenReturn(rows(row("sFileName", "readme")), rows());
        try (MockedConstruction<SysTableContollers> loTables = QuotationRowsTest.attachments()) {
            assertEquals("success", qt.willSave().get("result"));
        }
        assertEquals("readme_1", loAttachment.getModel().getFileName());
    }

    // =====================================================================
    // Uploads
    // =====================================================================
    @Test
    public void uploadMissingFile() throws Exception {
        qt.attachment(EditMode.ADDNEW, "sFileName", "none.png", "sImagePth", poDir.toString());
        assertTrue(((String) qt.uploadCASAttachments(gr, "x", 0, "none.png").get("message")).startsWith("Cannot locate file"));
    }

    @Test
    public void uploadUsesOriginalNameAndRefreshesToken() throws Exception {
        Files.write(poDir.resolve("orig.png"), "image".getBytes());
        writeToken(new Date(System.currentTimeMillis() - 60 * 60 * 1000L));
        TransactionAttachment loAttachment = qt.attachment(EditMode.ADDNEW, "sFileName", "new.png", "sImagePth", poDir.toString(),
                "sMD5Hashx", null);
        try (MockedStatic<WebFile> loWeb = mockStatic(WebFile.class);
                MockedStatic<RequestAccess> loAccess = mockStatic(RequestAccess.class)) {
            loWeb.when(() -> WebFile.UploadFile(any(), anyString(), anyString(), any(), any(), any(), any(), any(), any(), anyString()))
                    .thenReturn(ok());
            assertEquals("success", qt.uploadCASAttachments(gr, poDir.resolve("token.json").toString(), 0, "orig.png").get("result"));
            loAccess.verify(() -> RequestAccess.main(any(String[].class)));
        }
        assertEquals("1", loAttachment.getModel().getSendStatus());
    }

    @Test
    public void uploadWithoutTokenFile() throws Exception {
        Files.write(poDir.resolve("c.png"), "image".getBytes());
        qt.attachment(EditMode.ADDNEW, "sFileName", "c.png", "sImagePth", poDir.toString(), "sMD5Hashx", "h");
        Files.write(poDir.resolve("bad.json"), "{not json".getBytes());
        try (MockedStatic<WebFile> loWeb = mockStatic(WebFile.class)) {
            loWeb.when(() -> WebFile.UploadFile(any(), anyString(), anyString(), any(), any(), any(), any(), any(), any(), anyString()))
                    .thenReturn(ok());
            assertEquals("success", qt.uploadCASAttachments(gr, poDir.resolve("missing.json").toString(), 0, "c.png").get("result"));
            assertEquals("success", qt.uploadCASAttachments(gr, poDir.resolve("bad.json").toString(), 0, "c.png").get("result"));
            loWeb.verify(() -> WebFile.UploadFile(org.mockito.ArgumentMatchers.isNull(), anyString(), anyString(), any(), any(), any(), any(), any(), any(), anyString()),
                    org.mockito.Mockito.times(2));
        }
    }

    // =====================================================================
    // Save callbacks
    // =====================================================================
    @Test
    public void saveRunsTheValidator() {
        GValidator loValidator = mock(GValidator.class);
        when(loValidator.validate()).thenReturn(err("Supplier is not set."));
        try (MockedStatic<QuotationValidators> loValidators = mockStatic(QuotationValidators.class)) {
            loValidators.when(() -> QuotationValidators.quotation("09")).thenReturn(loValidator);
            assertEquals("Supplier is not set.", qt.save().get("message"));
        }
        verify(loValidator).setTransactionStatus(POQuotationStatus.OPEN);
        verify(loValidator).setMaster(master);
    }

    @Test
    public void saveOthersSavesChangedAttachments() throws Exception {
        TransactionAttachment loNew = qt.attachment(EditMode.ADDNEW, "sFileName", "a.png");
        TransactionAttachment loSame = qt.attachment(EditMode.READY, "sFileName", "b.png");
        assertEquals("success", qt.saveOthers().get("result"));
        verify(loNew).setWithParentClass(true);
        verify(loNew).saveRecord();
        verify(loSame, org.mockito.Mockito.never()).saveRecord();
        assertEquals("enc:U001", loNew.getModel().getModifyingId());

        when(loNew.saveRecord()).thenReturn(err("Duplicate"));
        assertEquals("Duplicate", qt.saveOthers().get("message"));

        doThrow(new SQLException("offline")).when(loNew).saveRecord();
        assertEquals("success", qt.saveOthers().get("result"));
        doThrow(new IllegalStateException("odd")).when(loNew).saveRecord();
        assertEquals("success", qt.saveOthers().get("result"));
    }

    @Test
    public void initFieldsDefaults() throws Exception {
        assertEquals("success", qt.initFields().get("result"));
        assertEquals("09", master.getIndustryId());
        assertEquals("C001", master.getCompanyId());
        assertEquals(POQuotationStatus.OPEN, master.getTransactionStatus());
        assertEquals("", master.getReferenceNo());
        assertNotNull(master.getValidityDate());

        when(gr.getServerDate()).thenThrow(new SQLException("Connection lost"));
        assertEquals("error", qt.initFields().get("result"));
    }

    // =====================================================================
    // History
    // =====================================================================
    @Test
    public void statusHistoryNames() throws Exception {
        CachedRowSet loRows = RowSetProvider.newFactory().createCachedRowSet();
        RowSetMetaDataImpl loMeta = new RowSetMetaDataImpl();
        loMeta.setColumnCount(1);
        loMeta.setColumnName(1, "cRefrStat");
        loMeta.setColumnLabel(1, "cRefrStat");
        loMeta.setColumnType(1, Types.VARCHAR);
        loRows.setMetaData(loMeta);
        String[] laCodes = {"", "0", "1", "2", "3", "4", "5", "6", "9", "A", "B", "C", "D", "E", "F", "G", "I"};
        for (String lsCode : laCodes) {
            loRows.moveToInsertRow();
            loRows.updateString(1, lsCode);
            loRows.insertRow();
            loRows.moveToCurrentRow();
        }
        qt.statusHistory = loRows;
        when(gr.executeQuery(anyString())).thenAnswer(i -> {
            String lsSQL = i.getArgument(0);
            if (lsSQL.contains("xxxSysUser")) {
                return rows(row("sCompnyNm", "Juan Dela Cruz"));
            }
            return rows(row("sModified", "U003", "dModified", LocalDateTime.of(2026, 11, 25, 8, 0)));
        });
        qt.ShowStatusHistory();
        assertEquals("Purchase Order Quotation", qt.shownHistory[0]);
        assertEquals(TRANS_NO, qt.shownHistory[1]);
        assertEquals("Juan Dela Cruz", qt.shownHistory[2]);
        assertEquals("11-25-2026 08:00:00", qt.shownHistory[3]);
        loRows.beforeFirst();
        int lnNamed = 0;
        while (loRows.next()) {
            if (loRows.getString(1).length() > 1 || "-".equals(loRows.getString(1))) {
                lnNamed++;
            }
        }
        assertEquals(14, lnNamed); //"6", "F" and "G" are no status
    }

    @Test
    public void entryBy() throws Exception {
        when(gr.executeQuery(anyString())).thenAnswer(i -> {
            String lsSQL = i.getArgument(0);
            if (lsSQL.contains("xxxSysUser")) {
                return rows(row("sCompnyNm", "Ana"));
            }
            return rows(row("sModified", "enc:U0000000009", "dModified", LocalDateTime.of(2026, 11, 24, 7, 0)));
        });
        JSONObject loEntry = qt.getEntryBy();
        assertEquals("Ana", loEntry.get("sCompnyNm"));
        assertEquals("11-24-2026 07:00:00", loEntry.get("sEntryDte"));

        when(gr.executeQuery(anyString())).thenReturn(rows(row("sModified", null, "dModified", null)));
        assertEquals("", qt.getEntryBy().get("sCompnyNm"));

        java.sql.ResultSet loBroken = mock(java.sql.ResultSet.class);
        when(loBroken.last()).thenReturn(true);
        when(loBroken.getRow()).thenReturn(1);
        when(loBroken.next()).thenReturn(true, false).thenThrow(new SQLException("lost"));
        when(gr.executeQuery(anyString())).thenReturn(loBroken);
        assertEquals("error", qt.getEntryBy().get("result"));
    }
}
