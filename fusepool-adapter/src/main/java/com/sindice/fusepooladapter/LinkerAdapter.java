/**
 * Copyright (c) 2014, Sindice Limited. All Rights Reserved.
 *
 * Fusepool-linker this is proprietary software do not use without authorization by Sindice Limited.
 */

package com.sindice.fusepooladapter;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sindice.fusepool.DukeRunner;
import com.sindice.fusepooladapter.storage.InputTripleStore;
import com.sindice.fusepooladapter.storage.JenaInputStoreImpl;
import com.sindice.fusepooladapter.storage.OutputStore;
/**
 * implementation of {@link com.sindice.fusepooladapter.SingleLinker} using Duke
 *
 */
public class LinkerAdapter implements SingleLinker {

  private static final Logger logger = LoggerFactory.getLogger(LinkerAdapter.class);
  private final String inpath;
  private final String outpath;
  private final String dukeConfigFileLocation;
  private final int threadsNo;
  /**
   * constructor using provided parameters
   * 
   * @param dukeConfigFileLocation - Duke XML configuraiton file path
   * @param inpath - data path for input store
   * @param outpath - data path for output store
   * @param threadsNo - number of threads to run Duke
   * @throws IOException
   */
  public LinkerAdapter(String dukeConfigFileLocation, String inpath, String outpath, int threadsNo)
      throws IOException {
    /*
     * Properties properties = new Properties();
     * properties.load(Thread.currentThread().getContextClassLoader()
     * .getResourceAsStream("conf.properties"));
     */
    this.inpath = FileSystems.getDefault().getPath(inpath).toAbsolutePath().toString();;
    this.outpath = FileSystems.getDefault().getPath(outpath).toAbsolutePath().toString();
    this.dukeConfigFileLocation = dukeConfigFileLocation;
    this.threadsNo = threadsNo;

  }
  /**
   * constructor for configuration in a property file on classpath
   * @param confFileName - file name of the property file
   * @throws IOException
   */
  public LinkerAdapter(String confFileName)
      throws IOException {

      Properties properties = new Properties();
      properties.load(Thread.currentThread().getContextClassLoader()
      .getResourceAsStream(confFileName));
      if (properties.getProperty("inpath") == null || properties.getProperty("outpath") == null ||properties.getProperty("dukeconfig") == null ||properties.getProperty("dukethrno") == null){
        throw new IllegalArgumentException("missing mandatory property");
      }
      this.inpath = FileSystems.getDefault().getPath(properties.getProperty("inpath")).toAbsolutePath().toString();;
      this.outpath = FileSystems.getDefault().getPath(properties.getProperty("outpath")).toAbsolutePath().toString();
      this.dukeConfigFileLocation = properties.getProperty("dukeconfig");
      this.threadsNo = Integer.parseInt(properties.getProperty("dukethrno"));

  } 
  
  @Override
  public TripleCollection interlink(TripleCollection dataToInterlink) {
    // populates input store
	logger.info("Populating input store ...");
    InputTripleStore instore = new JenaInputStoreImpl(inpath);
    int inputSize = instore.populate(dataToInterlink);
    logger.info("Input store populated with {} triples", inputSize);
    OutputStore outStore = new OutputStore(outpath);
    outStore.clean();
    
    // starts processing
    DukeRunner runner = null;
    try {
      runner = new DukeRunner(dukeConfigFileLocation,"jdbc:jena:tdb:location=" + inpath,  outpath, threadsNo) {
      //  runner = new DukeRunner(dukeConfigFileLocation,"jdbc:jena:tdb:location=" + inpath,  "jdbc:jena:tdb:location=" + outpath, threadsNo) {
      };
    } catch (SQLException e) {
      logger.error("error during initialization of the Duke");
    }
    logger.debug("going to start the Duke");
    runner.run();
    logger.info("Duke finished");
    
    // after Duke finish, exposes out store
    outStore.init();
    logger.info("Output store contains {} triples", outStore.size());
    return outStore;
  }

}
