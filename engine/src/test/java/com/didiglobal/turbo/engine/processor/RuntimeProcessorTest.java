package com.didiglobal.turbo.engine.processor;

import com.alibaba.fastjson.JSON;
import com.didiglobal.turbo.engine.bo.ElementInstance;
import com.didiglobal.turbo.engine.bo.NodeInstance;
import com.didiglobal.turbo.engine.common.Constants;
import com.didiglobal.turbo.engine.common.ErrorEnum;
import com.didiglobal.turbo.engine.common.FlowDeploymentStatus;
import com.didiglobal.turbo.engine.dao.mapper.FlowDeploymentMapper;
import com.didiglobal.turbo.engine.entity.FlowDeploymentPO;
import com.didiglobal.turbo.engine.model.InstanceData;
import com.didiglobal.turbo.engine.param.CommitTaskParam;
import com.didiglobal.turbo.engine.param.RollbackTaskParam;
import com.didiglobal.turbo.engine.param.StartProcessParam;
import com.didiglobal.turbo.engine.result.*;
import com.didiglobal.turbo.engine.runner.BaseTest;
import com.didiglobal.turbo.engine.util.EntityBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RuntimeProcessorTest extends BaseTest {

    @Resource
    private RuntimeProcessor runtimeProcessor;

    @Resource
    private FlowDeploymentMapper flowDeploymentMapper;

    private StartProcessResult startProcess() throws Exception {
        // prepare
        FlowDeploymentPO flowDeploymentPO = EntityBuilder.buildSpecialFlowDeploymentPO();
        FlowDeploymentPO _flowDeploymentPO = flowDeploymentMapper.selectByDeployId(flowDeploymentPO.getFlowDeployId());
        if (_flowDeploymentPO != null) {
            if (!StringUtils.equals(_flowDeploymentPO.getFlowModel(), flowDeploymentPO.getFlowModel())) {
                flowDeploymentMapper.deleteById(_flowDeploymentPO.getId());
                flowDeploymentMapper.insert(flowDeploymentPO);
            }
        } else {
            flowDeploymentMapper.insert(flowDeploymentPO);
        }

        // start process
        StartProcessParam startProcessParam = new StartProcessParam();
        startProcessParam.setFlowDeployId(flowDeploymentPO.getFlowDeployId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("orderId", "123"));
        variables.add(new InstanceData("orderStatus", "1"));
        startProcessParam.setVariables(variables);
        // build
        return runtimeProcessor.startProcess(startProcessParam);
    }

    @Test
    public void testStartProcess() throws Exception {
        StartProcessResult startProcessResult = startProcess();
        Assert.assertTrue(startProcessResult.getErrCode() == ErrorEnum.COMMIT_SUSPEND.getErrNo());
        Assert.assertTrue(StringUtils.equals(startProcessResult.getActiveTaskInstance().getModelKey(), "BranchUserTask_0scrl8d"));
    }

    // UserTask -> EndEvent
    @Test
    public void testNormalCommitToEnd() throws Exception {
        StartProcessResult startProcessResult = startProcess();

        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 1));
        commitTaskParam.setVariables(variables);

        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        LOGGER.info("testCommit.||commitTaskResult={}", commitTaskResult);
        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.SUCCESS.getErrNo());
        Assert.assertTrue(StringUtils.equals(commitTaskResult.getActiveTaskInstance().getModelKey(), "EndEvent_0s4vsxw"));
    }

    // UserTask -> ExclusiveGateway -> UserTask
    @Test
    public void testNormalCommitToUserTask() throws Exception {
        StartProcessResult startProcessResult = startProcess();

        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 0));
        commitTaskParam.setVariables(variables);

        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        LOGGER.info("testCommit.||commitTaskResult={}", commitTaskResult);
        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.COMMIT_SUSPEND.getErrNo());
        Assert.assertTrue(StringUtils.equals(commitTaskResult.getActiveTaskInstance().getModelKey(), "UserTask_0uld0u9"));
    }

    // UserTask -> ExclusiveGateway -> UserTask
    // UserTask ->
    @Test
    public void testRepeatedCommitToUserTask() throws Exception {
        StartProcessResult startProcessResult = startProcess();

        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 0));
        commitTaskParam.setVariables(variables);
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        LOGGER.info("testCommit.||commitTaskResult={}", commitTaskResult);

        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.COMMIT_SUSPEND.getErrNo());
        Assert.assertTrue(StringUtils.equals(commitTaskResult.getActiveTaskInstance().getModelKey(), "UserTask_0uld0u9"));
    }

    // UserTask -> EndEvent -> Commit again
    @Test
    public void testCommitCompletedFlowInstance() throws Exception {
        StartProcessResult startProcessResult = startProcess();

        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 1));
        commitTaskParam.setVariables(variables);
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        LOGGER.info("testCommit.||commitTaskResult={}", commitTaskResult);

        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.REENTRANT_WARNING.getErrNo());
    }

    @Test
    public void testCommitTerminatedFlowInstance() throws Exception {
        StartProcessResult startProcessResult = startProcess();

        runtimeProcessor.terminateProcess(startProcessResult.getFlowInstanceId(), false);

        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 1));
        commitTaskParam.setVariables(variables);
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.COMMIT_REJECTRD.getErrNo());
    }

    // UserTask <- ExclusiveGateway <- UserTask : Commit old UserTask
    @Test
    public void testRollbackToUserTaskAndCommitOldUserTask() throws Exception {
        // start process
        StartProcessResult startProcessResult = startProcess();
        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 0));
        commitTaskParam.setVariables(variables);

        // UserTask -> ExclusiveGateway -> UserTask
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        // UserTask <- ExclusiveGateway <- UserTask
        RollbackTaskParam rollbackTaskParam = new RollbackTaskParam();
        rollbackTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        rollbackTaskParam.setTaskInstanceId(commitTaskResult.getActiveTaskInstance().getNodeInstanceId());
        RollbackTaskResult rollbackTaskResult = runtimeProcessor.rollback(rollbackTaskParam);

        // commit old UserTask
        commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        LOGGER.info("testRollbackToUserTaskAndCommitOldUserTask.||commitTaskResult={}", commitTaskResult);
        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.COMMIT_FAILED.getErrNo());
        Assert.assertTrue(StringUtils.equals(commitTaskResult.getActiveTaskInstance().getModelKey(), "BranchUserTask_0scrl8d"));
    }

    @Test
    public void testRollbackFromMiddleUserTask() throws Exception {
        // start process
        StartProcessResult startProcessResult = startProcess();
        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        String branchUserTaskNodeInstanceId = startProcessResult.getActiveTaskInstance().getNodeInstanceId();
        commitTaskParam.setTaskInstanceId(branchUserTaskNodeInstanceId);
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 0));
        commitTaskParam.setVariables(variables);

        // UserTask -> ExclusiveGateway -> UserTask
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        // StartEvent <- UserTask
        RollbackTaskParam rollbackTaskParam = new RollbackTaskParam();
        rollbackTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        // Previous UserTask node
        rollbackTaskParam.setTaskInstanceId(branchUserTaskNodeInstanceId);
        RollbackTaskResult rollbackTaskResult = runtimeProcessor.rollback(rollbackTaskParam);

        // Ignore current userTask
        LOGGER.info("testRollbackFromMiddleUserTask.||rollbackTaskResult={}", rollbackTaskResult);
        Assert.assertTrue(rollbackTaskResult.getErrCode() == ErrorEnum.ROLLBACK_SUSPEND.getErrNo());
        Assert.assertTrue(StringUtils.equals(rollbackTaskResult.getActiveTaskInstance().getModelKey(), "BranchUserTask_0scrl8d"));
    }



    // UserTask <- ExclusiveGateway <- UserTask
    @Test
    public void testRollbackToUserTask() throws Exception {
        // start process
        StartProcessResult startProcessResult = startProcess();
        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 0));
        commitTaskParam.setVariables(variables);

        // UserTask -> ExclusiveGateway -> UserTask
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        // UserTask <- ExclusiveGateway <- UserTask
        RollbackTaskParam rollbackTaskParam = new RollbackTaskParam();
        rollbackTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        rollbackTaskParam.setTaskInstanceId(commitTaskResult.getActiveTaskInstance().getNodeInstanceId());
        RollbackTaskResult rollbackTaskResult = runtimeProcessor.rollback(rollbackTaskParam);

        LOGGER.info("testRollback.||rollbackTaskResult={}", rollbackTaskResult);
        Assert.assertTrue(rollbackTaskResult.getErrCode() == ErrorEnum.ROLLBACK_SUSPEND.getErrNo());
        Assert.assertTrue(StringUtils.equals(rollbackTaskResult.getActiveTaskInstance().getModelKey(), "BranchUserTask_0scrl8d"));
    }

    // StartEvent <- UserTask
    @Test
    public void testRollbackToStartEvent() throws Exception {
        // start process
        StartProcessResult startProcessResult = startProcess();
        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 0));
        commitTaskParam.setVariables(variables);

        // UserTask -> ExclusiveGateway -> UserTask
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        // UserTask <- ExclusiveGateway <- UserTask
        RollbackTaskParam rollbackTaskParam = new RollbackTaskParam();
        rollbackTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        rollbackTaskParam.setTaskInstanceId(commitTaskResult.getActiveTaskInstance().getNodeInstanceId());
        RollbackTaskResult rollbackTaskResult = runtimeProcessor.rollback(rollbackTaskParam);

        // StartEvent <- UserTask
        rollbackTaskParam = new RollbackTaskParam();
        rollbackTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        rollbackTaskParam.setTaskInstanceId(rollbackTaskResult.getActiveTaskInstance().getNodeInstanceId());
        rollbackTaskResult = runtimeProcessor.rollback(rollbackTaskParam);
        LOGGER.info("testRollback.||rollbackTaskResult={}", rollbackTaskResult);
        Assert.assertTrue(rollbackTaskResult.getErrCode() == ErrorEnum.NO_USER_TASK_TO_ROLLBACK.getErrNo());
    }

    // rollback completed process
    @Test
    public void testRollbackFromEndEvent() throws Exception {
        // start process
        StartProcessResult startProcessResult = startProcess();
        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 1));
        commitTaskParam.setVariables(variables);

        // UserTask -> EndEvent
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        // rollback EndEvent
        RollbackTaskParam rollbackTaskParam = new RollbackTaskParam();
        rollbackTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        rollbackTaskParam.setTaskInstanceId(commitTaskResult.getActiveTaskInstance().getNodeInstanceId());
        RollbackTaskResult rollbackTaskResult = runtimeProcessor.rollback(rollbackTaskParam);

        LOGGER.info("testRollback.||rollbackTaskResult={}", rollbackTaskResult);
        Assert.assertTrue(rollbackTaskResult.getErrCode() == ErrorEnum.ROLLBACK_REJECTRD.getErrNo());
    }

    @Test
    public void testTerminateProcess() throws Exception {
        StartProcessResult startProcessResult = startProcess();
        TerminateResult terminateResult = runtimeProcessor.terminateProcess(startProcessResult.getFlowInstanceId(), false);
        LOGGER.info("testTerminateProcess.||terminateResult={}", terminateResult);
        Assert.assertTrue(terminateResult.getErrCode() == ErrorEnum.SUCCESS.getErrNo());
    }

    @Test
    public void testGetHistoryUserTaskList() throws Exception {
        StartProcessResult startProcessResult = startProcess();
        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 0));
        commitTaskParam.setVariables(variables);

        // UserTask -> ExclusiveGateway -> UserTask
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        NodeInstanceListResult nodeInstanceListResult = runtimeProcessor.getHistoryUserTaskList(commitTaskResult.getFlowInstanceId(), false);
        LOGGER.info("testGetHistoryUserTaskList.||nodeInstanceListResult={}", nodeInstanceListResult);
        StringBuilder sb = new StringBuilder();
        for (NodeInstance elementInstanceResult : nodeInstanceListResult.getNodeInstanceList()) {
            sb.append("[");
            sb.append(elementInstanceResult.getModelKey());
            sb.append(" ");
            sb.append(elementInstanceResult.getStatus());
            sb.append("]->");
        }
        LOGGER.info("testGetHistoryUserTaskList.||snapshot={}", sb.toString());

        Assert.assertTrue(nodeInstanceListResult.getNodeInstanceList().size() == 2);
        Assert.assertTrue(StringUtils.equals(nodeInstanceListResult.getNodeInstanceList().get(0).getModelKey(), "UserTask_0uld0u9"));
    }


    @Test
    public void testGetFailedHistoryElementList() throws Exception {
        StartProcessResult startProcessResult = startProcess();
        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 0));
        variables.add(new InstanceData("orderId", "notExistOrderId"));
        commitTaskParam.setVariables(variables);

        // UserTask -> ExclusiveGateway : Failed
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        LOGGER.info("testGetFailedHistoryElementList.||commitTaskResult={}", commitTaskResult);
        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.GET_OUTGOING_FAILED.getErrNo());

        ElementInstanceListResult elementInstanceListResult = runtimeProcessor.getHistoryElementList(commitTaskResult.getFlowInstanceId(), false);
        LOGGER.info("testGetHistoryElementList.||elementInstanceListResult={}", elementInstanceListResult);
        StringBuilder sb = new StringBuilder();
        for (ElementInstance elementInstanceResult : elementInstanceListResult.getElementInstanceList()) {
            sb.append("[");
            sb.append(elementInstanceResult.getModelKey());
            sb.append(" ");
            sb.append(elementInstanceResult.getStatus());
            sb.append("]->");
        }
        LOGGER.info("testGetHistoryElementList.||snapshot={}", sb.toString());

        Assert.assertTrue(elementInstanceListResult.getElementInstanceList().size() == 5);
        Assert.assertTrue(StringUtils.equals(elementInstanceListResult.getElementInstanceList().get(4).getModelKey(), "ExclusiveGateway_0yq2l0s"));
    }

    @Test
    public void testGetCompletedHistoryElementList() throws Exception {
        StartProcessResult startProcessResult = startProcess();
        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 1));
        commitTaskParam.setVariables(variables);

        // UserTask -> EndEvent
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        ElementInstanceListResult elementInstanceListResult = runtimeProcessor.getHistoryElementList(commitTaskResult.getFlowInstanceId(), false);
        LOGGER.info("testGetHistoryElementList.||elementInstanceListResult={}", elementInstanceListResult);
        StringBuilder sb = new StringBuilder();
        for (ElementInstance elementInstanceResult : elementInstanceListResult.getElementInstanceList()) {
            sb.append("[");
            sb.append(elementInstanceResult.getModelKey());
            sb.append(" ");
            sb.append(elementInstanceResult.getStatus());
            sb.append("]->");
        }
        LOGGER.info("testGetHistoryElementList.||snapshot={}", sb.toString());

        Assert.assertTrue(elementInstanceListResult.getElementInstanceList().size() == 5);
        Assert.assertTrue(StringUtils.equals(elementInstanceListResult.getElementInstanceList().get(4).getModelKey(), "EndEvent_0s4vsxw"));
    }


    @Test
    public void testGetInstanceData() throws Exception {
        StartProcessResult startProcessResult = startProcess();
        String flowInstanceId = startProcessResult.getFlowInstanceId();
        InstanceDataListResult instanceDataList = runtimeProcessor.getInstanceData(flowInstanceId, false);
        LOGGER.info("testGetInstanceData 1.||instanceDataList={}", instanceDataList);

        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 0));
        variables.add(new InstanceData("commitTime", 1));
        commitTaskParam.setVariables(variables);

        // UserTask -> ExclusiveGateway -> UserTask
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        // UserTask -> UserTask
        CommitTaskParam commitTaskParam1 = new CommitTaskParam();
        commitTaskParam1.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam1.setTaskInstanceId(commitTaskResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables1 = new ArrayList<>();
        variables1.add(new InstanceData("orderStatus", "2"));
        variables1.add(new InstanceData("commitTime", 2));
        commitTaskParam1.setVariables(variables1);
        CommitTaskResult commitTaskResult1 = runtimeProcessor.commit(commitTaskParam1);

        instanceDataList = runtimeProcessor.getInstanceData(flowInstanceId, false);
        LOGGER.info("testGetInstanceData 2.||instanceDataList={}", instanceDataList);

        // UserTask <- UserTask
        RollbackTaskParam rollbackTaskParam = new RollbackTaskParam();
        rollbackTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        rollbackTaskParam.setTaskInstanceId(commitTaskResult1.getActiveTaskInstance().getNodeInstanceId());
        RollbackTaskResult rollbackTaskResult = runtimeProcessor.rollback(rollbackTaskParam);
        LOGGER.info("rollbackTaskResult 3.||rollbackTaskResult.variables={}", rollbackTaskResult.getVariables());

        // UserTask <- ExclusiveGateway <- UserTask
        RollbackTaskParam rollbackTaskParam1 = new RollbackTaskParam();
        rollbackTaskParam1.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        rollbackTaskParam1.setTaskInstanceId(rollbackTaskResult.getActiveTaskInstance().getNodeInstanceId());
        RollbackTaskResult rollbackTaskResult1 = runtimeProcessor.rollback(rollbackTaskParam1);
        LOGGER.info("rollbackTaskResult 4.||rollbackTaskResult.variables={}", rollbackTaskResult1.getVariables());

        instanceDataList = runtimeProcessor.getInstanceData(flowInstanceId, false);
        LOGGER.info("testGetInstanceData 5.||instanceDataList={}", instanceDataList);
        String initData = JSON.toJSONString(startProcessResult.getVariables());
        String rollbackData = JSON.toJSONString(rollbackTaskResult1.getVariables());
        Assert.assertTrue(StringUtils.equals(initData, rollbackData));
    }

    @Test
    public void testGetNodeInstance() throws Exception {
        StartProcessResult startProcessResult = startProcess();
        String flowInstanceId = startProcessResult.getFlowInstanceId();
        NodeInstanceResult nodeInstanceResult = runtimeProcessor.getNodeInstance(flowInstanceId, startProcessResult.getActiveTaskInstance().getNodeInstanceId(), false);
        LOGGER.info("testGetNodeInstance.||nodeInstanceResult={}", nodeInstanceResult);

        Assert.assertTrue(StringUtils.equals(nodeInstanceResult.getNodeInstance().getNodeInstanceId(), startProcessResult.getActiveTaskInstance().getNodeInstanceId()));
    }

    // ==================== CallActivity 子流程测试 ====================

    /**
     * CallActivity 子流程完整链路测试 - 短请假分支（days < 3）
     *
     * 主流程: StartEvent_main → UserTask_fillForm → CallActivity_approval → UserTask_archive → EndEvent_main
     * 子流程: StartEvent_sub → ExclusiveGateway_days → UserTask_leader（days<3） → EndEvent_sub1
     */
    @Test
    public void testCallActivityFlow() throws Exception {
        // ========== 1. 部署子流程 ==========
        String subFlowModuleId = "subFlowModuleId_callActivity";
        String subFlowDeployId = "subFlowDeployId_callActivity";

//        FlowDeploymentPO subFlowDeploymentPO = new FlowDeploymentPO();
//        subFlowDeploymentPO.setFlowName("subFlowName_callActivity");
//        subFlowDeploymentPO.setFlowKey("subFlowKey_callActivity");
//        subFlowDeploymentPO.setFlowModuleId(subFlowModuleId);
//        subFlowDeploymentPO.setFlowDeployId(subFlowDeployId);
//        subFlowDeploymentPO.setFlowModel(EntityBuilder.buildSubFlowModelStr());
//        subFlowDeploymentPO.setStatus(FlowDeploymentStatus.DEPLOYED);
//        subFlowDeploymentPO.setCreateTime(new Date());
//        subFlowDeploymentPO.setModifyTime(new Date());
//        subFlowDeploymentPO.setOperator("testOperator");
//        subFlowDeploymentPO.setRemark("subFlow for CallActivity test");
//        flowDeploymentMapper.insert(subFlowDeploymentPO);
//
//        // ========== 2. 部署主流程（CallActivity 引用子流程） ==========
        String mainFlowDeployId = "mainFlowDeployId_callActivity";
//
//        FlowDeploymentPO mainFlowDeploymentPO = new FlowDeploymentPO();
//        mainFlowDeploymentPO.setFlowName("mainFlowName_callActivity");
//        mainFlowDeploymentPO.setFlowKey("mainFlowKey_callActivity");
//        mainFlowDeploymentPO.setFlowModuleId("mainFlowModuleId_callActivity");
//        mainFlowDeploymentPO.setFlowDeployId(mainFlowDeployId);
//        mainFlowDeploymentPO.setFlowModel(EntityBuilder.buildMainFlowWithCallActivityModelStr(subFlowModuleId));
//        mainFlowDeploymentPO.setStatus(FlowDeploymentStatus.DEPLOYED);
//        mainFlowDeploymentPO.setCreateTime(new Date());
//        mainFlowDeploymentPO.setModifyTime(new Date());
//        mainFlowDeploymentPO.setOperator("testOperator");
//        mainFlowDeploymentPO.setRemark("mainFlow for CallActivity test");
//        flowDeploymentMapper.insert(mainFlowDeploymentPO);

        // ========== 3. 启动主流程 → 挂起在 UserTask_fillForm ==========
        StartProcessParam startProcessParam = new StartProcessParam();
        startProcessParam.setFlowDeployId(mainFlowDeployId);
        List<InstanceData> startVariables = new ArrayList<>();
        startVariables.add(new InstanceData("applicant", "张三"));
        startVariables.add(new InstanceData("days", 2));
        startProcessParam.setVariables(startVariables);

        StartProcessResult startProcessResult = runtimeProcessor.startProcess(startProcessParam);
        LOGGER.info("testCallActivityFlow step1 startProcess.||result={}", startProcessResult);
        Assert.assertTrue(startProcessResult.getErrCode() == ErrorEnum.COMMIT_SUSPEND.getErrNo());
        Assert.assertTrue(StringUtils.equals(startProcessResult.getActiveTaskInstance().getModelKey(), "UserTask_fillForm"));

        // ========== 4. 提交 UserTask_fillForm → 推进到 CallActivity_approval ==========
        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());

        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        LOGGER.info("testCallActivityFlow step2 commitFillForm.||result={}", commitTaskResult);
        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.COMMIT_SUSPEND.getErrNo());
        Assert.assertTrue(StringUtils.equals(commitTaskResult.getActiveTaskInstance().getModelKey(), "CallActivity_approval"));

        // ========== 5. 提交 CallActivity_approval → 启动子流程 ==========
        commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(commitTaskResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(commitTaskResult.getActiveTaskInstance().getNodeInstanceId());

        // 从 CallActivity 节点属性中读取子流程 flowModuleId
        String callActivityFlowModuleId = commitTaskResult.getActiveTaskInstance().getProperties()
            .get(Constants.ELEMENT_PROPERTIES.CALL_ACTIVITY_FLOW_MODULE_ID).toString();
        commitTaskParam.setCallActivityFlowModuleId(callActivityFlowModuleId);

        // 传入子流程所需的变量（days 用于 ExclusiveGateway 条件判断）
        List<InstanceData> callActivityVariables = new ArrayList<>();
        callActivityVariables.add(new InstanceData("days", 2));
        commitTaskParam.setVariables(callActivityVariables);

        commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        LOGGER.info("testCallActivityFlow step3 commitCallActivity.||result={}", commitTaskResult);
        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.COMMIT_SUSPEND.getErrNo());
        Assert.assertTrue(StringUtils.equals(commitTaskResult.getActiveTaskInstance().getModelKey(), "CallActivity_approval"));

        // 子流程挂起在 UserTask_leader（因为 days=2 < 3）
        List<RuntimeResult> subNodeResultList = commitTaskResult.getActiveTaskInstance().getSubNodeResultList();
        Assert.assertNotNull(subNodeResultList);
        Assert.assertTrue(subNodeResultList.size() == 1);
        Assert.assertTrue(StringUtils.equals(subNodeResultList.get(0).getActiveTaskInstance().getModelKey(), "UserTask_leader"));

        // ========== 6. 提交子流程 UserTask_leader → 子流程结束 → CallActivity完成 → UserTask_archive ==========
        // 注意：使用父流程的 flowInstanceId + 子流程的 nodeInstanceId 提交
        commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(commitTaskResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(subNodeResultList.get(0).getActiveTaskInstance().getNodeInstanceId());

        commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        LOGGER.info("testCallActivityFlow step4 commitSubFlowUserTask.||result={}", commitTaskResult);
        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.COMMIT_SUSPEND.getErrNo());
        Assert.assertTrue(StringUtils.equals(commitTaskResult.getActiveTaskInstance().getModelKey(), "UserTask_archive"));

        // ========== 7. 提交 UserTask_archive → 主流程结束 ==========
        commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(commitTaskResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(commitTaskResult.getActiveTaskInstance().getNodeInstanceId());

        commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        LOGGER.info("testCallActivityFlow step5 commitArchive.||result={}", commitTaskResult);
        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.SUCCESS.getErrNo());
    }

    /**
     * CallActivity 子流程完整链路测试 - 长请假分支（days >= 3）
     *
     * 主流程: StartEvent_main → UserTask_fillForm → CallActivity_approval → UserTask_archive → EndEvent_main
     * 子流程: StartEvent_sub → ExclusiveGateway_days → UserTask_manager（days>=3） → EndEvent_sub2
     */
    @Test
    public void testCallActivityFlowWithLongLeave() throws Exception {
        // ========== 1. 部署子流程 ==========
        String subFlowModuleId = "subFlowModuleId_callActivity_long_" + System.currentTimeMillis();
        String subFlowDeployId = "subFlowDeployId_callActivity_long_" + System.currentTimeMillis();

        FlowDeploymentPO subFlowDeploymentPO = new FlowDeploymentPO();
        subFlowDeploymentPO.setFlowName("subFlowName_callActivity_long");
        subFlowDeploymentPO.setFlowKey("subFlowKey_callActivity_long");
        subFlowDeploymentPO.setFlowModuleId(subFlowModuleId);
        subFlowDeploymentPO.setFlowDeployId(subFlowDeployId);
        subFlowDeploymentPO.setFlowModel(EntityBuilder.buildSubFlowModelStr());
        subFlowDeploymentPO.setStatus(FlowDeploymentStatus.DEPLOYED);
        subFlowDeploymentPO.setCreateTime(new Date());
        subFlowDeploymentPO.setModifyTime(new Date());
        subFlowDeploymentPO.setOperator("testOperator");
        subFlowDeploymentPO.setRemark("subFlow for CallActivity long leave test");
        flowDeploymentMapper.insert(subFlowDeploymentPO);

        // ========== 2. 部署主流程 ==========
        String mainFlowDeployId = "mainFlowDeployId_callActivity_long_" + System.currentTimeMillis();

        FlowDeploymentPO mainFlowDeploymentPO = new FlowDeploymentPO();
        mainFlowDeploymentPO.setFlowName("mainFlowName_callActivity_long");
        mainFlowDeploymentPO.setFlowKey("mainFlowKey_callActivity_long");
        mainFlowDeploymentPO.setFlowModuleId("mainFlowModuleId_callActivity_long_" + System.currentTimeMillis());
        mainFlowDeploymentPO.setFlowDeployId(mainFlowDeployId);
        mainFlowDeploymentPO.setFlowModel(EntityBuilder.buildMainFlowWithCallActivityModelStr(subFlowModuleId));
        mainFlowDeploymentPO.setStatus(FlowDeploymentStatus.DEPLOYED);
        mainFlowDeploymentPO.setCreateTime(new Date());
        mainFlowDeploymentPO.setModifyTime(new Date());
        mainFlowDeploymentPO.setOperator("testOperator");
        mainFlowDeploymentPO.setRemark("mainFlow for CallActivity long leave test");
        flowDeploymentMapper.insert(mainFlowDeploymentPO);

        // ========== 3. 启动主流程 ==========
        StartProcessParam startProcessParam = new StartProcessParam();
        startProcessParam.setFlowDeployId(mainFlowDeployId);
        List<InstanceData> startVariables = new ArrayList<>();
        startVariables.add(new InstanceData("applicant", "李四"));
        startVariables.add(new InstanceData("days", 5));
        startProcessParam.setVariables(startVariables);

        StartProcessResult startProcessResult = runtimeProcessor.startProcess(startProcessParam);
        Assert.assertTrue(startProcessResult.getErrCode() == ErrorEnum.COMMIT_SUSPEND.getErrNo());
        Assert.assertTrue(StringUtils.equals(startProcessResult.getActiveTaskInstance().getModelKey(), "UserTask_fillForm"));

        // ========== 4. 提交 UserTask_fillForm ==========
        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());

        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        Assert.assertTrue(StringUtils.equals(commitTaskResult.getActiveTaskInstance().getModelKey(), "CallActivity_approval"));

        // ========== 5. 提交 CallActivity_approval → 启动子流程 ==========
        commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(commitTaskResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(commitTaskResult.getActiveTaskInstance().getNodeInstanceId());
        commitTaskParam.setCallActivityFlowModuleId(
            commitTaskResult.getActiveTaskInstance().getProperties()
                .get(Constants.ELEMENT_PROPERTIES.CALL_ACTIVITY_FLOW_MODULE_ID).toString());
        List<InstanceData> callActivityVariables = new ArrayList<>();
        callActivityVariables.add(new InstanceData("days", 5));
        commitTaskParam.setVariables(callActivityVariables);

        commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.COMMIT_SUSPEND.getErrNo());

        // 子流程挂起在 UserTask_manager（因为 days=5 >= 3）
        List<RuntimeResult> subNodeResultList = commitTaskResult.getActiveTaskInstance().getSubNodeResultList();
        Assert.assertNotNull(subNodeResultList);
        Assert.assertTrue(subNodeResultList.size() == 1);
        Assert.assertTrue(StringUtils.equals(subNodeResultList.get(0).getActiveTaskInstance().getModelKey(), "UserTask_manager"));

        // ========== 6. 提交子流程 UserTask_manager ==========
        commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(commitTaskResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(subNodeResultList.get(0).getActiveTaskInstance().getNodeInstanceId());

        commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.COMMIT_SUSPEND.getErrNo());
        Assert.assertTrue(StringUtils.equals(commitTaskResult.getActiveTaskInstance().getModelKey(), "UserTask_archive"));

        // ========== 7. 提交 UserTask_archive → 主流程结束 ==========
        commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(commitTaskResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(commitTaskResult.getActiveTaskInstance().getNodeInstanceId());

        commitTaskResult = runtimeProcessor.commit(commitTaskParam);
        Assert.assertTrue(commitTaskResult.getErrCode() == ErrorEnum.SUCCESS.getErrNo());
    }
}
