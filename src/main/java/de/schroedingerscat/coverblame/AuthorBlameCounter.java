package de.schroedingerscat.coverblame;

/**
 *
 */
class AuthorBlameCounter {

    private int testedLines;
    private int untestedLines;
    private int testedBranches;
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

    @Override
    public String toString() {
        return "testedLines=" + testedLines + ", untestedLines=" + untestedLines + ", testedBranches=" + testedBranches + ", untestedBranches=" + untestedBranches;
    }

}
