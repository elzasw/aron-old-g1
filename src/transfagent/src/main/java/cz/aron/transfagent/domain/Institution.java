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
@Table(name = "institution")
public class Institution {
	
	public static final String JPA_APUSOURCE = "apuSource";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "institution_id")
    private Integer id;

    @Column(nullable = false)
    private UUID uuid;

    @Column(length = 50, nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apusource_id", nullable = false)
    private ApuSource apuSource;

    @Column(nullable = false)
    private String source;

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

}
