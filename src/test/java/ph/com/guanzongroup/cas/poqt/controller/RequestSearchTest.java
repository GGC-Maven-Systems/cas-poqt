package ph.com.guanzongroup.cas.poqt.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.client.account.AP_Client_Master;
import org.guanzon.cas.client.model.Model_AP_Client_Master;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.Category;
import org.guanzon.cas.parameter.CategoryLevel2;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Department;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.ModelVariant;
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Brand;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Category_Level2;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Department;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.model.Model_Model_Variant;
import org.guanzon.cas.parameter.model.Model_Term;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Supplier;

/** Search methods of the quotation request (lookups mocked). */
public class RequestSearchTest extends RequestTestBase {

    /** ParamControllers whose lookups are the given mocks. */
    private static MockedConstruction<ParamControllers> params(Object... faLookups) {
        return mockConstruction(ParamControllers.class, (m, c) -> {
            for (Object loLookup : faLookups) {
                if (loLookup instanceof Branch) {
                    when(m.Branch()).thenReturn((Branch) loLookup);
                } else if (loLookup instanceof Department) {
                    when(m.Department()).thenReturn((Department) loLookup);
                } else if (loLookup instanceof CategoryLevel2) {
                    when(m.CategoryLevel2()).thenReturn((CategoryLevel2) loLookup);
                } else if (loLookup instanceof Category) {
                    when(m.Category()).thenReturn((Category) loLookup);
                } else if (loLookup instanceof Brand) {
                    when(m.Brand()).thenReturn((Brand) loLookup);
                } else if (loLookup instanceof ModelVariant) {
                    when(m.ModelVariant()).thenReturn((ModelVariant) loLookup);
                } else if (loLookup instanceof Term) {
                    when(m.Term()).thenReturn((Term) loLookup);
                } else if (loLookup instanceof Company) {
                    when(m.Company()).thenReturn((Company) loLookup);
                } else if (loLookup instanceof Industry) {
                    when(m.Industry()).thenReturn((Industry) loLookup);
                }
            }
        });
    }

    private static Branch branch(JSONObject foResult) throws Exception {
        Branch loBranch = smart(Branch.class);
        Model_Branch loModel = model(Model_Branch.class, "sBranchCd", "B002", "sBranchNm", "Cebu");
        when(loBranch.getModel()).thenReturn(loModel);
        when(loBranch.searchRecord(anyString(), anyBoolean())).thenReturn(foResult);
        return loBranch;
    }

    private static Department department(JSONObject foResult) throws Exception {
        Department loDept = smart(Department.class);
        Model_Department loModel = model(Model_Department.class, "sDeptIDxx", "033", "sDeptName", "Procurement");
        when(loDept.getModel()).thenReturn(loModel);
        when(loDept.searchRecord(anyString(), anyBoolean())).thenReturn(foResult);
        return loDept;
    }

    // =====================================================================
    // Header lookups
    // =====================================================================
    @Test
    public void destinationBranchDepartment() throws Exception {
        try (MockedConstruction<ParamControllers> loParams = params(branch(ok()), department(ok()))) {
            assertEquals("success", req.SearchDestination("ce", false).get("result"));
            assertEquals("B002", master.getDestination());

            req.SearchBranch("ce", false, true);
            assertEquals("Cebu", req.getSearchBranch());
            req.SearchBranch("ce", false, false);
            assertEquals("B002", master.getBranchCode());

            req.SearchDepartment("pro", false, true);
            assertEquals("Procurement", req.getSearchDepartment());
            req.SearchDepartment("pro", false, false);
            assertEquals("033", master.getDepartmentId());

            req.SearchDestination("ce", false, true);
            assertEquals("Cebu", req.getSearchDestination());
            req.SearchDestination("ce", false, false);
            assertEquals("B002", master.getDestination());
        }
    }

    @Test
    public void lookupNotFoundChangesNothing() throws Exception {
        try (MockedConstruction<ParamControllers> loParams = params(branch(err("none")), department(err("none")))) {
            assertEquals("error", req.SearchDestination("x", false).get("result"));
            assertEquals("M001", master.getDestination());
            req.SearchBranch("x", false, true);
            req.SearchDepartment("x", false, false);
            req.SearchDestination("x", false, true);
            assertEquals(null, req.getSearchBranch());
            assertEquals(null, master.getDepartmentId());
        }
    }

