package cz.aron.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.inqool.eas.common.domain.store.DomainObject;
import cz.inqool.eas.common.storage.file.File;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Entity
@Getter
@Setter
public class DigitalObjectFile extends DomainObject<DigitalObjectFile> {
    private String permalink;
    private int order;

    @Enumerated(EnumType.STRING)
    private DigitalObjectType type;

    @OneToMany(mappedBy = "file", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @JsonIgnore
    private List<Metadatum> metadata = new ArrayList<>();

    @OneToOne
    private File file;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    @JsonIgnore
    private DigitalObject digitalObject;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    @JsonIgnore
    private ApuAttachment attachment;

    private String name;

    private String referencedFile;

    private String contentType;

    private Long size;
}
