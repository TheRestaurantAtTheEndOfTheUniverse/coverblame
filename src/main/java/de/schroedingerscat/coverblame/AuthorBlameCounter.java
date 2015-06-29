package de.schroedingerscat.coverblame;

/**
 *
 */
class AuthorBlameCounter {

    /**
     * Lines that have been tested
     */
    private int testedLines;
    
    /**
     * Lines with no test
     */
    private int untestedLines;
    
    /**
     * Total tested branches
     */
    private int testedBranches;
    
    /**
     * Total untested branches
     */
    private int untestedBranches;

    public void incTestedLines() {
        incTestedLines(1);
    }

    public void incTestedLines(int count) {
        testedLines += count;
    }

    public void incUntestedLines() {
        incUntestedLines(1);
    }

    public void incUntestedLines(int count) {
        untestedLines += count;
    }

    public void incTestedBranches(int count) {
        testedBranches += count;
    }

    public void incUntestedBranches(int count) {
        untestedBranches += count;
    }

    public int getTestedLines() {
        return testedLines;
    }

    public int getUntestedLines() {
        return untestedLines;
    }

    public int getTestedBranches() {
        return testedBranches;
    }

    public int getUntestedBranches() {
        return untestedBranches;
    }

    public int getTotalLines() {
        return testedLines + untestedLines;
    }
    
    @Override
    public String toString() {
        return "testedLines=" + testedLines + ", untestedLines=" + untestedLines + ", testedBranches=" + testedBranches + ", untestedBranches=" + untestedBranches;
    }

}
