package de.caluga.test.mongo.suite;

import de.caluga.morphium.*;
import de.caluga.morphium.annotations.Entity;
import de.caluga.morphium.annotations.Id;
import de.caluga.morphium.annotations.Reference;
import de.caluga.morphium.driver.bson.MorphiumId;
import de.caluga.morphium.query.Query;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Stephan Bösebeck
 * Date: 29.05.12
 * Time: 00:02
 * <p/>
 */
@SuppressWarnings("AssertWithSideEffects")
public class LazyLoadingTest extends MongoTest {

    private boolean wouldDeref = false;
    private boolean didDeref = false;

    @Test
    public void deRefTest() throws Exception {
        MorphiumSingleton.get().clearCollection(LazyLoadingObject.class);
        LazyLoadingObject lz = new LazyLoadingObject();
        UncachedObject o = new UncachedObject();
        o.setCounter(15);
        o.setValue("A uncached value");
        MorphiumSingleton.get().store(o);

        CachedObject co = new CachedObject();
        co.setCounter(22);
        co.setValue("A cached Value");
        MorphiumSingleton.get().store(co);

        waitForWrites();

        lz.setName("Lazy");
        lz.setLazyCached(co);
        lz.setLazyUncached(o);
        MorphiumSingleton.get().store(lz);

        waitForWrites();
        Query<LazyLoadingObject> q = MorphiumSingleton.get().createQueryFor(LazyLoadingObject.class);
        q = q.f("name").eq("Lazy");
        LazyLoadingObject lzRead = q.get();
        Object id = MorphiumSingleton.get().getId(lzRead);
        assert (id != null);
        assert (lzRead.getLazyUncached().getCounter() == 15);
        assert (lzRead.getLazyUncached().getValue().equals("A uncached value"));
        co = lzRead.getLazyCached();
        Thread.sleep(1000);
        id = MorphiumSingleton.get().getId(co);
        assert (co.getCounter() == 22);
        assert (id != null);

    }

    @Test
    public void lazyLoadingTest() throws Exception {
        Query<LazyLoadingObject> q = MorphiumSingleton.get().createQueryFor(LazyLoadingObject.class);
        //clean
        MorphiumSingleton.get().delete(q);


        LazyLoadingObject lz = new LazyLoadingObject();
        UncachedObject o = new UncachedObject();
        o.setCounter(15);
        o.setValue("A uncached value");
        MorphiumSingleton.get().store(o);

        CachedObject co = new CachedObject();
        co.setCounter(22);
        co.setValue("A cached Value");
        MorphiumSingleton.get().store(co);

        waitForWrites();

        lz.setName("Lazy");
        lz.setLazyCached(co);
        lz.setLazyUncached(o);

        List<UncachedObject> lst = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UncachedObject uc = new UncachedObject();
            uc.setValue("Part of list");
            uc.setCounter(i * 5 + 7);
            lst.add(uc);
        }
        lz.setLazyLst(lst);

        MorphiumSingleton.get().store(lz);

        waitForWrites();

        //Test for lazy loading


        q = q.f("name").eq("Lazy");
        LazyLoadingObject lzRead = q.get();

        assert (lzRead != null) : "Not found????";
        log.info("LZRead: " + lzRead.getClass().getName());
        assert (!lzRead.getClass().getName().contains("$EnhancerByCGLIB$")) : "Lazy loader in Root-Object?";
        Double rd = MorphiumSingleton.get().getStatistics().get(StatisticKeys.READS.name());
        if (rd == null) rd = 0.0;
        //Field f=MorphiumSingleton.get().getConfig().getMapper().getField(LazyLoadingObject.class,"lazy_uncached");

        int cnt = lzRead.getLazyUncached().getCounter();
        log.info("uncached: " + lzRead.getLazyUncached().getClass().getName());
        assert (lzRead.getLazyUncached().getClass().getName().contains("$EnhancerByCGLIB$")) : "Not lazy loader?";

        assert (cnt == o.getCounter()) : "Counter not equal";
        double rd2 = MorphiumSingleton.get().getStatistics().get(StatisticKeys.READS.name());
        assert (rd2 > rd) : "No read?";

