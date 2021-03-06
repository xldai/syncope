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
package org.apache.syncope.core.persistence.jpa.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.core.persistence.api.RoleEntitlementUtil;
import org.apache.syncope.core.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.SubjectCond;
import org.apache.syncope.core.persistence.api.entity.role.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AttributableSearchTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private SubjectSearchDAO searchDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Test
    public void userMatch() {
        User user = userDAO.find(1L);
        assertNotNull(user);

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(5L);

        assertFalse(searchDAO.matches(user, SearchCond.getLeafCond(membershipCond), SubjectType.USER));

        membershipCond.setRoleId(1L);

        assertTrue(searchDAO.matches(user, SearchCond.getLeafCond(membershipCond), SubjectType.USER));
    }

    @Test
    public void roleMatch() {
        Role role = roleDAO.find(1L);
        assertNotNull(role);

        AttributeCond attrCond = new AttributeCond();
        attrCond.setSchema("show");
        attrCond.setType(AttributeCond.Type.ISNOTNULL);

        assertTrue(searchDAO.matches(role, SearchCond.getLeafCond(attrCond), SubjectType.ROLE));
    }

    @Test
    public void searchWithLikeCondition() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        SearchCond subCond = SearchCond.getAndCond(SearchCond.getLeafCond(fullnameLeafCond), SearchCond.getLeafCond(
                membershipCond));

        assertTrue(subCond.isValid());

        SearchCond cond = SearchCond.getAndCond(subCond, SearchCond.getLeafCond(loginDateCond));

        assertTrue(cond.isValid());

        List<User> users =
                searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), cond, SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchWithNotCondition() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.EQ);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("Giuseppe Verdi");

        SearchCond cond = SearchCond.getNotLeafCond(fullnameLeafCond);
        assertTrue(cond.isValid());

        List<User> users =
                searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), cond, SubjectType.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        Set<Long> ids = new HashSet<>(users.size());
        for (User user : users) {
            ids.add(user.getKey());
        }
        assertTrue(ids.contains(1L));
        assertTrue(ids.contains(3L));
    }

    @Test
    public void searchByBoolean() {
        AttributeCond coolLeafCond = new AttributeCond(AttributeCond.Type.EQ);
        coolLeafCond.setSchema("cool");
        coolLeafCond.setExpression("true");

        SearchCond cond = SearchCond.getLeafCond(coolLeafCond);
        assertTrue(cond.isValid());

        List<User> users =
                searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), cond, SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        assertEquals(Long.valueOf(4L), users.get(0).getKey());
    }

    @Test
    public void searchByPageAndSize() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        SearchCond subCond = SearchCond.getAndCond(SearchCond.getLeafCond(fullnameLeafCond), SearchCond.getLeafCond(
                membershipCond));

        assertTrue(subCond.isValid());

        SearchCond cond = SearchCond.getAndCond(subCond, SearchCond.getLeafCond(loginDateCond));

        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                cond, 1, 2, Collections.<OrderByClause>emptyList(),
                SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        users = searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                cond, 2, 2, Collections.<OrderByClause>emptyList(),
                SubjectType.USER);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void searchByMembership() {
        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        List<User> users = searchDAO.search(
                RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), SearchCond.getLeafCond(membershipCond),
                SubjectType.USER);
        assertNotNull(users);
        assertEquals(2, users.size());

        membershipCond = new MembershipCond();
        membershipCond.setRoleId(5L);

        users = searchDAO.search(
                RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), SearchCond.getNotLeafCond(membershipCond),
                SubjectType.USER);
        assertNotNull(users);
        assertEquals(5, users.size());
    }

    @Test
    public void searchByIsNull() {
        AttributeCond coolLeafCond = new AttributeCond(AttributeCond.Type.ISNULL);
        coolLeafCond.setSchema("cool");

        List<User> users = searchDAO.search(
                RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), SearchCond.getLeafCond(coolLeafCond),
                SubjectType.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        coolLeafCond = new AttributeCond(AttributeCond.Type.ISNOTNULL);
        coolLeafCond.setSchema("cool");

        users = searchDAO.search(
                RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), SearchCond.getLeafCond(coolLeafCond),
                SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByResource() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResourceName("ws-target-resource-2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceName("ws-target-resource-list-mappings-2");

        SearchCond searchCondition = SearchCond.getAndCond(SearchCond.getNotLeafCond(ws2), SearchCond.getLeafCond(ws1));

        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(
                RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), searchCondition,
                SubjectType.USER);

        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByBooleanSubjectCond() {
        SubjectCond booleanCond = new SubjectCond(SubjectCond.Type.EQ);
        booleanCond.setSchema("inheritPlainAttrs");
        booleanCond.setExpression("true");

        SearchCond searchCondition = SearchCond.getLeafCond(booleanCond);

        List<Role> matchingRoles = searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                searchCondition, SubjectType.ROLE);
        assertNotNull(matchingRoles);
        assertFalse(matchingRoles.isEmpty());
    }

    @Test
    public void searchByUsernameAndKey() {
        SubjectCond usernameLeafCond = new SubjectCond(SubjectCond.Type.LIKE);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("%ini");

        SubjectCond idRightCond = new SubjectCond(SubjectCond.Type.LT);
        idRightCond.setSchema("key");
        idRightCond.setExpression("2");

        SearchCond searchCondition = SearchCond.getAndCond(SearchCond.getLeafCond(usernameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        List<User> matchingUsers = searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                searchCondition, SubjectType.USER);

        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.size());
        assertEquals("rossini", matchingUsers.iterator().next().getUsername());
        assertEquals(1L, matchingUsers.iterator().next().getKey().longValue());
    }

    @Test
    public void searchByRolenameAndKey() {
        SubjectCond rolenameLeafCond = new SubjectCond(SubjectCond.Type.EQ);
        rolenameLeafCond.setSchema("name");
        rolenameLeafCond.setExpression("root");

        SubjectCond idRightCond = new SubjectCond(SubjectCond.Type.LT);
        idRightCond.setSchema("key");
        idRightCond.setExpression("2");

        SearchCond searchCondition = SearchCond.getAndCond(SearchCond.getLeafCond(rolenameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        assertTrue(searchCondition.isValid());

        List<Role> matchingRoles = searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                searchCondition, SubjectType.ROLE);

        assertNotNull(matchingRoles);
        assertEquals(1, matchingRoles.size());
        assertEquals("root", matchingRoles.iterator().next().getName());
        assertEquals(1L, matchingRoles.iterator().next().getKey().longValue());
    }

    @Test
    public void searchByUsernameAndFullname() {
        SubjectCond usernameLeafCond = new SubjectCond(SubjectCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("rossini");

        AttributeCond idRightCond = new AttributeCond(AttributeCond.Type.LIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("Giuseppe V%");

        SearchCond searchCondition = SearchCond.getOrCond(SearchCond.getLeafCond(usernameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        List<User> matchingUsers =
                searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), searchCondition,
                        SubjectType.USER);

        assertNotNull(matchingUsers);
        assertEquals(2, matchingUsers.size());
    }

    @Test
    public void searchById() {
        SubjectCond idLeafCond = new SubjectCond(SubjectCond.Type.LT);
        idLeafCond.setSchema("id");
        idLeafCond.setExpression("2");

        SearchCond searchCondition = SearchCond.getLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        List<User> users =
                searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), searchCondition,
                        SubjectType.USER);

        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(1L, users.iterator().next().getKey().longValue());

        idLeafCond = new SubjectCond(SubjectCond.Type.LT);
        idLeafCond.setSchema("id");
        idLeafCond.setExpression("4");

        searchCondition = SearchCond.getNotLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        users = searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), searchCondition,
                SubjectType.USER);

        assertNotNull(users);
        assertEquals(2, users.size());
        boolean found = false;
        for (User user : users) {
            if (user.getKey() == 4) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void userOrderBy() {
        SubjectCond usernameLeafCond = new SubjectCond(SubjectCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("rossini");
        AttributeCond idRightCond = new AttributeCond(AttributeCond.Type.LIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("Giuseppe V%");
        SearchCond searchCondition = SearchCond.getOrCond(
                SearchCond.getLeafCond(usernameLeafCond), SearchCond.getLeafCond(idRightCond));

        List<OrderByClause> orderByClauses = new ArrayList<OrderByClause>();
        OrderByClause orderByClause = new OrderByClause();
        orderByClause.setField("username");
        orderByClause.setDirection(OrderByClause.Direction.DESC);
        orderByClauses.add(orderByClause);
        orderByClause = new OrderByClause();
        orderByClause.setField("fullname");
        orderByClause.setDirection(OrderByClause.Direction.ASC);
        orderByClauses.add(orderByClause);

        List<User> users = searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                searchCondition, Collections.singletonList(orderByClause),
                SubjectType.USER);
        assertEquals(searchDAO.count(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                searchCondition, SubjectType.USER),
                users.size());
    }

    @Test
    public void roleOrderBy() {
        SubjectCond idLeafCond = new SubjectCond(SubjectCond.Type.LIKE);
        idLeafCond.setSchema("name");
        idLeafCond.setExpression("%r");
        SearchCond searchCondition = SearchCond.getLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        OrderByClause orderByClause = new OrderByClause();
        orderByClause.setField("name");

        List<Role> roles = searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                searchCondition, Collections.singletonList(orderByClause), SubjectType.ROLE);
        assertEquals(searchDAO.count(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                searchCondition, SubjectType.ROLE),
                roles.size());
    }

    @Test
    public void issue202() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResourceName("ws-target-resource-2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceName("ws-target-resource-list-mappings-1");

        SearchCond searchCondition =
                SearchCond.getAndCond(SearchCond.getNotLeafCond(ws2), SearchCond.getNotLeafCond(ws1));
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                searchCondition, SubjectType.USER);
        assertNotNull(users);
        assertEquals(2, users.size());
        boolean found = false;
        for (User user : users) {
            if (user.getKey() == 4) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void issue242() {
        SubjectCond cond = new SubjectCond(AttributeCond.Type.LIKE);
        cond.setSchema("id");
        cond.setExpression("test%");

        SearchCond searchCondition = SearchCond.getLeafCond(cond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                searchCondition, SubjectType.USER);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void issueSYNCOPE46() {
        SubjectCond cond = new SubjectCond(AttributeCond.Type.LIKE);
        cond.setSchema("username");
        cond.setExpression("%ossin%");

        SearchCond searchCondition = SearchCond.getLeafCond(cond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                searchCondition, SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void issueSYNCOPE433() {
        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNULL);
        isNullCond.setSchema("loginDate");

        SubjectCond likeCond = new SubjectCond(AttributeCond.Type.LIKE);
        likeCond.setSchema("username");
        likeCond.setExpression("%ossin%");

        SearchCond searchCond = SearchCond.getOrCond(
                SearchCond.getLeafCond(isNullCond), SearchCond.getLeafCond(likeCond));

        Integer count = searchDAO.count(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), searchCond,
                SubjectType.USER);
        assertNotNull(count);
        assertTrue(count > 0);
    }
}
