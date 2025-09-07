package cz.aron.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.inqool.eas.common.domain.store.DomainObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Entity
@Getter
@Setter
public class ApuPartItem extends DomainObject<ApuPartItem> {

    private String value;
    private boolean visible;    //!indexOnly
    private String href;    //only for link items

    private String type;

    private String label;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    @JsonIgnore
    private ApuPart apuPart;

    @Transient
    @JsonIgnore
    private String targetLabel;   //for APU_REF types, we preload referred item's name here (before indexing)
}
