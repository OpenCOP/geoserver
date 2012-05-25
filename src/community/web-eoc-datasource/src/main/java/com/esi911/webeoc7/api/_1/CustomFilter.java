
package com.esi911.webeoc7.api._1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CustomFilter complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CustomFilter">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CustomFilterItems" type="{urn:com:esi911:webeoc7:api:1.0}ArrayOfCustomFilterItem" minOccurs="0"/>
 *         &lt;element name="CustomFilterBoolean" type="{urn:com:esi911:webeoc7:api:1.0}CustomFilterBoolean"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CustomFilter", propOrder = {
    "customFilterItems",
    "customFilterBoolean"
})
public class CustomFilter {

    @XmlElement(name = "CustomFilterItems")
    protected ArrayOfCustomFilterItem customFilterItems;
    @XmlElement(name = "CustomFilterBoolean", required = true)
    protected CustomFilterBoolean customFilterBoolean;

    /**
     * Gets the value of the customFilterItems property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfCustomFilterItem }
     *     
     */
    public ArrayOfCustomFilterItem getCustomFilterItems() {
        return customFilterItems;
    }

    /**
     * Sets the value of the customFilterItems property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfCustomFilterItem }
     *     
     */
    public void setCustomFilterItems(ArrayOfCustomFilterItem value) {
        this.customFilterItems = value;
    }

    /**
     * Gets the value of the customFilterBoolean property.
     * 
     * @return
     *     possible object is
     *     {@link CustomFilterBoolean }
     *     
     */
    public CustomFilterBoolean getCustomFilterBoolean() {
        return customFilterBoolean;
    }

    /**
     * Sets the value of the customFilterBoolean property.
     * 
     * @param value
     *     allowed object is
     *     {@link CustomFilterBoolean }
     *     
     */
    public void setCustomFilterBoolean(CustomFilterBoolean value) {
        this.customFilterBoolean = value;
    }

}
