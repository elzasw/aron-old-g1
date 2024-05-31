package cz.aron.core.model;

import javax.persistence.*;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import java.util.ArrayList;
import java.util.List;

import cz.inqool.eas.common.domain.store.DomainObject;
import lombok.Getter;

@Getter
@Immutable
@Entity(name = "apu_entity_simple")
@Table(name = "apu_entity")
@BatchSize(size = 100)
public class ApuEntitySimple extends DomainObject<ApuEntitySimple> {

    private String name;
    private String description;
    private int order;

    @OneToMany(mappedBy = "apu", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.JOIN)
    private List<ApuPart> parts = new ArrayList<>();

}
