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
package org.apache.syncope.core.provisioning.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.MembershipDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.AttributableUtil;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.MappingItem;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.role.RVirAttr;
import org.apache.syncope.core.persistence.api.entity.role.RVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.role.Role;
import org.apache.syncope.core.persistence.api.entity.user.UVirAttr;
import org.apache.syncope.core.persistence.api.entity.user.UVirSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class VirAttrHandler {

    private static final Logger LOG = LoggerFactory.getLogger(VirAttrHandler.class);

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private VirAttrDAO virAttrDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private MembershipDAO membershipDAO;

    @Autowired
    private AttributableUtilFactory attrUtilFactory;

    public <T extends VirSchema> T getVirSchema(final String virSchemaName, final Class<T> reference) {
        T virtualSchema = null;
        if (StringUtils.isNotBlank(virSchemaName)) {
            virtualSchema = virSchemaDAO.find(virSchemaName, reference);

            if (virtualSchema == null) {
                LOG.debug("Ignoring invalid virtual schema {}", virSchemaName);
            }
        }

        return virtualSchema;
    }

    public void setVirAttrSchema(final Attributable<?, ?, ?> attributable,
            final VirAttr virAttr, final VirSchema virSchema) {

        if (virAttr instanceof UVirAttr) {
            ((UVirAttr) virAttr).setSchema((UVirSchema) virSchema);
        } else if (virAttr instanceof RVirAttr) {
            RVirAttrTemplate template = ((Role) attributable).
                    getAttrTemplate(RVirAttrTemplate.class, virSchema.getKey());
            if (template != null) {
                ((RVirAttr) virAttr).setTemplate(template);
            }
        } else if (virAttr instanceof MVirAttr) {
            MVirAttrTemplate template =
                    ((Membership) attributable).getRole().
                    getAttrTemplate(MVirAttrTemplate.class, virSchema.getKey());
            if (template != null) {
                ((MVirAttr) virAttr).setTemplate(template);
            }
        }
    }

    public void updateOnResourcesIfMappingMatches(final AttributableUtil attrUtil, final String schemaKey,
            final Set<ExternalResource> resources, final IntMappingType mappingType,
            final PropagationByResource propByRes) {

        for (ExternalResource resource : resources) {
            for (MappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                if (schemaKey.equals(mapItem.getIntAttrName()) && mapItem.getIntMappingType() == mappingType) {
                    propByRes.add(ResourceOperation.UPDATE, resource.getKey());
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public PropagationByResource fillVirtual(final Attributable attributable,
            final Set<String> vAttrsToBeRemoved, final Set<AttrMod> vAttrsToBeUpdated,
            final AttributableUtil attrUtil) {

        PropagationByResource propByRes = new PropagationByResource();

        final Set<ExternalResource> externalResources = new HashSet<>();
        if (attributable instanceof Subject) {
            externalResources.addAll(((Subject<?, ?, ?>) attributable).getResources());
        } else if (attributable instanceof Membership) {
            externalResources.addAll(((Membership) attributable).getUser().getResources());
            externalResources.addAll(((Membership) attributable).getRole().getResources());
        }

        // 1. virtual attributes to be removed
        for (String vAttrToBeRemoved : vAttrsToBeRemoved) {
            VirSchema virSchema = getVirSchema(vAttrToBeRemoved, attrUtil.virSchemaClass());
            if (virSchema != null) {
                VirAttr virAttr = attributable.getVirAttr(virSchema.getKey());
                if (virAttr == null) {
                    LOG.debug("No virtual attribute found for schema {}", virSchema.getKey());
                } else {
                    attributable.removeVirAttr(virAttr);
                    virAttrDAO.delete(virAttr);
                }

                for (ExternalResource resource : externalResources) {
                    for (MappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                        if (virSchema.getKey().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == attrUtil.virIntMappingType()) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getKey());

                            // Using virtual attribute as AccountId must be avoided
                            if (mapItem.isAccountid() && virAttr != null && !virAttr.getValues().isEmpty()) {
                                propByRes.addOldAccountId(resource.getKey(), virAttr.getValues().get(0));
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Virtual attributes to be removed:\n{}", propByRes);

        // 2. virtual attributes to be updated
        for (AttrMod vAttrToBeUpdated : vAttrsToBeUpdated) {
            VirSchema virSchema = getVirSchema(vAttrToBeUpdated.getSchema(), attrUtil.virSchemaClass());
            VirAttr virAttr = null;
            if (virSchema != null) {
                virAttr = attributable.getVirAttr(virSchema.getKey());
                if (virAttr == null) {
                    virAttr = attrUtil.newVirAttr();
                    setVirAttrSchema(attributable, virAttr, virSchema);
                    if (virAttr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", vAttrToBeUpdated);
                    } else {
                        attributable.addVirAttr(virAttr);
                    }
                }
            }

            if (virSchema != null && virAttr != null && virAttr.getSchema() != null) {
                if (attributable instanceof Subject) {
                    updateOnResourcesIfMappingMatches(attrUtil, virSchema.getKey(),
                            externalResources, attrUtil.derIntMappingType(), propByRes);
                } else if (attributable instanceof Membership) {
                    updateOnResourcesIfMappingMatches(attrUtil, virSchema.getKey(),
                            externalResources, IntMappingType.MembershipVirtualSchema, propByRes);
                }

                final List<String> values = new ArrayList<>(virAttr.getValues());
                values.removeAll(vAttrToBeUpdated.getValuesToBeRemoved());
                values.addAll(vAttrToBeUpdated.getValuesToBeAdded());

                virAttr.getValues().clear();
                virAttr.getValues().addAll(values);

                // Owner cannot be specified before otherwise a virtual attribute remove will be invalidated.
                virAttr.setOwner(attributable);
            }
        }

        LOG.debug("Virtual attributes to be added:\n{}", propByRes);

        return propByRes;
    }

    /**
     * Add virtual attributes and specify values to be propagated.
     *
     * @param attributable attributable.
     * @param vAttrs virtual attributes to be added.
     * @param attrUtil attributable util.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void fillVirtual(final Attributable attributable, final Collection<AttrTO> vAttrs,
            final AttributableUtil attrUtil) {

        for (AttrTO attributeTO : vAttrs) {
            VirAttr virAttr = attributable.getVirAttr(attributeTO.getSchema());
            if (virAttr == null) {
                VirSchema virSchema = getVirSchema(attributeTO.getSchema(), attrUtil.virSchemaClass());
                if (virSchema != null) {
                    virAttr = attrUtil.newVirAttr();
                    setVirAttrSchema(attributable, virAttr, virSchema);
                    if (virAttr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", attributeTO);
                    } else {
                        virAttr.setOwner(attributable);
                        attributable.addVirAttr(virAttr);
                        virAttr.getValues().clear();
                        virAttr.getValues().addAll(attributeTO.getValues());
                    }
                }
            } else {
                virAttr.getValues().clear();
                virAttr.getValues().addAll(attributeTO.getValues());
            }
        }
    }

    /**
     * SYNCOPE-459: build virtual attribute changes in case no other changes were made.
     *
     * @param key user id
     * @param vAttrsToBeRemoved virtual attributes to be removed.
     * @param vAttrsToBeUpdated virtual attributes to be updated.
     * @return operations to be performed on external resources for virtual attributes changes
     */
    public PropagationByResource fillVirtual(
            final Long key, final Set<String> vAttrsToBeRemoved, final Set<AttrMod> vAttrsToBeUpdated) {

        return fillVirtual(
                userDAO.authFetch(key),
                vAttrsToBeRemoved,
                vAttrsToBeUpdated,
                attrUtilFactory.getInstance(AttributableType.USER));
    }

    private Set<String> getAttrNames(final List<? extends VirAttr> virAttrs) {
        final Set<String> virAttrNames = new HashSet<>();
        for (VirAttr attr : virAttrs) {
            virAttrNames.add(attr.getSchema().getKey());
        }
        return virAttrNames;
    }

    /**
     * SYNCOPE-501: build membership virtual attribute changes in case no other changes were made.
     *
     * @param key user key
     * @param roleKey role key
     * @param membershipKey membership key
     * @param vAttrsToBeRemoved virtual attributes to be removed.
     * @param vAttrsToBeUpdated virtual attributes to be updated.
     * @param isRemoval flag to check if fill is on removed or added membership
     * @return operations to be performed on external resources for membership virtual attributes changes
     */
    public PropagationByResource fillMembershipVirtual(
            final Long key, final Long roleKey, final Long membershipKey, final Set<String> vAttrsToBeRemoved,
            final Set<AttrMod> vAttrsToBeUpdated, final boolean isRemoval) {

        final Membership membership = membershipKey == null
                ? userDAO.authFetch(key).getMembership(roleKey)
                : membershipDAO.authFetch(membershipKey);

        return membership == null ? new PropagationByResource() : isRemoval
                ? fillVirtual(
                        membership,
                        getAttrNames(membership.getVirAttrs()),
                        vAttrsToBeUpdated,
                        attrUtilFactory.getInstance(AttributableType.MEMBERSHIP))
                : fillVirtual(
                        membership,
                        vAttrsToBeRemoved,
                        vAttrsToBeUpdated,
                        attrUtilFactory.getInstance(AttributableType.MEMBERSHIP));
    }

}
