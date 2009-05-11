package ru.ifmo.team.filenetwork.actions;

import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.fileprotocol.FilePartType;
import ru.ifmo.team.fileprotocol.FileType;

/**
 * User: Daniel Penkin
 * Date: May 3, 2009
 * Version: 1.0
 */
public class TransferAction extends Action {

    private final SharedFile file;
    private final int partNumber;
    private final int partsTotal;
    private final String partHash;
    private final byte[] data;

    public TransferAction(SharedFile file, int partNumber, int partsTotal, String partHash, byte[] data) {
        super(ActionType.TRANSFER);
        this.file = file;
        this.partNumber = partNumber;
        this.partsTotal = partsTotal;
        this.partHash = partHash;
        this.data = data;
    }

    public SharedFile getFile() {
        return file;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public int getPartsTotal() {
        return partsTotal;
    }

    public String getPartHash() {
        return partHash;
    }

    public byte[] getData() {
        return data;
    }

    public FilePartType toFilePartType() {
        FilePartType filePartSection = FilePartType.Factory.newInstance();
        FileType xmlFile = filePartSection.addNewFile();
        xmlFile.setName(file.getName());
        if (file.getDescription() != null) {
            xmlFile.setDescription(file.getDescription());
        }
        if (file.getSize() != 0) {
            xmlFile.setSize(file.getSize());
        }
        if (file.getHash() != null) {
            xmlFile.setHash(file.getHash());
        }
        filePartSection.setNumber(partNumber);
        filePartSection.setTotal(partsTotal);
        filePartSection.setHash(partHash);
        filePartSection.setPart(data);
        return filePartSection;
    }
}
