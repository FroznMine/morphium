package de.caluga.morphium.driver.mongodb;/**
 * Created by stephan on 05.11.15.
 */

import com.mongodb.*;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import de.caluga.morphium.Logger;
import de.caluga.morphium.driver.MorphiumDriver;
import de.caluga.morphium.driver.MorphiumDriverException;
import de.caluga.morphium.driver.MorphiumDriverOperation;
import de.caluga.morphium.driver.ReadPreference;
import de.caluga.morphium.driver.bson.MorphiumId;
import de.caluga.morphium.driver.bulk.BulkRequestContext;
import org.bson.*;
import org.bson.types.BSONTimestamp;
import org.bson.types.ObjectId;

import java.lang.reflect.Array;
import java.util.*;

/**
 * TODO: Add Documentation here
 **/
public class Driver implements MorphiumDriver {
    private Logger log = new Logger(Driver.class);
    private String[] hostSeed;
    private int maxConnectionsPerHost = 50;
    private int minConnectionsPerHost = 10;
    private int maxConnectionLifetime = 60000;
    private int maxConnectionIdleTime = 20000;
    private int socketTimeout = 1000;
    private int connectionTimeout = 1000;
    private int defaultW = 1;
    private int maxBlockintThreadMultiplier = 5;
    private int heartbeatFrequency = 1000;
    private int heartbeatSocketTimeout = 1000;
    private boolean useSSL = false;
    private boolean defaultJ = false;
    private int writeTimeout = 1000;
    private int localThreshold = 15;
    private boolean defaultFsync;
    private boolean socketKeepAlive;
    private int heartbeatConnectTimeout;
    private int maxWaitTime;

    private int defaultBatchSize = 100;
    private int retriesOnNetworkError = 2;
    private int sleepBetweenErrorRetries = 500;

    private ReadPreference defaultReadPreference;

    private Map<String, String[]> credentials = new HashMap<>();
    private MongoClient mongo;
    private Maximums maximums;

    @Override
    public void setCredentials(String db, String login, char[] pwd) {
        String[] cred = new String[2];
        cred[0] = login;
        cred[1] = new String(pwd);
        credentials.put(db, cred);
    }

    public ReadPreference getDefaultReadPreference() {
        return defaultReadPreference;
    }

    @Override
    public void setDefaultReadPreference(ReadPreference defaultReadPreference) {
        this.defaultReadPreference = defaultReadPreference;
    }

    @Override
    public String[] getCredentials(String db) {
        return credentials.get(db);
    }

    @Override
    public boolean isDefaultFsync() {
        return defaultFsync;
    }

    @Override
    public String[] getHostSeed() {
        return hostSeed;
    }

    @Override
    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    @Override
    public int getMinConnectionsPerHost() {
        return minConnectionsPerHost;
    }

    @Override
    public int getMaxConnectionLifetime() {
        return maxConnectionLifetime;
    }

    @Override
    public int getMaxConnectionIdleTime() {
        return maxConnectionIdleTime;
    }

    @Override
    public int getSocketTimeout() {
        return socketTimeout;
    }

    @Override
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    @Override
    public int getDefaultW() {
        return defaultW;
    }

    @Override
    public int getMaxBlockintThreadMultiplier() {
        return maxBlockintThreadMultiplier;
    }

    @Override
    public int getHeartbeatFrequency() {
        return heartbeatFrequency;
    }


    @Override
    public void setHeartbeatSocketTimeout(int heartbeatSocketTimeout) {
        this.heartbeatSocketTimeout = heartbeatSocketTimeout;
    }

    @Override
    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    @Override
    public void setHeartbeatFrequency(int heartbeatFrequency) {
        this.heartbeatFrequency = heartbeatFrequency;
    }

    @Override
    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    @Override
    public void setDefaultBatchSize(int defaultBatchSize) {
        this.defaultBatchSize = defaultBatchSize;
    }

    @Override
    public void setCredentials(Map<String, String[]> credentials) {
        this.credentials = credentials;
    }

    public void setMongo(MongoClient mongo) {
        this.mongo = mongo;
    }

