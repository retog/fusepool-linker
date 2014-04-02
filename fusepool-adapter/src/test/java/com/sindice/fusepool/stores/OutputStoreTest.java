/**
 * Copyright (c) 2014, Sindice Limited. All Rights Reserved.
 *
 * Fusepool-linker this is proprietary software do not use without authorization by Sindice Limited.
 */

package com.sindice.fusepool.stores;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.sindice.fusepooladapter.storage.OutputStore;

public class OutputStoreTest {

  private static final String PAKOS = "pakos";
  private static final String HTTP_TT_COM_P2 = "http://tt.com/p2";
  private static final String HTTP_TT_COM_S2 = "http://tt.com/s2";
  private static final String PAKO = "pako";
  private static final String HTTP_TT_COM_P = "http://tt.com/p";
  private static final String HTTP_TT_COM_S0 = "http://tt.com/s0";
  
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void test() throws IOException {

    Triple testTriple = new TripleImpl(new UriRef(HTTP_TT_COM_S0),
        new UriRef(HTTP_TT_COM_P), new PlainLiteralImpl(PAKO));
    Triple testTriple1 = new TripleImpl(new UriRef(HTTP_TT_COM_S2), new UriRef(
        HTTP_TT_COM_P2), new PlainLiteralImpl(PAKOS));
    Set<Triple> expectedSet = new HashSet<Triple>();
    expectedSet.add(testTriple1);
    expectedSet.add(testTriple);
    OutputStore store = null;
    try {
      String tmpDataPath = folder.newFolder("data").getAbsolutePath();
      store = new OutputStore(tmpDataPath);
      store.clean();
      Dataset dataset = TDBFactory.createDataset(tmpDataPath);
      dataset.begin(ReadWrite.WRITE);
      // Get model inside the transaction
      Model model = dataset.getDefaultModel();
      model.add(model.createResource(HTTP_TT_COM_S0), model.createProperty(HTTP_TT_COM_P),
          PAKO);
      model.add(model.createResource(HTTP_TT_COM_S2), model.createProperty(HTTP_TT_COM_P2),
          PAKOS);
      dataset.commit();
      TDB.sync(dataset);
      dataset.end();

      store.init();
      assertEquals(2, store.size());
      Iterator<Triple> iter = store.iterator();
      Set<Triple> resultSet = new HashSet<Triple>();
      resultSet.add(iter.next());
      resultSet.add(iter.next());
      assertEquals(expectedSet, resultSet);
    } finally {
      if (store != null) {
         store.destroy();
      }
    }
  }

}
