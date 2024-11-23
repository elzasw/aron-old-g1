package cz.aron.core.integration;

import cz.aron.apux._2020.DaoFile;
import cz.aron.apux._2020.MetadataItem;
import cz.aron.core.model.*;
import cz.inqool.eas.common.storage.file.File;
import cz.inqool.eas.common.storage.file.FileManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * @author Lukas Jane (inQool) 4.11.2020.
 */
@Service
@Slf4j
public class FileInputProcessor {

    // full path to file on local filesystem
    private static String ATTR_PATH = "path";
    // mimetype of file
    private static String ATTR_MIMETYPE = "mimeType";
    // reference flag, no data only uuid and/or path
    private static String ATTR_REFERENCE = "reference";
    // size of file
    private static String ATTR_SIZE = "size";
    
    private static String ATTR_SELECTED = "selected";

    @Inject private FileManager fileManager;

    public void processFile(DaoFile daoFile, DigitalObjectType digitalObjectType, ApuAttachment apuAttachment, DigitalObject digitalObject, Map<String, Path> filesMap) {

        String name;
        DigitalObjectFile digitalObjectFile;
        if (apuAttachment != null) {
            digitalObjectFile = getNewOrExisting(apuAttachment, daoFile.getUuid());
            name = apuAttachment.getName();
        }
        else if (digitalObject != null) {
            digitalObjectFile = getNewOrExisting(digitalObject, daoFile.getUuid());
            name = digitalObject.getName();
        }
        else {
            throw new RuntimeException("missing parent object");
        }

        digitalObjectFile.setType(digitalObjectType);
        digitalObjectFile.setOrder(daoFile.getPos());
        digitalObjectFile.setPermalink(daoFile.getPrmLnk());
        digitalObjectFile.setReferencedFile(null);
        digitalObjectFile.setContentType(null);
        digitalObjectFile.setName(null);
        digitalObjectFile.setSize(null);

        var attributes = processMetadata(daoFile, digitalObjectFile);
        var mimeType = attributes.get(ATTR_MIMETYPE);
        if (mimeType==null) {
            mimeType = "application/octet-stream";
        }
        if (daoFile.getMtdt() != null) {
            for (MetadataItem itm : daoFile.getMtdt().getItms()) {
                Metadatum metadatum = new Metadatum();
                metadatum.setType(itm.getCode());
                metadatum.setValue(itm.getValue());
                metadatum.setFile(digitalObjectFile);
                digitalObjectFile.getMetadata().add(metadatum);
                if (metadatum.getType().equals("mimeType")) {
                    mimeType = metadatum.getValue();
                }
            }
        }
        Path uploadedFile = filesMap.get(digitalObjectFile.getId());
        if (name == null) {
            name = UUID.randomUUID().toString();
        }
        try (InputStream is = Files.newInputStream(uploadedFile)) {
            File file = fileManager.store(
                    name,
                    Files.size(uploadedFile),
                    mimeType,
                    is);
            digitalObjectFile.setFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void processFileReference(DaoFile daoFile, DigitalObjectType digitalObjectType, DigitalObject digitalObject) {
        DigitalObjectFile digitalObjectFile = getNewOrExisting(digitalObject, daoFile.getUuid());
        digitalObjectFile.setType(digitalObjectType);
        digitalObjectFile.setOrder(daoFile.getPos());
        digitalObjectFile.setPermalink(daoFile.getPrmLnk());
        digitalObjectFile.setDigitalObject(digitalObject);
        digitalObjectFile.setFile(null);
        digitalObjectFile.setSelected(isSelected(daoFile)?true:null);

        var attributes = processMetadata(daoFile, digitalObjectFile);
        digitalObjectFile.setReferencedFile(attributes.get(ATTR_PATH));
        digitalObjectFile.setContentType(attributes.get(ATTR_MIMETYPE));
        var size = attributes.get(ATTR_SIZE);
        if (size!=null) {
             digitalObjectFile.setSize(Long.parseLong(size));
        } else {
             digitalObjectFile.setSize(null);
        }
        //TODO transfer filename from transformagent
        String name = null;
        if (name==null&&digitalObjectFile.getReferencedFile()!=null) {
            name = Paths.get(digitalObjectFile.getReferencedFile()).getFileName().toString();
        }
        digitalObjectFile.setName(name);
    }

    /**
     * Add metadata. Remove unused when exist
     */
    private Map<String, String> processMetadata(DaoFile daoFile, DigitalObjectFile digitalObjectFile) {
        var attributes = new HashMap<String,String>();
        var usedMetadata = new HashSet<String>();
        if (daoFile.getMtdt() != null) {
            for (MetadataItem itm : daoFile.getMtdt().getItms()) {
                attributes.put(itm.getCode(), itm.getValue());
                if (!ATTR_PATH.equals(itm.getCode())&&!ATTR_REFERENCE.equals(itm.getCode())) {
                    usedMetadata.add(itm.getCode());
                    attributes.put(itm.getCode(), itm.getValue());
                    Metadatum metadatum = getNewOrExisting(digitalObjectFile, itm.getCode());
                    metadatum.setValue(itm.getValue());
                }
            }
        }

        // remove unused metadata
        var it = digitalObjectFile.getMetadata().iterator();
        while(it.hasNext()) {
            var metadatum = it.next();
            if (!usedMetadata.contains(metadatum.getType())) {
                it.remove();
            }
        }
        return attributes;
    }

    private DigitalObjectFile getNewOrExisting(DigitalObject digitalObject, String uuid) {
	for(DigitalObjectFile digitalObjectFile:digitalObject.getFiles()) {
            if (uuid.equals(digitalObjectFile.getId())) {
                return digitalObjectFile;
            }
        }
        DigitalObjectFile digitalObjectFile = new DigitalObjectFile();
        digitalObjectFile.setId(uuid);
        digitalObjectFile.setDigitalObject(digitalObject);
        digitalObject.getFiles().add(digitalObjectFile);
        return digitalObjectFile;
    }

    private DigitalObjectFile getNewOrExisting(ApuAttachment apuAttachment, String uuid) {
        DigitalObjectFile digitalObjectFile = apuAttachment.getFile();
        if (digitalObjectFile!=null && uuid.equals(digitalObjectFile.getId())) {
            return digitalObjectFile;
        }
        digitalObjectFile = new DigitalObjectFile();
        digitalObjectFile.setId(uuid);
        digitalObjectFile.setAttachment(apuAttachment);
        apuAttachment.setFile(digitalObjectFile);
        return digitalObjectFile;
    }

    private Metadatum getNewOrExisting(DigitalObjectFile digitalObjectFile, String code) {
        for(Metadatum metadatum:digitalObjectFile.getMetadata()) {
            if (code.equals(metadatum.getType())) {
                return metadatum;
            }
        }
        Metadatum metadatum = new Metadatum();
        metadatum.setType(code);
        metadatum.setFile(digitalObjectFile);
        digitalObjectFile.getMetadata().add(metadatum);
        return metadatum;
    }

    public static boolean isReference(DaoFile daoFile) {
        if (daoFile.getMtdt() != null) {
            for (MetadataItem itm : daoFile.getMtdt().getItms()) {
                if (ATTR_REFERENCE.equals(itm.getCode()) && "1".equals(itm.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSelected(DaoFile daoFile) {
        if (daoFile.getMtdt() != null) {
            for (MetadataItem itm : daoFile.getMtdt().getItms()) {
                if (ATTR_SELECTED.equals(itm.getCode()) && "1".equals(itm.getValue())) {
                    return true;
                }
            }
        }
        return false;    	
    }

}
