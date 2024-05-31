package cz.aron.transfagent.config;

import java.util.Objects;

public class ConfigElzaInheritedType {

	private String type;

	private String spec;
	
	public ConfigElzaInheritedType() {
	}
		
	public ConfigElzaInheritedType(String type, String spec) {
		this.type = type;
		this.spec = spec;
	}	

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSpec() {
		return spec;
	}

	public void setSpec(String spec) {
		this.spec = spec;
	}

	@Override
	public int hashCode() {
		return Objects.hash(spec, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConfigElzaInheritedType other = (ConfigElzaInheritedType) obj;
		return Objects.equals(spec, other.spec) && Objects.equals(type, other.type);
	}

}
