package ru.ifmo.team.filenetwork;

import org.apache.xmlbeans.XmlException;
import ru.ifmo.team.filenetwork.actions.*;
import ru.ifmo.team.fileprotocol.*;
import ru.ifmo.team.util.KeyGen;

import java.util.HashSet;
import java.util.Set;

/**
 * User: Daniel Penkin
 * Date: May 5, 2009
 * Version: 1.0
 */
public class Message {

    public static final int SID_LENGTH = 16;

    private final Action action;
    private final FileProtocolType.Direction.Enum direction;
    private final String clientID;
    private final String SID;
    private final String status;

    public Message(Action action, FileProtocolType.Direction.Enum direction,
                   String clientID, String status) {
        this(action, direction, clientID, null, status);
    }

    public Message(Action action, FileProtocolType.Direction.Enum direction,
                   String clientID, String SID, String status) {
        this.action = action;
        this.direction = direction;
        this.clientID = clientID;
        this.status = status;
        if (SID == null) {
            this.SID = KeyGen.generate(SID_LENGTH);
        } else {
            this.SID = SID;
        }
    }

    public Action getAction() {
        return action;
    }

    public FileProtocolType.Direction.Enum getDirection() {
        return direction;
    }

    public String getClientID() {
        return clientID;
    }

    public String getSID() {
        return SID;
    }

    public String getStatus() {
        return status;
    }

    public String encodeMessage() {
        FileProtocolDocument doc = FileProtocolDocument.Factory.newInstance();
        FileProtocolType xmlMsg = doc.addNewFileProtocol();
        xmlMsg.setDirection(direction);
        xmlMsg.setClientID(clientID);
        xmlMsg.setSID(SID);
        if (status != null) {
            xmlMsg.setStatus(status);
        }
        if (action != null) {
            switch (action.getActionType()) {
                case ADD:
                    xmlMsg.setAdd(((AddAction) action).toFileSetType());
                    break;
                case REMOVE:
                    xmlMsg.setRemove(((RemoveAction) action).toFileSetType());
                    break;
                case GET:
                    xmlMsg.setGet(((GetAction) action).toGetType());
                    break;
                case TRANSFER:
                    xmlMsg.setFilePart(((TransferAction) action).toFilePartType());
                    break;
            }
        }
        return doc.toString();
    }

    public static Message decodeMessage(String message) throws MessagingException {
        Action action = null;
        try {
            FileProtocolDocument doc = FileProtocolDocument.Factory.parse(message);
            FileProtocolType xmlMsg = doc.getFileProtocol();
            if (xmlMsg.isSetAdd()) {
                action = processAdd(xmlMsg.getAdd());
            } else if (xmlMsg.isSetRemove()) {
                action = processRemove(xmlMsg.getRemove());
            } else if (xmlMsg.isSetGet()) {
                action = processGet(xmlMsg.getGet());
            } else if (xmlMsg.isSetFilePart()) {
                action = processFilePart(xmlMsg.getFilePart());
            }
            return new Message(action, xmlMsg.getDirection(),
                    xmlMsg.getClientID(), xmlMsg.getSID(), xmlMsg.getStatus());
        } catch (XmlException e) {
            throw new MessagingException("Unable to parse doc", e);
        }

    }

    private static AddAction processAdd(FileSetType addSection) {
        Set<SharedFile> fileSet = readFileSet(addSection);
        return new AddAction(fileSet);
    }

    private static RemoveAction processRemove(FileSetType removeSection) {
        Set<SharedFile> fileSet = readFileSet(removeSection);
        return new RemoveAction(fileSet);
    }

    private static Set<SharedFile> readFileSet(FileSetType fileSetSection) {
        FileType[] files = fileSetSection.getFileArray();
        Set<SharedFile> fileSet = new HashSet<SharedFile>();
        for (FileType file : files) {
            SharedFile sharedFile = readSharedFile(file);
            fileSet.add(sharedFile);
        }
        return fileSet;
    }

    private static GetAction processGet(GetType getSection) {
        SharedFile sharedFile = readSharedFile(getSection);
        return new GetAction(sharedFile, getSection.getFromPart(), getSection.getTillPart());
    }

    private static TransferAction processFilePart(FilePartType filePartSection) {
        SharedFile sharedFile = readSharedFile(filePartSection.getFile());
        return new TransferAction(sharedFile, filePartSection.getNumber(),
                filePartSection.getTotal(), filePartSection.getHash(),
                filePartSection.getPart());
    }

    private static SharedFile readSharedFile(FileType fileSection) {
        SharedFile file = new SharedFile(fileSection.getSize(), fileSection.getHash());
        file.setName(fileSection.getName());
        file.setDescription(fileSection.getDescription());
        return file;
    }

}
