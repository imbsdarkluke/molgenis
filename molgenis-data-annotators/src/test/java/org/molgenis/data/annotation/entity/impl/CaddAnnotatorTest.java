package org.molgenis.data.annotation.entity.impl;

import static org.mockito.Mockito.mock;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.annotation.AnnotationService;
import org.molgenis.data.annotation.RepositoryAnnotator;
import org.molgenis.data.annotation.resources.Resources;
import org.molgenis.data.annotation.resources.impl.ResourcesImpl;
import org.molgenis.data.annotator.websettings.CaddAnnotatorSettings;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.vcf.VcfRepository;
import org.molgenis.util.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration(classes =
{ CaddAnnotatorTest.Config.class, CaddAnnotator.class })
public class CaddAnnotatorTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	RepositoryAnnotator annotator;

	@Autowired
	Resources resourcess;

	public DefaultEntityMetaData metaDataCanAnnotate = new DefaultEntityMetaData("test");
	public DefaultEntityMetaData metaDataCantAnnotate = new DefaultEntityMetaData("test");

	public ArrayList<Entity> input;
	public ArrayList<Entity> input1;
	public ArrayList<Entity> input2;
	public ArrayList<Entity> input3;
	public ArrayList<Entity> input4;
	public ArrayList<Entity> input5;
	public ArrayList<Entity> input6;
	public ArrayList<Entity> input7;
	public static Entity entity;
	public static Entity entity1;
	public static Entity entity2;
	public static Entity entity3;
	public static Entity entity4;
	public static Entity entity5;
	public static Entity entity6;
	public static Entity entity7;
	
	public void setValues()
	{
		AttributeMetaData attributeMetaDataChrom = new DefaultAttributeMetaData(VcfRepository.CHROM,
				MolgenisFieldTypes.FieldTypeEnum.STRING);
		AttributeMetaData attributeMetaDataPos = new DefaultAttributeMetaData(VcfRepository.POS,
				MolgenisFieldTypes.FieldTypeEnum.LONG);
		AttributeMetaData attributeMetaDataRef = new DefaultAttributeMetaData(VcfRepository.REF,
				MolgenisFieldTypes.FieldTypeEnum.TEXT);
		AttributeMetaData attributeMetaDataAlt = new DefaultAttributeMetaData(VcfRepository.ALT,
				MolgenisFieldTypes.FieldTypeEnum.TEXT);
		AttributeMetaData attributeMetaDataCantAnnotateChrom = new DefaultAttributeMetaData(VcfRepository.CHROM,
				MolgenisFieldTypes.FieldTypeEnum.LONG);

		metaDataCanAnnotate.addAttributeMetaData(attributeMetaDataChrom, ROLE_ID);
		metaDataCanAnnotate.addAttributeMetaData(attributeMetaDataPos);
		metaDataCanAnnotate.addAttributeMetaData(attributeMetaDataRef);
		metaDataCanAnnotate.addAttributeMetaData(attributeMetaDataAlt);

		metaDataCantAnnotate.addAttributeMetaData(attributeMetaDataCantAnnotateChrom);
		metaDataCantAnnotate.addAttributeMetaData(attributeMetaDataPos);
		metaDataCantAnnotate.addAttributeMetaData(attributeMetaDataRef);
		metaDataCantAnnotate.addAttributeMetaData(attributeMetaDataAlt);

		entity = new MapEntity(metaDataCanAnnotate);
		entity1 = new MapEntity(metaDataCanAnnotate);
		entity2 = new MapEntity(metaDataCanAnnotate);
		entity3 = new MapEntity(metaDataCanAnnotate);
		entity4 = new MapEntity(metaDataCanAnnotate);
		entity5 = new MapEntity(metaDataCanAnnotate);
		entity6 = new MapEntity(metaDataCanAnnotate);
		entity7 = new MapEntity(metaDataCanAnnotate);
	}

	@BeforeClass
	public void beforeClass() throws IOException
	{
		input = new ArrayList<>();
		input1 = new ArrayList<>();
		input2 = new ArrayList<>();
		input3 = new ArrayList<>();
		input4 = new ArrayList<>();
		input5 = new ArrayList<>();
		input6 = new ArrayList<>();
		input7 = new ArrayList<>();
		
		setValues();

		entity1.set(VcfRepository.CHROM, "1");
		entity1.set(VcfRepository.POS, 100);
		entity1.set(VcfRepository.REF, "C");
		entity1.set(VcfRepository.ALT, "T");

		input1.add(entity1);

		entity2.set(VcfRepository.CHROM, "2");
		entity2.set(VcfRepository.POS, new Long(200));
		entity2.set(VcfRepository.REF, "A");
		entity2.set(VcfRepository.ALT, "C");

		input2.add(entity2);

		entity3.set(VcfRepository.CHROM, "3");
		entity3.set(VcfRepository.POS, new Long(300));
		entity3.set(VcfRepository.REF, "G");
		entity3.set(VcfRepository.ALT, "C");

		input3.add(entity3);
		
		entity4.set(VcfRepository.CHROM, "3");
		entity4.set(VcfRepository.POS, new Long(300));
		entity4.set(VcfRepository.REF, "G");
		entity4.set(VcfRepository.ALT, "T,A,C");

		input4.add(entity4);
		
		entity5.set(VcfRepository.CHROM, "3");
		entity5.set(VcfRepository.POS, new Long(300));
		entity5.set(VcfRepository.REF, "GC");
		entity5.set(VcfRepository.ALT, "T,A");

		input5.add(entity5);
		
		entity6.set(VcfRepository.CHROM, "3");
		entity6.set(VcfRepository.POS, new Long(300));
		entity6.set(VcfRepository.REF, "C");
		entity6.set(VcfRepository.ALT, "GX,GC");

		input6.add(entity6);
		
		entity7.set(VcfRepository.CHROM, "3");
		entity7.set(VcfRepository.POS, new Long(300));
		entity7.set(VcfRepository.REF, "C");
		entity7.set(VcfRepository.ALT, "GC");

		input7.add(entity7);
	}

	@Test
	public void testThreeOccurencesOneMatch()
	{
		List<Entity> expectedList = new ArrayList<Entity>();
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();

		resultMap.put(CaddAnnotator.CADD_ABS, "-0.03");
		resultMap.put(CaddAnnotator.CADD_SCALED, "2.003");

		Entity expectedEntity = new MapEntity(resultMap);
		expectedList.add(expectedEntity);

		Iterator<Entity> results = annotator.annotate(input1);
		Entity resultEntity = results.next();

		assertEquals(resultEntity.get(CaddAnnotator.CADD_ABS), expectedEntity.get(CaddAnnotator.CADD_ABS));
		assertEquals(resultEntity.get(CaddAnnotator.CADD_SCALED), expectedEntity.get(CaddAnnotator.CADD_SCALED));
	}

	@Test
	public void testTwoOccurencesNoMatch()
	{
		List<Entity> expectedList = new ArrayList<Entity>();
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();

		Entity expectedEntity = new MapEntity(resultMap);
		expectedList.add(expectedEntity);

		Iterator<Entity> results = annotator.annotate(input2);
		Entity resultEntity = results.next();

		assertEquals(resultEntity.get(CaddAnnotator.CADD_ABS), expectedEntity.get(CaddAnnotator.CADD_ABS));
		assertEquals(resultEntity.get(CaddAnnotator.CADD_SCALED), expectedEntity.get(CaddAnnotator.CADD_SCALED));
	}

	@Test
	public void testFourOccurences()
	{
		List<Entity> expectedList = new ArrayList<Entity>();
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();

		resultMap.put(CaddAnnotator.CADD_ABS, "0.5");
		resultMap.put(CaddAnnotator.CADD_SCALED, "14.5");

		Entity expectedEntity = new MapEntity(resultMap);
		expectedList.add(expectedEntity);

		Iterator<Entity> results = annotator.annotate(input3);
		Entity resultEntity = results.next();

		assertEquals(resultEntity.get(CaddAnnotator.CADD_ABS), expectedEntity.get(CaddAnnotator.CADD_ABS));
		assertEquals(resultEntity.get(CaddAnnotator.CADD_SCALED), expectedEntity.get(CaddAnnotator.CADD_SCALED));
	}
	
	@Test
	public void testFiveMultiAllelic()
	{
		List<Entity> expectedList = new ArrayList<Entity>();
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();

		resultMap.put(CaddAnnotator.CADD_ABS, "-2.4,0.2,0.5");
		resultMap.put(CaddAnnotator.CADD_SCALED, "0.123,23.1,14.5");

		Entity expectedEntity = new MapEntity(resultMap);
		expectedList.add(expectedEntity);

		Iterator<Entity> results = annotator.annotate(input4);
		Entity resultEntity = results.next();

		assertEquals(resultEntity.get(CaddAnnotator.CADD_ABS), expectedEntity.get(CaddAnnotator.CADD_ABS));
		assertEquals(resultEntity.get(CaddAnnotator.CADD_SCALED), expectedEntity.get(CaddAnnotator.CADD_SCALED));
	}
	
	@Test
	public void testSixMultiAllelicDel()
	{
		List<Entity> expectedList = new ArrayList<Entity>();
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();

		resultMap.put(CaddAnnotator.CADD_ABS, "-3.4,1.2");
		resultMap.put(CaddAnnotator.CADD_SCALED, "1.123,24.1");

		Entity expectedEntity = new MapEntity(resultMap);
		expectedList.add(expectedEntity);

		Iterator<Entity> results = annotator.annotate(input5);
		Entity resultEntity = results.next();

		assertEquals(resultEntity.get(CaddAnnotator.CADD_ABS), expectedEntity.get(CaddAnnotator.CADD_ABS));
		assertEquals(resultEntity.get(CaddAnnotator.CADD_SCALED), expectedEntity.get(CaddAnnotator.CADD_SCALED));
	}
	
	@Test
	public void testSevenMultiAllelicIns()
	{
		List<Entity> expectedList = new ArrayList<Entity>();
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();

		resultMap.put(CaddAnnotator.CADD_ABS, "-1.002,1.5");
		resultMap.put(CaddAnnotator.CADD_SCALED, "3.3,15.5");

		Entity expectedEntity = new MapEntity(resultMap);
		expectedList.add(expectedEntity);

		Iterator<Entity> results = annotator.annotate(input6);
		Entity resultEntity = results.next();

		assertEquals(resultEntity.get(CaddAnnotator.CADD_ABS), expectedEntity.get(CaddAnnotator.CADD_ABS));
		assertEquals(resultEntity.get(CaddAnnotator.CADD_SCALED), expectedEntity.get(CaddAnnotator.CADD_SCALED));
	}
	
	@Test
	public void testEightSingleAllelicIns()
	{
		List<Entity> expectedList = new ArrayList<Entity>();
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();

		resultMap.put(CaddAnnotator.CADD_ABS, "1.5");
		resultMap.put(CaddAnnotator.CADD_SCALED, "15.5");

		Entity expectedEntity = new MapEntity(resultMap);
		expectedList.add(expectedEntity);

		Iterator<Entity> results = annotator.annotate(input7);
		Entity resultEntity = results.next();

		assertEquals(resultEntity.get(CaddAnnotator.CADD_ABS), expectedEntity.get(CaddAnnotator.CADD_ABS));
		assertEquals(resultEntity.get(CaddAnnotator.CADD_SCALED), expectedEntity.get(CaddAnnotator.CADD_SCALED));
	}

	@Test
	public void canAnnotateTrueTest()
	{
		assertEquals(annotator.canAnnotate(metaDataCanAnnotate), "true");
	}

	@Test
	public void canAnnotateFalseTest()
	{
		assertEquals(annotator.canAnnotate(metaDataCantAnnotate), "a required attribute has the wrong datatype");
	}

	public static class Config
	{
		@Autowired
		private DataService dataService;

		@Bean
		public Entity caddAnnotatorSettings()
		{
			Entity settings = new MapEntity();
			settings.set(CaddAnnotatorSettings.Meta.CADD_LOCATION,
					ResourceUtils.getFile(getClass(), "/cadd_test.vcf.gz").getPath());
			return settings;
		}

		@Bean
		public DataService dataService()
		{
			return mock(DataService.class);
		}

		@Bean
		public AnnotationService annotationService()
		{
			return mock(AnnotationService.class);
		}

		@Bean
		public Resources resources()
		{
			return new ResourcesImpl();
		}
	}
}
