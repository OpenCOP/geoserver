
package com.esi911.webeoc7.api._1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GetFilteredDataV2Result" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getFilteredDataV2Result"
})
@XmlRootElement(name = "GetFilteredDataV2Response")
public class GetFilteredDataV2Response {

    @XmlElement(name = "GetFilteredDataV2Result")
    protected String getFilteredDataV2Result;

    /**
     * Gets the value of the getFilteredDataV2Result property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGetFilteredDataV2Result() {
        return getFilteredDataV2Result;
    }

    /**
     * Sets the value of the getFilteredDataV2Result property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGetFilteredDataV2Result(String value) {
        this.getFilteredDataV2Result = value;
    }

}