    @Override
    public int getHeartbeatSocketTimeout() {
        return heartbeatSocketTimeout;
    }

    @Override
    public boolean isUseSSL() {
        return useSSL;
    }

    @Override
    public boolean isDefaultJ() {
        return defaultJ;
    }

    @Override
    public int getWriteTimeout() {
        return writeTimeout;
    }

    @Override
    public int getLocalThreshold() {
        return localThreshold;
    }

    @Override
    public void setHostSeed(String... host) {
        hostSeed = host;
    }

    @Override
    public void setMaxConnectionsPerHost(int mx) {
        maxConnectionsPerHost = mx;
    }

    @Override
    public void setMinConnectionsPerHost(int mx) {
        minConnectionsPerHost = mx;
    }

    @Override
    public void setMaxConnectionLifetime(int timeout) {
        maxConnectionLifetime = timeout;
    }

    @Override
    public void setMaxConnectionIdleTime(int time) {
        maxConnectionIdleTime = time;
    }

    @Override
    public void setSocketTimeout(int timeout) {
        socketTimeout = timeout;
    }

    @Override
    public void setConnectionTimeout(int timeout) {
        connectionTimeout = timeout;
    }

    @Override
    public void setDefaultW(int w) {
        defaultW = w;
    }

    @Override
    public void setMaxBlockingThreadMultiplier(int m) {
        maxBlockintThreadMultiplier = m;
    }

    @Override
    public void heartBeatFrequency(int t) {
        heartbeatFrequency = t;
    }

    @Override
    public void heartBeatSocketTimeout(int t) {
        heartbeatSocketTimeout = t;
    }

    @Override
    public void useSsl(boolean ssl) {
        useSSL = ssl;
    }

    @Override
    public void connect() throws MorphiumDriverException {
        connect(null);
    }

    @Override
    public void connect(String replicasetName) throws MorphiumDriverException {
        try {
            MongoClientOptions.Builder o = MongoClientOptions.builder();
            WriteConcern w = new WriteConcern(getDefaultW(), getWriteTimeout(), isDefaultFsync(), isDefaultJ());
            o.writeConcern(w);
            o.socketTimeout(getSocketTimeout());
            o.connectTimeout(getConnectionTimeout());
            o.connectionsPerHost(getMaxConnectionsPerHost());
            o.socketKeepAlive(isSocketKeepAlive());
            o.threadsAllowedToBlockForConnectionMultiplier(getMaxBlockintThreadMultiplier());
//        o.cursorFinalizerEnabled(isCursorFinalizerEnabled()); //Deprecated?
//        o.alwaysUseMBeans(isAlwaysUseMBeans());
            o.heartbeatConnectTimeout(getHeartbeatConnectTimeout());
            o.heartbeatFrequency(getHeartbeatFrequency());
            o.heartbeatSocketTimeout(getHeartbeatSocketTimeout());
            o.minConnectionsPerHost(getMinConnectionsPerHost());
            o.minHeartbeatFrequency(getHeartbeatFrequency());
            o.localThreshold(getLocalThreshold());
            o.maxConnectionIdleTime(getMaxConnectionIdleTime());
            o.maxConnectionLifeTime(getMaxConnectionLifetime());
            if (replicasetName != null) {
                o.requiredReplicaSetName(replicasetName);
            }
            o.maxWaitTime(getMaxWaitTime());


            List<MongoCredential> lst = new ArrayList<>();
            for (Map.Entry<String, String[]> e : credentials.entrySet()) {
                MongoCredential cred = MongoCredential.createMongoCRCredential(e.getValue()[0], e.getKey(), e.getValue()[1].toCharArray());
                lst.add(cred);
            }


            if (hostSeed.length == 1) {
                ServerAddress adr = new ServerAddress(hostSeed[0]);
                mongo = new MongoClient(adr, lst, o.build());
            } else {
                List<ServerAddress> adrLst = new ArrayList<>();
                for (String h : hostSeed) {
                    adrLst.add(new ServerAddress(h));
                }
                mongo = new MongoClient(adrLst, lst, o.build());
            }
        } catch (Exception e) {
            throw new MorphiumDriverException("Error creating connection to mongo", e);
        }
    }


