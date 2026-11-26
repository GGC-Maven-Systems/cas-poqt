package ph.com.guanzongroup.cas.poqt.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.model;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.row;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.rows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.MockedConstruction;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Master;
import ph.com.guanzongroup.cas.poqt.services.QuotationModels;

/** Row checks, computations and attachments of the quotation. */
public class QuotationRowsTest extends QuotationTestBase {

    /** SysTableContollers whose TransactionAttachment() gives new ADDNEW attachments. */
    static MockedConstruction<SysTableContollers> attachments() {
        return mockConstruction(SysTableContollers.class, (m, c) ->
                when(m.TransactionAttachment()).thenAnswer(i -> TestQuotation.attachmentMock(EditMode.ADDNEW,
                        "sTransNox", "", "sFileName", "", "cRecdStat", RecordStatus.ACTIVE)));
    }

    // =====================================================================
    // Row checks
    // =====================================================================
    @Test
    public void reverseItemRemovesDuplicateOrExcludes() {
        qt.line("sDescript", "Pen");
        qt.line("sDescript", "Pen");
        qt.ReverseItem(1);
        assertEquals(1, qt.getDetailCount());
        qt.ReverseItem(0);
        assertFalse(qt.Detail(0).isReverse());
    }

    @Test
    public void removedExcessRequestItem() {
        Model_PO_Quotation_Detail loRow = qt.line("sDescript", "Pen", "sReplacDs", "Ballpen");
        qt.line("sDescript", "Pen");
        assertEquals("success", qt.RemovedExcessRequestItem(0).get("result"));
        assertEquals(1, qt.getDetailCount());

        //a saved row without replacement keeps the updated row from saving
        Model_PO_Quotation_Detail loSaved = qt.line("sDescript", "Pen", "sReplacDs", null);
        QtKit.edit(loSaved, EditMode.READY);
        QtKit.edit(loRow, EditMode.UPDATE);
        assertEquals("Replace Description cannot be empty.", qt.RemovedExcessRequestItem(0).get("message"));
        QtKit.edit(loRow, EditMode.READY);
        assertEquals("success", qt.RemovedExcessRequestItem(0).get("result"));
    }

    @Test
    public void requestMultipleItem() {
        qt.line("sDescript", "Pen");
        assertFalse(qt.RequestMultipleItem("Pen"));
        qt.line("sDescript", "Pen");
        assertTrue(qt.RequestMultipleItem("Pen"));
    }

    @Test
    public void checkRequestItem() {
        qt.line("sDescript", "Pen");
        qt.line();
        JSONObject loResult = qt.checkRequestItem(1, "Pen");
        assertEquals(true, loResult.get("reverse"));
        assertEquals(0, loResult.get("row"));

        qt.Detail(0).isReverse(false);
        assertEquals(false, qt.checkRequestItem(1, "Pen").get("reverse"));

        qt.Detail(0).setReplaceId("S2");
        assertEquals(false, qt.checkRequestItem(1, "Pen").get("reverse"));
        qt.Detail(0).isReverse(true);
        assertEquals("success", qt.checkRequestItem(1, "Pen").get("result"));
        assertEquals("success", qt.checkRequestItem(1, null).get("result"));
    }

    @Test
    public void checkExistingDetail() {
        qt.line("sStockIDx", "S1", "sDescript", "Pen");
        qt.line();
        JSONObject loResult = qt.checkExistingDetail(1, "S1", "Pen");
        assertEquals(true, loResult.get("reverse"));
        qt.Detail(0).isReverse(false);
        assertEquals(false, qt.checkExistingDetail(1, "X", "Pen").get("reverse"));
        assertEquals("success", qt.checkExistingDetail(1, "X", "Other").get("result"));
        assertEquals("success", qt.checkExistingDetail(1, null, null).get("result"));
    }

    @Test
    public void checkExistingReverse() {
        qt.line("sDescript", "Pen");
        qt.line("sDescript", "Pen").isReverse(false);
        assertEquals("success", qt.checkExistingReverse(0).get("result"));
    }

