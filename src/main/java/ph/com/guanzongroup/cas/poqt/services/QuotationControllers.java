package ph.com.guanzongroup.cas.poqt.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.LogWrapper;
import ph.com.guanzongroup.cas.poqt.controller.POQuotation;
import ph.com.guanzongroup.cas.poqt.controller.POQuotationRequest;

/**
 * Factory for the quotation controllers (one of each per factory), set up the
 * same way as cas-purchasing's org.guanzon.cas.purchasing.services.QuotationControllers.
 */
public class QuotationControllers {

    private final GRiderCAS poGRider;
    private final LogWrapper poLogWrapper;

    private POQuotationRequest poQuotationRequest;
    private POQuotation poQuotation;

    public QuotationControllers(GRiderCAS applicationDriver, LogWrapper logWrapper) {
        poGRider = applicationDriver;
        poLogWrapper = logWrapper;
    }

    public POQuotationRequest POQuotationRequest() {
        if (!hasDriver("POQuotationRequest")) {
            return null;
        }
        if (poQuotationRequest == null) {
            poQuotationRequest = new POQuotationRequest();
            setUp(poQuotationRequest);
        }
        return poQuotationRequest;
    }

    public POQuotation POQuotation() {
        if (!hasDriver("POQuotation")) {
            return null;
        }
        if (poQuotation == null) {
            poQuotation = new POQuotation();
            setUp(poQuotation);
        }
        return poQuotation;
    }

    private void setUp(org.guanzon.appdriver.agent.services.Transaction foTrans) {
        foTrans.setApplicationDriver(poGRider);
        foTrans.setBranchCode(poGRider.getBranchCode());
        foTrans.setVerifyEntryNo(true);
        foTrans.setWithParent(false);
        foTrans.setLogWrapper(poLogWrapper);
    }

    private boolean hasDriver(String fsMethod) {
        if (poGRider != null) {
            return true;
        }
        if (poLogWrapper != null) {
            poLogWrapper.severe("QuotationControllers." + fsMethod + ": Application driver is not set.");
        }
        return false;
    }
}
