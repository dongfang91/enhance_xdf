package edu.arizona.biosemantics.semanticmarkup.enhance.transform;

import org.jdom2.Element;


public class BiologicalEntityPosition {
	public Element biologicalEntity;
	public int index;
	
	public BiologicalEntityPosition(Element biologicalEntity, int index) {
		this.biologicalEntity = biologicalEntity;
		this.index = index;
		
	}
	
	public Element getBiologicalEntity() {
		return this.biologicalEntity;
	}
	
	public void setBiologicalEntity(Element biologicalEntity){
		this.biologicalEntity=biologicalEntity;
	}
	
	public void setBiologicalEntity(int index){
		this.index=index;
	}
	
	public int getIndex(){
		return this.index;
		
	}

}
