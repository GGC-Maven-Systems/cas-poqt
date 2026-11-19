package ph.com.guanzongroup.cas.poqt.validator;

import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.poqt.status.POQuotationStatus;
import ph.com.guanzongroup.cas.poqt.utility.Results;

/**
 * Which status changes a quotation or quotation request allows.
 *
 * <p>
 * From cas-purchasing's StatusChangeValidator.validatePOQStatChange and
 * validatePOQReqestStatChange, which had the same rules and messages (both
 * documents use the same status codes). A pending letter counts as its status
 * ('A' = 1 ...). Only CONFIRMED, POSTED, CANCELLED and VOID are restricted.</p>
 */
public final class QuotationStatusRules {

    private QuotationStatusRules() {
    }

    public static JSONObject validateStatusChange(String fsCurrent, String fsNew) {
        String lsCurrent = fsCurrent == null ? "" : fsCurrent.toUpperCase();
        if (lsCurrent.length() == 1 && "ABCDEFGHIJ".contains(lsCurrent)) {
            lsCurrent = String.valueOf(lsCurrent.charAt(0) - 64);
        }
        if (fsNew.equalsIgnoreCase(lsCurrent)) {
            switch (fsNew) {
                case POQuotationStatus.CONFIRMED:
                    return Results.error("Transaction was already confirmed.");
                case POQuotationStatus.POSTED:
                    return Results.error("Transaction was already posted.");
                case POQuotationStatus.CANCELLED:
                    return Results.error("Transaction was already CANCELLED.");
                case POQuotationStatus.VOID:
                    return Results.error("Transaction was already tagged VOID.");
                default:
                    return Results.success();
            }
        }
        switch (fsNew) {
            case POQuotationStatus.CONFIRMED:
                return isAnyOf(lsCurrent, POQuotationStatus.OPEN, POQuotationStatus.RETURNED)
                        ? Results.success()
                        : Results.error("Transaction confirmation failed! Please check transaction status.");
            case POQuotationStatus.POSTED:
                //only a confirmed document can be posted
                return isAnyOf(lsCurrent, POQuotationStatus.CONFIRMED)
                        ? Results.success()
                        : Results.error("Transaction posting failed! Please check transaction status.");
            case POQuotationStatus.CANCELLED:
                return isAnyOf(lsCurrent, POQuotationStatus.CONFIRMED, POQuotationStatus.RETURNED)
                        ? Results.success()
                        : Results.error("Transaction CANCELLATION failed! Please check transaction status.");
            case POQuotationStatus.VOID:
                return isAnyOf(lsCurrent, POQuotationStatus.OPEN, POQuotationStatus.RETURNED)
                        ? Results.success()
                        : Results.error("Transaction tagging to VOID failed! Please check transaction status.");
            default:
                return Results.success();
        }
    }

    private static boolean isAnyOf(String fsValue, String... faAllowed) {
        for (String lsAllowed : faAllowed) {
            if (lsAllowed.equalsIgnoreCase(fsValue)) {
                return true;
            }
        }
        return false;
    }
}
