package ph.com.guanzongroup.cas.poqt.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Date;
import org.json.simple.JSONObject;
import org.junit.Test;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Master;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Master;

/** Which validator each industry gets, and the saving rules. */
public class QuotationValidatorsTest {

    @Test
    public void industries() {
        assertTrue(QuotationValidators.quotation("05") instanceof POQuotation_General);
        assertTrue(QuotationValidators.quotation("09") instanceof POQuotation_General);
        for (String lsIndustry : new String[]{"01", "02", "03", "04", "07", "99", null}) {
            assertEquals(POQuotationValidator.class, QuotationValidators.quotation(lsIndustry).getClass());
            assertEquals(POQuotationRequestValidator.class, QuotationValidators.quotationRequest(lsIndustry).getClass());
        }
    }

    /** A quotation that passes every saving rule, without a source. */
    private static Model_PO_Quotation_Master quotationWithoutSource() {
        Model_PO_Quotation_Master lo = mock(Model_PO_Quotation_Master.class);
        Date ldDate = Date.valueOf("2026-11-18");
        when(lo.getTransactionDate()).thenReturn(ldDate);
        when(lo.getValidityDate()).thenReturn(ldDate);
        when(lo.getReferenceDate()).thenReturn(ldDate);
        when(lo.getIndustryId()).thenReturn("02");
        when(lo.getCompanyId()).thenReturn("0001");
        when(lo.getCategoryCode()).thenReturn("0002");
        when(lo.getBranchCode()).thenReturn("M001");
        when(lo.getSourceNo()).thenReturn("");
        when(lo.getSourceCode()).thenReturn("");
        when(lo.getSupplierId()).thenReturn("C001");
        when(lo.getTerm()).thenReturn("T001");
        when(lo.getGrossAmount()).thenReturn(100.0);
        when(lo.getTransactionTotal()).thenReturn(100.0);
        when(lo.getPrepared()).thenReturn("user");
        when(lo.getModifyingId()).thenReturn("user");
        when(lo.getTransactionStatus()).thenReturn("0");
        return lo;
    }

    private static JSONObject save(String fsIndustry, Object foMaster) {
        org.guanzon.appdriver.iface.GValidator lo = QuotationValidators.quotation(fsIndustry);
        lo.setTransactionStatus("0");
        lo.setMaster(foMaster);
        return lo.validate();
    }

    @Test
    public void sourceRequiredExceptGeneral() {
        JSONObject loJSON = save("02", quotationWithoutSource());
        assertEquals("error", loJSON.get("result"));
        assertEquals("Source No is not set.", loJSON.get("message"));

        assertEquals("success", save("09", quotationWithoutSource()).get("result"));
    }

    @Test
    public void missingSupplier() {
        Model_PO_Quotation_Master lo = quotationWithoutSource();
        when(lo.getSupplierId()).thenReturn("");
        assertEquals("Supplier is not set.", save("09", lo).get("message"));
    }

    @Test
    public void requestRules() {
        Model_PO_Quotation_Request_Master lo = mock(Model_PO_Quotation_Request_Master.class);
        when(lo.getTransactionDate()).thenReturn(Date.valueOf("2026-11-18"));
        when(lo.getIndustryId()).thenReturn("02");
        when(lo.getCategoryCode()).thenReturn("0002");
        when(lo.getBranchCode()).thenReturn("M001");
        when(lo.getCategoryLevel2()).thenReturn("0010");
        when(lo.getDestination()).thenReturn("M001");
        when(lo.getDepartmentId()).thenReturn("");
        when(lo.getTransactionStatus()).thenReturn("0");

        org.guanzon.appdriver.iface.GValidator loValidator = QuotationValidators.quotationRequest("02");
        loValidator.setTransactionStatus("0");
        loValidator.setMaster(lo);
        assertEquals("Department is not set.", loValidator.validate().get("message"));

        //confirming an already confirmed request
        when(lo.getTransactionStatus()).thenReturn("1");
        loValidator.setTransactionStatus("1");
        assertEquals("Transaction was already confirmed.", loValidator.validate().get("message"));
    }
}
