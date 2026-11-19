package ph.com.guanzongroup.cas.poqt.status;

/**
 * Transaction status codes (cTranStat) of the PO Quotation Request (same values as
 * org.guanzon.cas.purchasing.status.POQuotationRequestStatus).
 *
 * <p>
 * 0 open, 1 confirmed, 2 approved, 3 cancelled, 4 void, 5 posted (ordered),
 * 9 returned. A pending letter (A = 1, B = 2, ...) is a status waiting for
 * matrix approval.</p>
 */
public final class POQuotationRequestStatus {

    public static final String OPEN = "0";
    public static final String CONFIRMED = "1";
    public static final String APPROVED = "2";
    public static final String CANCELLED = "3";
    public static final String VOID = "4";
    public static final String POSTED = "5";
    public static final String RETURNED = "9";

    private POQuotationRequestStatus() {
    }

    /** Inventory ledger direction. */
    public static final class Reverse {

        public static final String INCLUDE = "+";
        public static final String EXCLUDE = "-";

        private Reverse() {
        }
    }
}
