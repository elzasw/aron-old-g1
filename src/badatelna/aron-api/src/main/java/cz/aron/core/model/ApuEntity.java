package cz.aron.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.inqool.eas.common.domain.store.DomainObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Entity
@Getter
@Setter
public class ApuEntity extends DomainObject<ApuEntity> {
    private String name;
    private String description;
    private String permalink;
    private int order;
    private boolean published;
    private int depth;
    private int pos;
    private int childCnt;

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JsonIgnore
    private ApuSource source;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private ApuEntity parent;

    @OneToMany(mappedBy = "apu", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    private List<ApuPart> parts = new ArrayList<>();

    @OneToMany(mappedBy = "apu", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    private List<ApuAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "apu", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    private List<DigitalObject> digitalObjects = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ApuType type;

    @Transient
    @JsonIgnore
    private List<String> incomingRelTypeGroups = new ArrayList<>();

    @Transient
    @JsonIgnore
    private List<String> incomingRelTypes = new ArrayList<>();
}
