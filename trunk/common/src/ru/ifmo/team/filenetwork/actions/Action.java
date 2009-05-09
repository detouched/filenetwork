package ru.ifmo.team.filenetwork.actions;

import ru.ifmo.team.filenetwork.SharedFile;
import org.apache.xmlbeans.XmlObject;

import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

/**
 * User: Daniel Penkin
 * Date: May 3, 2009
 * Version: 1.0
 */
public abstract class Action {

    protected final ActionType actionType;


    protected Action(ActionType type) {
        actionType = type;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public static enum ActionType {
        ADD,
        REMOVE,
        GET,
        TRANSFER
    }

}
