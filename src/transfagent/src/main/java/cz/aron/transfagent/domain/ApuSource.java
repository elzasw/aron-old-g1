package cz.aron.transfagent.domain;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "apu_source")
public class ApuSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "apusource_id")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(nullable = false)
    private UUID uuid;
    
    @Column(name="orig_dir")
    private String origDir;
    
    @Column(name="data_dir")
    private String dataDir;

    @Column(nullable = false)
    private boolean deleted;
    
    @Column(nullable = false)
    private boolean reimport;
   

    @JoinColumn(name = "date_imported", nullable = true)
    private ZonedDateTime dateImported;
    
    @Column(name="last_sent")
    private Long lastSent;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getOrigDir() {
		return origDir;
	}

	public void setOrigDir(String origDir) {
		this.origDir = origDir;
	}

	public String getDataDir() {
		return dataDir;
	}

	public void setDataDir(String dataDir) {
		this.dataDir = dataDir;
	}

	public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isReimport() {
		return reimport;
	}

	public void setReimport(boolean reimport) {
		this.reimport = reimport;
	}

	public ZonedDateTime getDateImported() {
        return dateImported;
    }

    public void setDateImported(ZonedDateTime dateImported) {
        this.dateImported = dateImported;
    }

	public Long getLastSent() {
		return lastSent;
	}

	public void setLastSent(Long lastSent) {
		this.lastSent = lastSent;
	}

}
