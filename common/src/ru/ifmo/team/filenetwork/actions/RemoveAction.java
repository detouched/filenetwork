package ru.ifmo.team.filenetwork.actions;

import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.fileprotocol.FileSetType;
import ru.ifmo.team.fileprotocol.FileType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * User: Daniel Penkin
 * Date: May 3, 2009
 * Version: 1.0
 */
public class RemoveAction extends Action {

    private final Set<SharedFile> files = new HashSet<SharedFile>();

    public RemoveAction(Set<SharedFile> files) {
        super(ActionType.REMOVE);
        this.files.addAll(files);
    }

    public Set<SharedFile> getFiles() {
        return Collections.unmodifiableSet(files);
    }

    public FileSetType toFileSetType() {
        FileSetType removeSection = FileSetType.Factory.newInstance();
        for (SharedFile file : files) {
            FileType xmlFile = removeSection.addNewFile();
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
        }
        return removeSection;
    }
}
