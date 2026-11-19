package ph.com.guanzongroup.cas.poqt.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

/** The status-change template every status procedure now calls. */
public class QuotationTransactionTest {

    /** Records the statusChange call instead of writing to the database. */
    static class Recording extends QuotationTransaction {

        String status;
        String remarks;
        Boolean passedParent; //null: the 5-argument statusChange was used

        @Override
        protected JSONObject statusChange(String fsTable, String fsTransNo, String fsRemarks, String fsStatus, boolean fbConfirm) {
            status = fsStatus;
            remarks = fsRemarks;
            return success();
        }

        @Override
        protected JSONObject statusChange(String fsTable, String fsTransNo, String fsRemarks, String fsStatus, boolean fbConfirm, boolean fbParent) {
            status = fsStatus;
            remarks = fsRemarks;
            passedParent = fbParent;
            return success();
        }

        @SuppressWarnings("unchecked")
        private static JSONObject success() {
            JSONObject loJSON = new JSONObject();
            loJSON.put("result", "success");
            return loJSON;
        }

        void loaded() {
            pnEditMode = EditMode.READY;
        }

        @Override
        public void resetMaster() {
        }

        @Override
        public void resetOthers() {
        }
    }

    private Recording trans;

    @Before
    public void setUp() {
        trans = new Recording();
        Model loMaster = mock(Model.class);
        when(loMaster.getValue("sTransNox")).thenReturn("M00126000001");
        when(loMaster.getTable()).thenReturn("PO_Quotation_Master");
        trans.poMaster = loMaster;
    }

    @Test
    public void nothingLoaded() throws Exception {
        JSONObject loJSON = trans.changeStatus("1", "", "Confirm Transaction", "ok", false, false);
        assertEquals("error", loJSON.get("result"));
        assertEquals("No transacton was loaded.", loJSON.get("message"));
        assertEquals(null, trans.status);
    }

    @Test
    public void withParentSavesTheStatusDirectly() throws Exception {
        trans.setWithParent(true);
        trans.loaded();

        JSONObject loJSON = trans.changeStatus("1", "note", "Confirm Transaction", "Transaction confirmed successfully.", false, false);

        assertEquals("success", loJSON.get("result"));
        assertEquals("Transaction confirmed successfully.", loJSON.get("message"));
        assertEquals("1", trans.status);
        assertEquals("note", trans.remarks);
        assertEquals(null, trans.passedParent);
    }

    @Test
    public void passParentUsesTheParentAwareStatusChange() throws Exception {
        trans.setWithParent(true);
        trans.loaded();

        trans.changeStatus("2", "", "Approve Transaction", "Transaction approved successfully.", true, false);

        assertEquals("2", trans.status);
        assertTrue(trans.passedParent);
    }

    @Test
    public void filtersAndCounts() {
        trans.setSearchBranch("Main");
        trans.setSearchSupplier("ACME");
        assertEquals("Main", trans.getSearchBranch());
        assertEquals("ACME", trans.getSearchSupplier());
        assertEquals(0, trans.getDetailCount());
        assertEquals(0, trans.getDetailRemovedCount());
        assertEquals("2026-11-18", QuotationTransaction.xsDateShort(java.sql.Date.valueOf("2026-11-18")));
        assertFalse(QuotationTransaction.strToDate("2026-11-18").isLeapYear());
    }
}
