package com.enioka.jqm.test;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hsqldb.Server;

import com.enioka.jqm.api.JqmClientFactory;
import com.enioka.jqm.jpamodel.GlobalParameter;
import com.enioka.jqm.jpamodel.JobDef;
import com.enioka.jqm.jpamodel.JobInstance;
import com.enioka.jqm.jpamodel.Node;
import com.enioka.jqm.jpamodel.Profile;
import com.enioka.jqm.jpamodel.Queue;
import com.enioka.jqm.jpamodel.RuntimeParameter;
import com.enioka.jqm.jpamodel.State;
import com.enioka.jqm.tools.JqmSingleRunner;

/**
 * This class allows to start a stripped-down version of the JQM engine and run a payload synchronously inside it.<br>
 * It is the only supported way to unit test a payload (be it with JUnit or a simple main() method). <br>
 * <br>
 * Limitations:
 * <ul>
 * <li>The job will actually run inside the current class loader (in the full engine, each job instance has its own class loader)</li>
 * <li>This for testing one job only. Only one job instance will run! If the test itself enqueues new launch request, they will be ignored.
 * For testing interactions between job instances, integration tests on an embedded JQM engine are required.</li>
 * <li>If using resources (JNDI), they must be put inside a resource.xml file at the root of classloader search.</li>
 * <li>Resource providers and corresponding drivers must be put inside testing class path (for example by putting them inside pom.xml with a
 * <code>test</code> scope)</li>
 * <li>To ease tests, the launch is synchronous. Obviously, real life instances are asynchronous. To test asynchronous launches, use an
 * embedded engine (integration test).</li>
 * <li>If files are created by the payload, they are stored inside a temporary directory that is not removed at the end of the run.</li>
 * </ul>
 * <br>
 * For example, a simple Junit test could be:
 * 
 * <pre>
 * {@code public void testOne()
 * {
 *     JobInstance res = JqmTester.create("com.enioka.jqm.test.Payload1").run();
 *     Assert.assertEquals(State.ENDED, res.getState());
 * }
 * </pre>
 */
public class JqmTester
{
    private static Server s;
    private String dbName;
    private EntityManagerFactory emf = null;
    private EntityManager em = null;
    private Node node = null;
    private Profile profile = null;
    private JobDef jd = null;
    private Queue q = null;
    private JobInstance ji = null;
    private File resDirectoryPath;

    private JqmTester(String className)
    {
        s = new Server();
        dbName = "testdb_" + className.hashCode() + Math.random();
        s.setDatabaseName(0, dbName);
        s.setDatabasePath(0, "mem:" + dbName);
        s.setLogWriter(null);
        s.setSilent(true);
        s.start();

        Properties p = new Properties();
        p.put("hibernate.hbm2ddl.auto", "update");
        p.put("hibernate.dialect", "com.enioka.jqm.tools.HSQLDialect7479");
        p.put("hibernate.pool_size", 5);
        p.put("javax.persistence.nonJtaDataSource", "");
        p.put("hibernate.connection.url", "jdbc:hsqldb:hsql://localhost/" + dbName);

        emf = Persistence.createEntityManagerFactory("jobqueue-api-pu", p);
        em = emf.createEntityManager();
        em.getTransaction().begin();
        JqmSingleRunner.setConnection(emf);

        Properties p2 = new Properties();
        p2.put("emf", emf);
        JqmClientFactory.setProperties(p2);

        // Needed parameters
        GlobalParameter gp = new GlobalParameter();
        gp.setKey("defaultConnection");
        gp.setValue("");
        em.persist(gp);

        // Ext dir
        File extDir = new File("./ext");
        if (!extDir.exists() && !extDir.mkdir())
        {
            throw new RuntimeException(new IOException("./ext directory does not exist and cannot create it"));
        }
        
        // Create profile
        profile = new Profile();
        profile.setDescription("tester profile");
        profile.setName("tester");
        em.persist(profile);

        // Create node
        resDirectoryPath = createTempDirectory();
        node = new Node();
        node.setProfile(profile);
        node.setDlRepo(resDirectoryPath.getAbsolutePath());
        node.setDns("test");
        node.setName("testtempnode");
        node.setRepo(resDirectoryPath.getAbsolutePath());
        node.setTmpDirectory(resDirectoryPath.getAbsolutePath());
        node.setPort(12);
        em.persist(node);

        q = new Queue(); // Only useful because JobDef.queue is non-null
        q.setProfile(profile);
        q.setDefaultQueue(true);
        q.setName("default");
        q.setDescription("default test queue");
        em.persist(q);

        jd = new JobDef();
        jd.setProfile(profile);
        jd.setApplicationName("TestApplication");
        jd.setJarPath("/dev/null");
        jd.setJavaClassName(className);
        jd.setQueue(q);
        em.persist(jd);

        ji = new JobInstance();
        ji.setProfile(profile);
        ji.setApplication("TestApplication");
        ji.setCreationDate(Calendar.getInstance());
        ji.setAttributionDate(Calendar.getInstance());
        ji.setInternalPosition(0);
        ji.setJd(jd);
        ji.setNode(node);
        ji.setQueue(q);
        ji.setState(State.ATTRIBUTED);
        em.persist(ji);

        em.getTransaction().commit();
    }

    public static JqmTester create(String className)
    {
        return new JqmTester(className);
    }

    private File createTempDirectory()
    {
        final File temp;
        try
        {
            temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

            if (!(temp.delete()))
            {
                throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
            }

            if (!(temp.mkdir()))
            {
                throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
            }
        }
        catch (Exception e)
        {
            // Hatred of checked exceptions.
            throw new RuntimeException(e);
        }
        return (temp);
    }

    public JqmTester addParameter(String key, String value)
    {
        RuntimeParameter rp = new RuntimeParameter();
        rp.setJi(ji.getId());
        rp.setKey(key);
        rp.setValue(value);

        em.getTransaction().begin();
        em.persist(rp);
        em.getTransaction().commit();

        return this;
    }

    private void close()
    {
        s.stop();
        s.shutdown();
        emf.close();
        JqmClientFactory.resetClient();
        JqmClientFactory.setProperties(new Properties());
    }

    public com.enioka.jqm.api.JobInstance run()
    {
        com.enioka.jqm.api.JobInstance res = JqmSingleRunner.run(ji);

        close();
        return res;
    }
}
