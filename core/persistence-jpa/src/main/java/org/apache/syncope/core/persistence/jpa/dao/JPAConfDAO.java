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
package org.apache.syncope.core.persistence.jpa.dao;

import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilFactory;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainSchema;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPAConf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAConfDAO extends AbstractDAO<Conf, Long> implements ConfDAO {

    @Autowired
    private PlainSchemaDAO schemaDAO;

    @Autowired
    private PlainAttrDAO attrDAO;

    @Autowired
    private AttributableUtilFactory attrUtilFactory;

    @Override
    public Conf get() {
        Conf instance = entityManager.find(JPAConf.class, 1L);
        if (instance == null) {
            instance = new JPAConf();
            instance.setKey(1L);

            instance = entityManager.merge(instance);
        }

        return instance;
    }

    @Transactional(readOnly = true)
    @Override
    public CPlainAttr find(final String key) {
        return get().getPlainAttr(key);
    }

    @Transactional(readOnly = true)
    @Override
    public CPlainAttr find(final String key, final String defaultValue) {
        CPlainAttr result = find(key);
        if (result == null) {
            result = new JPACPlainAttr();
            result.setSchema(schemaDAO.find(key, CPlainSchema.class));

            result.addValue(defaultValue, attrUtilFactory.getInstance(AttributableType.CONFIGURATION));
        }

        return result;
    }

    @Override
    public Conf save(final CPlainAttr attr) {
        Conf instance = get();

        CPlainAttr old = instance.getPlainAttr(attr.getSchema().getKey());
        if (old != null && (!attr.getSchema().isUniqueConstraint()
                || (!attr.getUniqueValue().getStringValue().equals(old.getUniqueValue().getStringValue())))) {

            instance.removePlainAttr(old);
            attrDAO.delete(old.getKey(), CPlainAttr.class);
        }

        instance.addPlainAttr(attr);
        attr.setOwner(instance);

        return entityManager.merge(instance);
    }

    @Override
    public Conf delete(final String key) {
        Conf instance = get();
        CPlainAttr attr = instance.getPlainAttr(key);
        if (attr != null) {
            instance.removePlainAttr(attr);
            instance = entityManager.merge(instance);
        }

        return instance;
    }
}
