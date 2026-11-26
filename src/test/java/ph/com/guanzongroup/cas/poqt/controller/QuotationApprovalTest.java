package ph.com.guanzongroup.cas.poqt.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.err;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.json;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.model;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.ok;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.row;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.rows;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.MatrixAuthChecker;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.base.GRiderCAS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Master;
import ph.com.guanzongroup.cas.poqt.services.QuotationModels;

/** Authority checks of the quotation transactions (no parent: the user's rights are checked). */
public class QuotationApprovalTest extends QuotationTestBase {

    /** A quotation that records statusChange and returns a set validator result. */
    static class Approving extends TestQuotation {

        final List<String> statuses = new ArrayList<>();
        JSONObject entry = ok();
        JSONObject change = ok();

        Approving(GRiderCAS foGRider, Model_PO_Quotation_Master foMaster) {
            super(foGRider, foMaster);
            pbWthParent = false;
        }

        @Override
        protected JSONObject isEntryOkay(String fsStatus) {
            return entry;
        }

        @Override
        protected JSONObject statusChange(String fsTable, String fsTransNo, String fsRemarks, String fsStatus, boolean fbConfirm) {
            statuses.add(fsStatus);
            return copy(change);
        }

        @Override
        protected JSONObject statusChange(String fsTable, String fsTransNo, String fsRemarks, String fsStatus, boolean fbConfirm, boolean fbParent) {
            statuses.add(fsStatus + (fbParent ? "p" : ""));
            return copy(change);
        }

        /** The callers add to the result, so every call gets its own. */
        private static JSONObject copy(JSONObject foResult) {
            return json("result", foResult.get("result"), "message", foResult.get("message"));
        }
    }

    private Approving ap;
    private MockedStatic<ShowMessageFX> poMessages;
    private MockedStatic<ShowDialogFX> poDialog;

    @Before
    public void setUpApproving() {
        ap = new Approving(gr, master);
        poMessages = mockStatic(ShowMessageFX.class);
        poDialog = mockStatic(ShowDialogFX.class);
        approver(json("result", "success", "sUserIDxx", "U2", "nUserLevl", "16"));
    }

    @After
    public void closeStatics() {
        poMessages.close();
        poDialog.close();
    }

    private void approver(JSONObject foResult) {
        poDialog.when(() -> ShowDialogFX.getUserApproval(any(GRiderCAS.class))).thenReturn(foResult);
    }

    private static MockedConstruction<ActionAuthManager> rights(String fsCurrent, String fsApprover) {
        return mockConstruction(ActionAuthManager.class, (m, c) -> {
            when(m.isAuthorized()).thenReturn(json("result", fsCurrent, "warning", "No rights."));
            when(m.isAuthorized(anyString(), anyInt())).thenReturn(json("result", fsApprover, "warning", "No rights."));
        });
    }

    private static JSONObject matrix() {
        JSONArray laMatrix = new JSONArray();
        laMatrix.add(json("sAuthType", "PQ"));
        return json("result", "success", "matrix", laMatrix);
    }

    // =====================================================================
    // seekApproval through the procedures
    // =====================================================================
    @Test
    public void proceduresCheckTheUsersRights() throws Exception {
        try (MockedConstruction<ActionAuthManager> loAuth = rights("true", "true");
                MockedConstruction<QuotationModels> loModels = mockConstruction(QuotationModels.class, (m, c) -> {
                    when(m.POQuotationMaster()).thenAnswer(i -> model(Model_PO_Quotation_Master.class));
                    when(m.POQuotationDetails()).thenAnswer(i -> model(Model_PO_Quotation_Detail.class));
                })) {
            assertEquals("success", ap.NewTransaction().get("result"));
            assertEquals("success", ap.UpdateTransaction().get("result"));
            assertEquals("success", ap.OpenTransaction("T1").get("result"));
            assertEquals("success", ap.SaveTransaction().get("result"));
        }
        assertEquals("[new, update, open T1, save]", ap.calls.toString());
    }

    @Test
    public void proceduresStopWithoutRights() throws Exception {
        approver(err("Cancelled"));
        try (MockedConstruction<ActionAuthManager> loAuth = rights("false", "false")) {
            assertEquals("Cancelled", ap.NewTransaction().get("message"));
            assertEquals("Cancelled", ap.UpdateTransaction().get("message"));
            assertEquals("Cancelled", ap.OpenTransaction("T1").get("message"));
        }
        assertTrue(ap.calls.isEmpty());
    }

    @Test
    public void approvingOfficer() throws Exception {
        try (MockedConstruction<ActionAuthManager> loAuth = rights("false", "false")) {
            assertEquals("User is not an authorized approving officer..", ap.seekApproval().get("message"));
        }
        try (MockedConstruction<ActionAuthManager> loAuth = rights("false", "true")) {
            assertEquals("success", ap.seekApproval().get("result"));
            verify(loAuth.constructed().get(0)).isAuthorized("U2", 16);
        }
    }

    // =====================================================================
    // changeStatus without a parent
    // =====================================================================
    @Test
    public void validatorStopsTheStatusChange() throws Exception {
        ap.entry = err("Supplier is not set.");
        assertEquals("Supplier is not set.", ap.ConfirmTransaction("").get("message"));
        assertTrue(ap.statuses.isEmpty());
    }