    @Override
    public Maximums getMaximums() {
        if (maximums == null) {
            maximums = new Maximums();
            try {
                Map<String, Object> cmd = new HashMap<>();
                cmd.put("isMaster", 1);
                Map<String, Object> res = runCommand("admin", cmd);
                maximums.setMaxBsonSize((Integer) res.get("maxBsonObjectSize"));
                maximums.setMaxMessageSize((Integer) res.get("maxMessageSizeBytes"));
                maximums.setMaxWriteBatchSize((Integer) res.get("maxWriteBatchSize"));
            } catch (Exception e) {
                log.error("Error reading max avalues from DB", e);
//            maxBsonSize = 0;
//            maxMessageSize = 0;
//            maxWriteBatchSize = 0;
            }
        }
        return maximums;
    }

    @Override
    public boolean isConnected() {
        return mongo != null;
    }

    @Override
    public void setDefaultJ(boolean j) {
        defaultJ = j;
    }

    @Override
    public void setDefaultWriteTimeout(int wt) {
        writeTimeout = wt;
    }

    @Override
    public void setLocalThreshold(int thr) {
        localThreshold = thr;
    }

    @Override
    public void setDefaultFsync(boolean j) {
        defaultFsync = j;
    }

    @Override
    public void close() throws MorphiumDriverException {
        try {
            mongo.close();
        } catch (Exception e) {
            throw new MorphiumDriverException("error closing", e);
        }
    }

    @Override
    public Map<String, Object> getReplsetStatus() throws MorphiumDriverException {
        return new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                Document ret = mongo.getDatabase("admin").runCommand(new BasicDBObject("replSetGetStatus", 1));
                List<Document> mem = (List) ret.get("members");
                if (mem == null) return null;
                for (Document d : mem) {
                    if (d.get("optime") instanceof Map) {
                        d.put("optime", ((Map<String, Document>) d.get("optime")).get("ts"));
                    }
                }
                return convertBSON(ret);
            }

        }, retriesOnNetworkError, sleepBetweenErrorRetries);
//        return null;
    }

    @Override
    public Map<String, Object> getDBStats(String db) throws MorphiumDriverException {
        return new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                Document ret = mongo.getDatabase(db).runCommand(new BasicDBObject("dbstats", 1));
                return convertBSON(ret);
            }

        }, retriesOnNetworkError, sleepBetweenErrorRetries);
