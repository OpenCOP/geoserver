
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
 *         &lt;element name="UpdateRelatedDataResult" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "updateRelatedDataResult"
})
@XmlRootElement(name = "UpdateRelatedDataResponse")
public class UpdateRelatedDataResponse {

    @XmlElement(name = "UpdateRelatedDataResult")
    protected int updateRelatedDataResult;

    /**
     * Gets the value of the updateRelatedDataResult property.
     * 
     */
    public int getUpdateRelatedDataResult() {
        return updateRelatedDataResult;
    }

    /**
     * Sets the value of the updateRelatedDataResult property.
     * 
     */
    public void setUpdateRelatedDataResult(int value) {
        this.updateRelatedDataResult = value;
    }

}