    @Test
    public void noMatrixChecksTheUsersRights() throws Exception {
        try (MockedConstruction<ActionAuthManager> loAuth = rights("true", "true")) {
            assertEquals("Transaction confirmed successfully.", ap.ConfirmTransaction("ok").get("message"));
            assertEquals("Transaction approved successfully.", ap.ApproveTransaction("ok").get("message"));
        }
        assertEquals("[1, 2]", ap.statuses.toString());

        approver(err("Cancelled"));
        try (MockedConstruction<ActionAuthManager> loAuth = rights("false", "false")) {
            assertEquals("Cancelled", ap.VoidTransaction("").get("message"));
        }
    }

    @Test
    public void statusChangeFailure() throws Exception {
        ap.change = err("Locked");
        try (MockedConstruction<ActionAuthManager> loAuth = rights("true", "true")) {
            assertEquals("Locked", ap.CancelTransaction("").get("message"));
        }
    }

    @Test
    public void matrixLoadFailure() throws Exception {
        ap.entry = matrix();
        try (MockedConstruction<MatrixAuthChecker> loCheck = mockConstruction(MatrixAuthChecker.class,
                (m, c) -> when(m.loadAuth()).thenReturn(err("No matrix")))) {
            assertEquals("No matrix", ap.PostTransaction("").get("message"));
        }
    }

    @Test
    public void matrixApprovedPostsTheAuthorization() throws Exception {
        ap.entry = matrix();
        try (MockedConstruction<MatrixAuthChecker> loCheck = mockConstruction(MatrixAuthChecker.class, (m, c) -> {
            when(m.loadAuth()).thenReturn(ok());
            when(m.isAuthOkay()).thenReturn(true);
        })) {
            assertEquals("success", ap.PostTransaction("").get("result"));
            verify(loCheck.constructed().get(0)).postAuth();
        }
        assertEquals("[5]", ap.statuses.toString());
    }

    @Test
    public void matrixPendingSavesTheLetter() throws Exception {
        ap.entry = matrix();
        try (MockedConstruction<MatrixAuthChecker> loCheck = mockConstruction(MatrixAuthChecker.class, (m, c) -> {
            when(m.loadAuth()).thenReturn(ok());
            when(m.authTrans(anyString(), anyString())).thenReturn(ok());
        })) {
            assertEquals("matrix", ap.ApproveTransaction("").get("result"));
            assertEquals("success", ap.ReturnTransaction("").get("result"));
            verify(loCheck.constructed().get(0)).authTrans("PQ", "U001");
            verify(loCheck.constructed().get(1), never()).postAuth();
        }
        assertEquals("[B, I]", ap.statuses.toString());
        verify(gr, org.mockito.Mockito.times(2)).commitTrans();

        ap.change = err("Locked");
        try (MockedConstruction<MatrixAuthChecker> loCheck = mockConstruction(MatrixAuthChecker.class, (m, c) -> {
            when(m.loadAuth()).thenReturn(ok());
            when(m.isAllowSys()).thenReturn(true);
        })) {
            assertEquals("Locked", ap.ApproveTransaction("").get("message"));
        }
        verify(gr).rollbackTrans();
    }

    @Test
    public void matrixAsksAnApprovingOfficer() throws Exception {
        ap.entry = matrix();
        try (MockedConstruction<MatrixAuthChecker> loCheck = mockConstruction(MatrixAuthChecker.class, (m, c) -> {
            when(m.loadAuth()).thenReturn(ok());
            when(m.isAuthOkay()).thenReturn(false, true);
            when(m.authTrans(anyString(), eq("U001"))).thenReturn(err("Not yours"));
            when(m.authTrans(anyString(), eq("U2"))).thenReturn(ok());
        })) {
            assertEquals("success", ap.ApproveTransaction("").get("result"));
        }
        assertEquals("[2]", ap.statuses.toString()); //no parent: pbWthParent (false) is passed

        try (MockedConstruction<MatrixAuthChecker> loCheck = mockConstruction(MatrixAuthChecker.class, (m, c) -> {
            when(m.loadAuth()).thenReturn(ok());
            when(m.authTrans(anyString(), anyString())).thenReturn(err("Not authorized"));
        })) {
            assertEquals("Not authorized", ap.ApproveTransaction("").get("message"));
            approver(err("Cancelled"));
            assertEquals("Cancelled", ap.ApproveTransaction("").get("message"));
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    @Test
    public void contextAndSysUser() throws Exception {
        ap.setIndustryId("02");
        ap.setCompanyId("C9");
        ap.setCategoryId("0005");
        ap.saveComplete();
        when(gr.executeQuery(anyString())).thenReturn(rows());
        ap.loadPOQuotationList("", "", "", "", "", false);
        verify(gr).executeQuery(org.mockito.ArgumentMatchers.contains("a.sIndstCdx = '02' AND a.sCategrCd = '0005'"));

        when(gr.executeQuery(anyString())).thenReturn(rows(row("sCompnyNm", "Ana")));
        assertEquals("Ana", ap.getSysUser("U1"));
        when(gr.executeQuery(anyString())).thenReturn(rows());
        assertEquals("", ap.getSysUser("U1"));

        ap.computeDiscount(0); //poJSON is set
        ResultSet loBroken = mock(ResultSet.class);
        when(loBroken.next()).thenThrow(new SQLException("lost"));
        when(gr.executeQuery(anyString())).thenReturn(loBroken);
        assertEquals("", ap.getSysUser("U1"));
        assertFalse(ap.calls.contains("save"));
    }
}
