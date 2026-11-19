package ph.com.guanzongroup.cas.poqt.services;

import org.guanzon.appdriver.base.GRiderCAS;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Master;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Master;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Supplier;

/**
 * Creates the quotation request and quotation models (one of each per
 * instance), set up with their metadata (Model_*.xml) and table.
 */
public class QuotationModels {

    private final GRiderCAS poGRider;
    private Model_PO_Quotation_Request_Master poRequestMaster;
    private Model_PO_Quotation_Request_Detail poRequestDetail;
    private Model_PO_Quotation_Request_Supplier poRequestSupplier;
    private Model_PO_Quotation_Master poQuotationMaster;
    private Model_PO_Quotation_Detail poQuotationDetail;

    public QuotationModels(GRiderCAS applicationDriver) {
        poGRider = applicationDriver;
    }

    public Model_PO_Quotation_Request_Master POQuotationRequestMaster() {
        if (poGRider == null) {
            System.err.println("QuotationModels.POQuotationRequestMaster: Application driver is not set.");
            return null;
        }
        if (poRequestMaster == null) {
            poRequestMaster = new Model_PO_Quotation_Request_Master();
            poRequestMaster.setApplicationDriver(poGRider);
            poRequestMaster.setXML("Model_PO_Quotation_Request_Master");
            poRequestMaster.setTableName("PO_Quotation_Request_Master");
            poRequestMaster.initialize();
        }
        return poRequestMaster;
    }

    public Model_PO_Quotation_Request_Detail POQuotationRequestDetails() {
        if (poGRider == null) {
            System.err.println("QuotationModels.POQuotationRequestDetails: Application driver is not set.");
            return null;
        }
        if (poRequestDetail == null) {
            poRequestDetail = new Model_PO_Quotation_Request_Detail();
            poRequestDetail.setApplicationDriver(poGRider);
            poRequestDetail.setXML("Model_PO_Quotation_Request_Detail");
            poRequestDetail.setTableName("PO_Quotation_Request_Detail");
            poRequestDetail.initialize();
        }
        return poRequestDetail;
    }

    public Model_PO_Quotation_Request_Supplier POQuotationRequestSupplier() {
        if (poGRider == null) {
            System.err.println("QuotationModels.POQuotationRequestSupplier: Application driver is not set.");
            return null;
        }
        if (poRequestSupplier == null) {
            poRequestSupplier = new Model_PO_Quotation_Request_Supplier();
            poRequestSupplier.setApplicationDriver(poGRider);
            poRequestSupplier.setXML("Model_PO_Quotation_Request_Supplier");
            poRequestSupplier.setTableName("PO_Quotation_Request_Supplier");
            poRequestSupplier.initialize();
        }
        return poRequestSupplier;
    }

    public Model_PO_Quotation_Master POQuotationMaster() {
        if (poGRider == null) {
            System.err.println("QuotationModels.POQuotationMaster: Application driver is not set.");
            return null;
        }
        if (poQuotationMaster == null) {
            poQuotationMaster = new Model_PO_Quotation_Master();
            poQuotationMaster.setApplicationDriver(poGRider);
            poQuotationMaster.setXML("Model_PO_Quotation_Master");
            poQuotationMaster.setTableName("PO_Quotation_Master");
            poQuotationMaster.initialize();
        }
        return poQuotationMaster;
    }

    public Model_PO_Quotation_Detail POQuotationDetails() {
        if (poGRider == null) {
            System.err.println("QuotationModels.POQuotationDetails: Application driver is not set.");
            return null;
        }
        if (poQuotationDetail == null) {
            poQuotationDetail = new Model_PO_Quotation_Detail();
            poQuotationDetail.setApplicationDriver(poGRider);
            poQuotationDetail.setXML("Model_PO_Quotation_Detail");
            poQuotationDetail.setTableName("PO_Quotation_Detail");
            poQuotationDetail.initialize();
        }
        return poQuotationDetail;
    }
}