    // =====================================================================
    // Computations
    // =====================================================================
    @Test
    public void computeFieldsVatExclusive() throws Exception {
        qt.line("sDescript", "Pen", "nUnitPrce", 100.0, "nQuantity", 2.0, "nDiscRate", 10.0, "nDiscAmtx", 5.0);
        qt.line("sDescript", "Ink", "nUnitPrce", 50.0, "nQuantity", 1.0).isReverse(false);
        master.setValue("nFreightx", 10.0);
        master.setValue("nDiscount", 10.0);
        master.setValue("nAddDiscx", 5.0);
        qt.computeFields();
        //(100 - (10 + 5)) * 2 = 170
        assertEquals(170.0, master.getGrossAmount(), 0.0001);
        //170 + 10 - (5 + 17) = 158; VAT 12%
        assertEquals(158.0, master.getTransactionTotal(), 0.0001);
        assertEquals(18.96, master.getVatAmount(), 0.0001);
        assertEquals(12.0, master.getVatRate(), 0.0001);
    }

    @Test
    public void computeFieldsVatable() throws Exception {
        qt.line("sDescript", "Pen", "nUnitPrce", 100.0, "nQuantity", 1.0);
        master.isVatable(true);
        master.setValue("nVATRatex", 12.0);
        qt.computeFields();
        assertEquals(112.0, master.getTransactionTotal(), 0.0001);

        master.setValue("nVATRatex", 0.0);
        QtKit.edit(master, EditMode.UNKNOWN);
        qt.computeFields();
        assertEquals(0.0, master.getVatRate(), 0.0001);
    }

    @Test
    public void costAndDiscount() {
        qt.line("nUnitPrce", 100.0, "nQuantity", 3.0, "nDiscRate", 10.0, "nDiscAmtx", 5.0);
        qt.line("nUnitPrce", 40.0, "nQuantity", 1.0);
        assertEquals(15.0, qt.getDiscount(0), 0.0001);
        assertEquals(255.0, qt.getCost(0), 0.0001);
        assertEquals(40.0, qt.getCost(1), 0.0001);
    }

    @Test
    public void computeCost() {
        qt.line("nUnitPrce", 100.0, "nQuantity", 1.0);
        qt.line("nUnitPrce", 0.0, "nQuantity", 1.0);
        assertEquals("success", qt.computeCost(0, 10, 5).get("result"));
        assertEquals("Invalid discount rate.", qt.computeCost(1, 10, 0).get("message"));
        assertEquals("Invalid discount.", qt.computeCost(0, 50, 60).get("message"));
        qt.Detail(0).setQuantity(-1.0);
        assertEquals("Invalid computed cost amount.", qt.computeCost(0, 0, 0).get("message"));
    }

    @Test
    public void computeDiscount() {
        qt.line("nUnitPrce", 100.0, "nQuantity", 2.0);
        assertEquals("success", qt.computeDiscount(50).get("result"));
        assertEquals("error", qt.computeDiscount(-1).get("result"));
        assertEquals(0.0, master.getAdditionalDiscountAmount(), 0.0001);
        assertEquals("error", qt.computeDiscount(500).get("result"));
        master.setValue("nDiscount", 90.0);
        assertEquals("Invalid transaction net total.", qt.computeDiscount(100).get("message"));
    }

    @Test
    public void computeDiscountRate() {
        qt.line("nUnitPrce", 100.0, "nQuantity", 2.0);
        assertEquals("success", qt.computeDiscountRate(10).get("result"));
        assertEquals("error", qt.computeDiscountRate(101).get("result"));
        assertEquals(0.0, master.getDiscountRate(), 0.0001);
        assertEquals("error", qt.computeDiscountRate(-1).get("result"));
        master.setValue("nAddDiscx", 190.0);
        assertEquals("Invalid transaction net total.", qt.computeDiscountRate(10).get("message"));
    }

    // =====================================================================
    // Detail rows
    // =====================================================================
    @Test
    public void addDetailRefusesEmptyLastRow() throws Exception {
        assertEquals("success", qt.AddDetail().get("result"));
        assertEquals("Last row has empty item.", qt.AddDetail().get("message"));
        qt.Detail(0).setDescription("Pen");
        assertEquals("success", qt.AddDetail().get("result"));
        assertEquals(2, qt.getDetailCount());
    }

