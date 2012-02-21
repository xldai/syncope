/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 */
package org.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.dao.VirAttrDAO;

@Repository
public class VirAttrDAOImpl extends AbstractDAOImpl implements VirAttrDAO {

    @Override
    public <T extends AbstractVirAttr> T find(
            final Long id, final Class<T> reference) {

        return entityManager.find(reference, id);
    }

    @Override
    public <T extends AbstractVirAttr> List<T> findAll(
            final Class<T> reference) {

        Query query = entityManager.createQuery(
                "SELECT e FROM " + reference.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    public <T extends AbstractVirAttr> T save(
            final T virtualAttribute) {

        return entityManager.merge(virtualAttribute);
    }

    @Override
    public <T extends AbstractVirAttr> void delete(
            final Long id, final Class<T> reference) {

        T virtualAttribute = find(id, reference);
        if (virtualAttribute == null) {
            return;
        }

        delete(virtualAttribute);
    }

    @Override
    public <T extends AbstractVirAttr> void delete(
            final T virtualAttribute) {

        if (virtualAttribute.getOwner() != null) {
            virtualAttribute.getOwner().
                    removeVirtualAttribute(virtualAttribute);
        }

        entityManager.remove(virtualAttribute);
    }
}
