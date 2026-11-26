package ph.com.guanzongroup.cas.poqt.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.model;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.row;
import static ph.com.guanzongroup.cas.poqt.controller.QtKit.rows;

import java.io.File;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Detail;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Master;
import ph.com.guanzongroup.cas.poqt.model.Model_PO_Quotation_Request_Supplier;
import ph.com.guanzongroup.cas.poqt.services.QuotationModels;
import ph.com.guanzongroup.cas.poqt.status.POQuotationRequestStatus;

/** Export to PDF, report list, print, status history (JasperReports mocked). */
public class RequestReportTest extends RequestTestBase {

    private File poConfig;
    private File poTemp;
    private String psOldConfig;
    private String psOldTemp;

    @Before
    public void folders() throws Exception {
        poConfig = Files.createTempDirectory("cfg").toFile();
        poTemp = Files.createTempDirectory("tmp").toFile();
        new File(poConfig, "Reports").mkdirs();
        psOldConfig = System.getProperty("sys.default.path.config");
        psOldTemp = System.getProperty("sys.default.path.temp");
        System.setProperty("sys.default.path.config", poConfig.getPath());
        System.setProperty("sys.default.path.temp", poTemp.getPath());

        //getSysUser, getPreparedDate, getApprover
        when(gr.executeQuery(anyString())).thenAnswer(i -> {
            String lsSQL = i.getArgument(0);
            if (lsSQL.contains("xxxSysUser")) {
                return rows(row("sCompnyNm", "Juan Dela Cruz"));
            } else if (lsSQL.contains("Transaction_Status_History")) {
                return rows(row("sModified", "enc:U0000000002", "dModified", LocalDateTime.of(2026, 11, 26, 9, 30)));
            } else if (lsSQL.contains("xxxAuditLogMaster")) {
                return rows(row("sModified", "U003", "dModified", LocalDateTime.of(2026, 11, 25, 8, 0)));
            }
            return rows(row("dPrepared", LocalDateTime.of(2026, 11, 26, 8, 15)));
        });
    }

    @After
    public void restore() {
        restore("sys.default.path.config", psOldConfig);
        restore("sys.default.path.temp", psOldTemp);
    }

    private static void restore(String fsKey, String fsValue) {
        if (fsValue == null) {
            System.clearProperty(fsKey);
        } else {
            System.setProperty(fsKey, fsValue);
        }
    }

    private Model_PO_Quotation_Request_Supplier exportable() throws Exception {
        master.setValue("sPrepared", "enc:U0000000001");
        master.setTransactionStatus(POQuotationRequestStatus.APPROVED);
        master.setExpectedPurchaseDate(java.sql.Date.valueOf("2026-12-01"));
        master.Department().setValue("sDeptName", "Procurement");
        master.Destination().setValue("sBranchNm", "Main Office");
        Model_PO_Quotation_Request_Detail loItem = req.line("sStockIDx", "S1", "sDescript", "Pen", "nQuantity", 2.0);
        loItem.Inventory().Brand().setValue("sDescript", "Hbw");
        loItem.Inventory().Model().setValue("sDescript", "");
        req.line("sStockIDx", "", "sDescript", "Free text", "nQuantity", 1.0);
        req.line("sStockIDx", "S2", "sDescript", "Excluded", "cReversex", "-");
        Model_PO_Quotation_Request_Supplier loSupplier = req.supplier("sSupplier", "SUP1", "sCompnyID", "C1");
        loSupplier.Company().setValue("sCompnyNm", "Guanzon");
        loSupplier.Supplier().setValue("sCompnyNm", "Supplier Inc");
        req.supplier("sSupplier", "SUP2", "cReversex", "-");
        return loSupplier;
    }

    @Test
    public void exportNeedsSupplier() {
        assertEquals("No supplier to be export.", req.exportFile().get("message"));
    }

    @Test
    public void exportNeedsReportFile() throws Exception {
        exportable();
        assertTrue(((String) req.exportFile().get("message")).startsWith("Jasper file does not exist."));
    }

