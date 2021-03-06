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
package org.apache.syncope.core.workflow.activiti.task;

import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.apache.syncope.core.workflow.activiti.ActivitiUserWorkflowAdapter;
import org.springframework.stereotype.Component;

@Component
public class PasswordReset extends AbstractActivitiServiceTask {

    @Override
    protected void doExecute(final String executionId) {
        User user = runtimeService.getVariable(executionId, ActivitiUserWorkflowAdapter.USER, User.class);
        String token = runtimeService.getVariable(executionId, ActivitiUserWorkflowAdapter.TOKEN, String.class);
        String password = runtimeService.getVariable(executionId, ActivitiUserWorkflowAdapter.PASSWORD, String.class);

        if (!user.checkToken(token)) {
            throw new WorkflowException(new IllegalArgumentException("Wrong token: " + token + " for " + user));
        }

        user.removeToken();
        user.setPassword(password, user.getCipherAlgorithm());
        runtimeService.setVariable(executionId, ActivitiUserWorkflowAdapter.USER, user);
    }

}
