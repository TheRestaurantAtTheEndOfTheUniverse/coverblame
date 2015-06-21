package de.schroedingerscat.coverblame;

import de.schroedingerscat.coverblame.blame.Blamer;
import de.schroedingerscat.coverblame.blame.SVNBlamer;
import jacoco.report.Line;
import jacoco.report.Package;
import jacoco.report.Report;
import jacoco.report.Sourcefile;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 *
 * @author
 */
public class CoverBlame {

    private final Collection<File> sourceDirs = new ArrayList<>();
    private final Collection<File> executionData = new ArrayList<>();

    private final Map<String, AuthorBlameCounter> counters = new HashMap<>();

    private Blamer blamer;

    public CoverBlame() {
        blamer=new SVNBlamer();
    }

    public Map<String, AuthorBlameCounter> getCounters() {
        return counters;
    }
    
    public void addSourceDir(File sourceDir) {
        sourceDirs.add(sourceDir);
    }
    
    public void addExecutionData(File executionFile) {
        executionData.add(executionFile);
    }

    public void blame() throws JAXBException, XMLStreamException {
        for(File s : executionData) {
            evaluate(s);
        }

        for(Map.Entry<String, AuthorBlameCounter> entry : counters.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue().toString());
        }
    }

    private void evaluate(File jacocoFile) throws JAXBException, XMLStreamException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Report.class);
        Unmarshaller u = jaxbContext.createUnmarshaller();

        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLStreamReader xsr = xif.createXMLStreamReader(new StreamSource(jacocoFile));

        final Report report = (Report)u.unmarshal(xsr);
        evaluate(report);
    }

    private void evaluate(Report report) {
        for(Package p : report.getPackage()) {
            evaluate(p);
        }
    }

    private void evaluate(Package p) {
        for(Object child : p.getClazzOrSourcefile()) {
            if(child instanceof Sourcefile) {
                evaluate((Sourcefile)child, p.getName());
            }
        }
    }

    private void evaluate(Sourcefile s, String packageName) {
        final File sourceFile = getSourceFile(packageName + "/" + s.getName());
        if(sourceFile == null) {
            System.err.println("Source file not found: " + s.getName());
            return;
        }

        final Map<Integer, String> info = blamer.blame(sourceFile);

        for(Line l : s.getLine()) {
            final String author = info.get(l.getNr().intValue());
            if(author != null) {
                final AuthorBlameCounter counter = getCounter(author);
                if(l.getCi().intValue() > 0) {
                    counter.incTestedLines();
                }
                else {
                    if(l.getMi().intValue() > 0) {
                        counter.incUntestedLines();
                    }
                }

                counter.incTestedBranches(l.getCb().intValue());
                counter.incUntestedBranches(l.getMb().intValue());
            }
        }
    }

    /**
     * Try to find the source file in all source dirs.
     * 
     * @param sourceName
     * @return 
     */
    private File getSourceFile(String sourceName) {
        for(File sourceDir : sourceDirs) {
            final File target = new File(sourceDir, sourceName);
            if(target.exists() && target.isFile() && target.canRead()) {
                return target;
            }
        }

        return null;
    }

    /**
     * Get count data for an author. 
     * Create if it does not exist yet.
     * 
     * @param author
     * @return 
     */
    private AuthorBlameCounter getCounter(String author) {
        if(!counters.containsKey(author)) {
            counters.put(author, new AuthorBlameCounter());
        }

        return counters.get(author);
    }

    public static void main(String[] args) throws Exception {
        final CoverBlame c = new CoverBlame();

        final OptionParser parser = new OptionParser();
        final OptionSpec<String> jacocoOpts = parser.accepts("jacoco").withRequiredArg();
        final OptionSpec<String> sourceOpts = parser.accepts("source").withRequiredArg();

        final OptionSet options = parser.parse(args);

        for(String jacocoFile : options.valuesOf(jacocoOpts)) {
            c.addExecutionData(new File(jacocoFile));
        }

        for(String sourceDir : options.valuesOf(sourceOpts)) {
            c.addSourceDir(new File(sourceDir));
        }

        c.blame();

    }

}