    @Test
    public void exportWritesPdfPerSupplier() throws Exception {
        exportable();
        new File(poConfig, "Reports/POQuotationRequest.jrxml").createNewFile();
        JasperReport loReport = mock(JasperReport.class);
        JasperPrint loPrint = mock(JasperPrint.class);
        try (MockedStatic<JasperCompileManager> loCompile = mockStatic(JasperCompileManager.class);
                MockedStatic<JasperFillManager> loFill = mockStatic(JasperFillManager.class);
                MockedStatic<JasperExportManager> loExport = mockStatic(JasperExportManager.class)) {
            loCompile.when(() -> JasperCompileManager.compileReport(anyString())).thenReturn(loReport);
            loFill.when(() -> JasperFillManager.fillReport(any(JasperReport.class), anyMap(), any(JRDataSource.class))).thenReturn(loPrint);
            JSONObject loResult = req.exportFile();
            assertEquals("success", loResult.get("result"));
            assertTrue(((String) loResult.get("message")).startsWith("Export complete."));
            loExport.verify(() -> JasperExportManager.exportReportToPdfFile(any(JasperPrint.class),
                    endsWith(TRANS_NO + " - Guanzon Supplier Inc.pdf")));
            loFill.verify(() -> JasperFillManager.fillReport(any(JasperReport.class),
                    org.mockito.ArgumentMatchers.argThat((java.util.Map<String, Object> m) ->
                            "Main Office - Procurement".equals(m.get("sDestination"))
                            && ((String) m.get("sApprovedBy")).startsWith("Approved by: Juan Dela Cruz 11-26-2026")
                            && ((String) m.get("sPreparedBy")).contains("11-26-2026 08:15:00")
                            && "December 01, 2026".equals(m.get("dExpPrchseDte"))),
                    any(JRDataSource.class)));
        }
        assertTrue(new File(poTemp, "Export").isDirectory());
    }

    @Test
    public void exportReportError() throws Exception {
        master.setValue("sPrepared", "U001");
        req.supplier("sSupplier", "SUP1", "sCompnyID", "C1");
        new File(poConfig, "Reports/POQuotationRequest.jrxml").createNewFile();
        try (MockedStatic<JasperCompileManager> loCompile = mockStatic(JasperCompileManager.class)) {
            loCompile.when(() -> JasperCompileManager.compileReport(anyString())).thenThrow(new JRException("bad jrxml"));
            assertEquals("error", req.exportFile().get("result"));
        }
    }

    @Test
    public void approverAndPreparedDate() throws Exception {
        JSONObject loApprover = req.getApprover();
        assertEquals("success", loApprover.get("result"));
        assertEquals("11-26-2026 09:30:00", loApprover.get("dModified"));
        assertEquals("11-26-2026 08:15:00", req.getPreparedDate());

        when(gr.executeQuery(anyString())).thenReturn(rows());
        assertEquals("error", req.getApprover().get("result"));
        assertEquals("", req.getPreparedDate());
    }

    @Test
    public void entryBy() throws Exception {
        JSONObject loEntry = req.getEntryBy();
        assertEquals("Juan Dela Cruz", loEntry.get("sCompnyNm"));
        assertEquals("11-25-2026 08:00:00", loEntry.get("sEntryDte"));

        when(gr.executeQuery(contains("xxxAuditLogMaster"))).thenReturn(rows(row("sModified", "enc:U0000000009", "dModified",
                LocalDateTime.of(2026, 11, 24, 7, 0))));
        assertEquals("11-24-2026 07:00:00", req.getEntryBy().get("sEntryDte"));

        when(gr.executeQuery(contains("xxxAuditLogMaster"))).thenReturn(rows(row("sModified", null, "dModified", null)));
        assertEquals("", req.getEntryBy().get("sCompnyNm"));
    }

