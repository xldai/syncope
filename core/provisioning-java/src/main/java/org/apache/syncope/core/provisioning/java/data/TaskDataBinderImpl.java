/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractAttributableTO;
import org.apache.syncope.common.lib.to.AbstractProvisioningTaskTO;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtil;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.misc.spring.BeanUtils;
import org.apache.syncope.core.misc.jexl.JexlUtil;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class TaskDataBinderImpl implements TaskDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TaskDataBinder.class);

    private static final String[] IGNORE_TASK_PROPERTIES = {
        "executions", "resource", "matchingRule", "unmatchingRule" };

    private static final String[] IGNORE_TASK_EXECUTION_PROPERTIES = { "key", "task" };

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private SchedulerFactoryBean scheduler;

    private void checkJexl(final AbstractAttributableTO attributableTO, final SyncopeClientException sce) {
        for (AttrTO attrTO : attributableTO.getPlainAttrs()) {
            if (!attrTO.getValues().isEmpty() && !JexlUtil.isExpressionValid(attrTO.getValues().get(0))) {
                sce.getElements().add("Invalid JEXL: " + attrTO.getValues().get(0));
            }
        }

        for (AttrTO attrTO : attributableTO.getVirAttrs()) {
            if (!attrTO.getValues().isEmpty() && !JexlUtil.isExpressionValid(attrTO.getValues().get(0))) {
                sce.getElements().add("Invalid JEXL: " + attrTO.getValues().get(0));
            }
        }
    }

    private void fill(final ProvisioningTask task, final AbstractProvisioningTaskTO taskTO) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSyncTask);

        if (task instanceof PushTask && taskTO instanceof PushTaskTO) {
            final PushTask pushTask = (PushTask) task;
            final PushTaskTO pushTaskTO = (PushTaskTO) taskTO;

            pushTask.setUserFilter(pushTaskTO.getUserFilter());
            pushTask.setRoleFilter(pushTaskTO.getRoleFilter());

            pushTask.setMatchingRule(pushTaskTO.getMatchingRule() == null
                    ? MatchingRule.LINK : pushTaskTO.getMatchingRule());

            pushTask.setUnmatchingRule(pushTaskTO.getUnmatchingRule() == null
                    ? UnmatchingRule.ASSIGN : pushTaskTO.getUnmatchingRule());

        } else if (task instanceof SyncTask && taskTO instanceof SyncTaskTO) {
            final SyncTask syncTask = (SyncTask) task;
            final SyncTaskTO syncTaskTO = (SyncTaskTO) taskTO;

            syncTask.setMatchingRule(syncTaskTO.getMatchingRule() == null
                    ? MatchingRule.UPDATE : syncTaskTO.getMatchingRule());

            syncTask.setUnmatchingRule(syncTaskTO.getUnmatchingRule() == null
                    ? UnmatchingRule.PROVISION : syncTaskTO.getUnmatchingRule());

            // 1. validate JEXL expressions in user and role templates
            if (syncTaskTO.getUserTemplate() != null) {
                UserTO template = syncTaskTO.getUserTemplate();

                if (StringUtils.isNotBlank(template.getUsername())
                        && !JexlUtil.isExpressionValid(template.getUsername())) {

                    sce.getElements().add("Invalid JEXL: " + template.getUsername());
                }
                if (StringUtils.isNotBlank(template.getPassword())
                        && !JexlUtil.isExpressionValid(template.getPassword())) {

                    sce.getElements().add("Invalid JEXL: " + template.getPassword());
                }

                checkJexl(template, sce);

                for (MembershipTO memb : template.getMemberships()) {
                    checkJexl(memb, sce);
                }
            }
            if (syncTaskTO.getRoleTemplate() != null) {
                RoleTO template = syncTaskTO.getRoleTemplate();

                if (StringUtils.isNotBlank(template.getName()) && !JexlUtil.isExpressionValid(template.getName())) {
                    sce.getElements().add("Invalid JEXL: " + template.getName());
                }

                checkJexl(template, sce);
            }
            if (!sce.isEmpty()) {
                throw sce;
            }

            // 2. all JEXL expressions are valid: accept user and role templates
            syncTask.setUserTemplate(syncTaskTO.getUserTemplate());
            syncTask.setRoleTemplate(syncTaskTO.getRoleTemplate());

            syncTask.setFullReconciliation(syncTaskTO.isFullReconciliation());
        }

        // 3. fill the remaining fields
        task.setPerformCreate(taskTO.isPerformCreate());
        task.setPerformUpdate(taskTO.isPerformUpdate());
        task.setPerformDelete(taskTO.isPerformDelete());
        task.setSyncStatus(taskTO.isSyncStatus());
        task.getActionsClassNames().clear();
        task.getActionsClassNames().addAll(taskTO.getActionsClassNames());
    }

    @Override
    public SchedTask createSchedTask(final SchedTaskTO taskTO, final TaskUtil taskUtil) {
        final Class<? extends AbstractTaskTO> taskTOClass = taskUtil.taskTOClass();

        if (taskTOClass == null || !taskTOClass.equals(taskTO.getClass())) {
            throw new IllegalArgumentException(
                    String.format("taskUtil is type %s but task is not: %s", taskTOClass, taskTO.getClass()));
        }

        SchedTask task = taskUtil.newTask();
        task.setCronExpression(taskTO.getCronExpression());
        task.setName(taskTO.getName());
        task.setDescription(taskTO.getDescription());

        if (taskUtil.getType() == TaskType.SCHEDULED) {
            task.setJobClassName(taskTO.getJobClassName());
        } else if (taskTO instanceof AbstractProvisioningTaskTO) {
            final AbstractProvisioningTaskTO provisioningTaskTO = (AbstractProvisioningTaskTO) taskTO;

            ExternalResource resource = resourceDAO.find(provisioningTaskTO.getResource());
            if (resource == null) {
                throw new NotFoundException("Resource " + provisioningTaskTO.getResource());
            }
            ((ProvisioningTask) task).setResource(resource);

            fill((ProvisioningTask) task, provisioningTaskTO);
        }

        return task;
    }

    @Override
    public void updateSchedTask(final SchedTask task, final SchedTaskTO taskTO, final TaskUtil taskUtil) {
        Class<? extends Task> taskClass = taskUtil.taskClass();
        Class<? extends AbstractTaskTO> taskTOClass = taskUtil.taskTOClass();

        if (taskClass == null || !taskClass.isAssignableFrom(task.getClass())) {
            throw new IllegalArgumentException(
                    String.format("taskUtil is type %s but task is not: %s", taskClass, task.getClass()));
        }

        if (taskTOClass == null || !taskTOClass.equals(taskTO.getClass())) {
            throw new IllegalArgumentException(
                    String.format("taskUtil is type %s but task is not: %s", taskTOClass, taskTO.getClass()));
        }

        task.setCronExpression(taskTO.getCronExpression());
        if (StringUtils.isNotBlank(taskTO.getName())) {
            task.setName(taskTO.getName());
        }
        if (StringUtils.isNotBlank(taskTO.getDescription())) {
            task.setDescription(taskTO.getDescription());
        }

        if (task instanceof ProvisioningTask) {
            fill((ProvisioningTask) task, (AbstractProvisioningTaskTO) taskTO);
        }
    }

    @Override
    public TaskExecTO getTaskExecTO(final TaskExec execution) {
        TaskExecTO executionTO = new TaskExecTO();
        BeanUtils.copyProperties(execution, executionTO, IGNORE_TASK_EXECUTION_PROPERTIES);

        if (execution.getKey() != null) {
            executionTO.setKey(execution.getKey());
        }

        if (execution.getTask() != null && execution.getTask().getKey() != null) {
            executionTO.setTask(execution.getTask().getKey());
        }

        return executionTO;
    }

    private void setExecTime(final SchedTaskTO taskTO, final Task task) {
        String triggerName = JobNamer.getTriggerName(JobNamer.getJobName(task));

        Trigger trigger = null;
        try {
            trigger = scheduler.getScheduler().getTrigger(new TriggerKey(triggerName, Scheduler.DEFAULT_GROUP));
        } catch (SchedulerException e) {
            LOG.warn("While trying to get to " + triggerName, e);
        }

        if (trigger != null) {
            taskTO.setLastExec(trigger.getPreviousFireTime());
            taskTO.setNextExec(trigger.getNextFireTime());
        }
    }

    @Override
    public <T extends AbstractTaskTO> T getTaskTO(final Task task, final TaskUtil taskUtil) {
        T taskTO = taskUtil.newTaskTO();
        BeanUtils.copyProperties(task, taskTO, IGNORE_TASK_PROPERTIES);

        TaskExec latestExec = taskExecDAO.findLatestStarted(task);
        taskTO.setLatestExecStatus(latestExec == null ? "" : latestExec.getStatus());
        taskTO.setStartDate(latestExec == null ? null : latestExec.getStartDate());
        taskTO.setEndDate(latestExec == null ? null : latestExec.getEndDate());

        for (TaskExec execution : task.getExecs()) {
            taskTO.getExecutions().add(getTaskExecTO(execution));
        }

        switch (taskUtil.getType()) {
            case PROPAGATION:
                if (!(task instanceof PropagationTask)) {
                    throw new IllegalArgumentException("taskUtil is type Propagation but task is not PropagationTask: "
                            + task.getClass().getName());
                }
                ((PropagationTaskTO) taskTO).setResource(((PropagationTask) task).getResource().getKey());
                break;

            case SCHEDULED:
                if (!(task instanceof SchedTask)) {
                    throw new IllegalArgumentException("taskUtil is type Sched but task is not SchedTask: "
                            + task.getClass().getName());
                }
                setExecTime((SchedTaskTO) taskTO, task);
                ((SchedTaskTO) taskTO).setName(((SchedTask) task).getName());
                ((SchedTaskTO) taskTO).setDescription(((SchedTask) task).getDescription());
                break;

            case SYNCHRONIZATION:
                if (!(task instanceof SyncTask)) {
                    throw new IllegalArgumentException("taskUtil is type Sync but task is not SyncTask: "
                            + task.getClass().getName());
                }
                setExecTime((SchedTaskTO) taskTO, task);
                ((SyncTaskTO) taskTO).setName(((SyncTask) task).getName());
                ((SyncTaskTO) taskTO).setDescription(((SyncTask) task).getDescription());
                ((SyncTaskTO) taskTO).setResource(((SyncTask) task).getResource().getKey());
                ((SyncTaskTO) taskTO).setMatchingRule(((SyncTask) task).getMatchingRule() == null
                        ? MatchingRule.UPDATE : ((SyncTask) task).getMatchingRule());
                ((SyncTaskTO) taskTO).setUnmatchingRule(((SyncTask) task).getUnmatchingRule() == null
                        ? UnmatchingRule.PROVISION : ((SyncTask) task).getUnmatchingRule());
                break;

            case PUSH:
                if (!(task instanceof PushTask)) {
                    throw new IllegalArgumentException("taskUtil is type Push but task is not PushTask: "
                            + task.getClass().getName());
                }
                setExecTime((SchedTaskTO) taskTO, task);
                ((PushTaskTO) taskTO).setName(((PushTask) task).getName());
                ((PushTaskTO) taskTO).setDescription(((PushTask) task).getDescription());
                ((PushTaskTO) taskTO).setResource(((PushTask) task).getResource().getKey());
                ((PushTaskTO) taskTO).setMatchingRule(((PushTask) task).getMatchingRule() == null
                        ? MatchingRule.LINK : ((PushTask) task).getMatchingRule());
                ((PushTaskTO) taskTO).setUnmatchingRule(((PushTask) task).getUnmatchingRule() == null
                        ? UnmatchingRule.ASSIGN : ((PushTask) task).getUnmatchingRule());
                break;

            case NOTIFICATION:
                if (((NotificationTask) task).isExecuted() && StringUtils.isBlank(taskTO.getLatestExecStatus())) {
                    taskTO.setLatestExecStatus("[EXECUTED]");
                }
                break;

            default:
        }

        return taskTO;
    }
}
