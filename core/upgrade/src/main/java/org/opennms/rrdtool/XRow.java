/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.rrdtool;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The Class XRow (XPort Row).
 * 
 * @author Alejandro Galue <agalue@opennms.org>
 */
@XmlRootElement(name="row")
@XmlAccessorType(XmlAccessType.FIELD)
public class XRow {

    /** The time stamp expressed in seconds since 1970-01-01 UTC. */
    @XmlElement(name="t")
    private Long timestamp;
    
    /** The values. */
    @XmlElement(name="v")
    private List<Double> values = new ArrayList<Double>();

    /**
     * Gets the time stamp.
     * <p>Expressed in seconds since 1970-01-01 UTC</p>
     * 
     * @return the time stamp
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the time stamp.
     *
     * @param timestamp the new time stamp
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the values.
     *
     * @return the values
     */
    public List<Double> getValues() {
        return values;
    }

    /**
     * Sets the values.
     *
     * @param values the new values
     */
    public void setValues(List<Double> values) {
        this.values = values;
    }

}
