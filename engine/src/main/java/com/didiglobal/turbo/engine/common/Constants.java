package com.didiglobal.turbo.engine.common;

public class Constants {

    public static final int DEFAULT_TIMEOUT = 3000;

    public static final String ELEMENTLIST = "flowElementList";

    public static final class ELEMENT_PROPERTIES {
        public static final String NAME = "name";
        public static final String CONDITION = "conditionsequenceflow";
        public static final String DEFAULT_CONDITION = "defaultConditions";
        public static final String HOOK_INFO_IDS = "hookInfoIds";
        // 执行方式
        public static final String CALL_ACTIVITY_EXECUTE_TYPE = "callActivityExecuteType";
        // 实例模式
        public static final String CALL_ACTIVITY_INSTANCE_TYPE = "callActivityInstanceType";
        // 子流程定义标识
        public static final String CALL_ACTIVITY_FLOW_MODULE_ID = "callActivityFlowModuleId";
        // 入参映射方式（主流程 -> 子流程）
        public static final String CALL_ACTIVITY_IN_PARAM_TYPE = "callActivityInParamType";
        // 入参映射规则
        public static final String CALL_ACTIVITY_IN_PARAM = "callActivityInParam";
        // 出参映射方式（子流程 -> 主流程）
        public static final String CALL_ACTIVITY_OUT_PARAM_TYPE = "callActivityOutParamType";
        // 出参映射规则
        public static final String CALL_ACTIVITY_OUT_PARAM = "callActivityOutParam";
    }

    public static final class CALL_ACTIVITY_PARAM_TYPE {
        // 不传递任何变量
        public static final String NONE = "none";
        // 按照 callActivityInParam 配置的映射规则，只传递指定变量
        public static final String PART = "part";
        // 传递全部变量
        public static final String FULL = "full";
    }

    public static final class CALL_ACTIVITY_EXECUTE_TYPE {
        // 同步执行：子流程在主流程的同一个线程/事务中执行，子流程挂起时主流程也挂起，子流程结束时立即回到主流程
        public static final String SYNC = "sync";
        // 异步执行：启动子流程后立即返回，子流程独立运行
        public static final String ASYNC = "async";
    }

    public static final class CALL_ACTIVITY_INSTANCE_TYPE {
        // 单实例：每次执行 CallActivity 只启动一个子流程实例
        public static final String SINGLE = "single";
        // 多实例：并行启动多个子流程实例
        public static final String MULTIPLE = "multiple";
    }

    public static final class CALL_ACTIVITY_DATA_TRANSFER_TYPE {
        // 从流程上下文变量中取值
        public static String SOURCE_TYPE_CONTEXT = "context";
        // 使用固定值
        public static String SOURCE_TYPE_FIXED = "fixed";
    }

    public static final String NODE_INFO_FORMAT = "nodeKey={0}, nodeName={1}, nodeType={2}";
    public static final String NODE_INSTANCE_FORMAT = "nodeKey={0}, nodeName={1}, nodeInstanceId={2}";
    public static final String MODEL_DEFINITION_ERROR_MSG_FORMAT = "message={0}, elementName={1}, elementKey={2}";
}
