package cz.aron.core.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import cz.aron.core.model.ResultRowItemValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * ResultRowItem
 */

public class ResultRowItem {

  private String t;

  @Valid
  private List<@Valid ResultRowItemValue> v = new ArrayList<>();

  public ResultRowItem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ResultRowItem(String t, List<@Valid ResultRowItemValue> v) {
    this.t = t;
    this.v = v;
  }

  public ResultRowItem t(String t) {
    this.t = t;
    return this;
  }

  /**
   * type of item
   * @return t
   */
  @NotNull 
  @JsonProperty("t")
  public String getT() {
    return t;
  }

  public void setT(String t) {
    this.t = t;
  }

  public ResultRowItem v(List<@Valid ResultRowItemValue> v) {
    this.v = v;
    return this;
  }

  public ResultRowItem addVItem(ResultRowItemValue vItem) {
    if (this.v == null) {
      this.v = new ArrayList<>();
    }
    this.v.add(vItem);
    return this;
  }

  /**
   * value of item
   * @return v
   */
  @NotNull @Valid 
  @JsonProperty("v")
  public List<@Valid ResultRowItemValue> getV() {
    return v;
  }

  public void setV(List<@Valid ResultRowItemValue> v) {
    this.v = v;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResultRowItem resultRowItem = (ResultRowItem) o;
    return Objects.equals(this.t, resultRowItem.t) &&
        Objects.equals(this.v, resultRowItem.v);
  }

  @Override
  public int hashCode() {
    return Objects.hash(t, v);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResultRowItem {\n");
    sb.append("    t: ").append(toIndentedString(t)).append("\n");
    sb.append("    v: ").append(toIndentedString(v)).append("\n");
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

