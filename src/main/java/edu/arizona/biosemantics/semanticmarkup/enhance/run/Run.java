package edu.arizona.biosemantics.semanticmarkup.enhance.run;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import au.com.bytecode.opencsv.CSVReader;
import edu.arizona.biosemantics.common.biology.TaxonGroup;
import edu.arizona.biosemantics.common.ling.Token;
import edu.arizona.biosemantics.common.ling.know.ICharacterKnowledgeBase;
import edu.arizona.biosemantics.common.ling.know.IGlossary;
import edu.arizona.biosemantics.common.ling.know.SingularPluralProvider;
import edu.arizona.biosemantics.common.ling.know.Term;
import edu.arizona.biosemantics.common.ling.know.lib.GlossaryBasedCharacterKnowledgeBase;
import edu.arizona.biosemantics.common.ling.know.lib.InMemoryGlossary;
import edu.arizona.biosemantics.common.ling.know.lib.WordNetPOSKnowledgeBase;
import edu.arizona.biosemantics.common.ling.transform.IInflector;
import edu.arizona.biosemantics.common.ling.transform.ITokenizer;
import edu.arizona.biosemantics.common.ling.transform.lib.SentencesTokenizer;
import edu.arizona.biosemantics.common.ling.transform.lib.SomeInflector;
import edu.arizona.biosemantics.common.ling.transform.lib.WhitespaceTokenizer;
import edu.arizona.biosemantics.common.log.LogLevel;
import edu.arizona.biosemantics.oto.client.oto.OTOClient;
import edu.arizona.biosemantics.oto.model.GlossaryDownload;
import edu.arizona.biosemantics.oto.model.TermCategory;
import edu.arizona.biosemantics.oto.model.TermSynonym;
import edu.arizona.biosemantics.oto.model.lite.Decision;
import edu.arizona.biosemantics.oto.model.lite.Download;
import edu.arizona.biosemantics.oto.model.lite.Synonym;
import edu.arizona.biosemantics.oto.model.lite.UploadResult;
import edu.arizona.biosemantics.semanticmarkup.enhance.config.Configuration;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsClassHierarchy;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsEntityExistence;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsPartOf;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsSynonyms;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsSynonyms.SynonymSet;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.lib.CSVKnowsClassHierarchy;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.lib.CSVKnowsPartOf;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.lib.CSVKnowsSynonyms;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.lib.KeyWordBasedKnowsCharacterConstraintType;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.AbstractTransformer;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.CollapseBiologicalEntities;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.CollapseBiologicalEntityToName;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.CollapseCharacterToValue;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.CollapseCharacters;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.CreateRelationFromCharacterConstraint;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.MoveModifierCharactersToBiologicalEntityConstraint;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.MoveRelationToBiologicalEntityConstraint;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.RemoveDuplicateValues;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.RemoveNonSpecificBiologicalEntities;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.RemoveNonSpecificBiologicalEntitiesByBackwardConnectors;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.RemoveNonSpecificBiologicalEntitiesByCollections;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.RemoveNonSpecificBiologicalEntitiesByForwardConnectors;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.RemoveNonSpecificBiologicalEntitiesByPassedParents;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.RemoveNonSpecificBiologicalEntitiesByRelations;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.RemoveOrphanRelations;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.RemoveSynonyms;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.SimpleRemoveSynonyms;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.CreateOrPopulateWholeOrganism;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.MoveCharacterToStructureConstraint;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.ReplaceNegationCharacterByNegationOrAbsence;
//import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.MoveCharactersToAlternativeParent;
//import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.MoveNegationCharacterToBiologicalEntityConstraint;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.MoveNegationOrAdverbBiologicalEntityConstraint;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.OrderBiologicalEntityConstraint;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.RemoveUselessCharacterConstraint;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.RemoveUselessWholeOrganism;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.RenameCharacter;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.ReplaceTaxonNameByWholeOrganism;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.SortBiologicalEntityNameWithDistanceCharacter;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.SplitCompoundBiologicalEntitiesCharacters;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.SplitCompoundBiologicalEntity;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.StandardizeCount;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.StandardizeQuantityPresence;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.StandardizeStructureName;
import edu.arizona.biosemantics.semanticmarkup.enhance.transform.old.StandardizeTerminology;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.PTBTokenizer;

