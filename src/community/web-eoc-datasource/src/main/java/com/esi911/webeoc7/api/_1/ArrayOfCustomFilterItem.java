
package com.esi911.webeoc7.api._1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfCustomFilterItem complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfCustomFilterItem">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CustomFilterItem" type="{urn:com:esi911:webeoc7:api:1.0}CustomFilterItem" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfCustomFilterItem", propOrder = {
    "customFilterItem"
})
public class ArrayOfCustomFilterItem {

    @XmlElement(name = "CustomFilterItem", nillable = true)
    protected List<CustomFilterItem> customFilterItem;

    /**
     * Gets the value of the customFilterItem property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the customFilterItem property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCustomFilterItem().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CustomFilterItem }
     * 
     * 
     */
    public List<CustomFilterItem> getCustomFilterItem() {
        if (customFilterItem == null) {
            customFilterItem = new ArrayList<CustomFilterItem>();
        }
        return this.customFilterItem;
    }

}
