package de.schroedingerscat.coverblame;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import de.schroedingerscat.coverblame.blame.Blamer;
import de.schroedingerscat.coverblame.blame.SVNBlamer;
import de.schroedingerscat.coverblame.result.AuthorInfo;
import de.schroedingerscat.coverblame.result.LineInfo;
import de.schroedingerscat.coverblame.result.Lines;
import de.schroedingerscat.coverblame.result.Result;
import de.schroedingerscat.coverblame.result.Summary;
import de.schroedingerscat.coverblame.result.UntestedLine;
import jacoco.report.Group;
import jacoco.report.Line;
import jacoco.report.Package;
import jacoco.report.Report;
import jacoco.report.Sourcefile;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
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

    private int classesChecked;
    private final SortedSet<String> checkedSourceFiles = new TreeSet<>();

    private final Collection<File> sourceDirs = new ArrayList<>();
    private final Collection<File> executionData = new ArrayList<>();

    private final Map<String, AuthorBlameCounter> counters = new TreeMap<>();
    private final Multimap<String, LineInfo> untestedLines = TreeMultimap.create();

    private Blamer blamer;

    public CoverBlame() {
        blamer = new SVNBlamer();
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
        classesChecked = 0;
        checkedSourceFiles.clear();
        for (File s : executionData) {
            evaluate(s);
        }

        generateOutput();
    }

    private void generateOutput() {

        int totalLines = 0;
        for (Map.Entry<String, AuthorBlameCounter> entry : counters.entrySet()) {
            System.out.println(entry.getKey()
                    + "\t" + entry.getValue().getTestedLines()
                    + "\t" + entry.getValue().getUntestedLines()
                    + "\t" + entry.getValue().getTestedBranches()
                    + "\t" + entry.getValue().getUntestedBranches());

            totalLines += entry.getValue().getTotalLines();

        }

        System.out.println("Classes checked=" + classesChecked);
        System.out.println("Total lines=" + totalLines);

        final Result result = new Result();
        for (String author : counters.keySet()) {
            final AuthorInfo authorInfo = new AuthorInfo();
            result.getAuthorInfo().add(authorInfo);
            final Summary summary = new Summary();

            authorInfo.setName(author);
            authorInfo.setSummary(summary);

            final AuthorBlameCounter counter = counters.get(author);
            summary.setTestedLines(BigInteger.valueOf(counter.getTestedLines()));
            summary.setUntestedLines(BigInteger.valueOf(counter.getUntestedLines()));
            summary.setTestedBranches(BigInteger.valueOf(counter.getTestedBranches()));
            summary.setUntestedBranches(BigInteger.valueOf(counter.getUntestedBranches()));

            if (untestedLines.containsKey(author)) {
                final Lines lines = new Lines();
                authorInfo.setLines(lines);
                for (LineInfo line : untestedLines.get(author)) {
                    final UntestedLine untested = new UntestedLine();
                    lines.getUntestedLine().add(untested);
                    untested.setFqn(line.getFqn());
                    untested.setLineNumber(BigInteger.valueOf(line.getLineNo()));
                }
            }
        }

        try {

            final JAXBContext jaxbContext = JAXBContext.newInstance(Result.class);
            final Marshaller marshaller = jaxbContext.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(result, new File("coverblame.xml"));
        } catch (JAXBException ex) {
            Logger.getLogger(CoverBlame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void evaluate(File jacocoFile) throws JAXBException, XMLStreamException {
        System.out.println("Checking file " + jacocoFile.getAbsolutePath());
        JAXBContext jaxbContext = JAXBContext.newInstance(Report.class);
        Unmarshaller u = jaxbContext.createUnmarshaller();

        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLStreamReader xsr = xif.createXMLStreamReader(new StreamSource(jacocoFile));

        final Report report = (Report) u.unmarshal(xsr);
        evaluate(report);
        generateOutput();
    }

    private void evaluate(Report report) {
        //System.out.println("Checking report "+report);
        for (Package p : report.getPackage()) {
            evaluate(p);
        }

        for (Group g : report.getGroup()) {
            evaluate(g);
        }
    }

    private void evaluate(Package p) {
        //System.out.println("Checking package "+p);
        for (Object child : p.getClazzOrSourcefile()) {
            if (child instanceof Sourcefile) {
                evaluate((Sourcefile) child, p.getName());
            }
        }
    }

    private void evaluate(Group g) {
        System.out.println("Checking group " + g.getName());

        for (Package p : g.getPackage()) {
            evaluate(p);
        }
    }

    private void evaluate(Sourcefile s, String packageName) {
        final String fqn = getFqn(packageName, s);
        final File sourceFile = getSourceFile(fqn);

        if (sourceFile == null) {
            System.err.println("Source file not found: " + s.getName());
            return;
        }

        classesChecked++;
        System.out.println("Checking " + sourceFile.getAbsolutePath());

        final Map<Integer, String> info = blamer.blame(sourceFile);

        for (Line l : s.getLine()) {
            final String author = info.get(l.getNr().intValue());
            if (author != null) {
                final AuthorBlameCounter counter = getCounter(author);
                if (isTested(l)) {
                    counter.incTestedLines();
                } else {
                    if (isUntested(l)) {
                        counter.incUntestedLines();
                        untestedLines.put(author, new LineInfo(fqn, l.getNr().intValue()));
                    }
                }

                counter.incTestedBranches(l.getCb().intValue());
                counter.incUntestedBranches(l.getMb().intValue());
            }
        }

        generateOutput();
    }

    private String getFqn(String packageName, Sourcefile s) {
        return packageName + "/" + s.getName();
    }

    private static boolean isUntested(Line l) {
        return l.getMi().intValue() > 0;
    }

    private static boolean isTested(Line l) {
        return l.getCi().intValue() > 0;
    }

    /**
     * Try to find the source file in all source dirs.
     *
     * @param sourceName
     * @return
     */
    private File getSourceFile(String sourceName) {
        for (File sourceDir : sourceDirs) {
            final File target = new File(sourceDir, sourceName);
            if (target.exists() && target.isFile() && target.canRead()) {
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
        if (!counters.containsKey(author)) {
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

        for (String jacocoFile : options.valuesOf(jacocoOpts)) {
            c.addExecutionData(new File(jacocoFile));
        }

        for (String sourceDir : options.valuesOf(sourceOpts)) {
            c.addSourceDir(new File(sourceDir));
        }

        c.blame();

    }

}