    @Test
    public void reloadDetailDropsBlankNewRowsAndAddsOne() throws Exception {
        qt.line("sDescript", "Pen");
        qt.line();
        Model_PO_Quotation_Detail loSaved = qt.line();
        QtKit.edit(loSaved, EditMode.READY);
        qt.ReloadDetail();
        assertTrue(qt.calls.contains("deleteDetail 1"));
        assertEquals(2, qt.getDetailCount());

        qt.Detail().clear();
        qt.ReloadDetail();
        assertEquals(1, qt.getDetailCount());

        qt.Detail(0).setReplaceDescription("Ballpen");
        qt.ReloadDetail();
        assertEquals(2, qt.getDetailCount());
    }

    @Test
    public void removeDetailsKeepsSavedRows() {
        Model_PO_Quotation_Detail loSaved = qt.line("sDescript", "Pen");
        QtKit.edit(loSaved, EditMode.UPDATE);
        qt.line("sDescript", "Ink");
        assertEquals("success", qt.removeDetails().get("result"));
        assertEquals(0, qt.getDetailCount());
        assertEquals(1, qt.removedCount());
    }

    @Test
    public void resetTransactionClearsHeader() throws Exception {
        master.setSupplierId("SUP1");
        master.setSourceNo("RQ1");
        qt.line("sDescript", "Pen");
        qt.setSearchBranch("Cebu");
        qt.resetTransaction();
        assertEquals("", master.getSupplierId());
        assertEquals("", master.getSourceNo());
        assertEquals("", master.getBranchCode());
        assertEquals("", qt.getSearchBranch());
        assertEquals(1, qt.getDetailCount());
    }

    @Test
    public void listsAndGetters() {
        try (MockedConstruction<QuotationModels> loModels = mockConstruction(QuotationModels.class, (m, c) ->
                when(m.POQuotationMaster()).thenAnswer(i -> model(Model_PO_Quotation_Master.class)))) {
            qt.resetMaster();
            assertTrue(qt.Master() != master);
        }
        qt.paMasterList.add(master);
        assertSame(master, qt.POQuotationList(0));
        assertEquals(1, qt.POQuotationList().size());
        qt.requestSupplier("sTransNox", "RQ1");
        assertEquals("RQ1", qt.POQuotationRequestSupplierList(0).getTransactionNo());
        assertEquals(1, qt.POQuotationRequestSupplierList().size());
        TransactionAttachment loAttachment = qt.attachment(EditMode.READY, "sFileName", "a.png");
        assertSame(loAttachment, qt.TransactionAttachmentList(0));
        assertEquals(null, qt.getDetail());

        qt.paMasterList = null;
        qt.paPORequestSupplier = null;
        qt.paAttachments = null;
        assertEquals(0, qt.getPOQuotationCount());
        assertEquals(0, qt.getPOQuotationRequestSupplierCount());
        assertEquals(0, qt.getTransactionAttachmentCount());
    }

    // =====================================================================
    // Attachments
    // =====================================================================
    @Test
    public void addAndRemoveAttachments() throws Exception {
        try (MockedConstruction<SysTableContollers> loTables = attachments()) {
            assertEquals("success", qt.addAttachment().get("result"));
            assertEquals("Unable to add transaction attachment.", qt.addAttachment().get("message"));
            qt.TransactionAttachmentList(0).getModel().setTransactionNo("A1");
            assertEquals("success", qt.addAttachment().get("result"));
            assertEquals(2, qt.getTransactionAttachmentCount());
        }
        assertEquals("success", qt.removeAttachment(1).get("result"));
        assertEquals(1, qt.getTransactionAttachmentCount());
    }

    @Test
    public void removeSavedAttachmentDeactivates() throws Exception {
        assertEquals("No transaction attachment to be removed.", qt.removeAttachment(0).get("message"));
        TransactionAttachment loAttachment = qt.attachment(EditMode.READY, "sFileName", "a.png", "cRecdStat", "1");
        qt.removeAttachment(0);
        assertEquals(RecordStatus.INACTIVE, loAttachment.getModel().getRecordStatus());

        //adding the same file name again reactivates it
        assertEquals(0, qt.addAttachment("a.png"));
        assertEquals(RecordStatus.ACTIVE, loAttachment.getModel().getRecordStatus());
    }

