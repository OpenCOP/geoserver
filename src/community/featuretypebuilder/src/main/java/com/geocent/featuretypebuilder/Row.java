package com.geocent.featuretypebuilder;

import java.io.Serializable;

/**
 *
 * @author thanthese
 */
public class Row implements Serializable {
  private String name;
  private String type;
  public Row(String name, String type) {
    this.name = name;
    this.type = type;
  }
  public String getName() { return name; }
  public String getType() { return type; }

  @Override
  public String toString() {
    return "Row{" + "name=" + name + ", type=" + type + '}';
  }
}