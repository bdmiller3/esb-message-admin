/*
 Copyright 2015 esbtools Contributors and/or its affiliates.

 This file is part of esbtools.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.esbtools.message.admin.model;

import java.io.Serializable;

public class Header implements Serializable {

    private HeaderType type;
    private String name;
    private String value;

    public Header() {
    }
    
    public Header(HeaderType type, String name, String value) {
        super();
        this.type = type;
        this.name = name;
        this.value = value;
    }


    public HeaderType getType() {
        return type;
    }
    public void setType(HeaderType typeCode) {
        this.type = typeCode;
    }
    public String getName() {
        return name;
    }
    public void setName(String path) {
        this.name = path;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("%s: %s = %s", type, name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Header header = (Header) o;

        if (type != header.type) {
            return false;
        }

        if (!name.equals(header.name)) {
            return false;
        }

        return value != null ? value.equals(header.value) : header.value == null;

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