    @Test
    public void addAttachmentByName() throws Exception {
        try (MockedConstruction<SysTableContollers> loTables = attachments()) {
            assertEquals(0, qt.addAttachment("b.png"));
        }
        assertEquals("b.png", qt.TransactionAttachmentList(0).getModel().getFileName());
        assertEquals(TRANS_NO, qt.TransactionAttachmentList(0).getModel().getSourceNo());
    }

    @Test
    public void copyFileToTempFolder() throws Exception {
        Path loDir = Files.createTempDirectory("qtatt");
        Path loFile = Files.createTempFile("qt", ".txt");
        Files.write(loFile, "x".getBytes());
        String lsOld = System.getProperty("sys.default.path.temp.attachments");
        System.setProperty("sys.default.path.temp.attachments", loDir.resolve("sub").toString());
        try {
            qt.copyFile(loFile.toString());
            assertTrue(new File(loDir.resolve("sub").toFile(), loFile.getFileName().toString()).exists());
            qt.copyFile(loDir.resolve("missing.txt").toString()); //logged only
        } finally {
            if (lsOld == null) {
                System.clearProperty("sys.default.path.temp.attachments");
            } else {
                System.setProperty("sys.default.path.temp.attachments", lsOld);
            }
        }
    }

    @Test
    public void checkExistingFileName() throws Exception {
        when(gr.executeQuery(anyString())).thenReturn(rows(row("sFileName", "a.png")));
        try (MockedConstruction<SysTableContollers> loTables = attachments()) {
            assertEquals("error", qt.checkExistingFileName("a.png").get("result"));
            when(gr.executeQuery(anyString())).thenReturn(rows());
            assertEquals(null, qt.checkExistingFileName("b.png").get("result"));
        }
    }

    @Test
    public void loadAttachmentsDownloads() throws Exception {
        java.util.List<String> laIds = java.util.Arrays.asList("A1", "A2");
        try (MockedConstruction<SysTableContollers> loTables = mockConstruction(SysTableContollers.class, (m, c) -> {
                    when(m.TransactionAttachment()).thenAnswer(i -> {
                        TransactionAttachment loRow = TestQuotation.attachmentMock(EditMode.READY, "sFileName", "a.png", "sSourceNo", TRANS_NO);
                        when(loRow.getAttachments(anyString(), anyString())).thenReturn(laIds);
                        return loRow;
                    });
                });
                org.mockito.MockedStatic<org.guanzon.appdriver.base.WebFile> loWeb = org.mockito.Mockito.mockStatic(org.guanzon.appdriver.base.WebFile.class)) {
            loWeb.when(() -> org.guanzon.appdriver.base.WebFile.getAccessToken(org.mockito.ArgumentMatchers.any())).thenReturn("tok");
            loWeb.when(() -> org.guanzon.appdriver.base.WebFile.DownloadFile(org.mockito.ArgumentMatchers.any(), anyString(), anyString(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyString()))
                    .thenAnswer(i -> QtKit.json("result", "success", "payload", QtKit.json("data", "eA==", "hash", "h", "filename", "a.png", "error", QtKit.json("message", "bad hash"))));
            loWeb.when(() -> org.guanzon.appdriver.base.WebFile.Base64ToFile(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(true, false);
            QtKit.edit(master, EditMode.UPDATE);
            qt.loadAttachments();
            assertEquals(2, qt.getTransactionAttachmentCount());
            verify(qt.TransactionAttachmentList(0)).updateRecord();

            loWeb.when(() -> org.guanzon.appdriver.base.WebFile.DownloadFile(org.mockito.ArgumentMatchers.any(), anyString(), anyString(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyString()))
                    .thenReturn(QtKit.err("offline"));
            QtKit.edit(master, EditMode.READY);
            assertEquals("error", qt.loadAttachments().get("result"));
        }
    }
}
