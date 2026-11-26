package ph.com.guanzongroup.cas.poqt.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.model;

import java.sql.Timestamp;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.UserRight;
import org.junit.Before;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Master;
import ph.com.guanzongroup.cas.poqt.status.POQuotationStatus;

/** A loaded quotation (READY, OPEN) with a mocked database. */
abstract class QuotationTestBase {

    static final String TRANS_NO = "M00126000101";

    GRiderCAS gr;
    Model_PO_Quotation_Master master;
    TestQuotation qt;

    @Before
    public void setUpQuotation() throws Exception {
        gr = mock(GRiderCAS.class);
        when(gr.getUserID()).thenReturn("U001");
        when(gr.getUserLevel()).thenReturn(UserRight.SUPERVISOR);
        when(gr.getBranchCode()).thenReturn("M001");
        when(gr.getServerDate()).thenReturn(Timestamp.valueOf("2026-11-26 10:00:00"));
        when(gr.Encrypt(anyString())).thenAnswer(i -> "enc:" + i.getArgument(0));
        when(gr.Decrypt(anyString())).thenAnswer(i -> ((String) i.getArgument(0)).replace("enc:", ""));

        master = model(Model_PO_Quotation_Master.class,
                "sTransNox", TRANS_NO,
                "cTranStat", POQuotationStatus.OPEN,
                "sIndstCdx", "09",
                "sCategrCd", "0001",
                "sSourceNo", "",
                "sSupplier", "",
                "sCompnyID", "",
                "sBranchCd", "M001",
                "sReferNox", "",
                "sTermCode", "",
                "sRemarksx", "",
                "cVATAdded", "0");
        QtKit.edit(master, EditMode.READY);
        qt = new TestQuotation(gr, master);
    }
}
