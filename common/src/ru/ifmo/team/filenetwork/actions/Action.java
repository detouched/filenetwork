package ru.ifmo.team.filenetwork.actions;

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
