package org.emonocot.job.scratchpads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.emonocot.job.scratchpads.model.EoLTaxonItem;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.xstream.AnnotationXStreamMarshaller;
import org.springframework.oxm.xstream.XStreamMarshaller;

import com.thoughtworks.xstream.io.xml.StaxDriver;


public class StaxEventItemReaderTest {
	EoLTransferSchemaQNameMapFactory eolTransferSchemaQNameMapFactory = new EoLTransferSchemaQNameMapFactory();
	private Unmarshaller unmarshaller;
	private String filename = "/org/emonocot/job/scratchpads/test.xml";
	
	@Test
	public void testParseDocument() throws Exception {
		StaxDriver streamDriver = new StaxDriver(eolTransferSchemaQNameMapFactory.createInstance());
		unmarshaller =  new AnnotationXStreamMarshaller();
		((AnnotationXStreamMarshaller)unmarshaller).setAnnotatedClass(EoLTaxonItem.class);
		((XStreamMarshaller)unmarshaller).setStreamDriver(streamDriver);
		StaxEventItemReader itemReader = new StaxEventItemReader();
		itemReader.setFragmentRootElementName("taxon");
		itemReader.setUnmarshaller(unmarshaller);
		itemReader.setResource(new ClassPathResource(filename));
		
		itemReader.afterPropertiesSet();
		itemReader.open(new ExecutionContext());
		EoLTaxonItem taxon = (EoLTaxonItem)itemReader.read();
		int counter = 0;
		while(taxon != null) {
			counter++;
			taxon = (EoLTaxonItem)itemReader.read();
		}
		
		assertNull("taxon variable should now be null",taxon);
		assertEquals("counter should equal three",counter,2);
	}

}