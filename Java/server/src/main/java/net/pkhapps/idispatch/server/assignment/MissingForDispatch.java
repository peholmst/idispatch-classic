package net.pkhapps.idispatch.server.assignment;

/**
 * What an assignment must have before resources can be dispatched to it; see {@link Assignment#missingForDispatch()}.
 */
public enum MissingForDispatch {
    TYPE, MUNICIPALITY, ADDRESS
}
