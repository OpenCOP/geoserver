
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
 *         &lt;element name="SaveListItemResult" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "saveListItemResult"
})
@XmlRootElement(name = "SaveListItemResponse")
public class SaveListItemResponse {

    @XmlElement(name = "SaveListItemResult")
    protected int saveListItemResult;

    /**
     * Gets the value of the saveListItemResult property.
     * 
     */
    public int getSaveListItemResult() {
        return saveListItemResult;
    }

    /**
     * Sets the value of the saveListItemResult property.
     * 
     */
    public void setSaveListItemResult(int value) {
        this.saveListItemResult = value;
    }

}
