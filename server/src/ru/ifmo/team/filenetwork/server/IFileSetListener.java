package ru.ifmo.team.filenetwork.server;

import ru.ifmo.team.filenetwork.SharedFile;

import java.util.Set;

/**
 * User: Daniel Penkin
 * Date: May 6, 2009
 * Version: 1.0
 */
public interface IFileSetListener {

    void fileSetUpdated(Set<SharedFile> added, Set<SharedFile> removed);
}
