package cz.aron.core.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import cz.aron.core.model.ResultRowItem;
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
 * Result
 */

public class Result {

  private String id;

  private String t;

  private String tn;

  @Valid
  private List<List<@Valid ResultRowItem>> l = new ArrayList<>();

  public Result() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Result(String id, String t, List<List<@Valid ResultRowItem>> l) {
    this.id = id;
    this.t = t;
    this.l = l;
  }

  public Result id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Unique identifier of the archive item
   * @return id
   */
  @NotNull
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Result t(String t) {
    this.t = t;
    return this;
  }

  /**
   * Type of the archive item
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

  public Result tn(String tn) {
    this.tn = tn;
    return this;
  }

  /**
   * Thumbnail URL
   * @return tn
   */

  @JsonProperty("tn")
  public String getTn() {
    return tn;
  }

  public void setTn(String tn) {
    this.tn = tn;
  }

  public Result l(List<List<@Valid ResultRowItem>> l) {
    this.l = l;
    return this;
  }

  public Result addLItem(List<@Valid ResultRowItem> lItem) {
    if (this.l == null) {
      this.l = new ArrayList<>();
    }
    this.l.add(lItem);
    return this;
  }

  /**
   * List of result rows
   * @return l
   */
  @NotNull @Valid 
  @JsonProperty("l")
  public List<List<@Valid ResultRowItem>> getL() {
    return l;
  }

  public void setL(List<List<@Valid ResultRowItem>> l) {
    this.l = l;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Result result = (Result) o;
    return Objects.equals(this.id, result.id) &&
        Objects.equals(this.t, result.t) &&
        Objects.equals(this.tn, result.tn) &&
        Objects.equals(this.l, result.l);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, t, tn, l);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Result {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    t: ").append(toIndentedString(t)).append("\n");
    sb.append("    tn: ").append(toIndentedString(tn)).append("\n");
    sb.append("    l: ").append(toIndentedString(l)).append("\n");
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

