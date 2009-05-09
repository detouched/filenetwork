package ru.ifmo.team.filenetwork.actions;

import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.fileprotocol.GetType;

/**
 * User: Daniel Penkin
 * Date: May 3, 2009
 * Version: 1.0
 */
public class GetAction extends Action {

    private final SharedFile file;
    private final int fromPart;
    private final int tillPart;

    public GetAction(SharedFile file, int fromPart, int tillPart) {
        super(ActionType.GET);
        this.file = file;
        this.fromPart = fromPart;
        this.tillPart = tillPart;
    }

    public SharedFile getFile() {
        return file;
    }

    public int getFromPart() {
        return fromPart;
    }

    public int getTillPart() {
        return tillPart;
    }

    public GetType toGetType() {
        GetType getSection = GetType.Factory.newInstance();
        if (fromPart != 0) {
            getSection.setFromPart(fromPart);
        }
        if (tillPart != 0) {
            getSection.setTillPart(tillPart);
        }
        getSection.setName(file.getName());
        if (file.getDescription() != null) {
            getSection.setDescription(file.getDescription());
        }
        if (file.getSize() != 0) {
            getSection.setSize(file.getSize());
        }
        if (file.getHash() != null) {
            getSection.setHash(file.getHash());
        }
        return getSection;
    }
}