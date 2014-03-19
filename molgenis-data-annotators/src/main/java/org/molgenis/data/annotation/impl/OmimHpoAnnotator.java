package org.molgenis.data.annotation.impl;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.*;
import org.molgenis.data.annotation.AnnotationService;
import org.molgenis.data.annotation.LocusAnnotator;
import org.molgenis.data.annotation.impl.datastructures.*;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Created by jvelde on 2/12/14.
 * 
 * uses:
 * <p>
 * - http://compbio.charite.de/hudson/job/hpo.annotations.monthly/lastStableBuild/artifact/annotation/
 * ALL_SOURCES_ALL_FREQUENCIES_diseases_to_genes_to_phenotypes.txt -
 * https://molgenis26.target.rug.nl/downloads/5gpm/GRCh37p13_HGNC_GeneLocations_noPatches.tsv -
 * ftp://ftp.omim.org/omim/morbidmap
 * </p>
 * 
 */
@Component("omimHpoService")
public class OmimHpoAnnotator extends LocusAnnotator
{
	private static final String GENE_LOCATIONS_URL = "https://molgenis26.target.rug.nl/downloads/5gpm/GRCh37p13_HGNC_GeneLocations_noPatches.tsv";
	private static final String OMIM_MORBIDMAP_URL = "ftp://ftp.omim.org/omim/morbidmap";
	private static final String DISEASES_TO_GENES_TO_PHENOTYPES_URL = "http://compbio.charite.de/hudson/job/hpo.annotations.monthly/lastStableBuild/artifact/annotation/ALL_SOURCES_ALL_FREQUENCIES_diseases_to_genes_to_phenotypes.txt";
	private AnnotationService annotatorService;
	private List<HPOTerm> HPO_TERMS;
	private List<OMIMTerm> OMIM_TERMS;
	private Map<String, List<HPOTerm>> GENE_TO_HPO;
	private Map<String, List<OMIMTerm>> GENE_TO_OMIM;

	// TODO: more fancy symptom information by using
	// http://compbio.charite.de/hudson/job/hpo.annotations/lastStableBuild/artifact/misc/phenotype_annotation.tab

	private static final String NAME = "OmimHpo";

	public static final String OMIM_CAUSAL_IDENTIFIER = "OMIM_Causal_ID";
	public static final String OMIM_DISORDERS = "OMIM_Disorders";
	public static final String OMIM_IDENTIFIERS = "OMIM_IDs";
	public static final String OMIM_TYPE = "OMIM_Type";
	public static final String OMIM_HGNC_IDENTIFIERS = "OMIM_HGNC_IDs";
	public static final String OMIM_CYTOGENIC_LOCATION = "OMIM_Cytogenic_Location";
	public static final String OMIM_ENTRY = "OMIM_Entry";

	public static final String HPO_IDENTIFIERS = "HPO_IDs";
	public static final String HPO_GENE_NAME = "HPO_Gene_Name";
	public static final String HPO_DESCRIPTIONS = "HPO_Descriptions";
	public static final String HPO_DISEASE_DATABASE = "HPO_Disease_Database";
	public static final String HPO_DISEASE_DATABASE_ENTRY = "HPO_Disease_Database_Entry";
	public static final String HPO_ENTREZ_ID = "HPO_Entrez_ID";

