package ph.com.guanzongroup.cas.poqt.validator;

import org.guanzon.appdriver.iface.GValidator;

/**
 * The validator each industry uses (replaces cas-purchasing's
 * POQuotationValidatorFactory and POQuotationRequestValidatorFactory).
 *
 * <p>
 * Same industry codes as the old factories: 01 Mobile Phone, 02 Motorcycle,
 * 03 Vehicle, 04 Monarch, 05 Los Pedritos, 07 Appliances, 09 General. Only
 * Los Pedritos / General have their own quotation rules
 * ({@link POQuotation_General}); every other industry uses the shared rules.
 * The old factories returned null (a crash) for any other code; the shared
 * rules apply to those now.</p>
 */
public final class QuotationValidators {

    private QuotationValidators() {
    }

    public static GValidator quotation(String fsIndustryId) {
        if ("05".equals(fsIndustryId) || "09".equals(fsIndustryId)) {
            return new POQuotation_General();
        }
        return new POQuotationValidator();
    }

    public static GValidator quotationRequest(String fsIndustryId) {
        return new POQuotationRequestValidator();
    }
}