    @Test
    public void statusHistoryNames() throws Exception {
        CachedRowSet loRows = RowSetProvider.newFactory().createCachedRowSet();
        RowSetMetaDataImpl loMeta = new RowSetMetaDataImpl();
        loMeta.setColumnCount(1);
        loMeta.setColumnName(1, "cRefrStat");
        loMeta.setColumnLabel(1, "cRefrStat");
        loMeta.setColumnType(1, Types.VARCHAR);
        loRows.setMetaData(loMeta);
        String[] laCodes = {"", "0", "1", "5", "3", "4", "2", "9", "A", "B", "C", "D", "E", "F", "I"};
        for (String lsCode : laCodes) {
            loRows.moveToInsertRow();
            loRows.updateString(1, lsCode);
            loRows.insertRow();
            loRows.moveToCurrentRow();
        }
        req.statusHistory = loRows;
        req.ShowStatusHistory();

        assertEquals("Purchase Order Quotation Request", req.shownHistory[0]);
        assertEquals(TRANS_NO, req.shownHistory[1]);
        assertEquals("Juan Dela Cruz", req.shownHistory[2]);
        loRows.beforeFirst();
        StringBuilder loNames = new StringBuilder();
        while (loRows.next()) {
            loNames.append(loRows.getString(1)).append(',');
        }
        //the in-memory row set lists inserted rows last-first; a letter is a pending status (A = 1, B = 2, ...),
        //"F" (6) is no status and stays
        assertEquals("RETURNED,F,POSTED,VOID,CANCELLED,APPROVED,CONFIRMED,RETURNED,APPROVED,VOID,CANCELLED,POSTED,CONFIRMED,OPEN,-,",
                loNames.toString());
    }

    // =====================================================================
    // Report list and print
    // =====================================================================
    private ResultSet reportRows() {
        return rows(row("sTransNox", TRANS_NO, "dTransact", java.sql.Date.valueOf("2026-11-26"), "sReferNox", "REF1",
                "xBranchNm", "Main", "xDestintn", "Main", "xDeptName", "Procurement", "xCompanyx", "Guanzon",
                "xCategLv2", "Paper", "xSupplier", "Supplier Inc", "xTermCode", "30 days", "xEntryNox", 1,
                "xDetDescx", "Pen", "xUnitPrce", 10.0, "xQuantity", 2.0, "xDetTotal", 20.0, "xTranTotal", 20.0,
                "xTranStat", "2", "dModified", Timestamp.valueOf("2026-11-26 10:00:00"), "nOther", 5L));
    }

    @Test
    public void loadReportDetailed() throws Exception {
        req.setSearchCompany("Gu");
        req.setSearchIndustry("Gen");
        req.setSearchCategory("Pap");
        req.setSearchBranch("Main");
        req.setSearchDestination("Main");
        req.setSearchDepartment("Proc");
        req.setSearchSupplier("Sup");
        when(gr.executeQuery(anyString())).thenReturn(reportRows());
        JSONObject loResult = req.loadReport(false, "2026-11-01", "2026-11-30", "APPROVED");
        assertEquals("success", loResult.get("result"));
        JSONObject loRecord = (JSONObject) ((JSONArray) loResult.get("data")).get(0);
        assertEquals("Approved", loRecord.get("xTranStat"));
        assertEquals(20.0, loRecord.get("xTranTotal"));
        List<?> laReport = req.getReport();
        assertEquals(1, laReport.size());
        POQuotationRequest.TransactionDetail loLine = (POQuotationRequest.TransactionDetail) laReport.get(0);
        assertEquals("Approved", loLine.getsStatus());
        assertEquals(Integer.valueOf(1), loLine.getnRowNo());
        assertEquals("Pen", loLine.getsItem());
        assertEquals(20.0, loLine.getnTransTotal(), 0.001);
        assertEquals(TRANS_NO + "|REF1|Main|Main|Procurement|Guanzon|Paper|Supplier Inc|30 days|1|10.0|2.0|20.0",
                loLine.getsTransNo() + "|" + loLine.getsRefNo() + "|" + loLine.getsBranch() + "|" + loLine.getsDestination()
                + "|" + loLine.getsDepartment() + "|" + loLine.getsCompany() + "|" + loLine.getsCategory()
                + "|" + loLine.getsSupplier() + "|" + loLine.getsTerm() + "|" + loLine.getnRow() + "|" + loLine.getnCost()
                + "|" + loLine.getnQty() + "|" + loLine.getnTotal());
        assertEquals(java.sql.Date.valueOf("2026-11-26"), loLine.getdTransDate());
        verify(gr).executeQuery(contains("e.sCompnyNm LIKE '%Gu%'"));
        verify(gr).executeQuery(contains("a.cTranStat LIKE '2'"));
    }

