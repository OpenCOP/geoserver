
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
 *         &lt;element name="SaveListResult" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "saveListResult"
})
@XmlRootElement(name = "SaveListResponse")
public class SaveListResponse {

    @XmlElement(name = "SaveListResult")
    protected int saveListResult;

    /**
     * Gets the value of the saveListResult property.
     * 
     */
    public int getSaveListResult() {
        return saveListResult;
    }

    /**
     * Sets the value of the saveListResult property.
     * 
     */
    public void setSaveListResult(int value) {
        this.saveListResult = value;
    }

}
