package cz.aron.core.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * ResultRowItemValue
 */

public class ResultRowItemValue {

  private String v;

  private String id;

  public ResultRowItemValue() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ResultRowItemValue(String v) {
    this.v = v;
  }

  public ResultRowItemValue v(String v) {
    this.v = v;
    return this;
  }

  /**
   * Get v
   * @return v
   */
  @NotNull 
  @JsonProperty("v")
  public String getV() {
    return v;
  }

  public void setV(String v) {
    this.v = v;
  }

  public ResultRowItemValue id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Unique identifier of the referenced archive item
   * @return id
   */

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResultRowItemValue resultRowItemValue = (ResultRowItemValue) o;
    return Objects.equals(this.v, resultRowItemValue.v) &&
        Objects.equals(this.id, resultRowItemValue.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(v, id);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResultRowItemValue {\n");
    sb.append("    v: ").append(toIndentedString(v)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

