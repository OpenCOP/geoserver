
package com.esi911.webeoc7.api._1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CustomFilterBoolean.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CustomFilterBoolean">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="And"/>
 *     &lt;enumeration value="Or"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CustomFilterBoolean")
@XmlEnum
public enum CustomFilterBoolean {

    @XmlEnumValue("And")
    AND("And"),
    @XmlEnumValue("Or")
    OR("Or");
    private final String value;

    CustomFilterBoolean(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CustomFilterBoolean fromValue(String v) {
        for (CustomFilterBoolean c: CustomFilterBoolean.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
