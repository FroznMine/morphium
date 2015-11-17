package de.caluga.test.mongo.suite;

import de.caluga.morphium.MorphiumSingleton;
import de.caluga.morphium.driver.bson.MorphiumId;
import de.caluga.morphium.query.QueryImpl;
import org.junit.Test;

/**
 * User: Stephan Bösebeck
 * Date: 19.04.13
 * Time: 12:15
 * <p/>
 * TODO: Add documentation here
 */
public class IDConversionTest extends MongoTest {
    @Test
    public void testIdConversion() throws Exception {
        QueryImpl qu = new QueryImpl();
        qu.setMorphium(MorphiumSingleton.get());
        qu.setType(UncachedObject.class);
        qu.setCollectionName("uncached");
        qu.f("_id").eq(new MorphiumId().toString());

        System.out.println(qu.toQueryObject().toString());
        assert (qu.toQueryObject().toString().contains("$oid"));

        qu = new QueryImpl();
        qu.setMorphium(MorphiumSingleton.get());
        qu.setType(UncachedObject.class);
        qu.setCollectionName("uncached");
        qu.f("value").eq(new MorphiumId());
        System.out.println(qu.toQueryObject().toString());
        assert (!qu.toQueryObject().toString().contains("$oid"));
    }
}
