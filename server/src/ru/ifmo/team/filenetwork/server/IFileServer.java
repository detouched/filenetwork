package ru.ifmo.team.filenetwork.server;

import ru.ifmo.team.filenetwork.Message;
import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.util.IClientManager;

import java.util.Set;

/**
 * User: Daniel Penkin
 * Date: May 6, 2009
 * Version: 1.0
 */
public interface IFileServer extends IClientManager {

    Set<SharedFile> getFileSet();

    void addFiles(Set<SharedFile> files, String clientOwner);

    void removeFiles(Set<SharedFile> files, String clientOwner);

    void addMessage(Message message);

    void clientLeft(String clientID);

    void shutDown();
}