	@Autowired
	public OmimHpoAnnotator(AnnotationService annotatorService) throws IOException
	{
		this.annotatorService = annotatorService;
		this.HPO_TERMS = getHpoTerms();
		this.OMIM_TERMS = getOmimTerms();
		this.GENE_TO_HPO = getGeneToHpoTerms();
		this.GENE_TO_OMIM = getGeneToOmimTerms();
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event)
	{
		annotatorService.addAnnotator(this);
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public boolean annotationDataExists()
	{
		boolean dataExists = true;

		// TODO Check if online resources are available

		return dataExists;
	}

	@Override
	public List<Entity> annotateEntity(Entity entity) throws IOException
	{
		List<Entity> results = new ArrayList<Entity>();

		String chromosome = entity.getString(CHROMOSOME);
		Long position = entity.getLong(POSITION);

		Locus locus = new Locus(chromosome, position);

		List<String> geneSymbols = locationToHGNC(locus);

		try
		{
			for (String geneSymbol : geneSymbols)
			{
				if (geneSymbol != null && GENE_TO_OMIM.containsKey(geneSymbol) && GENE_TO_HPO.containsKey(geneSymbol))
				{
					HashMap<String, Object> resultMap = new HashMap<String, Object>();

					Set<String> OMIMDisorders = new HashSet<String>();
					Set<String> OMIMCytoLocations = new HashSet<String>();
					Set<String> OMIMHgncIdentifiers = new HashSet<String>();
					Set<Integer> OMIMEntries = new HashSet<Integer>();
					Set<Integer> OMIMTypes = new HashSet<Integer>();
					Set<Integer> OMIMCausedBy = new HashSet<Integer>();

					Set<String> HPOPDescriptions = new HashSet<String>();
					Set<String> HPOIdentifiers = new HashSet<String>();
					Set<Integer> HPODiseaseDatabaseEntries = new HashSet<Integer>();
					Set<String> HPODiseaseDatabases = new HashSet<String>();
					Set<String> HPOGeneNames = new HashSet<String>();
					Set<Integer> HPOEntrezIdentifiers = new HashSet<Integer>();

					for (OMIMTerm omimTerm : GENE_TO_OMIM.get(geneSymbol))
					{
						OMIMDisorders.add(omimTerm.getName());
						OMIMEntries.add(omimTerm.getEntry());
						OMIMTypes.add(omimTerm.getType());
						OMIMCausedBy.add(omimTerm.getCausedBy());
						OMIMCytoLocations.add(omimTerm.getCytoLoc());

						for (String hgncSymbol : omimTerm.getHgncIds())
						{
							OMIMHgncIdentifiers.add(hgncSymbol);
						}

					}

					for (HPOTerm hpoTerm : GENE_TO_HPO.get(geneSymbol))
					{
						HPOPDescriptions.add(hpoTerm.getDescription());
						HPOIdentifiers.add(hpoTerm.getId());
						HPOGeneNames.add(hpoTerm.getGeneName());
						HPOEntrezIdentifiers.add(hpoTerm.getGeneEntrezID());
						HPODiseaseDatabaseEntries.add(hpoTerm.getDiseaseDbEntry());
						HPODiseaseDatabases.add(hpoTerm.getDiseaseDb());
					}

					resultMap.put(CHROMOSOME, locus.getChrom());
					resultMap.put(POSITION, locus.getPos());
					resultMap.put(OMIM_DISORDERS, OMIMDisorders);
					resultMap.put(HPO_DESCRIPTIONS, HPOPDescriptions);
					resultMap.put(OMIM_CAUSAL_IDENTIFIER, OMIMCausedBy);
					resultMap.put(OMIM_TYPE, OMIMTypes);
					resultMap.put(OMIM_HGNC_IDENTIFIERS, OMIMHgncIdentifiers);
					resultMap.put(OMIM_CYTOGENIC_LOCATION, OMIMCytoLocations);
					resultMap.put(OMIM_ENTRY, OMIMEntries);
					resultMap.put(HPO_IDENTIFIERS, HPOIdentifiers);
					resultMap.put(HPO_GENE_NAME, HPOGeneNames);
					resultMap.put(HPO_DISEASE_DATABASE, HPODiseaseDatabases);
					resultMap.put(HPO_DISEASE_DATABASE_ENTRY, HPODiseaseDatabaseEntries);
					resultMap.put(HPO_ENTREZ_ID, HPOEntrezIdentifiers);

					results.add(new MapEntity(resultMap));
				}
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		return results;
	}

	/**
	 * e.g. OMIM:614887 PEX14 5195 HP:0002240 Hepatomegaly
	 * 
	 * becomes: HPOTerm{id='HP:0002240', description='Hepatomegaly', diseaseDb='OMIM', diseaseDbEntry=614887,
	 * geneName='PEX14', geneEntrezID=5195}
	 * 
	 * @return
	 * @throws IOException
	 */
	private List<HPOTerm> getHpoTerms() throws IOException
	{
		List<HPOTerm> hpoTermsList = new ArrayList<HPOTerm>();

		String cacheName = "diseases_to_genes_to_phenotypes.txt";
		ArrayList<String> hpoLines = readLinesFromURL(DISEASES_TO_GENES_TO_PHENOTYPES_URL, cacheName);

		for (String line : hpoLines)
		{
			if (!line.startsWith("#"))
			{
				String[] split = line.split("\t");
				String diseaseDb = split[0].split(":")[0];
				String geneSymbol = split[1];
				String hpoId = split[3];
				String description = split[4];

				Integer diseaseDbID = Integer.parseInt(split[0].split(":")[1]);
				Integer geneEntrezId = Integer.parseInt(split[2]);

				HPOTerm hpoTerm = new HPOTerm(hpoId, description, diseaseDb, diseaseDbID, geneSymbol, geneEntrezId);
				hpoTermsList.add(hpoTerm);
			}
		}

		return hpoTermsList;
	}

	/**
	 * Get and parse OMIM entries.
	 * 
	 * Do not store entries without OMIM identifier... e.g. this one: Leukemia, acute myelogenous (3)|KRAS, KRAS2,
	 * RASK2, NS, CFC2|190070|12p12.1
	 * 
	 * But do store this one: Leukemia, acute myelogenous, 601626 (3)|GMPS|600358|3q25.31
	 * 
	 * becomes: OMIMTerm{id=601626, name='Leukemia, acute myelogenous', type=3, causedBy=600358, cytoLoc='3q25.31',
	 * hgncIds=[GMPS]}
	 * 
	 * @return
	 * @throws IOException
	 */
	private List<OMIMTerm> getOmimTerms() throws IOException
	{
		List<OMIMTerm> omimTermList = new ArrayList<OMIMTerm>();

		String cacheName = "morbid_map";
		ArrayList<String> omimLines = readLinesFromURL(OMIM_MORBIDMAP_URL, cacheName);

		try
		{
			for (String line : omimLines)
			{
				String[] split = line.split("\\|");

				String entry = split[0].substring(split[0].length() - 10, split[0].length() - 4);
				if (entry.matches("[0-9]+"))
				{
					List<String> genes = Arrays.asList(split[1].split(", "));

					String name = split[0].substring(0, split[0].length() - 12);
					String cytoLoc = split[3];

					Integer mutationId = Integer.parseInt(split[2]);
					Integer type = Integer.parseInt(split[0].substring(split[0].length() - 2, split[0].length() - 1));

					Integer omimEntry = Integer.parseInt(entry);

					OMIMTerm omimTerm = new OMIMTerm(omimEntry, name, type, mutationId, cytoLoc, genes);

					omimTermList.add(omimTerm);
				}
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		return omimTermList;
	}

	/**
	 * 
	 * @return map of GeneSymbol to HGNCLoc(GeneSymbol, Start, End, Chrom)
	 * @throws IOException
	 */
	public HashMap<String, HGNCLocations> getHgncLocations() throws IOException
	{
		String cacheName = "HGNC_gene_locations_GRCH37.tsv";
		ArrayList<String> geneLocations = this.readLinesFromURL(GENE_LOCATIONS_URL, cacheName);

		HashMap<String, HGNCLocations> res = new HashMap<String, HGNCLocations>();
		for (String line : geneLocations)
		{
			String[] split = line.split("\t");
			HGNCLocations hgncLoc = new HGNCLocations(split[0], Long.parseLong(split[1]), Long.parseLong(split[2]),
					split[3]);
			if (hgncLoc.getChrom().matches("[0-9]+|X"))
			{
				res.put(hgncLoc.getHgnc(), hgncLoc);
			}
		}

		return res;
	}

	private Map<String, List<OMIMTerm>> getGeneToOmimTerms() throws IOException
	{
		Map<String, List<OMIMTerm>> omimTermListMap = new HashMap<String, List<OMIMTerm>>();

		for (OMIMTerm omimTerm : OMIM_TERMS)
		{
			for (String geneSymbol : omimTerm.getHgncIds())
			{
				if (omimTermListMap.containsKey(geneSymbol))
				{
					omimTermListMap.get(geneSymbol).add(omimTerm);
				}
				else
				{
					ArrayList<OMIMTerm> omimTermList = new ArrayList<OMIMTerm>();
					omimTermList.add(omimTerm);
					omimTermListMap.put(geneSymbol, omimTermList);
				}
			}
		}

		return omimTermListMap;
	}

	private Map<String, List<HPOTerm>> getGeneToHpoTerms() throws IOException
	{
		Map<String, List<HPOTerm>> hpoTermListMap = new HashMap<String, List<HPOTerm>>();
		for (HPOTerm hpoTerm : HPO_TERMS)
		{
			if (hpoTermListMap.containsKey(hpoTerm.getGeneName()))
			{
				hpoTermListMap.get(hpoTerm.getGeneName()).add(hpoTerm);
			}
			else
			{
				ArrayList<HPOTerm> hpoTermList = new ArrayList<HPOTerm>();
				hpoTermList.add(hpoTerm);
				hpoTermListMap.put(hpoTerm.getGeneName(), hpoTermList);
			}
		}

		return hpoTermListMap;
	}

	public List<String> locationToHGNC(Locus locus) throws IOException
	{
		HashMap<String, HGNCLocations> hgncLocations = this.getHgncLocations();
		List<String> hgncSymbols = new ArrayList<String>();

		boolean variantMapped = false;
		for (HGNCLocations hgncLocation : hgncLocations.values())
		{
			if (hgncLocation.getChrom().equals(locus.getChrom()) && locus.getPos() >= (hgncLocation.getStart() - 5)
					&& locus.getPos() <= (hgncLocation.getEnd() + 5))
			{
				hgncSymbols.add(hgncLocation.getHgnc());
				variantMapped = true;
				break;
			}
		}

		if (!variantMapped)
		{
			hgncSymbols.add(null);
		}

		return hgncSymbols;
	}

	public ArrayList<String> readLinesFromURL(String webLocation, String cacheName) throws IOException
	{
		ArrayList<String> outputLines = new ArrayList<String>();
		File cacheLocation = new File(System.getProperty("java.io.tmpdir"), cacheName);

		if (cacheLocation.exists())
		{
			FileReader fileReader = new FileReader(cacheLocation);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while (bufferedReader.ready())
			{
				outputLines.add(bufferedReader.readLine());
			}

			bufferedReader.close();
		}
		else
		{
			URL url = new URL(webLocation);

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
			FileWriter writer = new FileWriter(cacheLocation);

			while (bufferedReader.ready())
			{
				outputLines.add(bufferedReader.readLine());
				writer.write(bufferedReader.readLine() + "\n");
			}

			bufferedReader.close();
			writer.close();
		}

		return outputLines;
	}

	@Override
	public EntityMetaData getOutputMetaData()
	{
		DefaultEntityMetaData metadata = new DefaultEntityMetaData(this.getClass().getName());
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(OMIM_CAUSAL_IDENTIFIER,
				MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(OMIM_DISORDERS,
				MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(OMIM_IDENTIFIERS,
				MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(OMIM_TYPE, MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(OMIM_HGNC_IDENTIFIERS,
				MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(OMIM_CYTOGENIC_LOCATION,
				MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(OMIM_ENTRY, MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(HPO_IDENTIFIERS,
				MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(HPO_GENE_NAME, MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(HPO_DESCRIPTIONS,
				MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(HPO_DISEASE_DATABASE,
				MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(HPO_DISEASE_DATABASE_ENTRY,
				MolgenisFieldTypes.FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(HPO_ENTREZ_ID, MolgenisFieldTypes.FieldTypeEnum.TEXT));
		return metadata;
	}
}