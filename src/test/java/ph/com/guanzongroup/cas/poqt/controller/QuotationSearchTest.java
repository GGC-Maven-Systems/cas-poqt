package ph.com.guanzongroup.cas.poqt.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.err;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.json;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.model;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.ok;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.smart;

import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.cas.client.account.AP_Client_Master;
import org.guanzon.cas.client.model.Model_AP_Client_Master;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.CategoryLevel2;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Department;
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Category_Level2;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Department;
import org.guanzon.cas.parameter.model.Model_Term;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Master;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Detail;
import ph.com.guanzongroup.cas.poqt.services.QuotationModels;

/** Search methods of the quotation (lookups mocked). */
public class QuotationSearchTest extends QuotationTestBase {

    private static MockedStatic<ShowDialogFX> browse(JSONObject foRow) {
        MockedStatic<ShowDialogFX> loDialog = mockStatic(ShowDialogFX.class);
        loDialog.when(() -> ShowDialogFX.Browse(any(GRiderCAS.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(foRow);
        return loDialog;
    }

    private static MockedConstruction<QuotationModels> models() {
        return mockConstruction(QuotationModels.class, (m, c) -> {
            when(m.POQuotationMaster()).thenAnswer(i -> model(Model_PO_Quotation_Master.class));
            when(m.POQuotationDetails()).thenAnswer(i -> model(Model_PO_Quotation_Detail.class));
        });
    }

    private static Inventory inventory(String fsStockId, String fsDescription) {
        Inventory loInventory = smart(Inventory.class);
        Model_Inventory loModel = model(Model_Inventory.class, "sStockIDx", fsStockId, "sDescript", fsDescription);
        when(loInventory.getModel()).thenReturn(loModel);
        when(loInventory.getSQ_Browse()).thenReturn("SELECT a.sStockIDx FROM Inventory a");
        return loInventory;
    }

    private static MockedConstruction<InvControllers> inv(Inventory foInventory) {
        return mockConstruction(InvControllers.class, (m, c) -> when(m.Inventory()).thenReturn(foInventory));
    }

    // =====================================================================
    // Procedures
    // =====================================================================
    @Test
    public void initTransaction() throws Exception {
        try (MockedConstruction<QuotationModels> loModels = models()) {
            assertEquals("success", qt.InitTransaction().get("result"));
        }
        assertTrue(qt.calls.contains("initialize"));
        assertEquals(0, qt.getDetailCount());
        assertEquals(0, qt.getPOQuotationCount());
        assertEquals(0, qt.getPOQuotationRequestSupplierCount());
        assertEquals(0, qt.getTransactionAttachmentCount());
        assertFalse(qt.getSourceCode().isEmpty());
    }

    @Test
    public void searchTransactionOpensPicked() throws Exception {
        qt.setTransactionStatus("012");
        try (MockedConstruction<QuotationModels> loModels = models();
                MockedStatic<ShowDialogFX> loDialog = browse(json("sTransNox", "T1"))) {
            assertEquals("success", qt.searchTransaction().get("result"));
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class),
                    contains("a.cTranStat IN ('0', '1', '2')"), eq(""), anyString(), anyString(), anyString(), eq(1)));
        }
        assertTrue(qt.calls.contains("open T1"));

        qt.setTransactionStatus("0");
        try (MockedConstruction<QuotationModels> loModels = models();
                MockedStatic<ShowDialogFX> loDialog = browse(json("sTransNox", "T2"))) {
            qt.searchTransaction("Cebu", "Proc", "Acme", "Paper", "M001");
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class),
                    contains("c.sBranchNm LIKE '%Cebu' AND h.sDeptName LIKE '%Proc' AND i.sCompnyNm LIKE '%Acme'"
                            + " AND f.sDescript LIKE '%Paper' AND a.sTransNox LIKE '%M001'"),
                    eq(""), anyString(), anyString(), anyString(), eq(1)));
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class),
                    contains("a.cTranStat = '0'"), eq(""), anyString(), anyString(), anyString(), eq(1)));
        }
        assertTrue(qt.calls.contains("open T2"));
    }

    @Test
    public void searchTransactionNothingPicked() throws Exception {
        try (MockedStatic<ShowDialogFX> loDialog = browse(null)) {
            assertEquals("No record loaded.", qt.searchTransaction().get("message"));
            assertEquals("No record loaded.", qt.searchTransaction(null, "", null, "", null).get("message"));
        }
    }

    // =====================================================================
    // Request item without a source (inventory)
    // =====================================================================
    @Test
    public void requestItemNeedsBranchAndSupplier() throws Exception {
        master.setBranchCode("");
        assertEquals("Branch is not set.", qt.SearchRequestItem("x", false, 0).get("message"));
        master.setBranchCode("M001");
        assertEquals("Supplier is not set.", qt.SearchRequestItem("x", false, 0).get("message"));
    }

    @Test
    public void requestItemFromInventory() throws Exception {
        master.setSupplierId("SUP1");
        Model_PO_Quotation_Detail loRow = qt.line();
        loRow.isReverse(false);
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S9", "Bond Paper"));
                MockedStatic<ShowDialogFX> loDialog = browse(json("sStockIDx", "S9"))) {
            JSONObject loResult = qt.SearchRequestItem("bond", true, 0);
            assertEquals("success", loResult.get("result"));
            assertEquals(0, loResult.get("row"));
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class),
                    eq("SELECT a.sStockIDx FROM Inventory a WHERE  a.sCategCd1 = '0001' GROUP BY a.sStockIDx "),
                    eq("bond"), anyString(), anyString(), anyString(), eq(0)));
        }
        assertEquals("S9", loRow.getStockId());
        assertEquals("Bond Paper", loRow.getDescription());
        assertTrue(loRow.isReverse());
    }

    @Test
    public void requestItemFromInventoryReusesExcludedRow() throws Exception {
        master.setSupplierId("SUP1");
        Model_PO_Quotation_Detail loOld = qt.line("sStockIDx", "S9", "sDescript", "Bond Paper");
        loOld.isReverse(false);
        qt.line();
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S9", "Bond Paper"));
                MockedStatic<ShowDialogFX> loDialog = browse(json("sStockIDx", "S9"))) {
            assertEquals(0, qt.SearchRequestItem("bond", false, 1).get("row"));
        }
        assertTrue(loOld.isReverse());
    }

    @Test
    public void requestItemFromInventoryDuplicate() throws Exception {
        master.setSupplierId("SUP1");
        qt.line("sStockIDx", "S9", "sDescript", "Bond Paper");
        qt.line();
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S9", "Bond Paper"));
                MockedStatic<ShowDialogFX> loDialog = browse(json("sStockIDx", "S9"))) {
            JSONObject loResult = qt.SearchRequestItem("bond", false, 1);
            assertEquals("error", loResult.get("result"));
            assertTrue(((String) loResult.get("message")).contains("already exists at row1"));
        }
    }

    @Test
    public void requestItemFromInventoryNothingPicked() throws Exception {
        master.setSupplierId("SUP1");
        qt.line();
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S9", "Bond Paper"));
                MockedStatic<ShowDialogFX> loDialog = browse(null)) {
            assertEquals("No record loaded.", qt.SearchRequestItem("bond", false, 0).get("message"));
        }
    }

    // =====================================================================
    // Request item from the source request
    // =====================================================================
    private static MockedConstruction<QuotationModels> requestDetail(Object... faValues) {
        return mockConstruction(QuotationModels.class, (m, c) ->
                when(m.POQuotationRequestDetails()).thenAnswer(i -> model(Model_PO_Quotation_Request_Detail.class, faValues)));
    }

    @Test
    public void requestItemFromSource() throws Exception {
        master.setSourceNo("RQ1");
        Model_PO_Quotation_Detail loRow = qt.line();
        try (MockedConstruction<QuotationModels> loModels = requestDetail("sStockIDx", "S1", "sDescript", "Pen");
                MockedStatic<ShowDialogFX> loDialog = browse(json("sTransNox", "RQ1", "nEntryNox", "1"))) {
            JSONObject loResult = qt.SearchRequestItem("pen", false, 0);
            assertEquals("success", loResult.get("result"));
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class),
                    contains("cReversex = '+' AND sTransNox = 'RQ1'"), eq("pen"), anyString(), anyString(), anyString(), eq(1)));
        }
        assertEquals("S1", loRow.getStockId());
        assertEquals("Pen", loRow.getDescription());
    }

    @Test
    public void requestItemFromSourceAlreadyListed() throws Exception {
        master.setSourceNo("RQ1");
        qt.line("sDescript", "Pen");
        qt.line();
        try (MockedConstruction<QuotationModels> loModels = requestDetail("sStockIDx", "S1", "sDescript", "Pen");
                MockedStatic<ShowDialogFX> loDialog = browse(json("sTransNox", "RQ1", "nEntryNox", "1"))) {
            JSONObject loResult = qt.SearchRequestItem("pen", false, 1);
            assertEquals(true, loResult.get("reverse"));
        }
    }

    @Test
    public void requestItemFromSourceReusesExcludedRow() throws Exception {
        master.setSourceNo("RQ1");
        Model_PO_Quotation_Detail loOld = qt.line("sDescript", "Pen");
        loOld.isReverse(false);
        qt.line();
        try (MockedConstruction<QuotationModels> loModels = requestDetail("sStockIDx", "S1", "sDescript", "Pen");
                MockedStatic<ShowDialogFX> loDialog = browse(json("sTransNox", "RQ1", "nEntryNox", "1"))) {
            assertEquals(0, qt.SearchRequestItem("pen", false, 1).get("row"));
        }
        assertTrue(loOld.isReverse());
        assertEquals("S1", loOld.getStockId());
    }

    @Test
    public void requestItemFromSourceNothingPicked() throws Exception {
        master.setSourceNo("RQ1");
        qt.line();
        try (MockedConstruction<QuotationModels> loModels = requestDetail();
                MockedStatic<ShowDialogFX> loDialog = browse(null)) {
            assertEquals("No record loaded.", qt.SearchRequestItem("pen", false, 0).get("message"));
        }
    }

    // =====================================================================
    // Replacement item
    // =====================================================================
    @Test
    public void replacementNeedsSourceAndDescription() throws Exception {
        qt.line();
        assertEquals("Source No is not set.", qt.SearchInventory("x", false, 0).get("message"));
        master.setSourceNo("RQ1");
        assertEquals("Request Description is not set.", qt.SearchInventory("x", false, 0).get("message"));
    }

    @Test
    public void replacementPicked() throws Exception {
        master.setSourceNo("RQ1");
        master.POQuotationRequest().setValue("sCategCd2", "0101");
        Model_PO_Quotation_Detail loRow = qt.line("sStockIDx", "S1", "sDescript", "Pen");
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S2", "Ballpen"));
                MockedStatic<ShowDialogFX> loDialog = browse(json("sStockIDx", "S2"))) {
            assertEquals("success", qt.SearchInventory("ball", false, 0).get("result"));
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class),
                    contains("a.sCategCd1 = '0001' AND a.sCategCd2 = '0101'"), eq("ball"), anyString(), anyString(), anyString(), eq(1)));
        }
        assertEquals("S2", loRow.getReplaceId());
        assertEquals("Ballpen", loRow.getReplaceDescription());
    }

    @Test
    public void replacementSameAsRequest() throws Exception {
        master.setSourceNo("RQ1");
        qt.line("sStockIDx", "S1", "sDescript", "Pen");
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S1", "Pen"));
                MockedStatic<ShowDialogFX> loDialog = browse(json("sStockIDx", "S1"))) {
            assertEquals("Selected item replacement description is the same in the request.",
                    qt.SearchInventory("pen", true, 0).get("message"));
        }
    }

    @Test
    public void replacementAlreadyInDetail() throws Exception {
        master.setSourceNo("RQ1");
        qt.line("sStockIDx", "S1", "sDescript", "Pen", "sReplacID", "S2", "sReplacDs", "Ballpen");
        qt.line("sStockIDx", "S3", "sDescript", "Marker");
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S2", "Ballpen"));
                MockedStatic<ShowDialogFX> loDialog = browse(json("sStockIDx", "S2"))) {
            JSONObject loResult = qt.SearchInventory("ball", false, 1);
            assertEquals(true, loResult.get("reverse"));
        }
    }

    @Test
    public void replacementReusesExcludedRow() throws Exception {
        master.setSourceNo("RQ1");
        Model_PO_Quotation_Detail loOld = qt.line("sStockIDx", "S1", "sDescript", "Pen", "sReplacID", "S2", "sReplacDs", "Ballpen");
        loOld.isReverse(false);
        qt.line("sStockIDx", "S3", "sDescript", "Marker");
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S2", "Ballpen"));
                MockedStatic<ShowDialogFX> loDialog = browse(json("sStockIDx", "S2"))) {
            assertEquals(0, qt.SearchInventory("ball", false, 1).get("row"));
        }
        assertTrue(loOld.isReverse());
    }

    @Test
    public void replacementNothingPicked() throws Exception {
        master.setSourceNo("RQ1");
        qt.line("sStockIDx", "S1", "sDescript", "Pen");
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S2", "Ballpen"));
                MockedStatic<ShowDialogFX> loDialog = browse(null)) {
            assertEquals("No record loaded.", qt.SearchInventory("ball", false, 0).get("message"));
        }
    }

    // =====================================================================
    // Header lookups
    // =====================================================================
    private static MockedConstruction<ParamControllers> params(Object... faLookups) {
        return mockConstruction(ParamControllers.class, (m, c) -> {
            for (Object loLookup : faLookups) {
                if (loLookup instanceof Branch) {
                    when(m.Branch()).thenReturn((Branch) loLookup);
                } else if (loLookup instanceof Department) {
                    when(m.Department()).thenReturn((Department) loLookup);
                } else if (loLookup instanceof CategoryLevel2) {
                    when(m.CategoryLevel2()).thenReturn((CategoryLevel2) loLookup);
                } else if (loLookup instanceof Term) {
                    when(m.Term()).thenReturn((Term) loLookup);
                } else if (loLookup instanceof Company) {
                    when(m.Company()).thenReturn((Company) loLookup);
                }
            }
        });
    }

    @Test
    public void headerLookups() throws Exception {
        Company loCompany = smart(Company.class);
        when(loCompany.getModel()).thenReturn(model(Model_Company.class, "sCompnyID", "C9"));
        when(loCompany.searchRecord(anyString(), anyBoolean())).thenReturn(ok());
        Branch loBranch = smart(Branch.class);
        when(loBranch.getModel()).thenReturn(model(Model_Branch.class, "sBranchCd", "B2", "sBranchNm", "Cebu"));
        when(loBranch.searchRecord(anyString(), anyBoolean())).thenReturn(ok());
        Department loDept = smart(Department.class);
        when(loDept.getModel()).thenReturn(model(Model_Department.class, "sDeptIDxx", "033", "sDeptName", "Procurement"));
        when(loDept.searchRecord(anyString(), anyBoolean())).thenReturn(ok());
        Term loTerm = smart(Term.class);
        when(loTerm.getModel()).thenReturn(model(Model_Term.class, "sTermCode", "T30"));
        when(loTerm.searchRecord(anyString(), anyBoolean())).thenReturn(ok());

        try (MockedConstruction<ParamControllers> loParams = params(loCompany, loBranch, loDept, loTerm)) {
            qt.SearchCompany("c", false);
            assertEquals("C9", master.getCompanyId());
            qt.SearchBranch("ce", false, true);
            assertEquals("Cebu", qt.getSearchBranch());
            qt.SearchBranch("ce", false, false);
            assertEquals("B2", master.getBranchCode());
            qt.SearchDepartment("pro", false);
            assertEquals("Procurement", qt.getSearchDepartment());
            qt.SearchTerm("30", true);
            assertEquals("T30", master.getTerm());
        }
    }

    @Test
    public void headerLookupsNotFound() throws Exception {
        Company loCompany = smart(Company.class);
        when(loCompany.searchRecord(anyString(), anyBoolean())).thenReturn(err("none"));
        Branch loBranch = smart(Branch.class);
        when(loBranch.searchRecord(anyString(), anyBoolean())).thenReturn(err("none"));
        Department loDept = smart(Department.class);
        when(loDept.searchRecord(anyString(), anyBoolean())).thenReturn(err("none"));
        Term loTerm = smart(Term.class);
        when(loTerm.searchRecord(anyString(), anyBoolean())).thenReturn(err("none"));
        try (MockedConstruction<ParamControllers> loParams = params(loCompany, loBranch, loDept, loTerm)) {
            assertEquals("error", qt.SearchCompany("c", false).get("result"));
            assertEquals("error", qt.SearchBranch("c", false, false).get("result"));
            assertEquals("error", qt.SearchDepartment("c", false).get("result"));
            assertEquals("error", qt.SearchTerm("c", false).get("result"));
        }
        assertEquals("", master.getCompanyId());
        assertEquals("M001", master.getBranchCode());
    }

    @Test
    public void supplierLookup() throws Exception {
        AP_Client_Master loClient = smart(AP_Client_Master.class);
        Model_AP_Client_Master loModel = model(Model_AP_Client_Master.class, "sClientID", "SUP1");
        loModel.Client().setValue("sCompnyNm", "Acme");
        when(loClient.getModel()).thenReturn(loModel);
        when(loClient.searchRecord(anyString(), anyBoolean())).thenReturn(ok());
        try (MockedConstruction<ClientControllers> loClients = mockConstruction(ClientControllers.class,
                (m, c) -> when(m.APClientMaster()).thenReturn(loClient))) {
            qt.SearchSupplier("ac", false, true);
            assertEquals("Acme", qt.getSearchSupplier());
            qt.SearchSupplier("ac", false, false);
            assertEquals("SUP1", master.getSupplierId());
            when(loClient.searchRecord(anyString(), anyBoolean())).thenReturn(err("none"));
            assertEquals("error", qt.SearchSupplier("zz", false, false).get("result"));
        }
    }

    @Test
    public void categoryLookup() throws Exception {
        CategoryLevel2 loCategory = smart(CategoryLevel2.class);
        when(loCategory.getModel()).thenReturn(model(Model_Category_Level2.class, "sCategrCd", "0102", "sDescript", "Paper"));
        when(loCategory.getSQ_Browse()).thenReturn("SELECT sCategrCd FROM Category_Level2");
        try (MockedConstruction<ParamControllers> loParams = params(loCategory);
                MockedStatic<ShowDialogFX> loDialog = browse(json("sCategrCd", "0102"))) {
            qt.SearchCategory("pa", false);
            assertEquals("Paper", qt.getSearchCategory());
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class),
                    eq("SELECT sCategrCd FROM Category_Level2 WHERE  sIndstCdx = '09'"), eq("pa"), anyString(), anyString(), anyString(), eq(1)));
        }
        try (MockedConstruction<ParamControllers> loParams = params(loCategory);
                MockedStatic<ShowDialogFX> loDialog = browse(null)) {
            assertEquals("No record loaded.", qt.SearchCategory("pa", true).get("message"));
        }
    }
}
