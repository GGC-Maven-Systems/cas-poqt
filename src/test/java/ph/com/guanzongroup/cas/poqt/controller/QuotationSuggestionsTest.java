package ph.com.guanzongroup.cas.poqt.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.parameter.ModelVariant;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import ph.com.guanzongroup.cas.poqt.utility.Suggestion;

/** The suggestion queries (no database: the SQL is checked, rows come from a fake result set). */
public class QuotationSuggestionsTest {

    private GRiderCAS poApp;
    private final List<String> paSQL = new ArrayList<>();
    private String[][] paRows = new String[0][];
    private String[] paColumns = new String[0];

    /** A result set over paColumns / paRows. */
    private ResultSet result() throws SQLException {
        ResultSet loRS = mock(ResultSet.class);
        final int[] lnRow = {-1};
        when(loRS.next()).thenAnswer(i -> ++lnRow[0] < paRows.length);
        when(loRS.getString(anyString())).thenAnswer(i -> {
            int lnCol = Arrays.asList(paColumns).indexOf((String) i.getArgument(0));
            return lnCol < 0 ? null : paRows[lnRow[0]][lnCol];
        });
        return loRS;
    }

    @Before
    public void setUp() throws Exception {
        poApp = mock(GRiderCAS.class);
        when(poApp.executeQuery(anyString())).thenAnswer(i -> {
            paSQL.add(i.getArgument(0));
            return result();
        });
    }

    @Test
    public void branchCodeStartsNameContains() throws Exception {
        paColumns = new String[]{"xCode", "xName", "xDetail"};
        paRows = new String[][]{{"M001", "Main Office", ""}};
        List<Suggestion> laRows = new QuotationSuggestions(poApp).branch("ma");

        String lsSQL = paSQL.get(0);
        assertTrue(lsSQL, lsSQL.contains("FROM Branch"));
        assertTrue(lsSQL, lsSQL.contains("sBranchCd LIKE 'ma%'"));
        assertTrue(lsSQL, lsSQL.contains("sBranchNm LIKE '%ma%'"));
        assertTrue(lsSQL, lsSQL.endsWith("LIMIT 15"));
        assertEquals(1, laRows.size());
        assertEquals("M001", laRows.get(0).getCode());
        assertEquals(Arrays.asList("M001", "Main Office"), laRows.get(0).getKeys());
    }

    @Test
    public void departmentCompanyTerm() throws Exception {
        paColumns = new String[]{"xCode", "xName", "xDetail"};
        QuotationSuggestions loSuggest = new QuotationSuggestions(poApp);
        loSuggest.department("acc");
        loSuggest.company("guan");
        loSuggest.term("30");
        assertTrue(paSQL.get(0), paSQL.get(0).contains("FROM Department") && paSQL.get(0).contains("sDeptName LIKE '%acc%'"));
        assertTrue(paSQL.get(1), paSQL.get(1).contains("FROM Company") && paSQL.get(1).contains("sCompnyNm LIKE '%guan%'"));
        assertTrue(paSQL.get(2), paSQL.get(2).contains("FROM Term") && paSQL.get(2).contains("sTermCode LIKE '30%'"));
    }

    @Test
    public void typedPercentIsLiteral() {
        assertEquals("50\\% off", QuotationSuggestions.escape(" 50% off "));
        assertEquals("a\\_b", QuotationSuggestions.escape("a_b"));
    }

    @Test
    public void modelNeedsBrand() throws Exception {
        try (MockedConstruction<ParamControllers> loParam = mockConstruction(ParamControllers.class)) {
            assertTrue(new QuotationSuggestions(poApp).model("abc", "").isEmpty());
            assertTrue(loParam.constructed().isEmpty());
        }
        verify(poApp, never()).executeQuery(anyString());
    }

    @Test
    public void modelKeysIncludeVariant() throws Exception {
        ModelVariant loModel = mock(ModelVariant.class);
        when(loModel.getSQ_Browse()).thenReturn("SELECT a.sDescript, IFNULL(b.sModelCde, '') xModelCde, IFNULL(b.sDescript, '') xModelNme"
                + " FROM Model_Variant a LEFT JOIN Model b ON a.sModelIDx = b.sModelIDx");
        paColumns = new String[]{"xModelCde", "xModelNme", "sDescript"};
        paRows = new String[][]{{"VIOS", "Vios", "1.3 E MT"}};
        try (MockedConstruction<ParamControllers> loParam = mockConstruction(ParamControllers.class,
                (m, c) -> when(m.ModelVariant()).thenReturn(loModel))) {
            List<Suggestion> laRows = new QuotationSuggestions(poApp).model("vi", "B001");
            assertEquals(Arrays.asList("VIOS", "Vios", "1.3 E MT"), laRows.get(0).getKeys());
        }
        assertTrue(paSQL.get(0), paSQL.get(0).contains("b.sBrandIDx = 'B001'"));
    }

    @Test
    public void inventoryFiltersLikeTheSearch() throws Exception {
        Inventory loInventory = mock(Inventory.class);
        when(loInventory.getSQ_Browse()).thenReturn("SELECT a.sStockIDx, a.sBarCodex, a.sDescript, '' xBrandNme, '' xModelNme,"
                + " '' xMeasurNm FROM Inventory a");
        paColumns = new String[]{"sBarCodex", "sDescript", "xMeasurNm", "xBrandNme", "xModelNme"};
        paRows = new String[][]{{"4800001", " Bond Paper A4 ", "REAM", "Hard Copy", ""}};
        try (MockedConstruction<InvControllers> loInv = mockConstruction(InvControllers.class,
                (m, c) -> when(m.Inventory()).thenReturn(loInventory))) {
            List<Suggestion> laRows = new QuotationSuggestions(poApp).inventory("bond", "0001", "0101", "", "M01");
            Suggestion loRow = laRows.get(0);
            assertEquals("REAM", loRow.getUnit());
            assertEquals("Hard Copy", loRow.getDetail());
            assertEquals(Arrays.asList("4800001", "Bond Paper A4"), loRow.getKeys());
        }
        String lsSQL = paSQL.get(0);
        assertTrue(lsSQL, lsSQL.contains("a.sCategCd1 = '0001'") && lsSQL.contains("a.sCategCd2 = '0101'"));
        assertTrue(lsSQL, !lsSQL.contains("a.sBrandIDx =") && lsSQL.contains("a.sModelIDx = 'M01'"));
        assertTrue(lsSQL, lsSQL.contains("GROUP BY a.sStockIDx") && lsSQL.endsWith("LIMIT 15"));
    }

    @Test
    public void suggestionDefaultKeys() {
        Suggestion loRow = new Suggestion("T30", "30 days", null, null);
        assertEquals(Arrays.asList("T30", "30 days"), loRow.getKeys());
        assertEquals("", loRow.getUnit());
    }
}
