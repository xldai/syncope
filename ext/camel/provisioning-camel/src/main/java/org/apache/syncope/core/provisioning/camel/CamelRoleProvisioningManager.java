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
package org.apache.syncope.core.provisioning.camel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.syncope.common.lib.mod.RoleMod;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.core.provisioning.api.RoleProvisioningManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;

public class CamelRoleProvisioningManager extends AbstractCamelProvisioningManager implements RoleProvisioningManager {

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final RoleTO subject) {
        return create(subject, Collections.<String>emptySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map.Entry<Long, List<PropagationStatus>> create(final RoleTO roleTO, final Set<String> excludedResources) {
        PollingConsumer pollingConsumer = getConsumer("direct:createRolePort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);

        sendMessage("direct:createRole", roleTO, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Map.Entry.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map.Entry<Long, List<PropagationStatus>> create(final RoleTO roleTO, final Map<Long, String> roleOwnerMap,
            final Set<String> excludedResources) throws PropagationException {

        PollingConsumer pollingConsumer = getConsumer("direct:createRoleInSyncPort");

        Map<String, Object> props = new HashMap<>();
        props.put("roleOwnerMap", roleOwnerMap);
        props.put("excludedResources", excludedResources);

        sendMessage("direct:createRoleInSync", roleTO, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Map.Entry.class);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(final RoleMod subjectMod) {
        return update(subjectMod, Collections.<String>emptySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map.Entry<Long, List<PropagationStatus>> update(
            final RoleMod subjectMod, final Set<String> excludedResources) {

        PollingConsumer pollingConsumer = getConsumer("direct:updateRolePort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);

        sendMessage("direct:updateRole", subjectMod, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Map.Entry.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> delete(final Long roleKey) {
        PollingConsumer pollingConsumer = getConsumer("direct:deleteRolePort");

        sendMessage("direct:deleteRole", roleKey);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    public Long unlink(final RoleMod roleMod) {
        PollingConsumer pollingConsumer = getConsumer("direct:unlinkRolePort");

        sendMessage("direct:unlinkRole", roleMod);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Long.class);
    }

    @Override
    public Long link(final RoleMod roleMod) {
        PollingConsumer pollingConsumer = getConsumer("direct:linkRolePort");

        sendMessage("direct:linkRole", roleMod);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Long.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> deprovision(final Long roleKey, Collection<String> resources) {
        PollingConsumer pollingConsumer = getConsumer("direct:deprovisionRolePort");

        Map<String, Object> props = new HashMap<>();
        props.put("resources", resources);

        sendMessage("direct:deprovisionRole", roleKey, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

}
