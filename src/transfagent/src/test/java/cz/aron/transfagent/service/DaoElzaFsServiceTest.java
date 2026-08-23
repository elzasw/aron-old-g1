package cz.aron.transfagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import cz.aron.transfagent.domain.DaoUuidMap;
import cz.aron.transfagent.repository.DaoUuidMapRepository;

public class DaoElzaFsServiceTest {

    @Test
    public void overlongHandleIsRefused() {
        assertNull(DaoElzaFsService.createHandle("DiskRepo", "x".repeat(300)));
    }

    /**
     * The uuid is the stored mapping's: a repeated ask returns the stored
     * value, a pre-loaded row (uuids dictated from an external source) wins
     * over generation, and distinct paths get distinct identities.
     */
    @Test
    public void uuidComesFromThePersistentMapping() {
        Map<String, DaoUuidMap> store = new HashMap<>();
        var repository = Mockito.mock(DaoUuidMapRepository.class);
        Mockito.when(repository.findByRepositoryAndHandle(Mockito.anyString(), Mockito.anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(
                        store.get(invocation.getArgument(0) + "|" + invocation.getArgument(1))));
        Mockito.when(repository.save(Mockito.any())).thenAnswer(invocation -> {
            DaoUuidMap mapping = invocation.getArgument(0);
            store.put(mapping.getRepository() + "|" + mapping.getHandle(), mapping);
            return mapping;
        });
        var service = new DaoElzaFsService(null, new cz.aron.transfagent.config.ConfigElzaFs(), repository);

        var external = UUID.randomUUID();
        var preloaded = new DaoUuidMap();
        preloaded.setRepository("DiskRepo");
        preloaded.setHandle("preloaded.jpg");
        preloaded.setUuid(external);
        store.put("DiskRepo|preloaded.jpg", preloaded);
        assertEquals(external, service.uuidFor("DiskRepo", "preloaded.jpg"));

        var first = service.uuidFor("DiskRepo", "3/3.1");
        assertEquals(first, service.uuidFor("DiskRepo", "3/3.1"));
        assertNotEquals(first, service.uuidFor("DiskRepo", "3/3.2"));
    }

}
