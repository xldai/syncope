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
package org.apache.syncope.core.rest.cxf.service;

import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.core.logic.SyncopeLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserSelfServiceImpl extends AbstractServiceImpl implements UserSelfService {

    @Autowired
    private UserLogic logic;

    @Autowired
    private SyncopeLogic syncopeLogic;

    @Override
    public Response create(final UserTO userTO, final boolean storePassword) {
        if (!syncopeLogic.isSelfRegAllowed()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unauthorized);
            sce.getElements().add("Self registration forbidden by configuration");
            throw sce;
        }

        UserTO created = logic.createSelf(userTO, storePassword);
        return createResponse(created.getKey(), created);
    }

    @Override
    public UserTO read() {
        return logic.readSelf();
    }

    @Override
    public Response update(final Long userKey, final UserMod userMod) {
        userMod.setKey(userKey);
        UserTO updated = logic.updateSelf(userMod);
        return modificationResponse(updated);
    }

    @Override
    public Response delete() {
        UserTO deleted = logic.deleteSelf();
        return modificationResponse(deleted);
    }

    @Override
    public void requestPasswordReset(final String username, final String securityAnswer) {
        if (!syncopeLogic.isPwdResetAllowed()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unauthorized);
            sce.getElements().add("Password reset forbidden by configuration");
            throw sce;
        }

        logic.requestPasswordReset(username, securityAnswer);
    }

    @Override
    public void confirmPasswordReset(final String token, final String password) {
        if (!syncopeLogic.isPwdResetAllowed()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unauthorized);
            sce.getElements().add("Password reset forbidden by configuration");
            throw sce;
        }

        logic.confirmPasswordReset(token, password);
    }

}
