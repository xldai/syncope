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
package org.apache.syncope.core.persistence.api.dao;

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.VirAttr;

public interface SubjectSearchDAO extends DAO<Subject<?, ?, ?>, Long> {

    /**
     * @param adminRoles the set of admin roles owned by the caller
     * @param searchCondition the search condition
     * @param type user or role
     * @return size of search result
     */
    int count(Set<Long> adminRoles, SearchCond searchCondition, SubjectType type);

    /**
     * @param adminRoles the set of admin roles owned by the caller
     * @param searchCondition the search condition
     * @param type user or role
     * @param <T> user/role
     * @return the list of users/roles matching the given search condition
     */
    <T extends Subject<?, ?, ?>> List<T> search(
            Set<Long> adminRoles, SearchCond searchCondition, SubjectType type);

    /**
     * @param adminRoles the set of admin roles owned by the caller
     * @param searchCondition the search condition
     * @param orderBy list of ordering clauses
     * @param type user or role
     * @param <T> user/role
     * @return the list of users/roles matching the given search condition
     */
    <T extends Subject<?, ?, ?>> List<T> search(
            Set<Long> adminRoles, SearchCond searchCondition, List<OrderByClause> orderBy, SubjectType type);

    /**
     * @param adminRoles the set of admin roles owned by the caller
     * @param searchCondition the search condition
     * @param page position of the first result, start from 1
     * @param itemsPerPage number of results per page
     * @param orderBy list of ordering clauses
     * @param type user or role
     * @param <T> user/role
     * @return the list of users/roles matching the given search condition (in the given page)
     */
    <T extends Subject<?, ?, ?>> List<T> search(
            Set<Long> adminRoles, SearchCond searchCondition, int page, int itemsPerPage,
            List<OrderByClause> orderBy, SubjectType type);

    /**
     * Verify if user/role matches the given search condition.
     *
     * @param subject to be checked
     * @param searchCondition to be verified
     * @param type user or role
     * @param <T> user/role
     * @return true if user/role matches searchCondition
     */
    <T extends Subject<?, ?, ?>> boolean matches(T subject, SearchCond searchCondition, SubjectType type);
}
