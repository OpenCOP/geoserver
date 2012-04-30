
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
 *         &lt;element name="GetUsersByPositionResult" type="{urn:com:esi911:webeoc7:api:1.0}ArrayOfWebEOCUser" minOccurs="0"/>
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
    "getUsersByPositionResult"
})
@XmlRootElement(name = "GetUsersByPositionResponse")
public class GetUsersByPositionResponse {

    @XmlElement(name = "GetUsersByPositionResult")
    protected ArrayOfWebEOCUser getUsersByPositionResult;

    /**
     * Gets the value of the getUsersByPositionResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfWebEOCUser }
     *     
     */
    public ArrayOfWebEOCUser getGetUsersByPositionResult() {
        return getUsersByPositionResult;
    }

    /**
     * Sets the value of the getUsersByPositionResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfWebEOCUser }
     *     
     */
    public void setGetUsersByPositionResult(ArrayOfWebEOCUser value) {
        this.getUsersByPositionResult = value;
    }

}
