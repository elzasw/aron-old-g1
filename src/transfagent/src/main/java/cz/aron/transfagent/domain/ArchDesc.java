package cz.aron.transfagent.domain;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "arch_desc")
public class ArchDesc {
	
	public static final String JPA_APUSOURCE = "apuSource";
	
	public static final String JPA_FUND = "fund";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "arch_desc_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apusource_id", nullable = true)
    private ApuSource apuSource;

    @Column(nullable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_id", nullable = true)
    private Fund fund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = true)
    private Collection collection;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ApuSource getApuSource() {
		return apuSource;
	}

	public void setApuSource(ApuSource apuSource) {
		this.apuSource = apuSource;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public Fund getFund() {
		return fund;
	}

	public void setFund(Fund fund) {
		this.fund = fund;
	}

	public Collection getCollection() {
		return collection;
	}

	public void setCollection(Collection collection) {
		this.collection = collection;
	}

}
