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
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Entity
@Getter
@Setter
public class DigitalObject extends DomainObject<DigitalObject> {
    private String name;
    private String permalink;
    private int order;

    @OneToMany(mappedBy = "digitalObject", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.JOIN)
    private List<DigitalObjectFile> files = new ArrayList<>();

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    @JsonIgnore
    private ApuEntity apu;
}
