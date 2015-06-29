package de.schroedingerscat.coverblame.blame;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNAnnotateHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SVNBlamer implements Blamer {

    @Override
    public Map<Integer, String> blame(File sourceFile) {
        final Map<Integer, String> blame = new HashMap<>();

        if(isVersioned(sourceFile)) {
            try {
                SVNLogClient log = new SVNLogClient(SVNWCUtil.createDefaultAuthenticationManager(), null);
                log.doAnnotate(sourceFile, SVNRevision.UNDEFINED, SVNRevision.create(1), SVNRevision.HEAD, new ISVNAnnotateHandler() {

                    @Override
                    public void handleLine(Date date, long l, String author, String line) throws SVNException {
                    }

                    @Override
                    public void handleLine(Date date, long rev, String author, String string1, Date date1, long merged, String string2, String string3, int line) throws SVNException {
                        blame.put(line, author);
                    }

                    @Override
                    public boolean handleRevision(Date date, long l, String string, File file) throws SVNException {
                        return false;
                    }

                    @Override
                    public void handleEOF() throws SVNException {
                    }
                });
            } catch(SVNException ex) {
                Logger.getLogger(SVNBlamer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return blame;
    }

    public boolean isVersioned(File path) {
        final SVNClientManager manager = SVNClientManager.newInstance();
        final SVNStatusClient client = manager.getStatusClient();

        final VersionedHandler handler = new VersionedHandler();

        try {
            client.doStatus(path, SVNRevision.UNDEFINED, SVNDepth.EMPTY,
              false, true, true, false, handler, null);
        } catch(SVNException svne) {
            final SVNErrorCode errorCode
              = svne.getErrorMessage().getErrorCode();
            if(errorCode == SVNErrorCode.WC_NOT_DIRECTORY || errorCode
              == SVNErrorCode.WC_NOT_FILE) {
                return false;
            }
        }

        return !handler.isUnversioned();
    }
}
