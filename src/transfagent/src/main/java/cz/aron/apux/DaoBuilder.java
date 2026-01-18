package cz.aron.apux;

import java.util.UUID;

import javax.xml.bind.JAXBElement;

import cz.aron.apux._2020.Dao;
import cz.aron.apux._2020.DaoBundle;
import cz.aron.apux._2020.DaoBundleType;
import cz.aron.apux._2020.DaoFile;
import cz.aron.apux._2020.Metadata;
import cz.aron.apux._2020.MetadataItem;

public class DaoBuilder {

    private Dao dao = ApuxFactory.getObjFactory().createDao();

    public void setUuid(String daoUuid) {
        dao.setUuid(daoUuid);
    }
    
    public void selLicense(String license) {
	    dao.setLicense(license);
    }

    public DaoBundle createDaoBundle(DaoBundleType bundleType) {
        var daoBundle = ApuxFactory.getObjFactory().createDaoBundle();
        daoBundle.setType(bundleType);
        dao.getBndl().add(daoBundle);
        return daoBundle;
    }

    public JAXBElement<Dao> build() {
        JAXBElement<Dao> result = ApuxFactory.getObjFactory().createDao(dao);
        return result;
    }

    public static Metadata getMetadata(DaoFile daoFile) {
        var metadata = daoFile.getMtdt();
        if (metadata == null) {
            metadata = ApuxFactory.getObjFactory().createMetadata();
            daoFile.setMtdt(metadata);
        }
        return metadata;
    }

    public static MetadataItem createMetadataItem(String code, String value) {
        var metadataItem = ApuxFactory.getObjFactory().createMetadataItem();
        metadataItem.setCode(code);
        metadataItem.setValue(value);

        return metadataItem;
    }

    public static void addFileName(DaoFile daoFile, String name) {
        getMetadata(daoFile).getItms().add(createMetadataItem("name", name));
    }

    public static void addFileSize(DaoFile daoFile, long size) {
        getMetadata(daoFile).getItms().add(createMetadataItem("size", String.valueOf(size)));
    }

    public static void addMimeType(DaoFile daoFile, String mimeType) {
        getMetadata(daoFile).getItms().add(createMetadataItem("mimeType", mimeType));
    }
    
    public static void addReferenceFlag(DaoFile daoFile) {
    	getMetadata(daoFile).getItms().add(createMetadataItem("reference", "1"));
    }
    
    public static void addSelectedFlag(DaoFile daoFile) {
    	getMetadata(daoFile).getItms().add(createMetadataItem("selected", "1"));
    }
    
    public static void addPath(DaoFile daoFile, String path) {
    	getMetadata(daoFile).getItms().add(createMetadataItem("path", path));
    }

    public static DaoFile createDaoFile(int pos, String mimetype) {
        return createDaoFile(null, null, pos, mimetype, null);
    }
    
    public static DaoFile createDaoFile(int pos, String mimetype, String uuid) {
        return createDaoFile(null, null, pos, mimetype, uuid);
    }

    public static DaoFile createDaoFile(String name, Long size, int pos, String mimetype, String uuid) {
        DaoFile daoFile = ApuxFactory.getObjFactory().createDaoFile();
        if (uuid!=null) {
        	daoFile.setUuid(uuid);
        } else {
        	daoFile.setUuid(UUID.randomUUID().toString());
        }
        daoFile.setPos(pos);

        if (name != null) {
            addFileName(daoFile, name);
        }
        if (size != null) {
            addFileSize(daoFile, size);
        }
        addMimeType(daoFile, mimetype);

        return daoFile;
    } 

}
