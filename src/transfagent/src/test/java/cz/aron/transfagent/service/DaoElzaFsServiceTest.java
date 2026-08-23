package cz.aron.transfagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DaoElzaFsServiceTest {

    /**
     * The APUX schema's Uuid pattern accepts only versions 1 and 4 and the RFC
     * variant - a version-3 name-based uuid fails the APUX marshalling, which
     * is exactly how the first derivation broke.
     */
    private static final String APUX_UUID_PATTERN =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[14][0-9a-fA-F]{3}-[8-9a-bA-B][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";

    @Test
    public void daoUuidIsDeterministicAndApuxSchemaValid() {
        var handle = "DiskRepo|d9823c9d-c4c6-4885-bc21-cf3713b12fd9|DSC02319.JPG";
        var uuid = DaoElzaFsService.daoUuidOf(handle);
        assertEquals(uuid, DaoElzaFsService.daoUuidOf(handle));
        assertTrue(uuid.toString().matches(APUX_UUID_PATTERN), "not schema-valid: " + uuid);
        assertEquals(4, uuid.version());
    }

    @Test
    public void overlongHandleIsRefused() {
        assertNull(DaoElzaFsService.createHandle("DiskRepo", "level", "x".repeat(300)));
    }

}
