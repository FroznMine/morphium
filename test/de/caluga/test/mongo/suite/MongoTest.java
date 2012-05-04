package de.caluga.test.mongo.suite;

import de.caluga.morphium.MongoDbMode;
import de.caluga.morphium.Morphium;
import de.caluga.morphium.MorphiumConfig;
import de.caluga.morphium.MorphiumSingleton;
import de.caluga.morphium.secure.DefaultSecurityManager;
import org.apache.log4j.Logger;


/**
 * User: Stpehan Bösebeck
 * Date: 26.02.12
 * Time: 16:17
 * <p/>
 */
public class MongoTest {
    protected Logger log;

    public MongoTest() {
        log = Logger.getLogger(getClass().getName());
    }

    @org.junit.Before
    public void setUp() throws Exception {

        try {
            log.info("Preparing collections...");
            MorphiumSingleton.get().clearCollection(UncachedObject.class);
            log.info("Uncached object prepared!");
            MorphiumSingleton.get().clearCollection(CachedObject.class);
            MorphiumSingleton.get().clearCollection(ComplexObject.class);
            MorphiumSingleton.get().clearCollection(EnumTest.class);

            MorphiumSingleton.get().ensureIndex(UncachedObject.class,"counter","value");
            MorphiumSingleton.get().ensureIndex(CachedObject.class,"counter","value");

            log.info("Preparation finished");
        } catch (Exception e) {
            log.fatal("Error during preparation!");
            e.printStackTrace();
        }

    }

    @org.junit.After
    public void tearDown() throws Exception {
        log.info("Cleaning up...");
        MorphiumSingleton.get().clearCollection(UncachedObject.class);
        MorphiumSingleton.get().clearCollection(CachedObject.class);
        log.info("done...");
    }


    @org.junit.BeforeClass
    public static void setUpClass() throws Exception {
        if (!MorphiumSingleton.isConfigured()) {
            MorphiumConfig cfg = new MorphiumConfig("calugaTestDb", MongoDbMode.SINGLE, 5, 50000, 5000, new DefaultSecurityManager(), "log4j-test.xml");
            cfg.addAddress("localhost", 27017);
            cfg.setWriteCacheTimeout(100);
            MorphiumSingleton.setConfig(cfg);
            MorphiumSingleton.get();
        }
    }

    @org.junit.AfterClass
    public static void tearDownClass() throws Exception {
        System.out.println("NOT Shutting down - might be reused!");
//        MorphiumSingleton.get().close();
    }
}