        rd = MorphiumSingleton.get().getStatistics().get(StatisticKeys.READS.name());
        double crd = MorphiumSingleton.get().getStatistics().get(StatisticKeys.CACHE_ENTRIES.name());
        cnt = lzRead.getLazyCached().getCounter();
        assert (cnt == co.getCounter()) : "Counter (cached) not equal";
        rd2 = MorphiumSingleton.get().getStatistics().get(StatisticKeys.READS.name());
        assert (MorphiumSingleton.get().getStatistics().get(StatisticKeys.CACHE_ENTRIES.name()) > crd) : "Not cached?";
        assert (rd2 > rd) : "No read?";
        log.info("Cache Entries:" + MorphiumSingleton.get().getStatistics().get(StatisticKeys.CACHE_ENTRIES.name()));

        assert (lzRead.getLazyLst().size() == lz.getLazyLst().size()) : "List sizes differ?!?!";
        for (UncachedObject uc : lzRead.getLazyLst()) {
            assert (uc.getClass().getName().contains("$EnhancerByCGLIB$")) : "Lazy list not lazy?";

        }


    }


    @Test
    public void lazyLoadingPerformanceTest() throws Exception {
        Query<LazyLoadingObject> q = MorphiumSingleton.get().createQueryFor(LazyLoadingObject.class);
        //clean
        MorphiumSingleton.get().delete(q);

        log.info("Creating lots of lazyobjects");
        int numberOfObjects = 20;
        for (int i = 0; i < numberOfObjects; i++) {
            LazyLoadingObject lz = new LazyLoadingObject();
            UncachedObject o = new UncachedObject();
            o.setCounter(i * 2 + 50);
            o.setValue("A uncached value " + i);
            MorphiumSingleton.get().store(o);

            CachedObject co = new CachedObject();
            co.setCounter(i + numberOfObjects);
            co.setValue("A cached Value " + i);
            MorphiumSingleton.get().store(co);

            waitForWrites();

            lz.setName("Lazy " + i);
            lz.setLazyCached(co);
            lz.setLazyUncached(o);
            log.info("Storing...");
            MorphiumSingleton.get().store(lz);
            log.info("Stored object " + i + "/" + 20);

        }
        waitForWrites();
        log.info("done - now creating not lazy referenced objects");
        for (int i = 0; i < numberOfObjects; i++) {
            ComplexObject co = new ComplexObject();
            co.setEinText("Txt " + i);
            UncachedObject o = new UncachedObject();
            o.setCounter(i * 2 + 50);
            o.setValue("A uncached value " + i);
            MorphiumSingleton.get().store(o);
            co.setRef(o);

            CachedObject cmo = new CachedObject();
            cmo.setCounter(i + numberOfObjects);
            cmo.setValue("A cached Value " + i);
            MorphiumSingleton.get().store(co);

            waitForWrites();

            co.setcRef(cmo);

            MorphiumSingleton.get().store(co);
        }
        log.info("done");

        log.info("Reading in the not-lazy objects");
        long start = System.currentTimeMillis();
        MorphiumSingleton.get().readAll(ComplexObject.class);
        long dur = System.currentTimeMillis() - start;
        log.info("Reading all took: " + dur + "ms ");

        log.info("now reading in the lazy objects");
        start = System.currentTimeMillis();
        List<LazyLoadingObject> lzlst = MorphiumSingleton.get().readAll(LazyLoadingObject.class);
        dur = System.currentTimeMillis() - start;
        log.info("Reading all lazy took: " + dur + "ms (" + lzlst.size() + " objects)");

        log.info("Reading them single...");
        start = System.currentTimeMillis();

        for (int i = 0; i < numberOfObjects; i++) {
            Query<ComplexObject> coq = MorphiumSingleton.get().createQueryFor(ComplexObject.class);
            coq = coq.f("einText").eq("Txt " + i);
            coq.get(); //should only be one!!!
        }
        dur = System.currentTimeMillis() - start;
        log.info("Reading single un-lazy took " + dur + " ms");

        log.info("Reading lazy objects single...");
        start = System.currentTimeMillis();
        //Store them to prefent finalizer() to be called causing the lazy loading to take place
        List<LazyLoadingObject> storage = new ArrayList<>();
        for (int i = 0; i < numberOfObjects; i++) {
            Query<LazyLoadingObject> coq = MorphiumSingleton.get().createQueryFor(LazyLoadingObject.class);
            coq = coq.f("name").eq("Lazy " + i);
//            storage.add(coq.get()); //should only be one!!!
            coq.get();
        }
        dur = System.currentTimeMillis() - start;
        log.info("Reading single lazy took " + dur + " ms");
    }


    @Test
    public void deReferenceListenerTest() {
        Query<LazyLoadingObject> q = MorphiumSingleton.get().createQueryFor(LazyLoadingObject.class);
        //clean
        MorphiumSingleton.get().delete(q);

        log.info("Creating...");
        LazyLoadingObject lz = new LazyLoadingObject();
        UncachedObject o = new UncachedObject();
        o.setCounter(11);
        o.setValue("A uncached value");
        MorphiumSingleton.get().store(o);

        CachedObject co = new CachedObject();
        co.setCounter(112);
        co.setValue("A cached Value");
        MorphiumSingleton.get().store(co);

        waitForWrites();

        lz.setName("Lazy");
        lz.setLazyCached(co);
        lz.setLazyUncached(o);
        log.info("Storing...");

        List<UncachedObject> lst = new ArrayList<>();
        lst.add(o);
        lz.setLazyLst(lst);
        MorphiumSingleton.get().store(lz);
        log.info("Stored object");


        DereferencingListener<Object, LazyLoadingObject, MorphiumId> refListener = new DereferencingListener<Object, LazyLoadingObject, MorphiumId>() {
            @Override
            public void wouldDereference(LazyLoadingObject entiyIncludingReference, String fieldInEntity, MorphiumId id, Class<Object> typeReferenced, boolean lazy) throws MorphiumAccessVetoException {
                wouldDeref = true;
                assert (lazy);
                assert (!typeReferenced.equals(Object.class));
                assert (entiyIncludingReference.getName().equals("Lazy"));
            }

            @Override
            public Object didDereference(LazyLoadingObject entitiyIncludingReference, String fieldInEntity, Object referencedObject, boolean lazy) {
                didDeref = true;
                assert (lazy);
                assert (referencedObject != null);
                return referencedObject;
            }
        };
        MorphiumSingleton.get().addDereferencingListener(refListener);
        lz = q.get();
        lz.getLazyCached().getCounter();
        assert (wouldDeref);
        assert (didDeref);


        wouldDeref = false;
        didDeref = false;
        lz.getLazyUncached().getCounter();

        assert (wouldDeref);
        assert (didDeref);

        wouldDeref = false;
        didDeref = false;

        lz.getLazyLst().get(0).getCounter();
        assert (wouldDeref);
        assert (didDeref);
        MorphiumSingleton.get().removeDerrferencingListener(refListener);


    }


    @Entity
    public static class SimpleEntity {

        protected int value;
        @Id
        MorphiumId id;
        @Reference
        SimpleEntity ref;
        @Reference(lazyLoading = true)
        SimpleEntity lazyRef;

        public SimpleEntity(int value) {
            this.value = value;
        }

        public SimpleEntity() {
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public SimpleEntity getLazyRef() {
            return lazyRef;
        }

        public void setLazyRef(SimpleEntity lazyRef) {
            this.lazyRef = lazyRef;
        }

        public SimpleEntity getRef() {
            return ref;
        }

        public void setRef(SimpleEntity ref) {
            this.ref = ref;
        }

        public MorphiumId getId() {
            return id;
        }

        public void setId(MorphiumId id) {
            this.id = id;
        }
    }


    @Test
    public void testLazyRef() {
        Morphium m = MorphiumSingleton.get();
        m.clearCollection(SimpleEntity.class);

        SimpleEntity s1 = new SimpleEntity(1);
        SimpleEntity s2 = new SimpleEntity(2);
        SimpleEntity s3 = new SimpleEntity(3);

        m.store(s1);
        m.store(s3);


        s2.ref = s1;
        s2.lazyRef = s3;
        m.store(s2);

        SimpleEntity s1Fetched = m.createQueryFor(SimpleEntity.class).f("value").eq(1).get();
        assert (s1Fetched.value == 1);
        SimpleEntity s2Fetched = m.createQueryFor(SimpleEntity.class).f("value").eq(2).get();
        assert (s2Fetched.value == 2);
        SimpleEntity s3Fetched = m.createQueryFor(SimpleEntity.class).f("value").eq(3).get();
        assert (s3Fetched.value == 3);
        assert (s2Fetched.getRef().getValue() == 1);
        System.out.println(s2Fetched.lazyRef.value);
        assert (s2Fetched.getLazyRef().getValue() == 3);

    }

}
