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
package org.apache.syncope.core.persistence.jpa.validation.entity;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.ConstraintValidatorContext;
import org.apache.syncope.common.lib.types.AccountPolicySpec;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.PasswordPolicySpec;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.role.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.misc.policy.AccountPolicyEnforcer;
import org.apache.syncope.core.misc.policy.AccountPolicyException;
import org.apache.syncope.core.misc.policy.PasswordPolicyEnforcer;
import org.apache.syncope.core.misc.policy.PolicyEvaluator;
import org.springframework.beans.factory.annotation.Autowired;

public class UserValidator extends AbstractValidator<UserCheck, User> {

    @Resource(name = "adminUser")
    private String adminUser;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private PolicyEvaluator evaluator;

    @Autowired
    private PasswordPolicyEnforcer ppEnforcer;

    @Autowired
    private AccountPolicyEnforcer apEnforcer;

    @Override
    public boolean isValid(final User user, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        // ------------------------------
        // Verify password policies
        // ------------------------------
        LOG.debug("Password Policy enforcement");

        try {
            int maxPPSpecHistory = 0;
            for (Policy policy : getPasswordPolicies(user)) {
                // evaluate policy
                final PasswordPolicySpec ppSpec = evaluator.evaluate(policy, user);
                // enforce policy
                ppEnforcer.enforce(ppSpec, policy.getType(), user);

                if (ppSpec.getHistoryLength() > maxPPSpecHistory) {
                    maxPPSpecHistory = ppSpec.getHistoryLength();
                }
            }

            // update user's password history with encrypted password
            if (maxPPSpecHistory > 0 && user.getPassword() != null) {
                user.getPasswordHistory().add(user.getPassword());
            }
            // keep only the last maxPPSpecHistory items in user's password history
            if (maxPPSpecHistory < user.getPasswordHistory().size()) {
                for (int i = 0; i < user.getPasswordHistory().size() - maxPPSpecHistory; i++) {
                    user.getPasswordHistory().remove(i);
                }
            }
        } catch (Exception e) {
            LOG.debug("Invalid password");

            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidPassword, e.getMessage())).
                    addPropertyNode("password").addConstraintViolation();

            return false;
        } finally {
            // password has been validated, let's remove its clear version
            user.removeClearPassword();
        }
        // ------------------------------

        // ------------------------------
        // Verify account policies
        // ------------------------------
        LOG.debug("Account Policy enforcement");

        try {
            if (adminUser.equals(user.getUsername()) || anonymousUser.equals(user.getUsername())) {
                throw new AccountPolicyException("Not allowed: " + user.getUsername());
            }

            // invalid username
            for (Policy policy : getAccountPolicies(user)) {
                // evaluate policy
                final AccountPolicySpec accountPolicy = evaluator.evaluate(policy, user);

                // enforce policy
                apEnforcer.enforce(accountPolicy, policy.getType(), user);
            }
        } catch (Exception e) {
            LOG.debug("Invalid username");

            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidUsername, e.getMessage())).
                    addPropertyNode("username").addConstraintViolation();

            return false;
        }
        // ------------------------------

        return true;
    }

    private List<PasswordPolicy> getPasswordPolicies(final User user) {
        final List<PasswordPolicy> policies = new ArrayList<>();

        // Add global policy
        PasswordPolicy policy = policyDAO.getGlobalPasswordPolicy();
        if (policy != null) {
            policies.add(policy);
        }

        // add resource policies
        for (ExternalResource resource : user.getResources()) {
            policy = resource.getPasswordPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        // add role policies
        for (Role role : user.getRoles()) {
            policy = role.getPasswordPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        return policies;
    }

    private List<AccountPolicy> getAccountPolicies(final User user) {
        final List<AccountPolicy> policies = new ArrayList<>();

        // add global policy
        AccountPolicy policy = policyDAO.getGlobalAccountPolicy();
        if (policy != null) {
            policies.add(policy);
        }

        // add resource policies
        for (ExternalResource resource : user.getResources()) {
            policy = resource.getAccountPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        // add role policies
        for (Role role : user.getRoles()) {
            policy = role.getAccountPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        return policies;
    }
}
