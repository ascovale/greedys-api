package org.openapitools.custom;

/**
 * Holder for response wrapper metadata loaded from response-wrappers.json
 */
public class WrapperInfo {
    private String wrappedType;
    private String mode;
    private boolean isPrimitive = false;

    public WrapperInfo() {}

    public WrapperInfo(String wrappedType, String mode) {
        this.wrappedType = wrappedType;
        this.mode = mode;
    }

    public String getWrappedType() {
        return wrappedType;
    }

    public void setWrappedType(String wrappedType) {
        this.wrappedType = wrappedType;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }

    public void setPrimitive(boolean primitive) {
        this.isPrimitive = primitive;
    }

    public static WrapperInfo fromJson(org.json.JSONObject obj) {
        WrapperInfo info = new WrapperInfo();
        if (obj == null) return info;
        info.wrappedType = obj.optString("dataType", null);
        info.mode = obj.optString("category", null);
        info.isPrimitive = obj.optBoolean("isPrimitive", false);
        return info;
    }

    @Override
    public String toString() {
        return "WrapperInfo{" +
                "wrappedType='" + wrappedType + '\'' +
                ", mode='" + mode + '\'' +
                ", isPrimitive=" + isPrimitive +
                '}';
    }
}
