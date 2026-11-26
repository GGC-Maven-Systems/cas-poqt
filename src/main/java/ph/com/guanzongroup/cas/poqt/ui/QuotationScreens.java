package ph.com.guanzongroup.cas.poqt.ui;

import java.util.Arrays;
import java.util.List;
import org.guanzon.appdriver.agent.screen.ScreenAction;
import org.guanzon.appdriver.agent.screen.ScreenEntry;
import org.guanzon.appdriver.agent.screen.ScreenProvider;

/**
 * The screens cas-poqt offers to cas-app: PO Quotation Request and PO
 * Quotation.
 *
 * <p>
 * Registered in
 * {@code META-INF/services/org.guanzon.appdriver.agent.screen.ScreenProvider}.
 * The menu keys are the integsys controllers the existing menu rows
 * (xxxSysMenuOthers.sObjectNm) point to, so the same menu opens the integsys
 * screens in integsys and these in cas-app.</p>
 *
 * <p>
 * The actions are the buttons user access can allow or deny; each screen
 * hides the denied ones with ScreenAccess in initialize(). Keep the keys the
 * same in both places.</p>
 */
public class QuotationScreens implements ScreenProvider {

    private static final String VIEWS = "/ph/com/guanzongroup/cas/poqt/ui/";
    private static final String INTEGSYS = "ph.com.guanzongroup.integsys.views.";

    private static final ScreenAction NEW = ScreenAction.of("NEW", "New");
    private static final ScreenAction UPDATE = ScreenAction.of("UPDATE", "Update");
    private static final ScreenAction CONFIRM = ScreenAction.of("CONFIRM", "Confirm");
    private static final ScreenAction APPROVE = ScreenAction.of("APPROVE", "Approve");
    private static final ScreenAction DISAPPROVE = ScreenAction.of("DISAPPROVE", "Disapprove");
    private static final ScreenAction RETURN = ScreenAction.of("RETURN", "Return");
    private static final ScreenAction VOID = ScreenAction.of("VOID", "Void");
    private static final ScreenAction EXPORT = ScreenAction.of("EXPORT", "Export");
    private static final ScreenAction ATTACH = ScreenAction.of("ATTACH", "Add / remove attachments");

    @Override
    public String getModule() {
        return "cas-poqt";
    }

    @Override
    public List<ScreenEntry> getScreens() {
        return Arrays.asList(
                //PO Quotation Request
                screen(POQuotationRequest_EntryController.class, "POQuotationRequest_Entry")
                        .withActions(NEW, UPDATE, VOID),
                screen(POQuotationRequest_ConfirmationController.class, "POQuotationRequest_Confirmation")
                        .withActions(UPDATE, CONFIRM, VOID, RETURN),
                screen(POQuotationRequest_ApprovalController.class, "POQuotationRequest_Approval")
                        .withActions(UPDATE, APPROVE, DISAPPROVE, RETURN, EXPORT),
                screen(POQuotationRequest_HistoryController.class, "POQuotationRequest_History"),
                screen(POQuotationRequest_ApprovalHistoryController.class, "POQuotationRequest_ApprovalHistory")
                        .withActions(EXPORT),
                //PO Quotation
                screen(POQuotation_EntryController.class, "POQuotation_Entry")
                        .withActions(NEW, UPDATE, VOID, ATTACH),
                screen(POQuotation_ConfirmationController.class, "POQuotation_Confirmation")
                        .withActions(UPDATE, CONFIRM, VOID, RETURN, ATTACH),
                screen(POQuotation_ApprovalController.class, "POQuotation_Approval")
                        .withActions(APPROVE, DISAPPROVE, RETURN),
                screen(POQuotation_HistoryController.class, "POQuotation_History"));
    }

    /** FXML VIEWS + name + ".fxml"; menu key = the integsys controller of the same name. */
    private static ScreenEntry screen(Class<? extends org.guanzon.appdriver.agent.screen.ScreenInterface> foController,
            String fsName) {
        return ScreenEntry.of(foController, VIEWS + fsName + ".fxml", INTEGSYS + fsName + "Controller");
    }
}
