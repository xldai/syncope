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
package org.apache.syncope.core.persistence.api.entity;

import java.util.List;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.TraceLevel;

public interface Notification extends Entity<Long> {

    boolean addEvent(String event);

    boolean addStaticRecipient(String staticRecipient);

    List<String> getEvents();

    String getRecipientAttrName();

    IntMappingType getRecipientAttrType();

    String getRecipients();

    String getRoleAbout();

    String getSender();

    List<String> getStaticRecipients();

    String getSubject();

    String getTemplate();

    TraceLevel getTraceLevel();

    String getUserAbout();

    boolean isActive();

    boolean isSelfAsRecipient();

    boolean removeEvent(String event);

    boolean removeStaticRecipient(String staticRecipient);

    void setActive(boolean active);

    void setRecipientAttrName(String recipientAttrName);

    void setRecipientAttrType(IntMappingType recipientAttrType);

    void setRecipients(String recipients);

    void setRoleAbout(String roleAbout);

    void setSelfAsRecipient(boolean selfAsRecipient);

    void setSender(String sender);

    void setSubject(String subject);

    void setTemplate(String template);

    void setTraceLevel(TraceLevel traceLevel);

    void setUserAbout(String userAbout);
}
