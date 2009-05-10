package ru.ifmo.team.filenetwork.client;

import ru.ifmo.team.filenetwork.SharedFile;

import java.io.File;
import java.util.Set;

/**
 * User: Daniel Penkin
 * Date: May 9, 2009
 * Version: 1.0
 */
public interface IFileWatcher {

    void downloadCompleted(String id, File file);

    void fileListUpdated(Set<SharedFile> local, Set<SharedFile> foreign);
}
