package cz.aron.transfagent.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "fund")
public class Fund {
	
    public static final String JPA_APUSOURCE = "apuSource";
    public static final String JPA_INSTITUTION = "institution";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fund_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false)
    private UUID uuid;

    @Column(length = 50, nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apusource_id", nullable = false)
    private ApuSource apuSource;
    
	@ManyToMany
	@JoinTable(name = "finding_aid_fund", joinColumns = @JoinColumn(name = "fund_id"), inverseJoinColumns = @JoinColumn(name = "finding_aid_id"))
	Set<FindingAid> findingAids = new HashSet<>();

    @Column(nullable = false)
    private String source;
    
    @Column(name = "name")
    private String name;
    
	@Column(name = "nad")
    private Integer nad;
    
	
    private Integer subNad;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ApuSource getApuSource() {
        return apuSource;
    }

    public void setApuSource(ApuSource apuSource) {
        this.apuSource = apuSource;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

	public Set<FindingAid> getFindingAids() {
		return findingAids;
	}

	public void setFindingAids(Set<FindingAid> findingAids) {
		this.findingAids = findingAids;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getNad() {
		return nad;
	}

	public void setNad(Integer nad) {
		this.nad = nad;
	}

	public Integer getSubNad() {
		return subNad;
	}

	public void setSubNad(Integer subNad) {
		this.subNad = subNad;
	}

	@Override
	public int hashCode() {
		return Objects.hash(apuSource, code, institution, source, uuid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Fund other = (Fund) obj;
		return Objects.equals(apuSource, other.apuSource) && Objects.equals(code, other.code)
				&& Objects.equals(institution, other.institution) && Objects.equals(source, other.source)
				&& Objects.equals(uuid, other.uuid);
	}
	
	public void addFindingAid(FindingAid findingAid) {
        this.findingAids.add(findingAid);
        findingAid.getFunds().add(this);
    }
 
}