//        return null;
    }

    @Override
    public Map<String, Object> getOps(long threshold) throws MorphiumDriverException {
        throw new RuntimeException("Not implemented yet, sorry...");
//        return null;
    }

    @Override
    public Map<String, Object> runCommand(String db, Map<String, Object> cmd) throws MorphiumDriverException {

        return new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                Document ret = mongo.getDatabase(db).runCommand(new BasicDBObject(cmd));
                return convertBSON(ret);
            }

        }, retriesOnNetworkError, sleepBetweenErrorRetries);
    }


    @Override
    @SuppressWarnings("ALL")
    public List<Map<String, Object>> find(String db, String collection, Map<String, Object> query, Map<String, Integer> sort, Map<String, Integer> projection, int skip, int limit, int batchSize, ReadPreference readPreference, final Map<String, Object> findMetaData) throws MorphiumDriverException {
        replaceMorphiumIdByObjectId(query);
        return (List<Map<String, Object>>) new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {

                DB database = mongo.getDB(db);
//                MongoDatabase database = mongo.getDatabase(db);
                DBCollection coll = getColl(database, collection, readPreference, null);

                DBCursor ret = coll.find(new BasicDBObject(query), projection != null ? new BasicDBObject(projection) : null);

                //TODO: Read Preference handling
                if (sort != null)
                    ret = ret.sort(new BasicDBObject(sort));
                if (skip != 0) ret = ret.skip(skip);
                if (limit != 0) ret = ret.limit(limit);
                if (batchSize != 0) ret.batchSize(batchSize);
                else ret.batchSize(defaultBatchSize);
                List<Map<String, Object>> values = new ArrayList<>();
                for (DBObject d : ret) {
                    Map<String, Object> obj = convertBSON((BasicDBObject) d);
                    values.add(obj);
                }
                Map<String, Object> r = new HashMap<String, Object>();
                r.put("result", values);

                if (findMetaData != null) {
                    if (ret.getServerAddress() != null)
                        findMetaData.put("server", ret.getServerAddress().getHost() + ":" + ret.getServerAddress().getPort());
                    findMetaData.put("cursorId", ret.getCursorId());
                    findMetaData.put("collection", ret.getCollection().getName());
                }
                return r;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries).get("result");
    }

    private Map<String, Object> convertBSON(Map d) {
        Map<String, Object> obj = new HashMap<String, Object>();

        for (Object k : d.keySet()) {
            Object value = d.get(k);
            if (value instanceof BsonTimestamp) {
                value = (((BsonTimestamp) value).getTime() * 1000);
            } else if (value instanceof BSONTimestamp) {
                value = (((BSONTimestamp) value).getTime() * 1000);
            } else if (value instanceof BsonDocument) {
                value = convertBSON((Map) value);
            } else if (value instanceof BsonBoolean) {
                value = ((BsonBoolean) value).getValue();
            } else if (value instanceof BsonDateTime) {
                value = ((BsonDateTime) value).getValue();
            } else if (value instanceof BsonInt64) {
                value = ((BsonInt32) value).getValue();
            } else if (value instanceof BsonInt64) {
                value = ((BsonInt32) value).getValue();
            } else if (value instanceof BsonDouble) {
                value = ((BsonDouble) value).getValue();
            } else if (value instanceof ObjectId) {
                value = ((ObjectId) value).toHexString();
            } else if (value instanceof BasicDBList) {
                Map m = new HashMap<>();
                m.put("list", new ArrayList(((BasicDBList) value)));
                value = convertBSON(m).get("list");
            } else if (value instanceof BasicBSONObject) {
                value = convertBSON((Map) value);
            } else if (value instanceof BsonString) {
                value = value.toString();
            } else if (value instanceof List) {
                List v = new ArrayList<>();

                for (Object o : (List) value) {
                    if (o instanceof BSON || o instanceof BsonValue || o instanceof Map)
                        v.add(convertBSON((Map) o));
                    else
                        v.add(o);
                }
                value = v;
            } else if (value instanceof BsonArray) {
                Map m = new HashMap<>();
                m.put("list", new ArrayList(((BsonArray) value).getValues()));
                value = convertBSON(m).get("list");
            } else if (value instanceof Document) {
                value = convertBSON((Map) value);
            } else if (value instanceof BSONObject) {
                value = convertBSON((Map) value);
            }
            obj.put(k.toString(), value);
        }
        return obj;
    }


    @SuppressWarnings("Duplicates")
    public DBCollection getColl(DB database, String collection, ReadPreference readPreference, de.caluga.morphium.driver.WriteConcern wc) {
        DBCollection coll = database.getCollection(collection);
        com.mongodb.ReadPreference prf = null;

        if (readPreference == null) readPreference = defaultReadPreference;
        if (readPreference != null) {
            TagSet tags = null;

            //Thanks to the poor downward compatibility, a more or less complete copy of this code was necessary!
            //great!
            if (readPreference.getTagSet() != null) {
                List<Tag> tagList = new ArrayList<Tag>();
                for (Map.Entry<String, String> e : readPreference.getTagSet().entrySet()) {
                    tagList.add(new Tag(e.getKey(), e.getValue()));
                }
                tags = new TagSet(tagList);

            }
            switch (readPreference.getType()) {
                case NEAREST:
                    if (tags != null) {
                        prf = com.mongodb.ReadPreference.nearest(tags);
                    } else {
                        prf = com.mongodb.ReadPreference.nearest();
                    }
                    break;
                case PRIMARY:
                    prf = com.mongodb.ReadPreference.primary();
                    if (tags != null) log.warn("Cannot use tags with primary only read preference!");
                    break;
                case PRIMARY_PREFERRED:
                    if (tags != null)
                        prf = com.mongodb.ReadPreference.primaryPreferred(tags);
                    else
                        prf = com.mongodb.ReadPreference.primaryPreferred();
                    break;
                case SECONDARY:
                    if (tags != null)
                        prf = com.mongodb.ReadPreference.secondary(tags);
                    else
                        prf = com.mongodb.ReadPreference.secondary();
                    break;
                case SECONDARY_PREFERRED:
                    if (tags != null)
                        prf = com.mongodb.ReadPreference.secondaryPreferred(tags);
                    else
                        prf = com.mongodb.ReadPreference.secondary();
                    break;
                default:
                    log.error("Unhandeled read preference: " + readPreference.toString());
                    prf = null;

            }
            if (prf != null) coll.setReadPreference(prf);
        }

        if (wc != null) {
            WriteConcern writeConcern;
            if (wc.getW() < 0) {
                //majority
                writeConcern = WriteConcern.MAJORITY;
                writeConcern = writeConcern.withFsync(wc.isFsync());
                writeConcern = writeConcern.withJ(wc.isJ());
            } else {
                writeConcern = new WriteConcern(wc.getW(), wc.getWtimeout(), wc.isFsync(), wc.isJ());
            }
            coll.setWriteConcern(writeConcern);
        }
        return coll;
    }


    @SuppressWarnings("Duplicates")
    public MongoCollection<Document> getCollection(MongoDatabase database, String collection, ReadPreference readPreference, de.caluga.morphium.driver.WriteConcern wc) {
        MongoCollection<Document> coll = database.getCollection(collection);
        com.mongodb.ReadPreference prf = null;

        if (readPreference == null) readPreference = defaultReadPreference;
        if (readPreference != null) {
            TagSet tags = null;
            if (readPreference.getTagSet() != null) {
                List<Tag> tagList = new ArrayList<Tag>();
                for (Map.Entry<String, String> e : readPreference.getTagSet().entrySet()) {
                    tagList.add(new Tag(e.getKey(), e.getValue()));
                }
                tags = new TagSet(tagList);

            }
            switch (readPreference.getType()) {
                case NEAREST:
                    if (tags != null) {
                        prf = com.mongodb.ReadPreference.nearest(tags);
                    } else {
                        prf = com.mongodb.ReadPreference.nearest();
                    }
                    break;
                case PRIMARY:
                    prf = com.mongodb.ReadPreference.primary();
                    if (tags != null) log.warn("Cannot use tags with primary only read preference!");
                    break;
                case PRIMARY_PREFERRED:
                    if (tags != null)
                        prf = com.mongodb.ReadPreference.primaryPreferred(tags);
                    else
                        prf = com.mongodb.ReadPreference.primaryPreferred();
                    break;
                case SECONDARY:
                    if (tags != null)
                        prf = com.mongodb.ReadPreference.secondary(tags);
                    else
                        prf = com.mongodb.ReadPreference.secondary();
                    break;
                case SECONDARY_PREFERRED:
                    if (tags != null)
                        prf = com.mongodb.ReadPreference.secondaryPreferred(tags);
                    else
                        prf = com.mongodb.ReadPreference.secondary();
                    break;
                default:
                    log.error("Unhandeled read preference: " + readPreference.toString());
                    prf = null;

            }
            if (prf != null) coll = coll.withReadPreference(prf);
        }

        if (wc != null) {
            WriteConcern writeConcern;
            if (wc.getW() < 0) {
                //majority
                writeConcern = WriteConcern.MAJORITY;
                writeConcern = writeConcern.withFsync(wc.isFsync());
                writeConcern = writeConcern.withJ(wc.isJ());
            } else {
                writeConcern = new WriteConcern(wc.getW(), wc.getWtimeout(), wc.isFsync(), wc.isJ());
            }
            coll = coll.withWriteConcern(writeConcern);
        }
        return coll;
    }


    @Override
    public long count(String db, String collection, Map<String, Object> query, ReadPreference rp) {
        MongoDatabase database = mongo.getDatabase(db);
        MongoCollection<Document> coll = getCollection(database, collection, rp, null);
        return coll.count(new BasicDBObject(query));

    }

    @Override
    public void store(String db, String collection, List<Map<String, Object>> objs, de.caluga.morphium.driver.WriteConcern wc) throws MorphiumDriverException {
        replaceMorphiumIdByObjectId(objs);
        List<Map<String, Object>> isnew = new ArrayList<>();
        final List<Map<String, Object>> notnew = new ArrayList<>();
        for (Map<String, Object> o : objs) {
            if (o.get("_id") == null) {
                //isnew!!!
                isnew.add(o);
            } else {
                notnew.add(o);
            }
        }
        insert(db, collection, isnew, wc);
        new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                MongoCollection c = mongo.getDatabase(db).getCollection(collection);

                for (Map<String, Object> toUpdate : notnew) {
                    UpdateOptions o = new UpdateOptions();
                    o.upsert(true);
                    Document filter = new Document();
                    filter.put("_id", toUpdate.get("_id"));
//                    toUpdate.remove("_id");
                    Document update = new Document("$set", toUpdate);
                    c.updateOne(filter, update, o);
                }

                return null;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries);
    }

    @Override
    public void insert(String db, String collection, List<Map<String, Object>> objs, de.caluga.morphium.driver.WriteConcern wc) throws MorphiumDriverException {
        replaceMorphiumIdByObjectId(objs);
        if (objs == null || objs.size() == 0) return;
        final List<Document> lst = new ArrayList<>();
        for (Map<String, Object> o : objs) {
            lst.add(new Document(o));
        }

        new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                MongoCollection c = mongo.getDatabase(db).getCollection(collection);
                if (lst.size() == 1) {
                    c.insertOne(lst.get(0));
                } else {
                    InsertManyOptions imo = new InsertManyOptions();
                    imo.ordered(false);

                    c.insertMany(lst, imo);
                }

                for (int i = 0; i < lst.size(); i++) {
                    objs.get(i).put("_id", lst.get(i).get("_id"));
                }
                return null;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries);

    }

    @Override
    public Map<String, Object> update(String db, String collection, Map<String, Object> query, Map<String, Object> op, boolean multiple, boolean upsert, de.caluga.morphium.driver.WriteConcern wc) throws MorphiumDriverException {
        replaceMorphiumIdByObjectId(query);
        replaceMorphiumIdByObjectId(op);
        return new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                UpdateOptions opts = new UpdateOptions();
                opts.upsert(upsert);
                UpdateResult res;
                if (multiple) {
                    res = mongo.getDatabase(db).getCollection(collection).updateMany(new BasicDBObject(query), new BasicDBObject(op), opts);
                } else {
                    res = mongo.getDatabase(db).getCollection(collection).updateOne(new BasicDBObject(query), new BasicDBObject(op), opts);
                }

                Map<String, Object> ret = new HashMap<>();
                ret.put("matched", res.getMatchedCount());
                ret.put("modified", res.getModifiedCount());
                ret.put("acc", res.wasAcknowledged());
                return ret;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries);

    }

    @Override
    public Map<String, Object> delete(String db, String collection, Map<String, Object> query, boolean multiple, de.caluga.morphium.driver.WriteConcern wc) throws MorphiumDriverException {
        replaceMorphiumIdByObjectId(query);
        return new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                MongoDatabase database = mongo.getDatabase(db);
                MongoCollection<Document> coll = database.getCollection(collection);
                DeleteResult res;
                if (multiple) {
                    res = coll.deleteMany(new BasicDBObject(query));
                } else {
                    res = coll.deleteOne(new BasicDBObject(query));
                }
                Map<String, Object> r = new HashMap<String, Object>();
                r.put("deleted", res.getDeletedCount());
                r.put("acc", res.wasAcknowledged());

                return r;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries);
    }

    @Override
    public void drop(String db, String collection, de.caluga.morphium.driver.WriteConcern wc) throws MorphiumDriverException {
        new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                MongoDatabase database = mongo.getDatabase(db);
                MongoCollection<Document> coll = database.getCollection(collection);

                coll.drop();
                return null;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries);
    }

    @Override
    public void drop(String db, de.caluga.morphium.driver.WriteConcern wc) throws
            MorphiumDriverException {
        new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                MongoDatabase database = mongo.getDatabase(db);
                if (wc != null) {
                    WriteConcern writeConcern = new WriteConcern(wc.getW(), wc.getWtimeout(), wc.isFsync(), wc.isJ());
                    database = database.withWriteConcern(writeConcern);
                }
                database.drop();
                return null;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries);
    }

    @Override
    public boolean exists(String db) throws MorphiumDriverException {
        for (String dbName : mongo.getDatabaseNames()) {
            if (dbName.equals(db)) return true;
        }
        return false;
    }


    @Override
    public List<Object> distinct(String db, String collection, String field, final Map<String, Object> filter) throws MorphiumDriverException {
        replaceMorphiumIdByObjectId(filter);
        final List<Object> ret = new ArrayList<>();
        new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                DistinctIterable<Object> it = mongo.getDatabase(db).getCollection(collection).distinct(field, null);
                it = it.filter(new Document(filter));
                for (Object value : it) {
                    ret.add(it);
                }
                return null;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries);

        return ret;
    }

    @Override
    public boolean exists(String db, String collection) throws MorphiumDriverException {
        Map<String, Object> found = new DriverHelper().doCall(new MorphiumDriverOperation() {

            @Override
            public Map<String, Object> execute() {
                final Map<String, Object> ret = new HashMap<String, Object>();

                Document result = mongo.getDatabase(db).runCommand(new Document("listCollections", 1));
                result.get("first_batch");
                return null;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries);


        return found != null;
    }

    @Override
    public Map<String, Object> getIndexes(String db, String collection) throws MorphiumDriverException {
        return new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                List<Map<String, Object>> values = new ArrayList<>();
                for (Document d : mongo.getDatabase(db).getCollection(collection).listIndexes()) {
                    values.add(new HashMap<>(d));
                }
                HashMap<String, Object> ret = new HashMap<>();
                ret.put("values", values);
                return ret;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries);

    }

    @Override
    public List<String> getCollectionNames(String db) throws MorphiumDriverException {
        final List<String> ret = new ArrayList<>();
        new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                for (String c : mongo.getDatabase(db).listCollectionNames()) {
                    ret.add(c);
                }
                return null;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries);
        return ret;
    }


    @Override
    public Map<String, Object> group(String db, String coll, Map<String, Object> query, Map<String, Object> initial, String jsReduce, String jsFinalize, ReadPreference rp, String... keys) {
        replaceMorphiumIdByObjectId(query);
        replaceMorphiumIdByObjectId(initial);
        BasicDBObject k = new BasicDBObject();
        BasicDBObject ini = new BasicDBObject();
        ini.putAll(initial);
        for (String ks : keys) {
            if (ks.startsWith("-")) {
                k.put(ks.substring(1), "false");
            } else if (ks.startsWith("+")) {
                k.put(ks.substring(1), "true");
            } else {
                k.put(ks, "true");
            }
        }
        if (!jsReduce.trim().startsWith("function(")) {
            jsReduce = "function (obj,data) { " + jsReduce + " }";
        }
        if (jsFinalize == null) {
            jsFinalize = "";
        }
        if (!jsFinalize.trim().startsWith("function(")) {
            jsFinalize = "function (data) {" + jsFinalize + "}";
        }
        DBCollection collection = mongo.getDB(db).getCollection(coll);
        GroupCommand cmd = new GroupCommand(collection,
                k, new BasicDBObject(query), ini, jsReduce, jsFinalize);
        return convertBSON((Map<? extends String, ?>) cmd);
    }

    @Override
    public Map<String, Object> killCursors(String db, String collection, List<Long> cursorIds) throws
            MorphiumDriverException {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public Map<String, Object> aggregate(String db, String collection, List<Map<String, Object>> pipeline,
                                         boolean explain, boolean allowDiskUse, ReadPreference readPreference) throws MorphiumDriverException {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public boolean isSocketKeepAlive() {
        return socketKeepAlive;
    }

    @Override
    public void setSocketKeepAlive(boolean socketKeepAlive) {
        this.socketKeepAlive = socketKeepAlive;
    }

    @Override
    public int getHeartbeatConnectTimeout() {
        return heartbeatConnectTimeout;
    }

    @Override
    public void setHeartbeatConnectTimeout(int heartbeatConnectTimeout) {
        this.heartbeatConnectTimeout = heartbeatConnectTimeout;
    }

    @Override
    public int getMaxWaitTime() {
        return maxWaitTime;
    }

    @Override
    public void setMaxWaitTime(int maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    public int getRetriesOnNetworkError() {
        return retriesOnNetworkError;
    }

    @Override
    public void setRetriesOnNetworkError(int retriesOnNetworkError) {
        this.retriesOnNetworkError = retriesOnNetworkError;
    }

    public int getSleepBetweenErrorRetries() {
        return sleepBetweenErrorRetries;
    }

    public void setSleepBetweenErrorRetries(int sleepBetweenErrorRetries) {
        this.sleepBetweenErrorRetries = sleepBetweenErrorRetries;
    }


    @Override
    public boolean isCapped(String db, String coll) throws MorphiumDriverException {
        //TODO: Change to new syntax? Using Command?
        return mongo.getDB(db).getCollection(coll).isCapped();
    }

    @Override
    public BulkRequestContext createBulkContext(String db, String collection, boolean ordered, de.caluga.morphium.driver.WriteConcern wc) {
        return new MongodbBulkContext(db, collection, this, ordered, defaultBatchSize, wc);
    }


    public MongoDatabase getDb(String db) {
        return mongo.getDatabase(db);
    }

    public MongoCollection getCollection(String db, String coll) {

        return mongo.getDatabase(db).getCollection(coll);
    }

    private void replaceMorphiumIdByObjectId(Object in) {
        if (in instanceof Map) {
            Map<String, Object> m = (Map) in;
            Map<String, Object> toSet = new HashMap<>();
            try {
                for (Map.Entry e : m.entrySet()) {
                    if (e.getValue() instanceof MorphiumId) {
                        toSet.put((String) e.getKey(), new ObjectId(((MorphiumId) e.getValue()).toString()));
                    } else if (e.getValue() instanceof Collection) {
                        for (Object o : (Collection) e.getValue()) {
                            if (o instanceof Map) {
                                replaceMorphiumIdByObjectId((Map) o);
                            } else if (o instanceof List) {
                                replaceMorphiumIdByObjectId(o);
                            } else if (o.getClass().isArray()) {
                                replaceMorphiumIdByObjectId(o);
                            }
                        }
                    }
                }
                for (Map.Entry<String, Object> e : toSet.entrySet()) {
                    ((Map) in).put(e.getKey(), e.getValue());
                }

            } catch (Exception e) {
                //TODO: Implement Handling
//                throw new RuntimeException(e);
            }
        } else if (in instanceof Collection) {
            Collection c = (Collection) in;
            c.forEach(this::replaceMorphiumIdByObjectId);
        } else if (in.getClass().isArray()) {

            for (int i = 0; i < Array.getLength(in); i++) {
                replaceMorphiumIdByObjectId(Array.get(in, i));
            }
        }
    }

    @Override
    public void createIndex(String db, String collection, Map<String, Object> index, Map<String, Object> options) throws MorphiumDriverException {
        new DriverHelper().doCall(new MorphiumDriverOperation() {
            @Override
            public Map<String, Object> execute() {
                if (options == null) {
                    mongo.getDB(db).getCollection(collection).createIndex(new BasicDBObject(index));
                } else {
                    mongo.getDB(db).getCollection(collection).createIndex(new BasicDBObject(index), new BasicDBObject(options));
                }
                return null;
            }
        }, retriesOnNetworkError, sleepBetweenErrorRetries);

    }
}
