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
package org.apache.syncope.persistence.jpa.entity;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.PolicySpec;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.persistence.api.entity.Policy;
import org.apache.syncope.persistence.jpa.validation.entity.PolicyCheck;
import org.apache.syncope.server.utils.serialization.POJOHelper;

@Entity
@Table(name = JPAPolicy.TABLE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE")
@PolicyCheck
public abstract class JPAPolicy extends AbstractEntity<Long> implements Policy {

    private static final long serialVersionUID = -5844833125843247458L;

    public static final String TABLE = "Policy";

    @Id
    private Long id;

    @NotNull
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    protected PolicyType type;

    @Lob
    private String specification;

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public PolicyType getType() {
        return type;
    }

    @Override
    public <T extends PolicySpec> T getSpecification(final Class<T> reference) {
        return POJOHelper.deserialize(specification, reference);
    }

    @Override
    public void setSpecification(final PolicySpec policy) {
        this.specification = POJOHelper.serialize(policy);
    }
}
