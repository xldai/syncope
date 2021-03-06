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
package org.apache.syncope.core.provisioning.api.sync;

import org.apache.syncope.core.persistence.api.entity.Subject;
import org.quartz.JobExecutionException;

/**
 * Interface for actions to be performed during PushJob execution.
 */
public interface PushActions extends ProvisioningActions {

    /**
     * Action to be executed before to assign (link & provision) a synchronized user / role to the resource.
     *
     * @param profile profile of the synchronization being executed.
     * @param subject user / role to be created.
     * @return subject.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends Subject<?, ?, ?>> T beforeAssign(
            final ProvisioningProfile<?, ?> profile,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to provision a synchronized user / role to the resource.
     *
     * @param profile profile of the synchronization being executed.
     * @param subject user / role to be created.
     * @return subject.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends Subject<?, ?, ?>> T beforeProvision(
            final ProvisioningProfile<?, ?> profile,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to update a synchronized user / role on the resource.
     *
     * @param profile profile of the synchronization being executed.
     * @param subject user / role to be updated.
     * @return subject.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends Subject<?, ?, ?>> T beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to link a synchronized user / role to the resource.
     *
     * @param profile profile of the synchronization being executed.
     * @param subject user / role to be created.
     * @return subject.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends Subject<?, ?, ?>> T beforeLink(
            final ProvisioningProfile<?, ?> profile,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to unlink a synchronized user / role from the resource.
     *
     * @param profile profile of the synchronization being executed.
     * @param subject user / role to be created.
     * @return subject.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends Subject<?, ?, ?>> T beforeUnlink(
            final ProvisioningProfile<?, ?> profile,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to unassign a synchronized user / role from the resource.
     *
     * @param profile profile of the synchronization being executed.
     * @param subject user / role to be created.
     * @return subject.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends Subject<?, ?, ?>> T beforeUnassign(
            final ProvisioningProfile<?, ?> profile,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to unassign a synchronized user / role from the resource.
     *
     * @param profile profile of the synchronization being executed.
     * @param subject user / role to be created.
     * @return subject.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends Subject<?, ?, ?>> T beforeDeprovision(
            final ProvisioningProfile<?, ?> profile,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before delete a synchronized user / role locally and from the resource.
     *
     * @param profile profile of the synchronization being executed.
     * @param subject user / role to be created.
     * @return subject.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends Subject<?, ?, ?>> T beforeDelete(
            final ProvisioningProfile<?, ?> profile,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed after each local user / role synchronization.
     *
     * @param profile profile of the synchronization being executed.
     * @param subject synchronized user / role.
     * @param result operation result.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends Subject<?, ?, ?>> void after(
            final ProvisioningProfile<?, ?> profile,
            final T subject,
            final ProvisioningResult result) throws JobExecutionException;
}
