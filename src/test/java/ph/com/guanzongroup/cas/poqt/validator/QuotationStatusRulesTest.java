package ph.com.guanzongroup.cas.poqt.validator;

import static org.junit.Assert.assertEquals;

import org.json.simple.JSONObject;
import org.junit.Test;

/** Same rules as cas-purchasing's validatePOQStatChange / validatePOQReqestStatChange. */
public class QuotationStatusRulesTest {

    private static String check(String fsCurrent, String fsNew) {
        JSONObject loJSON = QuotationStatusRules.validateStatusChange(fsCurrent, fsNew);
        return "success".equals(loJSON.get("result")) ? "ok" : (String) loJSON.get("message");
    }

    @Test
    public void confirm() {
        assertEquals("ok", check("0", "1"));
        assertEquals("ok", check("9", "1"));
        assertEquals("Transaction was already confirmed.", check("1", "1"));
        assertEquals("Transaction was already confirmed.", check("A", "1")); //pending confirm
        assertEquals("Transaction confirmation failed! Please check transaction status.", check("4", "1"));
    }

    @Test
    public void post() {
        assertEquals("ok", check("1", "5"));
        assertEquals("Transaction was already posted.", check("5", "5"));
        assertEquals("Transaction posting failed! Please check transaction status.", check("0", "5"));
    }

    @Test
    public void cancelAndVoid() {
        assertEquals("ok", check("1", "3"));
        assertEquals("ok", check("9", "3"));
        assertEquals("Transaction CANCELLATION failed! Please check transaction status.", check("0", "3"));
        assertEquals("Transaction was already CANCELLED.", check("3", "3"));
        assertEquals("ok", check("0", "4"));
        assertEquals("Transaction tagging to VOID failed! Please check transaction status.", check("1", "4"));
        assertEquals("Transaction was already tagged VOID.", check("4", "4"));
    }

    @Test
    public void unrestricted() {
        assertEquals("ok", check("0", "0"));
        assertEquals("ok", check("1", "2"));
        assertEquals("ok", check("5", "9"));
    }
}
