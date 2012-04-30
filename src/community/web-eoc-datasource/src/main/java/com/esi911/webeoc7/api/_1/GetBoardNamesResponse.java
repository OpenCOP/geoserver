
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
 *         &lt;element name="GetBoardNamesResult" type="{urn:com:esi911:webeoc7:api:1.0}ArrayOfString" minOccurs="0"/>
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
    "getBoardNamesResult"
})
@XmlRootElement(name = "GetBoardNamesResponse")
public class GetBoardNamesResponse {

    @XmlElement(name = "GetBoardNamesResult")
    protected ArrayOfString getBoardNamesResult;

    /**
     * Gets the value of the getBoardNamesResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfString }
     *     
     */
    public ArrayOfString getGetBoardNamesResult() {
        return getBoardNamesResult;
    }

    /**
     * Sets the value of the getBoardNamesResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfString }
     *     
     */
    public void setGetBoardNamesResult(ArrayOfString value) {
        this.getBoardNamesResult = value;
    }

}
