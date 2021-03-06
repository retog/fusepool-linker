/*
 * Created by Sindice LTD http://sindicetech.com
 * Sindice LTD licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sindice.fusepooladapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.sindice.fusepool.testutils.TestTripleCollectionPatents;
import com.sindice.fusepooladapter.storage.OutputStore;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.junit.Assert;

/**
 * Junit tests for manual testing of deduplication.
 * 
 * {@link #loadTestCollectionFull()} does the initial loading of the data later
 * used by {@link #testFull()}.
 * 
 * 
 */
public class LinkerAdapterTest {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * A simple test of deduplication of 3 agents loaded from threeAgents.xml
	 * via {@link TestTripleCollectionPatents}. Two match, one doesn't. Uses the
	 * conf-final.xml Duke configuration specified in conf.properties.
	 */
	@Ignore
	@Test
	public void testSmall() throws IOException {
		LinkerAdapter adapter = new ConfigurableLinkerAdapter("conf.properties");
		TripleCollection resultTriples = adapter
				.interlink(new TestTripleCollectionPatents());
		System.out.println(resultTriples.size());
	}

	/**
	 * A simple test of deduplication of 3 agents loaded from threeAgents.xml
	 * via {@link TestTripleCollectionPatents}. Two match, one doesn't. Uses the
	 * conf-final.xml Duke configuration specified in conf.properties.
	 */
	@Ignore
	@Test
	public void testSmallLinkerAdapter() throws IOException {
		LinkerAdapter adapter = new LinkerAdapter();
		TripleCollection resultTriples = adapter
				.interlink(new TestTripleCollectionPatents());
		System.out.println(resultTriples.size());
	}
	
	//@Ignore
	@Test
	public void testSmallDataFile() throws IOException {
		LinkerAdapter adapter = new LinkerAdapter();
		TripleCollection triples = Parser.getInstance().parse(getClass().getResourceAsStream("patent-data-sample-short.ttl"), SupportedFormat.TURTLE);
		TripleCollection resultTriples = adapter
				.interlink(triples);
		System.out.println("in short sample found: "+resultTriples.size());
		Assert.assertTrue("no interlink found, but urn:x-temp:/id/5caaf04a-30ab-4c6a-9d0e-07ffc3a569d0"
				+ " and urn:x-temp:/id/5efdb576-ebb5-4efd-8b00-d943425eaf59 look the same", 
				resultTriples.size() > 0);
	}
	
	/**
	 * Test that runs the full flow. 
	 * 
	 *  It is assumed that the "fullTestData" is a Jena TDB data folder loaded with the input dataset to deduplicate.
	 *  This store plays the role of the store that in reality backs the real Clerezza TripleCollection. 
	 *  
	 *  Output is stored to the specified outpath - another Jena TDB store.
	 * 
	 */
	@Ignore
	@Test
	public void testFull() throws IOException {
		logger.info("Full test");
		LinkerAdapter adapter = new ConfigurableLinkerAdapter("classpath:patents-csv.xml", 
				"/data/tmp_fusepool/in", // data is loaded from OutputStore (see below, a Clerezza TripleCollection) to this "inpath" 
										 //      so that it can be conveniently queried via SPARQL 
				"out", // results are stored in this "outpath"
				2);    // the number of matching threads used by Duke 
		OutputStore os = new OutputStore("fullTestData"); // contains the dataset to deduplicate. Can be loaded using loadTestCollectionFull()
		os.init();
		logger.info("Store contains " + os.size() + " triples.");
		TripleCollection resultTriples = adapter.interlink(os);
		logger.info("Triples in result: " + resultTriples.size());
		os.destroy();
	}

	/**
	 * A method to verify the number of triples in the "out" store populated by the full test.
	 * 
	 *  Commented code cleans the store.
	 */
	@Ignore
	@Test
	public void testVerify() throws IOException {
		OutputStore os = new OutputStore("out");
		os.init();
		System.out.println(os.size());
//		os.clean();
//		os.destroy();
	}

	/**
	 * Populates a Jena TDB store with the dataset that is to be deduplicated by the testFull() method.
	 *  
	 */
	@Ignore
	@Test
	public void loadTestCollectionFull() throws FileNotFoundException {
		Dataset dataset = TDBFactory.createDataset("fullTestData");
		dataset.begin(ReadWrite.WRITE);
		// Get model inside the transaction
		Model model = dataset.getDefaultModel();
		FileInputStream in = null;
		try {
			in = new FileInputStream("/data/patent-data-sample.nt");
			model.read(in, null, "N-TRIPLE");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println(model.size());
		dataset.commit();
		TDB.sync(dataset);
		dataset.end();
	}
}