    @Test
    public void categoryUsesIndustryFilter() throws Exception {
        CategoryLevel2 loCategory = smart(CategoryLevel2.class);
        Model_Category_Level2 loModel = model(Model_Category_Level2.class, "sCategrCd", "0102", "sDescript", "Paper");
        when(loCategory.getModel()).thenReturn(loModel);
        when(loCategory.getSQ_Browse()).thenReturn("SELECT sCategrCd, sDescript FROM Category_Level2");
        try (MockedConstruction<ParamControllers> loParams = params(loCategory);
                MockedStatic<ShowDialogFX> loDialog = mockStatic(ShowDialogFX.class)) {
            loDialog.when(() -> ShowDialogFX.Browse(any(GRiderCAS.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(json("sCategrCd", "0102"));
            req.SearchCategory("pap", false, false);
            assertEquals("0102", master.getCategoryLevel2());
            req.SearchCategory("pap", false, true);
            assertEquals("Paper", req.getSearchCategory());
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class),
                    eq("SELECT sCategrCd, sDescript FROM Category_Level2 WHERE cRecdStat = '1' AND (sIndstCdx = '09' OR ISNULL(sIndstCdx))"),
                    eq("pap"), anyString(), anyString(), anyString(), eq(1)), org.mockito.Mockito.times(2));

            loDialog.when(() -> ShowDialogFX.Browse(any(GRiderCAS.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(null);
            assertEquals("No record loaded.", req.SearchCategory("zz", true, false).get("message"));
        }
    }

    @Test
    public void brandNeedsCategoryAndClearsModelOnChange() throws Exception {
        master.setCategoryLevel2("");
        Model_PO_Quotation_Request_Detail loRow = req.line("sBrandIDx", "OLD", "sModelIDx", "M1", "sStockIDx", "S1", "sDescript", "Item");
        assertEquals("Category is not set.", req.SearchBrand("x", false, 0).get("message"));

        master.setCategoryLevel2("0101");
        Brand loBrand = smart(Brand.class);
        when(loBrand.getModel()).thenReturn(model(Model_Brand.class, "sBrandIDx", "NEW"));
        when(loBrand.searchRecord(anyString(), anyBoolean(), eq("09"))).thenReturn(ok());
        try (MockedConstruction<ParamControllers> loParams = params(loBrand)) {
            req.SearchBrand("ne", false, 0);
        }
        assertEquals("NEW", loRow.getBrandId());
        assertEquals("", loRow.getModelId());
        assertEquals("", loRow.getStockId());
        assertEquals("", loRow.getDescription());
    }

    @Test
    public void modelNeedsBrand() throws Exception {
        Model_PO_Quotation_Request_Detail loRow = req.line("sBrandIDx", "", "sModelIDx", "M1", "sStockIDx", "S1", "sDescript", "Item");
        assertEquals("Brand is not set.", req.SearchModel("x", false, 0).get("message"));

        loRow.setBrandId("B1");
        ModelVariant loModel = smart(ModelVariant.class);
        when(loModel.getModel()).thenReturn(model(Model_Model_Variant.class, "sModelIDx", "M2"));
        when(loModel.searchRecordByModel(anyString(), anyBoolean(), eq("B1"))).thenReturn(ok());
        try (MockedConstruction<ParamControllers> loParams = params(loModel)) {
            JSONObject loResult = req.SearchModel("vi", false, 0);
            assertEquals(0, loResult.get("row"));
        }
        assertEquals("M2", loRow.getModelId());
        assertEquals("", loRow.getStockId());
    }

    // =====================================================================
    // Items
    // =====================================================================
    private Inventory inventory(String fsStockId, String fsDescription, Object fnPrice) throws Exception {
        Inventory loInventory = smart(Inventory.class);
        Model_Inventory loModel = model(Model_Inventory.class, "sStockIDx", fsStockId, "sDescript", fsDescription,
                "sBrandIDx", "B1", "sModelIDx", "M1", "nSelPrice", fnPrice);
        when(loInventory.getModel()).thenReturn(loModel);
        when(loInventory.getSQ_Browse()).thenReturn("SELECT a.sStockIDx FROM Inventory a");
        return loInventory;
    }

    private MockedConstruction<InvControllers> inv(Inventory foInventory) {
        return mockConstruction(InvControllers.class, (m, c) -> when(m.Inventory()).thenReturn(foInventory));
    }

    @Test
    public void inventoryNeedsCategory() throws Exception {
        master.setCategoryLevel2(null);
        req.line();
        assertEquals("Category is not set.", req.SearchInventory("x", false, 0).get("message"));
    }

    @Test
    public void inventoryPicksItemWithPrice() throws Exception {
        Model_PO_Quotation_Request_Detail loRow = req.line("sBrandIDx", "B1", "sModelIDx", "M1", "sStockIDx", "", "sDescript", "");
        Inventory loInventory = inventory("S9", " Bond Paper ", new java.math.BigDecimal("150.50"));
        try (MockedConstruction<InvControllers> loInv = inv(loInventory);
                MockedStatic<ShowDialogFX> loDialog = mockStatic(ShowDialogFX.class)) {
            loDialog.when(() -> ShowDialogFX.Browse(any(GRiderCAS.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(json("sStockIDx", "S9"));
            JSONObject loResult = req.SearchInventory("bond", false, 0);
            assertEquals("success", loResult.get("result"));
            loDialog.verify(() -> ShowDialogFX.Browse(any(GRiderCAS.class),
                    eq("SELECT a.sStockIDx FROM Inventory a WHERE  a.sCategCd1 = '0001' AND a.sCategCd2 = '0101'"
                            + " AND a.sBrandIDx = 'B1' AND a.sModelIDx = 'M1' GROUP BY a.sStockIDx "),
                    eq("bond"), anyString(), anyString(), anyString(), eq(1)));
        }
        assertEquals("S9", loRow.getStockId());
        assertEquals("Bond Paper", loRow.getDescription());
        assertEquals(150.50, loRow.getUnitPrice(), 0.001);
    }

    @Test
    public void inventoryWithoutPriceAndFreeText() throws Exception {
        Model_PO_Quotation_Request_Detail loRow = req.line("sBrandIDx", "", "sModelIDx", "", "sStockIDx", "", "sDescript", "");
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S9", "Pen", null));
                MockedStatic<ShowDialogFX> loDialog = mockStatic(ShowDialogFX.class)) {
            loDialog.when(() -> ShowDialogFX.Browse(any(GRiderCAS.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(json("sStockIDx", "S9"));
            req.SearchInventory("pen", true, 0);
            assertEquals(0.0, loRow.getUnitPrice(), 0.001);

            //nothing picked: the typed text becomes a free-text item
            loDialog.when(() -> ShowDialogFX.Browse(any(GRiderCAS.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(null);
            JSONObject loResult = req.SearchInventory("Yellow pad", false, 0);
            assertEquals("success", loResult.get("result"));
        }
        assertEquals("", loRow.getStockId());
        assertEquals("Yellow pad", loRow.getDescription());
    }

    @Test
    public void inventoryAlreadyOnAnotherRow() throws Exception {
        req.line("sStockIDx", "S9", "sDescript", "Pen");
        req.line("sStockIDx", "", "sDescript", "");
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S9", "Pen", null));
                MockedStatic<ShowDialogFX> loDialog = mockStatic(ShowDialogFX.class)) {
            loDialog.when(() -> ShowDialogFX.Browse(any(GRiderCAS.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(json("sStockIDx", "S9"));
            JSONObject loResult = req.SearchInventory("pen", false, 1);
            assertEquals("error", loResult.get("result"));
            assertEquals(true, loResult.get("reverse"));
            assertTrue(((String) loResult.get("message")).contains("row 1"));

            //same free text as an excluded row: that row comes back
            req.Detail(0).isReverse(false);
            req.Detail(0).setStockId("");
            loDialog.when(() -> ShowDialogFX.Browse(any(GRiderCAS.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(null);
            loResult = req.SearchInventory("Pen", false, 1);
            assertEquals(0, loResult.get("row"));
            assertTrue(req.Detail(0).isReverse());
        }
    }

    @Test
    public void excludedRowWithSameItemComesBack() throws Exception {
        Model_PO_Quotation_Request_Detail loOld = req.line("sStockIDx", "S9", "sDescript", "Pen", "cReversex", "-");
        req.line("sStockIDx", "", "sDescript", "");
        try (MockedConstruction<InvControllers> loInv = inv(inventory("S9", "Pen", null));
                MockedStatic<ShowDialogFX> loDialog = mockStatic(ShowDialogFX.class)) {
            loDialog.when(() -> ShowDialogFX.Browse(any(GRiderCAS.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(json("sStockIDx", "S9"));
            JSONObject loResult = req.SearchInventory("pen", false, 1);
            assertEquals(0, loResult.get("row"));
        }
        assertTrue(loOld.isReverse());
    }

    @Test
    public void checkExistingDetail() {
        req.line("sStockIDx", "S1", "sDescript", "Pen");
        req.line("sStockIDx", "S2", "sDescript", "Pad", "cReversex", "-");
        assertEquals("success", req.checkExistingDetail(0, null, null, true).get("result"));
        assertEquals("success", req.checkExistingDetail(0, "S1", "Pen", true).get("result")); //own row
        assertEquals(true, req.checkExistingDetail(1, "S1", "", true).get("reverse"));
        assertEquals(false, req.checkExistingDetail(0, "", "Pad", false).get("reverse"));
        assertEquals("success", req.checkExistingDetail(1, "S1", "Other", false).get("result")); //stock id ignored when not searching
    }

    @Test
    public void checkExistStockInventory() {
        req.line("sStockIDx", "", "sDescript", "Free text");
        assertFalse(req.checkExistStockInventory());
        req.line("sStockIDx", "S1", "sDescript", "Pen", "cReversex", "-");
        assertFalse(req.checkExistStockInventory());
        req.line("sStockIDx", "S2", "sDescript", "Pad");
        assertTrue(req.checkExistStockInventory());
    }

    // =====================================================================
    // Suppliers
    // =====================================================================
    private AP_Client_Master client(JSONObject foResult, String fsClientId) throws Exception {
        AP_Client_Master loClient = smart(AP_Client_Master.class);
        Model_AP_Client_Master loModel = model(Model_AP_Client_Master.class, "sClientID", fsClientId);
        when(loClient.getModel()).thenReturn(loModel);
        when(loClient.searchRecord(anyString(), anyBoolean())).thenReturn(foResult);
        return loClient;
    }

    private MockedConstruction<ClientControllers> clients(AP_Client_Master foClient) {
        return mockConstruction(ClientControllers.class, (m, c) -> when(m.APClientMaster()).thenReturn(foClient));
    }

    @Test
    public void supplierPicked() throws Exception {
        Model_PO_Quotation_Request_Supplier loRow = req.supplier("sSupplier", "", "sCompnyID", "C1");
        try (MockedConstruction<ClientControllers> loClients = clients(client(ok(), "SUP1"))) {
            JSONObject loResult = req.SearchSupplier("ab", false, 0);
            assertEquals("success", loResult.get("result"));
        }
        assertEquals("SUP1", loRow.getSupplierId());
    }

    @Test
    public void supplierNotFoundStillSuccess() throws Exception {
        //as in cas-purchasing: the result is reset to success at the end
        req.supplier("sSupplier", "", "sCompnyID", "");
        try (MockedConstruction<ClientControllers> loClients = clients(client(err("none"), "SUP1"))) {
            assertEquals("success", req.SearchSupplier("ab", false, 0).get("result"));
        }
    }

    @Test
    public void supplierSameCompanyTwice() throws Exception {
        req.supplier("sSupplier", "SUP1", "sCompnyID", "C1");
        Model_PO_Quotation_Request_Supplier loNew = req.supplier("sSupplier", "X", "sCompnyID", "C1", "sAddrssID", "A", "sContctID", "P");
        try (MockedConstruction<ClientControllers> loClients = clients(client(ok(), "SUP1"))) {
            JSONObject loResult = req.SearchSupplier("ab", false, 1);
            assertEquals(true, loResult.get("reverse"));
            assertTrue(((String) loResult.get("message")).contains("row 1"));

            //the existing row is excluded: it comes back and the new row is cleared
            req.POQuotationRequestSupplierList(0).isReverse(false);
            loResult = req.SearchSupplier("ab", false, 1);
            assertEquals(0, loResult.get("row"));
        }
        assertTrue(req.POQuotationRequestSupplierList(0).isReverse());
        assertEquals("", loNew.getSupplierId());
        assertEquals("", loNew.getCompanyId());
    }

    @Test
    public void termAndCompanyNeedSupplier() throws Exception {
        req.supplier("sSupplier", "", "sCompnyID", "");
        assertEquals("Supplier cannot be empty.", req.SearchTerm("x", false, 0).get("message"));
        assertEquals("Supplier cannot be empty.", req.SearchCompany("x", false, 0).get("message"));
    }

    @Test
    public void termAndCompany() throws Exception {
        Model_PO_Quotation_Request_Supplier loRow = req.supplier("sSupplier", "SUP1", "sCompnyID", "");
        Term loTerm = smart(Term.class);
        when(loTerm.getModel()).thenReturn(model(Model_Term.class, "sTermCode", "T30"));
        when(loTerm.searchRecord(anyString(), anyBoolean())).thenReturn(ok());
        Company loCompany = smart(Company.class);
        when(loCompany.getModel()).thenReturn(model(Model_Company.class, "sCompnyID", "C9"));
        when(loCompany.searchRecord(anyString(), anyBoolean())).thenReturn(ok());
        try (MockedConstruction<ParamControllers> loParams = params(loTerm, loCompany)) {
            req.SearchTerm("30", false, 0);
            assertEquals("success", req.SearchCompany("gu", false, 0).get("result"));
        }
        assertEquals("C9", loRow.getCompanyId());
    }

    @Test
    public void companyMakesDuplicateSupplier() throws Exception {
        req.supplier("sSupplier", "SUP1", "sCompnyID", "C9");
        Model_PO_Quotation_Request_Supplier loNew = req.supplier("sSupplier", "SUP1", "sCompnyID", "");
        Company loCompany = smart(Company.class);
        when(loCompany.getModel()).thenReturn(model(Model_Company.class, "sCompnyID", "C9"));
        when(loCompany.searchRecord(anyString(), anyBoolean())).thenReturn(ok());
        try (MockedConstruction<ParamControllers> loParams = params(loCompany)) {
            assertEquals(true, req.SearchCompany("gu", false, 1).get("reverse"));
            req.POQuotationRequestSupplierList(0).isReverse(false);
            assertEquals(0, req.SearchCompany("gu", false, 1).get("row"));
        }
        assertEquals("", loNew.getSupplierId());
        assertEquals(EditMode.ADDNEW, loNew.getEditMode());
    }

    // =====================================================================
    // Report filters
    // =====================================================================
    @Test
    public void filterLookups() throws Exception {
        Category loCategory = smart(Category.class);
        when(loCategory.getModel()).thenReturn(model(Model_Category.class, "sDescript", "General"));
        when(loCategory.searchRecord(anyString(), anyBoolean())).thenReturn(ok());
        Company loCompany = smart(Company.class);
        when(loCompany.getModel()).thenReturn(model(Model_Company.class, "sCompnyNm", "Guanzon"));
        when(loCompany.searchRecord(anyString(), anyBoolean())).thenReturn(ok());
        Industry loIndustry = smart(Industry.class);
        when(loIndustry.getModel()).thenReturn(model(Model_Industry.class, "sDescript", "General Inventory"));
        when(loIndustry.searchRecord(anyString(), anyBoolean())).thenReturn(ok());
        AP_Client_Master loClient = client(ok(), "SUP1");
        loClient.getModel().Client().setValue("sCompnyNm", "Supplier Inc.");
        try (MockedConstruction<ParamControllers> loParams = params(loCategory, loCompany, loIndustry);
                MockedConstruction<ClientControllers> loClients = clients(loClient)) {
            req.SearchCategory("ge", false);
            req.SearchCompany("gu", false);
            req.SearchIndustry("ge", false);
            req.SearchSupplier("su", false);
        }
        assertEquals("General", req.getSearchCategory());
        assertEquals("Guanzon", req.getSearchCompany());
        assertEquals("General Inventory", req.getSearchIndustry());
        assertEquals("Supplier Inc.", req.getSearchSupplier());
    }

    @Test
    public void initializeFilterAndCompanyName() throws Exception {
        master.Industry().setValue("sDescript", "General Inventory");
        master.Category().setValue("sDescript", "General");
        Company loCompany = smart(Company.class);
        Model_Company loModel = model(Model_Company.class, "sCompnyNm", "Guanzon");
        when(loCompany.getModel()).thenReturn(loModel);
        try (MockedConstruction<ParamControllers> loParams = params(loCompany)) {
            req.initializeFilter();
            assertEquals("Guanzon", req.getSearchCompany());
            loModel.setValue("sCompnyNm", "");
            assertEquals("", req.getCompanyName("C2"));
        }
        assertEquals("General Inventory", req.getSearchIndustry());
        assertEquals("General", req.getSearchCategory());
        assertEquals("Main Office", req.getSearchBranch());
        assertEquals("09", master.getIndustryId());
    }
}
