package cz.aron.transfagent.domain;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "archival_entity")
public class ArchivalEntity {
	
	public static final String JPA_APUSOURCE = "apuSource";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entity_id")
    private Integer id;

    @Column(nullable = true)
    private UUID uuid;
    
    @Column(name = "elza_id", nullable = true)
    private Integer elzaId;

	@Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true)
    private EntityStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apusource_id", nullable = true)
    private ApuSource apuSource;

    @Column(name = "last_update", nullable = true)
    private ZonedDateTime lastUpdate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_entity_id", nullable = true)
    private ArchivalEntity parentEntity;

    @Column(nullable = false)
    private boolean download;
    
    @Column(name="entity_class", nullable = true)
    private String entityClass;
    
    @Column(nullable = true)
    private UUID camUuid;

    public String getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(String entityClass) {
        this.entityClass = entityClass;
    }

    public ArchivalEntity getParentEntity() {
		return parentEntity;
	}

	public void setParentEntity(ArchivalEntity parentEntity) {
		this.parentEntity = parentEntity;
	}

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

    public EntityStatus getStatus() {
        return status;
    }

    public void setStatus(EntityStatus status) {
        this.status = status;
    }

    public ApuSource getApuSource() {
        return apuSource;
    }

    public void setApuSource(ApuSource apuSource) {
        this.apuSource = apuSource;
    }

	public ZonedDateTime getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(ZonedDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

    public Integer getElzaId() {
        return elzaId;
    }

    public void setElzaId(Integer elzaId) {
        this.elzaId = elzaId;
    }

    public boolean isDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

	public UUID getCamUuid() {
		return camUuid;
	}

	public void setCamUuid(UUID camUuid) {
		this.camUuid = camUuid;
	}

}
