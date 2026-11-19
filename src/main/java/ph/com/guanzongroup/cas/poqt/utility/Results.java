package ph.com.guanzongroup.cas.poqt.utility;

import org.json.simple.JSONObject;

/**
 * The JSONObject results every cas-poqt class returns:
 * {"result": "success" | "error", "message": ...}.
 *
 * <p>
 * One copy for the whole module (the controller, validator and helper
 * classes each had their own).</p>
 */
public final class Results {

    private Results() {
    }

    @SuppressWarnings("unchecked")
    public static JSONObject success() {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        return loJSON;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject success(String fsMessage) {
        JSONObject loJSON = success();
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject error(String fsMessage) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    /** Whether foJSON is a success result (false for null). */
    public static boolean isSuccess(JSONObject foJSON) {
        return foJSON != null && "success".equals(foJSON.get("result"));
    }
}
