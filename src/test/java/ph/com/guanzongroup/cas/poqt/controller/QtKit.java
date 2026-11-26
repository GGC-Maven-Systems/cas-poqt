package ph.com.guanzongroup.cas.poqt.controller;

import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;
import org.mockito.stubbing.Answer;

/** Helpers for the quotation controller tests (no database, no screen). */
final class QtKit {

    /** Key of the edit mode in a model's values. */
    static final String EDIT = "__edit";
    static final String TABLE = "__table";

    private QtKit() {
    }

    @SuppressWarnings("unchecked")
    static JSONObject ok() {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        return loJSON;
    }

    @SuppressWarnings("unchecked")
    static JSONObject err(String fsMessage) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    @SuppressWarnings("unchecked")
    static JSONObject json(Object... faPairs) {
        JSONObject loJSON = new JSONObject();
        for (int lnCtr = 0; lnCtr < faPairs.length; lnCtr += 2) {
            loJSON.put(faPairs[lnCtr], faPairs[lnCtr + 1]);
        }
        return loJSON;
    }

    /** Any mock: JSONObject methods return success, others the defaults. */
    static <T> T smart(Class<T> foClass) {
        return mock(foClass, withSettings().defaultAnswer(inv ->
                inv.getMethod().getReturnType() == JSONObject.class ? ok() : RETURNS_DEFAULTS.answer(inv)));
    }

    /**
     * A model whose values live in a map: the model's own getters and setters
     * run (they read and write getValue / setValue), newRecord / openRecord /
     * updateRecord set the edit mode, reference getters (Inventory(), ...)
     * return map models too, other JSONObject methods return success.
     * cReversex starts as "+" (included).
     */
    static <T extends Model> T model(Class<T> foClass, Object... faValues) {
        Map<String, Object> loValues = new HashMap<>();
        loValues.put("cReversex", "+");
        loValues.put(EDIT, EditMode.ADDNEW);
        loValues.put(TABLE, foClass.getSimpleName().replace("Model_", ""));
        for (int lnCtr = 0; lnCtr < faValues.length; lnCtr += 2) {
            loValues.put((String) faValues[lnCtr], faValues[lnCtr + 1]);
        }
        Map<String, Object> loRefs = new HashMap<>();
        Answer<Object> loAnswer = inv -> {
            Method loMethod = inv.getMethod();
            String lsName = loMethod.getName();
            Object[] laArgs = inv.getArguments();
            Class<?> loType = loMethod.getReturnType();
            if ("getValue".equals(lsName) && laArgs.length == 1 && laArgs[0] instanceof String) {
                return loValues.get(laArgs[0]);
            }
            if ("setValue".equals(lsName) && laArgs.length == 2 && laArgs[0] instanceof String) {
                loValues.put((String) laArgs[0], laArgs[1]);
                return ok();
            }
            switch (lsName) {
                case "getEditMode":
                    return loValues.get(EDIT);
                case "newRecord":
                    loValues.put(EDIT, EditMode.ADDNEW);
                    return ok();
                case "openRecord":
                    loValues.put(EDIT, EditMode.READY);
                    return loValues.containsKey("__openResult") ? loValues.get("__openResult") : ok();
                case "updateRecord":
                    loValues.put(EDIT, EditMode.UPDATE);
                    return ok();
                case "saveRecord":
                    return loValues.containsKey("__saveResult") ? loValues.get("__saveResult") : ok();
                case "getTable":
                    return loValues.get(TABLE);
                case "getColumnCount":
                    return 1;
                case "getColumn":
                    return "sTransNox";
                case "getNextCode":
                    return "NEXT0001";
                case "toString":
                    return foClass.getSimpleName() + loValues;
                case "hashCode":
                    return System.identityHashCode(inv.getMock());
                case "equals":
                    return inv.getMock() == laArgs[0];
                default:
                    break;
            }
            boolean lbAccessor = lsName.startsWith("get") || lsName.startsWith("set") || lsName.startsWith("is");
            if (lbAccessor && loMethod.getDeclaringClass() != Model.class
                    && loMethod.getDeclaringClass() != Object.class && !Model.class.isAssignableFrom(loType)) {
                return inv.callRealMethod();
            }
            if (Model.class.isAssignableFrom(loType) && laArgs.length == 0) {
                return loRefs.computeIfAbsent(lsName, k -> model(loType.asSubclass(Model.class)));
            }
            if (loType == JSONObject.class) {
                return ok();
            }
            return RETURNS_DEFAULTS.answer(inv);
        };
        return mock(foClass, withSettings().defaultAnswer(loAnswer));
    }

