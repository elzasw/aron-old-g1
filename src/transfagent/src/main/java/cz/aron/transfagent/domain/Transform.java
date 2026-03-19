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
import javax.persistence.Table;

@Entity
@Table(name = "transform")
public class Transform {
	
	public static final String JPA_DAO_UUID = "daoUuid";
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transform_id")
    private Integer id;

    @Column(name="dao_uuid", nullable = false)
    private UUID daoUuid;
    
    @Column(name="file_uuid", nullable = false)
    private UUID fileUuid;

    @Column(name="file", nullable = false)
    private String file;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private TransformState state;
    
    @Column(name = "type", nullable = false)
    private String type;
    
    @Column(name = "last_update", nullable = true)
    private ZonedDateTime lastUpdate;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public UUID getDaoUuid() {
		return daoUuid;
	}

	public void setDaoUuid(UUID daoUuid) {
		this.daoUuid = daoUuid;
	}

	public UUID getFileUuid() {
		return fileUuid;
	}

	public void setFileUuid(UUID fileUuid) {
		this.fileUuid = fileUuid;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public TransformState getState() {
		return state;
	}

	public void setState(TransformState state) {
		this.state = state;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

    public ZonedDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(ZonedDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }	

}
