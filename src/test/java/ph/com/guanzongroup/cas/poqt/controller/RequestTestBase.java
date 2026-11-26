package ph.com.guanzongroup.cas.poqt.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.model;

import java.sql.Timestamp;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.constant.EditMode;
import org.junit.Before;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Master;
import ph.com.guanzongroup.cas.poqt.status.POQuotationRequestStatus;

/** A loaded quotation request (READY, OPEN) with a mocked database. */
abstract class RequestTestBase {

    static final String TRANS_NO = "M00126000001";

    GRiderCAS gr;
    Model_PO_Quotation_Request_Master master;
    TestRequest req;

    @Before
    public void setUpRequest() throws Exception {
        gr = mock(GRiderCAS.class);
        when(gr.getUserID()).thenReturn("U001");
        when(gr.getBranchCode()).thenReturn("M001");
        when(gr.getBranchName()).thenReturn("Main Office");
        when(gr.getServerDate()).thenReturn(Timestamp.valueOf("2026-11-26 10:00:00"));
        when(gr.Encrypt(anyString())).thenAnswer(i -> "enc:" + i.getArgument(0));
        when(gr.Decrypt(anyString())).thenAnswer(i -> ((String) i.getArgument(0)).replace("enc:", ""));
        when(gr.getProductID()).thenReturn("gRider");

        master = model(Model_PO_Quotation_Request_Master.class,
                "sTransNox", TRANS_NO,
                "cTranStat", POQuotationRequestStatus.OPEN,
                "sIndstCdx", "09",
                "sCategrCd", "0001",
                "sCategCd2", "0101",
                "sReferNox", "REF1",
                "sDestinat", "M001",
                "sRemarksx", "",
                "dTransact", Timestamp.valueOf("2026-11-26 00:00:00"));
        QtKit.edit(master, EditMode.READY);
        req = new TestRequest(gr, master);
    }
}
