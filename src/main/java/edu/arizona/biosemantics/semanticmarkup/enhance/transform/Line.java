package edu.arizona.biosemantics.semanticmarkup.enhance.transform;

import java.util.LinkedList;
import java.util.List;

public class Line {
	public List<String> line;
	
    
	public Line(List<String> line){
		this.line = line;
	}
	
	public List<String> getLine() {
		return this.line;
	}
	
	public void setLine(List<String> line){
		this.line=line;
	}
	
	public int lengthOfLine(){
		return line.size();
	}
	
	
}