    @Test
    public void loadReportSummaryGroups() throws Exception {
        System.setProperty("sys.main.industry", "00");
        System.setProperty("sys.general.industry", "09");
        for (String lsStatus : new String[]{"OPEN", "CONFIRMED", "POSTED", "RETURNED", "VOID", "CANCELLED", "ALL", "OTHER"}) {
            when(gr.executeQuery(anyString())).thenReturn(reportRows());
            assertEquals("success", req.loadReport(true, "2026-11-01", "2026-11-30", lsStatus).get("result"));
        }
        verify(gr, org.mockito.Mockito.atLeastOnce()).executeQuery(contains("GROUP BY a.sTransNox, c.sSupplier, c.sCompnyID"));

        TestRequest loOther = new TestRequest(gr, master);
        java.lang.reflect.Field loField = QuotationTransaction.class.getDeclaredField("psIndustryId");
        loField.setAccessible(true);
        loField.set(loOther, "05");
        when(gr.executeQuery(anyString())).thenReturn(reportRows());
        loOther.loadReport(true, "2026-11-01", "2026-11-30", "ALL");
        verify(gr, org.mockito.Mockito.atLeastOnce()).executeQuery(contains("GROUP BY a.sTransNox, c.sSupplier  ORDER"));

        when(gr.executeQuery(anyString())).thenReturn(rows());
        assertEquals("No record found.", req.loadReport(false, "2026-11-01", "2026-11-30", "ALL").get("message"));
    }

    @Test
    public void printNeedsData() throws Exception {
        assertEquals("No data loaded.", req.printReport(true, "2026-11-01", "2026-11-30").get("message"));
    }

    @Test
    public void printAbortedOnReportError() throws Exception {
        when(gr.executeQuery(anyString())).thenReturn(reportRows());
        req.loadReport(false, "2026-11-01", "2026-11-30", "ALL");
        JasperReport loReport = mock(JasperReport.class);
        try (MockedStatic<JasperCompileManager> loCompile = mockStatic(JasperCompileManager.class);
                MockedStatic<JasperFillManager> loFill = mockStatic(JasperFillManager.class);
                MockedConstruction<QuotationModels> loModels = mockConstruction(QuotationModels.class, (m, c) -> {
                    when(m.POQuotationRequestMaster()).thenAnswer(i -> model(Model_PO_Quotation_Request_Master.class));
                    when(m.POQuotationRequestSupplier()).thenAnswer(i -> model(Model_PO_Quotation_Request_Supplier.class));
                })) {
            loCompile.when(() -> JasperCompileManager.compileReport(endsWith("POQuotationRequestDetail.jrxml"))).thenReturn(loReport);
            loFill.when(() -> JasperFillManager.fillReport(any(JasperReport.class), anyMap(), any(JRDataSource.class)))
                    .thenThrow(new JRException("fill failed"));
            assertEquals("Transaction print aborted!", req.printReport(false, "2026-11-01", "2026-11-30").get("message"));

            loCompile.when(() -> JasperCompileManager.compileReport(endsWith("POQuotationRequestSummary.jrxml")))
                    .thenThrow(new JRException("missing"));
            assertEquals("Transaction print aborted!", req.printReport(true, "2026-11-01", "2026-11-30").get("message"));
        }
    }

    @Test
    public void orderDetailBean() {
        POQuotationRequest.OrderDetail loLine = new POQuotationRequest.OrderDetail(1, "Pen", "HBW", "X", "PC", "BLUE", 2.0);
        assertEquals("1|Pen|HBW|X|PC|BLUE|2.0", loLine.getnRowNo() + "|" + loLine.getsDescription() + "|" + loLine.getsBrand()
                + "|" + loLine.getsModel() + "|" + loLine.getsMeasure() + "|" + loLine.getsColor() + "|" + loLine.getnOrder());
    }
}
