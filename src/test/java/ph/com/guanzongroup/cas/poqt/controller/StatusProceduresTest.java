package ph.com.guanzongroup.cas.poqt.controller;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONObject;
import org.junit.Test;

/** Each status procedure passes the same status, texts and flags cas-purchasing had. */
public class StatusProceduresTest {

    private static String call(String fsStatus, String fsEvent, String fsMessage, boolean fbPassParent, boolean fbReturn) {
        return fsStatus + "|" + fsEvent + "|" + fsMessage + "|" + fbPassParent + "|" + fbReturn;
    }

    static class Quotation extends POQuotation {

        final List<String> calls = new ArrayList<>();

        @Override
        protected JSONObject changeStatus(String fsStatus, String fsRemarks, String fsEvent, String fsSuccessMessage, boolean fbPassParent, boolean fbReturn) {
            calls.add(call(fsStatus, fsEvent, fsSuccessMessage, fbPassParent, fbReturn));
            return new JSONObject();
        }
    }

    static class Request extends POQuotationRequest {

        final List<String> calls = new ArrayList<>();

        @Override
        protected JSONObject changeStatus(String fsStatus, String fsRemarks, String fsEvent, String fsSuccessMessage, boolean fbPassParent, boolean fbReturn) {
            calls.add(call(fsStatus, fsEvent, fsSuccessMessage, fbPassParent, fbReturn));
            return new JSONObject();
        }
    }

    @Test
    public void quotation() throws Exception {
        Quotation lo = new Quotation();
        lo.ConfirmTransaction("");
        lo.ApproveTransaction("");
        lo.DisApproveTransaction("");
        lo.VoidTransaction("");
        lo.CancelTransaction("");
        lo.PostTransaction("");
        lo.ReturnTransaction("");

        assertEquals("1|Confirm Transaction|Transaction confirmed successfully.|false|false", lo.calls.get(0));
        assertEquals("2|Approve Transaction|Transaction approved successfully.|true|false", lo.calls.get(1));
        assertEquals("3|Disapprove Transaction|Transaction disapproved successfully.|false|false", lo.calls.get(2));
        assertEquals("4|Void Transaction|Transaction voided successfully.|false|false", lo.calls.get(3));
        assertEquals("3|Cancel Transaction|Transaction cancelled successfully.|false|false", lo.calls.get(4));
        assertEquals("5|Post Transaction|Transaction posted successfully.|true|false", lo.calls.get(5));
        assertEquals("9|Return Transaction|Transaction returned successfully.|false|true", lo.calls.get(6));
    }

    @Test
    public void quotationRequest() throws Exception {
        Request lo = new Request();
        lo.ConfirmTransaction("");
        lo.ApproveTransaction("");
        lo.PostTransaction("");
        lo.VoidTransaction("");
        lo.CancelTransaction("");
        lo.DisApprovedTransaction("");
        lo.ReturnTransaction("");

        assertEquals("1|Confirm Transaction|Transaction confirmed successfully.|false|false", lo.calls.get(0));
        assertEquals("2|Approve Transaction|Transaction approved successfully.|false|false", lo.calls.get(1));
        assertEquals("5|Post Transaction|Transaction posted successfully.|false|false", lo.calls.get(2));
        assertEquals("4|Void Transaction|Transaction voided successfully.|false|false", lo.calls.get(3));
        assertEquals("3|Cancel Transaction|Transaction cancelled successfully.|false|false", lo.calls.get(4));
        assertEquals("3|Disapprove Transaction|Transaction dis-approved successfully.|false|false", lo.calls.get(5));
        assertEquals("9|Return Transaction|Transaction returned successfully.|false|true", lo.calls.get(6));
    }
}
