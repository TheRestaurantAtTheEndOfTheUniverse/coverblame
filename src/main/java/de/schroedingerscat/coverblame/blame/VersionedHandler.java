package de.schroedingerscat.coverblame.blame;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

public class VersionedHandler implements ISVNStatusHandler {

    boolean unversioned = true;

    public void handleStatus(SVNStatus status) throws SVNException {
        unversioned = status.getContentsStatus() == SVNStatusType.STATUS_UNVERSIONED;
    }

    public boolean isUnversioned() {
        return unversioned;
    }
}
