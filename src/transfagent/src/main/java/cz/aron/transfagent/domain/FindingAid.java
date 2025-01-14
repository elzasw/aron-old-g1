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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "finding_aid")
public class FindingAid {
	
	public static final String JPA_APUSOURCE = "apuSource";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "finding_aid_id")
    private Integer id;

    @Column(nullable = false)
    private UUID uuid;

    @Column(length = 50, nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apusource_id", nullable = false)
    private ApuSource apuSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToMany(mappedBy = "findingAids")
    private Set<Fund> funds = new HashSet<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

	public Set<Fund> getFunds() {
		return funds;
	}

	public void setFunds(Set<Fund> funds) {
		this.funds = funds;
	}

	@Override
	public int hashCode() {
		return Objects.hash(apuSource, code, funds, institution, uuid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FindingAid other = (FindingAid) obj;
		return Objects.equals(apuSource, other.apuSource) && Objects.equals(code, other.code)
				&& Objects.equals(funds, other.funds) && Objects.equals(institution, other.institution)
				&& Objects.equals(uuid, other.uuid);
	}
	
	public void addFund(Fund fund) {
		this.funds.add(fund);
		fund.getFindingAids().add(this);
	}

}