public class Run extends AbstractTransformer{

	private SAXBuilder saxBuilder = new SAXBuilder();
	private List<AbstractTransformer> transformers = new LinkedList<AbstractTransformer>();
	
	public Run() {
	}
	
	public void run(File inputDirectory, File outputDirectory) {
		for(File file : inputDirectory.listFiles()) {
			if(file.isFile()) {
				System.out.println("Transforming file " + file.getName());
				log(LogLevel.DEBUG, "Transforming file " + file.getName());
				Document document = null;
				try(InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), "UTF8")) {
					document = saxBuilder.build(inputStreamReader);
				} catch (JDOMException | IOException e) {
					log(LogLevel.ERROR, "Can't read xml from file " + file.getAbsolutePath(), e);
				}
				
				if(document != null){
 					for (Element description : this.descriptionXpath.evaluate(document)){
						List<Element> wholeStatements = new LinkedList<Element>();
						for(Element statement : description.getChildren("statement")) {
							wholeStatements.add(statement);
							for(Element biologicalEntity : statement.getChildren("biological_entity")) {
								transformers.get(0).transformAll(statement, biologicalEntity, wholeStatements);
							}
						}
						if(wholeStatements.size()==0) return;
						for(Element statement : description.getChildren("statement")) {
							for(Element biologicalEntity : statement.getChildren("biological_entity")) {
								runTransformer(transformers.subList(1,transformers.size() ), statement,biologicalEntity , wholeStatements);
							}
						}	
					}
				}
				File outputFile = new File(outputDirectory, file.getName());
				try {
					outputFile.getParentFile().mkdirs();
					outputFile.createNewFile();
				} catch (IOException e) {
					log(LogLevel.ERROR, "Can't create xml file " + file.getAbsolutePath(), e);
				}
				try(OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF8")) {
					XMLOutputter xmlOutput = new XMLOutputter(Format.getPrettyFormat());
					xmlOutput.output(document, outputStreamWriter);
				} catch (IOException e) {
					log(LogLevel.ERROR, "Can't write xml to file " + file.getAbsolutePath(), e);
				}
			}
		}
	}
	
	public void addTransformer(AbstractTransformer transformer) {
		this.transformers.add(transformer);
	}
	
	public boolean runTransformer(List<AbstractTransformer> transformers,Element statement, Element biologicalEntity,List<Element> wholeStatements){
		
		if(biologicalEntity.getAttributeValue("id").equals("o344"))
     		System.out.println("asdadwfsaf");
		List<Element> context = new LinkedList<Element>();
		context = getContext(statement, wholeStatements);

		String name = biologicalEntity.getAttributeValue("name");
		name = name == null ? "" : name.trim();
		String constraint = biologicalEntity.getAttributeValue("constraint");
		constraint = constraint == null ? "" : constraint.trim();
		String inferredConstraint = biologicalEntity.getAttributeValue("inferred_constraint");
		inferredConstraint = inferredConstraint == null ? "" : inferredConstraint.trim();
		if(RemoveNonSpecificBiologicalEntities.isNonSpecificPart(name)&&inferredConstraint.equals("")) {
			if(!RemoveNonSpecificBiologicalEntities.isPartOfAConstraint(name, constraint)) {
				for(AbstractTransformer transformer : transformers) {
					System.out.println("-------------------------------------------------------------------");
		            if(transformer.transform(transformers,statement, biologicalEntity, context,wholeStatements))
		            	return true;
		        }
			}
		}
		return false;
	}
	
	private List<Element> getContext(Element statement,
			List<Element> wholeStatements) {
		List<Element> context = new LinkedList<Element>();
		List<Element> forwardContext = new LinkedList<Element>();
		List<Element> backwardContext = new LinkedList<Element>();
		int index = wholeStatements.indexOf(statement);
		int total = wholeStatements.size();
		int i =1;
		while (index-i>=0&&i<=3){
			forwardContext.add(wholeStatements.get(index-i));
			i++;
		}
//		while (index+i<total&&i<=3){
//			backwardContext.add(wholeStatements.get(index+i));
//			i++;
//		}
		context.add(statement);
		context.addAll(forwardContext);
//		context.addAll( backwardContext);
		
		
		//another way to get context
//		if(forwardContext.size()<=2){
//			context.addAll(forwardContext);
//			context.addAll(backwardContext);
//		}
//		
//		else {
//			context.add(forwardContext.get(0));
//			context.add(forwardContext.get(1));
//			context.addAll(backwardContext);
//			context.add(forwardContext.get(2));
//		}
	    return context;	
		
	}
		
		
		
		
	
	

	public static void main(String[] args) throws IOException {
		/*for (String arg : args) {
		      // option #1: By sentence.
		      DocumentPreprocessor dp = new DocumentPreprocessor(arg);
		      for (List<HasWord> sentence : dp) {
		        System.out.println(sentence);
		      }
		      // option #2: By token
		      PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new FileReader(arg),
		              new CoreLabelTokenFactory(), "");
		      while (ptbt.hasNext()) {
		        CoreLabel label = ptbt.next();
		        System.out.println(label);
		      }
		    }*/
		
		ITokenizer tokenizer = new WhitespaceTokenizer(); /*ITokenizer() {		
			@Override
			public List<Token> tokenize(String text) {
				List<Token> result = new LinkedList<Token>();
				
				return null;
			}
		};*/
		TaxonGroup taxonGroup = TaxonGroup.PLANT;
		WordNetPOSKnowledgeBase wordNetPOSKnowledgeBase = new WordNetPOSKnowledgeBase(Configuration.wordNetDirectory, false);
		SingularPluralProvider singularPluralProvider = new SingularPluralProvider();
		IInflector inflector = new SomeInflector(wordNetPOSKnowledgeBase, singularPluralProvider.getSingulars(), singularPluralProvider.getPlurals());
		Map<String, String> renames = new HashMap<String, String>();
		renames.put("count", "quantity");
		renames.put("atypical_count", "atypical_quantity");
		renames.put("color", "coloration");
		IGlossary glossary = new InMemoryGlossary();
		initGlossary(glossary, inflector, taxonGroup);
		Set<String> lifeStyles = glossary.getWordsInCategory("life_style");
		lifeStyles.addAll(glossary.getWordsInCategory("growth_form"));
		Set<String> durations = glossary.getWordsInCategory("duration");
		String negWords = "no|not|never";
		String advModifiers = "at least|at first|at times";
		String stopWords = "a|about|above|across|after|along|also|although|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|being|"
				+ "beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|for|from|had|has|have|hence|here|how|however|if|in|into|inside|inward|is|it|its|"
				+ "may|might|more|most|near|of|off|on|onto|or|out|outside|outward|over|should|so|than|that|the|then|there|these|this|those|throughout|"
				+ "to|toward|towards|up|upward|was|were|what|when|where|which|why|with|within|without|would";
		String units = "(?:(?:pm|cm|mm|dm|ft|m|meters|meter|micro_m|micro-m|microns|micron|unes|µm|μm|um|centimeters|centimeter|millimeters|millimeter|transdiameters|transdiameter)[23]?)"; //squared or cubed
		ICharacterKnowledgeBase characterKnowledgeBase = new GlossaryBasedCharacterKnowledgeBase(glossary, 
				negWords, advModifiers, stopWords, units, inflector);
		Set<String> possessionTerms = getWordSet("with|has|have|having|possess|possessing|consist_of");
		
		
		
		//CSVReader reader = new CSVReader(new FileReader("/home/sbs0457/workspace2/enhance12/term_resources/category_mainterm_synonymterm-task-non-specific_structure_term_training_v2.csv"));
		CSVReader reader = new CSVReader(new FileReader("/home/sbs0457/git/enhance/Gordon_complexity_term_review/category_mainterm_synonymterm-task-Gordon_complexity.csv"));
		List<String[]> lines = reader.readAll();
		int i=0;
		final Map<String, SynonymSet> synonymSetsMap = new HashMap<String, SynonymSet>();
		for(String[] line : lines) {
			String preferredTerm = line[1];
			String synonym = line[2];
			if(!synonymSetsMap.containsKey(preferredTerm)) 
				synonymSetsMap.put(preferredTerm, new SynonymSet(preferredTerm, new HashSet<String>()));
			synonymSetsMap.get(preferredTerm).getSynonyms().add(synonym);
		}	
		
		/*List<KnowsSynonyms> hasBiologicalEntitySynonymsList = new LinkedList<KnowsSynonyms>();
		List<KnowsSynonyms> hasCharacterSynonymsList = new LinkedList<KnowsSynonyms>();
		hasBiologicalEntitySynonymsList.add(new CSVKnowsSynonyms() {
			@Override
			public Set<SynonymSet> getSynonyms(String term) {
				Set<SynonymSet> result = new HashSet<SynonymSet>();
				for(SynonymSet synonymSet : synonymSetsMap.values()) {
					if(synonymSet.getPreferredTerm().equals(term) || synonymSet.getSynonyms().contains(term)) 
						result.add(synonymSet);
				}
				if(result.isEmpty())
					result.add(new SynonymSet(term, new HashSet<String>()));
				return result;
			}
		});*/
		
		
		Run run = new Run();
		//AbstractTransformer transformer = new RemoveSynonyms(hasBiologicalEntitySynonymsList, hasBiologicalEntitySynonymsList);
		//AbstractTransformer transformer = new CreateRelationFromCharacterConstraint(new KeyWordBasedKnowsCharacterConstraintType(wordNetPOSKnowledgeBase), inflector);
		//AbstractTransformer transformer = new MoveRelationToBiologicalEntityConstraint();//new KeyWordBasedKnowsCharacterConstraintType(wordNetPOSKnowledgeBase), inflector);
		//AbstractTransformer transformer = new MoveNegationOrAdverbBiologicalEntityConstraint(wordNetPOSKnowledgeBase);
		/*AbstractTransformer transformer = new RemoveNonSpecificBiologicalEntitiesByRelations(new KnowsPartOf() {
			@Override
			public boolean isPartOf(String part, String parent) {
				if(part.equals("apex") && parent.equals("leaf")) {
					return true;
				}
				if(part.equals("base") && parent.equals("fruit")) {
					return true;
				}
				if(part.equals("base") && parent.equals("petal")) {
					return true;
				}
				return false;
			}
		}, tokenizer, new CollapseBiologicalEntityToName());*/
		
		//AbstractTransformer transformer1 = new MoveCharacterToStructureConstraint();
		//AbstractTransformer transformer2 = new ReplaceNegationCharacterByNegationOrAbsence();
		
		/*AbstractTransformer transformer = new MoveModifierCharactersToBiologicalEntityConstraint(tokenizer, new KnowsEntityExistence() {
			@Override
			public boolean isExistsEntity(String name) {
				if(name.equals("red leaf")) {
					return true;
				}
				return false;
			}
		});*/

		CSVKnowsSynonyms csvKnowsSynonyms = new CSVKnowsSynonyms("synonyms.csv", inflector);
		
		

//		RemoveNonSpecificBiologicalEntitiesByRelations transformer1 = new RemoveNonSpecificBiologicalEntitiesByRelations(
//				new CSVKnowsPartOf("thomas/part-of.csv", csvKnowsSynonyms, inflector), csvKnowsSynonyms,
//				tokenizer, new CollapseBiologicalEntityToName());
//			
//		RemoveNonSpecificBiologicalEntitiesByBackwardConnectors transformer2 = new RemoveNonSpecificBiologicalEntitiesByBackwardConnectors(
//				new CSVKnowsPartOf("thomas/part-of.csv", csvKnowsSynonyms, inflector), csvKnowsSynonyms, 
//				tokenizer, new CollapseBiologicalEntityToName(),inflector);
//		RemoveNonSpecificBiologicalEntitiesByForwardConnectors transformer3 = new RemoveNonSpecificBiologicalEntitiesByForwardConnectors(
//				new CSVKnowsPartOf("thomas/part-of.csv", csvKnowsSynonyms, inflector), csvKnowsSynonyms,
//				tokenizer, new CollapseBiologicalEntityToName());

		RemoveNonSpecificBiologicalEntitiesByPassedParents transformer4 = new RemoveNonSpecificBiologicalEntitiesByPassedParents(
				new CSVKnowsPartOf("dongfang/part-of.csv", csvKnowsSynonyms, inflector), 
				csvKnowsSynonyms, tokenizer, new CollapseBiologicalEntityToName(), inflector);
		
		//RemoveNonSpecificBiologicalEntitiesByCollections removeByCollections = new RemoveNonSpecificBiologicalEntitiesByCollections(
		//		new CSVKnowsPartOf(csvKnowsSynonyms, inflector), csvKnowsSynonyms, new CSVKnowsClassHierarchy(inflector), 
		//		tokenizer, new CollapseBiologicalEntityToName(), inflector);
		
		run.addTransformer(new SimpleRemoveSynonyms(csvKnowsSynonyms));
//		run.addTransformer(transformer1);
//		run.addTransformer(transformer2);
//		run.addTransformer(transformer3);
		run.addTransformer(transformer4);
		
		
		
		
		//run.addTransformer(removeByCollections);
		
		//run.addTransformer(transformer1);
		//run.addTransformer(transformer2);
		/*
		AbstractTransformer transformer = new CollapseCharacterToValue();
		run.addTransformer(new RemoveOrphanRelations());
		run.addTransformer(new RemoveDuplicateValues());
		run.addTransformer(new CollapseBiologicalEntityToName());
		run.addTransformer(new CollapseCharacterToValue());
		run.addTransformer(new CollapseBiologicalEntities());
		run.addTransformer(new CollapseCharacters()); 
		*/
		/*
		run.addTransformer(new SplitCompoundBiologicalEntity(inflector));
		run.addTransformer(new SplitCompoundBiologicalEntitiesCharacters(inflector));
		run.addTransformer(new RemoveUselessWholeOrganism());
		run.addTransformer(new RemoveUselessCharacterConstraint());
		run.addTransformer(new RenameCharacter(renames));
		run.addTransformer(new MoveCharacterToStructureConstraint());
		run.addTransformer(new MoveNegationCharacterToBiologicalEntityConstraint());
		run.addTransformer(new MoveNegationOrAdverbBiologicalEntityConstraint(wordNetPOSKnowledgeBase));
		run.addTransformer(new MoveCharactersToAlternativeParent());
		run.addTransformer(new ReplaceTaxonNameByWholeOrganism());
		run.addTransformer(new CreateOrPopulateWholeOrganism(lifeStyles, "growth_form"));
		run.addTransformer(new CreateOrPopulateWholeOrganism(durations, "duration"));
		run.addTransformer(new StandardizeQuantityPresence());
		run.addTransformer(new StandardizeCount());
		run.addTransformer(new SortBiologicalEntityNameWithDistanceCharacter());
		run.addTransformer(new OrderBiologicalEntityConstraint());
		run.addTransformer(new StandardizeStructureName(characterKnowledgeBase, possessionTerms));
		run.addTransformer(new StandardizeTerminology(characterKnowledgeBase));
		
		run.addTransformer(new RemoveOrphanRelations());
		run.addTransformer(new RemoveDuplicateValues());
		run.addTransformer(new CollapseBiologicalEntityToName());
		run.addTransformer(new CollapseCharacterToValue());
		run.addTransformer(new CollapseBiologicalEntities());
		run.addTransformer(new CollapseCharacters());
		
		
		*/
		
		run.run(new File("dongfang/in_mixedtaxon_reordered"), new File("dongfang/mixed_taxon_output_onlyonto"));

		//run.run(new File("thomas/in3_reordered"), new File("thomas/out3_old_task_ontology_only"));
	}
	
	private static Set<String> getWordSet(String regexString) {
		Set<String> set = new HashSet<String>();
		String[] wordsArray = regexString.split("\\|");
		for (String word : wordsArray)
			set.add(word.toLowerCase().trim());
		return set;
	}
	
	private static void initGlossary(IGlossary glossary, IInflector inflector, TaxonGroup taxonGroup) throws IOException {
		OTOClient otoClient = new OTOClient("http://biosemantics.arizona.edu:8080/OTO");
		GlossaryDownload glossaryDownload = new GlossaryDownload();		
		String glossaryVersion = "latest";
		otoClient.open();
		Future<GlossaryDownload> futureGlossaryDownload = otoClient.getGlossaryDownload(taxonGroup.getDisplayName(), glossaryVersion);
		
		try {
			glossaryDownload = futureGlossaryDownload.get();
		} catch (Exception e) {
			otoClient.close();
			e.printStackTrace();
		}
		otoClient.close();
				
		//add the syn set of the glossary
		HashSet<Term> gsyns = new HashSet<Term>();
		for(TermSynonym termSyn: glossaryDownload.getTermSynonyms()){

			//if(termSyn.getCategory().compareTo("structure")==0){
			if(termSyn.getCategory().matches("structure|taxon_name|substance")) {
				//take care of singular and plural forms
				String syns = ""; 
				String synp = "";
				String terms = "";
				String termp = "";
				if(inflector.isPlural(termSyn.getSynonym().replaceAll("_",  "-"))){ //must convert _ to -, as matching entity phrases will be converted from leg iii to leg-iii in the sentence.
					synp = termSyn.getSynonym().replaceAll("_",  "-");
					syns = inflector.getSingular(synp);					
				}else{
					syns = termSyn.getSynonym().replaceAll("_",  "-");
					synp = inflector.getPlural(syns);
				}

				if(inflector.isPlural(termSyn.getTerm().replaceAll("_",  "-"))){
					termp = termSyn.getTerm().replaceAll("_",  "-");
					terms = inflector.getSingular(termp);					
				}else{
					terms = termSyn.getTerm().replaceAll("_",  "-");
					termp = inflector.getPlural(terms);
				}
				glossary.addSynonym(syns, termSyn.getCategory(), terms);
				glossary.addSynonym(synp, termSyn.getCategory(), termp);
				gsyns.add(new Term(syns, termSyn.getCategory()));
				gsyns.add(new Term(synp, termSyn.getCategory()));
			}else{
				//glossary.addSynonym(termSyn.getSynonym().replaceAll("_",  "-"), "arrangement", termSyn.getTerm());
				glossary.addSynonym(termSyn.getSynonym().replaceAll("_",  "-"), termSyn.getCategory(), termSyn.getTerm());
				gsyns.add(new Term(termSyn.getSynonym().replaceAll("_",  "-"), termSyn.getCategory()));
				//gsyns.add(new Term(termSyn.getSynonym().replaceAll("_",  "-"), "arrangement"));
			}
		}

		//the glossary, excluding gsyns
		for(TermCategory termCategory : glossaryDownload.getTermCategories()) {
			if(!gsyns.contains(new Term(termCategory.getTerm().replaceAll("_", "-"), termCategory.getCategory())))
				glossary.addEntry(termCategory.getTerm().replaceAll("_", "-"), termCategory.getCategory()); //primocane_foliage =>primocane-foliage Hong 3/2014
		}	
		
		
		List<Synonym> synonyms = new LinkedList<Synonym>();
		//CSVReader reader = new CSVReader(new FileReader("/home/sbs0457/workspace2/enhance12/term_resources/category_mainterm_synonymterm-task-non-specific_structure_term_training_v2.csv"));
		CSVReader reader = new CSVReader(new FileReader("/home/sbs0457/git/enhance/Gordon_complexity_term_review/category_mainterm_synonymterm-task-Gordon_complexity.csv"));
		List<String[]> lines = reader.readAll();
		int i=0;
		Set<String> hasSynonym = new HashSet<String>();
		for(String[] line : lines) {
			synonyms.add(new Synonym(String.valueOf(i), line[1], line[0], line[2]));
			hasSynonym.add(line[1]);
		}	
		
		//reader = new CSVReader(new FileReader("/home/sbs0457/workspace2/enhance12/term_resources/category_term-task-non-specific_structure_term_training_v2.csv"));
		reader = new CSVReader(new FileReader("/home/sbs0457/git/enhance/Gordon_complexity_term_review/category_term-task-Gordon_complexity.csv"));
		lines = reader.readAll();
		List<Decision> decisions = new LinkedList<Decision>();
		i=0;
		for(String[] line : lines) {
			decisions.add(new Decision(String.valueOf(i), line[1], line[0], hasSynonym.contains(line[1]), ""));
		}

		Download download = new Download(true, decisions, synonyms);
		
		
		//add syn set of term_category
		HashSet<Term> dsyns = new HashSet<Term>();
		if(download != null) {
			for(Synonym termSyn: download.getSynonyms()){
				//Hong TODO need to add category info to synonym entry in OTOLite
				//if(termSyn.getCategory().compareTo("structure")==0){
				if(termSyn.getCategory().matches("structure|taxon_name|substance")){
					//take care of singular and plural forms
					String syns = ""; 
					String synp = "";
					String terms = "";
					String termp = "";
					if(inflector.isPlural(termSyn.getSynonym().replaceAll("_",  "-"))){
						synp = termSyn.getSynonym().replaceAll("_",  "-");
						syns = inflector.getSingular(synp);					
					}else{
						syns = termSyn.getSynonym().replaceAll("_",  "-");
						synp = inflector.getPlural(syns);
					}

					if(inflector.isPlural(termSyn.getTerm().replaceAll("_",  "-"))){
						termp = termSyn.getTerm().replaceAll("_",  "-");
						terms = inflector.getSingular(termp);					
					}else{
						terms = termSyn.getTerm().replaceAll("_",  "-");
						termp = inflector.getPlural(terms);
					}
					//glossary.addSynonym(syns, termSyn.getCategory(), terms);
					//glossary.addSynonym(synp, termSyn.getCategory(), termp);
					//dsyns.add(new Term(syns, termSyn.getCategory());
					//dsyns.add(new Term(synp, termSyn.getCategory());
					glossary.addSynonym(syns, termSyn.getCategory(), terms);
					glossary.addSynonym(synp,termSyn.getCategory(), termp);
					dsyns.add(new Term(syns, termSyn.getCategory()));
					dsyns.add(new Term(synp, termSyn.getCategory()));
				}else{//forking_1 and forking are syns 5/5/14 hong test, shouldn't _1 have already been removed?
					glossary.addSynonym(termSyn.getSynonym().replaceAll("_",  "-"), termSyn.getCategory(), termSyn.getTerm());
					dsyns.add(new Term(termSyn.getSynonym().replaceAll("_",  "-"), termSyn.getCategory()));
				}					
			}

			//term_category from OTO, excluding dsyns
			for(Decision decision : download.getDecisions()) {
				if(!dsyns.contains(new Term(decision.getTerm().replaceAll("_",  "-"), decision.getCategory())))//calyx_tube => calyx-tube
					glossary.addEntry(decision.getTerm().replaceAll("_",  "-"), decision.getCategory());  
			}
		}
	}


	@Override
	public boolean transform(List<AbstractTransformer> transformers,Element statement, Element biologicalEntity,
			List<Element> context, List<Element> wholeStatements) {
		// TODO Auto-generated method stub
		return true;

		
	}

	@Override
	public void transformAll(Element statement, Element biologicalEntity,
			List<Element> wholeStatement) {
		// TODO Auto-generated method stub
		
	}
	
}