    /** Sets a map model's edit mode. */
    static void edit(Model foModel, int fnMode) {
        foModel.setValue(EDIT, fnMode);
    }

    static Map<String, Object> row(Object... faPairs) {
        Map<String, Object> loRow = new LinkedHashMap<>();
        for (int lnCtr = 0; lnCtr < faPairs.length; lnCtr += 2) {
            loRow.put((String) faPairs[lnCtr], faPairs[lnCtr + 1]);
        }
        return loRow;
    }

    /**
     * In-memory ResultSet over rows (column label to value): navigation for
     * MiscUtil.RecordCount, getters by label or index, getMetaData.
     */
    @SafeVarargs
    static ResultSet rows(Map<String, Object>... faRows) {
        final List<Map<String, Object>> laRows = new ArrayList<>(Arrays.asList(faRows));
        final List<String> laColumns = new ArrayList<>(laRows.isEmpty() ? new ArrayList<String>() : laRows.get(0).keySet());
        final int[] lnRow = {-1};
        return (ResultSet) Proxy.newProxyInstance(QtKit.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "next":
                            lnRow[0]++;
                            return lnRow[0] < laRows.size();
                        case "isBeforeFirst":
                            return lnRow[0] < 0 && !laRows.isEmpty();
                        case "isAfterLast":
                            return lnRow[0] >= laRows.size() && !laRows.isEmpty();
                        case "getRow":
                            return lnRow[0] >= 0 && lnRow[0] < laRows.size() ? lnRow[0] + 1 : 0;
                        case "beforeFirst":
                            lnRow[0] = -1;
                            return null;
                        case "afterLast":
                            lnRow[0] = laRows.size();
                            return null;
                        case "first":
                            lnRow[0] = 0;
                            return !laRows.isEmpty();
                        case "last":
                            lnRow[0] = laRows.size() - 1;
                            return !laRows.isEmpty();
                        case "absolute":
                            lnRow[0] = (Integer) args[0] - 1;
                            return lnRow[0] >= 0 && lnRow[0] < laRows.size();
                        case "close":
                            return null;
                        case "isClosed":
                        case "wasNull":
                            return false;
                        case "getMetaData":
                            return metaData(laColumns, laRows);
                        case "getString":
                        case "getDate":
                        case "getTimestamp":
                        case "getObject":
                        case "getInt":
                        case "getDouble": {
                            Object loValue = value(laRows.get(lnRow[0]), laColumns, args[0]);
                            switch (method.getName()) {
                                case "getString":
                                    return loValue == null ? null : loValue.toString();
                                case "getInt":
                                    return loValue == null ? 0 : ((Number) loValue).intValue();
                                case "getDouble":
                                    return loValue == null ? 0.0 : ((Number) loValue).doubleValue();
                                default:
                                    return loValue;
                            }
                        }
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "rows" + laRows;
                        default:
                            throw new UnsupportedOperationException("rows." + method.getName());
                    }
                });
    }

    private static Object value(Map<String, Object> foRow, List<String> faColumns, Object foKey) {
        return foKey instanceof Integer ? foRow.get(faColumns.get((Integer) foKey - 1)) : foRow.get((String) foKey);
    }

    private static ResultSetMetaData metaData(List<String> faColumns, List<Map<String, Object>> faRows) {
        return (ResultSetMetaData) Proxy.newProxyInstance(QtKit.class.getClassLoader(), new Class<?>[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getColumnCount":
                            return faColumns.size();
                        case "getColumnLabel":
                        case "getColumnName":
                            return faColumns.get((Integer) args[0] - 1);
                        case "getColumnType": {
                            Object loValue = faRows.isEmpty() ? null : faRows.get(0).get(faColumns.get((Integer) args[0] - 1));
                            if (loValue instanceof Integer) {
                                return Types.INTEGER;
                            } else if (loValue instanceof Double) {
                                return Types.DOUBLE;
                            } else if (loValue instanceof java.sql.Timestamp) {
                                return Types.TIMESTAMP;
                            } else if (loValue instanceof java.sql.Date) {
                                return Types.DATE;
                            } else if (loValue instanceof String) {
                                return Types.VARCHAR;
                            }
                            return Types.OTHER;
                        }
                        default:
                            throw new UnsupportedOperationException("metaData." + method.getName());
                    }
                });
    }
}
