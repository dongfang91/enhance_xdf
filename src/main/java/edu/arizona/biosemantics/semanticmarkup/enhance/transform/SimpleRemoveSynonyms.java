package edu.arizona.biosemantics.semanticmarkup.enhance.transform;

import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;

import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsSynonyms;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsSynonyms.SynonymSet;

public class SimpleRemoveSynonyms extends AbstractTransformer {

	private KnowsSynonyms knowsSynonyms;

	public SimpleRemoveSynonyms(KnowsSynonyms knowsSynonyms) {
		this.knowsSynonyms = knowsSynonyms;
	}
	
	@Override
	public void transformAll(Element statement, Element biologicalEntity,List<Element>context) {
		removeBiologicalEntitySynonyms(biologicalEntity);
	}

	private void removeBiologicalEntitySynonyms(Element biologicalEntity) {
		
			String name = biologicalEntity.getAttributeValue("name");
			if(name != null) {
				String newName = createSynonymReplacedValue(name);
				biologicalEntity.setAttribute("name", newName);
			}
	}

	private String createSynonymReplacedValue(String name) {
		Set<SynonymSet> synonymSets = knowsSynonyms.getSynonyms(name);
		if(synonymSets.size() > 1) {
			return name;
		} else {
			return synonymSets.iterator().next().getPreferredTerm();
		}
	}

	@Override
	public boolean transform(List<AbstractTransformer> transformers,Element statement, Element biologicalEntity,
			List<Element> context, List<Element> wholeStatement) {
				return false;
		// TODO Auto-generated method stub
		
	}



}
