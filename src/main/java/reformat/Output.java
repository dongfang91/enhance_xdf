package reformat;

import java.util.LinkedList;
import java.util.List;

public class Output {
	public int id;
	public String term;
	public String label;
	public int coreferenceId;
	public List<String []> sentence;
    
	public Output(List<String []> sentence){
		
//		this.id = id;
//		this.term = term;
//		this.label = label; 
//		this.coreferenceId = coreferenceId;
		sentence =  this.sentence;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setId(int id){
		this.id=id;
	}
	
	public int getCoreferenceId(){
		return this.coreferenceId;
	}
	
	public void setCoreferenceId(int coreferenceId){
		this.coreferenceId = coreferenceId;
	}
	
	public String getTerm(){
		return this.term;
	}
	
	public void setTerm(String term){
		this.term = term;
	}
	
	public String getLabel(){
		return this.label;
	}
	
	public void setLabel(String label){
		this.label = label;
	}
	
	
}