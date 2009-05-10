package ru.ifmo.team.filenetwork.client;

import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.util.IMessageAcceptor;
import ru.ifmo.team.util.tcp.client.IClient;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * User: Daniel Penkin
 * Date: May 9, 2009
 * Version: 1.0
 */
public interface IFileClient extends IMessageAcceptor {

    boolean addLocalFiles(Map<File, String> files);

    boolean removeLocalFiles(Set<SharedFile> files);

    Set<SharedFile> getSharedFilesSet();

    Set<SharedFile> getLocalSharedFileSet();

    String downloadFile(SharedFile sharedFile, File file);

    void registerFileListener(IFileWatcher watcher);

    void registerTCPClient(IClient client);

}
