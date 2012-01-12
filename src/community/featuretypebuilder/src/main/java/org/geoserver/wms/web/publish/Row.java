package org.geoserver.wms.web.publish;

import java.io.Serializable;

/**
 *
 * @author thanthese
 */
public class Row implements Serializable {
  private String name;
  private String type;

  /**
   * Does some work towards turning name into something valid.
   *
   * @param name
   * @param type
   */
  public Row(String name, String type) {
    this.name = DbUtils.fixName(name);
    this.type = type;
  }
  public String getName() { return name; }
  public String getType() { return type; }

  public boolean isValid() {
    boolean hasName = !name.equals("");
    boolean hasType = !name.equals("");
    boolean hasRealType = !name.equals("Choose One");
    return hasName && hasType && hasRealType;
  }

  public boolean isGeometryType() {
    return type.equals("POINT")
        || type.equals("LINE")
        || type.equals("POLYGON")
        || type.equals("MULTIPOINT")
        || type.equals("MULTILINE")
        || type.equals("MULTIPOLYGON");
  }

  @Override
  public String toString() {
    return "Row{" + "name=" + name + ", type=" + type + '}';
  }
}