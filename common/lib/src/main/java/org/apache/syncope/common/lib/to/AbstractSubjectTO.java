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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType
public abstract class AbstractSubjectTO extends AbstractAttributableTO {

    private static final long serialVersionUID = 114668706977919206L;

    private final Set<String> resources = new HashSet<>();

    private final List<PropagationStatus> propagationStatusTOs = new ArrayList<>();

    @XmlElementWrapper(name = "resources")
    @XmlElement(name = "resource")
    @JsonProperty("resources")
    public Set<String> getResources() {
        return resources;
    }

    public boolean removePropagationTO(final String resource) {
        if (resource != null && getPropagationStatusTOs().isEmpty()) {
            final List<PropagationStatus> toBeRemoved = new ArrayList<>();

            for (PropagationStatus propagationTO : getPropagationStatusTOs()) {
                if (resource.equals(propagationTO.getResource())) {
                    toBeRemoved.add(propagationTO);
                }
            }

            return propagationStatusTOs.removeAll(toBeRemoved);
        }
        return false;
    }

    @XmlElementWrapper(name = "propagationStatuses")
    @XmlElement(name = "propagationStatus")
    @JsonProperty("propagationStatuses")
    public List<PropagationStatus> getPropagationStatusTOs() {
        return propagationStatusTOs;
    }

}
