package com.geocent.featuretypebuilder;

/**
 *
 * @author thanthese
 */
public class Row {
  private String name;
  private String type;
  public Row(String name, String type) {
    this.name = name;
    this.type = type;
  }
  public String getName() { return name; }
  public String getType() { return type; }
}