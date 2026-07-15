package com.didiglobal.turbo.engine.bo;

/**
 * The data transfer object is used in the scenario of parent to child
 * and child to parent of the CallActivity.
 * <p>
 * 1.parent to child
 * 2.child to parent
 */
public class DataTransferBO {

    // 来源类型：context / fixed
    private String sourceType;
    // 来源变量名（context使用）
    private String sourceKey;
    // 固定值（fixed使用）
    private String sourceValue;
    // 目标变量名（子流程中的变量名）
    private String targetKey;

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
    }

    public String getTargetKey() {
        return targetKey;
    }

    public void setTargetKey(String targetKey) {
        this.targetKey = targetKey;
    }
}
