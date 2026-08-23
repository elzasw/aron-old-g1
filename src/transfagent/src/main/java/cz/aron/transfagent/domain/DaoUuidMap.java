package cz.aron.transfagent.domain;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Persistent identity of a dao delivered by reference: the repository code and
 * the dao's id/path within it map to the uuid the dao is published under. The
 * row is created on first import and never changes afterwards, so
 * republications keep the published uuid. Rows may also be pre-loaded from an
 * external source (plain inserts) to dictate the uuids before the first
 * publication.
 */
@Entity
@Table(name = "dao_uuid_map")
public class DaoUuidMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dao_uuid_map_id")
    private Integer id;

    /** Code of the repository (Elza external system code). */
    @Column(length = 50, nullable = false)
    private String repository;

    /** Id of the dao within the repository - for a filesystem repository its relative path. */
    @Column(length = 1000, nullable = false)
    private String handle;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    public Integer getId() {
        return id;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

}
