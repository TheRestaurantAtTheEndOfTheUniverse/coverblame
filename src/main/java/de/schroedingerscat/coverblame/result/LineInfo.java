/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.schroedingerscat.coverblame.result;

import java.util.Objects;

/**
 *
 * @author kessinger
 */
public class LineInfo implements Comparable<LineInfo> {
    private final String fqn;
    private final int lineNo;

    public LineInfo(String fqn, int lineNo) {
        if(fqn == null)
            throw new IllegalArgumentException("fqn is required");
        
        this.fqn = fqn;
        this.lineNo = lineNo;
    }

    public String getFqn() {
        return fqn;
    }

    public int getLineNo() {
        return lineNo;
    }

    @Override
    public int compareTo(LineInfo o) {
        if(!fqn.equals(o.fqn))
            return fqn.compareTo(o.fqn);
        
        return lineNo-o.lineNo;
    }
}
