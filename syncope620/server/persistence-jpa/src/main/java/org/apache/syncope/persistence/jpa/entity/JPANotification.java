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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.persistence.api.entity.Notification;
import org.apache.syncope.persistence.jpa.validation.entity.NotificationCheck;

@Entity
@Table(name = JPANotification.TABLE)
@NotificationCheck
public class JPANotification extends AbstractEntity<Long> implements Notification {

    private static final long serialVersionUID = 3112582296912757537L;

    public static final String TABLE = "Notification";

    @Id
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "Notification_events",
            joinColumns =
            @JoinColumn(name = "Notification_id", referencedColumnName = "id"))
    @Column(name = "events")
    private List<String> events;

    private String userAbout;

    private String roleAbout;

    private String recipients;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "Notification_staticRecipients",
            joinColumns =
            @JoinColumn(name = "Notification_id", referencedColumnName = "id"))
    @Column(name = "staticRecipients")
    private List<String> staticRecipients;

    @NotNull
    @Enumerated(EnumType.STRING)
    private IntMappingType recipientAttrType;

    @NotNull
    private String recipientAttrName;

    @Column(nullable = false)
    @Basic
    @Min(0)
    @Max(1)
    private Integer selfAsRecipient;

    @NotNull
    private String sender;

    @NotNull
    private String subject;

    @NotNull
    private String template;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TraceLevel traceLevel;

    @Column(nullable = false)
    @Basic
    @Min(0)
    @Max(1)
    private Integer active;

    public JPANotification() {
        events = new ArrayList<>();
        staticRecipients = new ArrayList<>();
        selfAsRecipient = getBooleanAsInteger(false);
        active = getBooleanAsInteger(true);
        traceLevel = TraceLevel.ALL;
    }

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public String getUserAbout() {
        return userAbout;
    }

    @Override
    public void setUserAbout(final String userAbout) {
        this.userAbout = userAbout;
    }

    @Override
    public String getRoleAbout() {
        return roleAbout;
    }

    @Override
    public void setRoleAbout(final String roleAbout) {
        this.roleAbout = roleAbout;
    }

    @Override
    public String getRecipients() {
        return recipients;
    }

    @Override
    public void setRecipients(final String recipients) {
        this.recipients = recipients;
    }

    @Override
    public String getRecipientAttrName() {
        return recipientAttrName;
    }

    @Override
    public void setRecipientAttrName(final String recipientAttrName) {
        this.recipientAttrName = recipientAttrName;
    }

    @Override
    public IntMappingType getRecipientAttrType() {
        return recipientAttrType;
    }

    @Override

    public void setRecipientAttrType(final IntMappingType recipientAttrType) {
        this.recipientAttrType = recipientAttrType;
    }

    @Override
    public List<String> getEvents() {
        return events;
    }

    @Override
    public boolean addEvent(final String event) {
        return event != null && !events.contains(event) && events.add(event);
    }

    @Override
    public boolean removeEvent(final String event) {
        return event != null && events.remove(event);
    }

    @Override
    public List<String> getStaticRecipients() {
        return staticRecipients;
    }

    @Override
    public boolean addStaticRecipient(final String staticRecipient) {
        return staticRecipient != null && !staticRecipients.contains(staticRecipient)
                && staticRecipients.add(staticRecipient);
    }

    @Override
    public boolean removeStaticRecipient(final String staticRecipient) {
        return staticRecipient != null && staticRecipients.remove(staticRecipient);
    }

    @Override
    public boolean isSelfAsRecipient() {
        return isBooleanAsInteger(selfAsRecipient);
    }

    @Override
    public void setSelfAsRecipient(final boolean selfAsRecipient) {
        this.selfAsRecipient = getBooleanAsInteger(selfAsRecipient);
    }

    @Override
    public String getSender() {
        return sender;
    }

    @Override
    public void setSender(final String sender) {
        this.sender = sender;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(final String template) {
        this.template = template;
    }

    @Override
    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    @Override
    public void setTraceLevel(final TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
    }

    @Override
    public boolean isActive() {
        return isBooleanAsInteger(active);
    }

    @Override
    public void setActive(final boolean active) {
        this.active = getBooleanAsInteger(active);
    }
}
