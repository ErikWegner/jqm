import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import com.enioka.jqm.api.Dispatcher;
import com.enioka.jqm.api.JobDefinition;
import com.enioka.jqm.jpamodel.DatabaseProp;
import com.enioka.jqm.jpamodel.DeploymentParameter;
import com.enioka.jqm.jpamodel.JobDef;
import com.enioka.jqm.jpamodel.JobDefParameter;
import com.enioka.jqm.jpamodel.Node;
import com.enioka.jqm.jpamodel.Queue;
import com.enioka.jqm.tools.CreationTools;

/**
 * Copyright � 2013 enioka. All rights reserved
 * Authors: Pierre COPPEE (pierre.coppee@enioka.com)
 * Contributors : Marc-Antoine GOUILLART (marc-antoine.gouillart@enioka.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class Main
{
	static DatabaseProp db = null;

	static DeploymentParameter dp = null;
	static DeploymentParameter dpNormal = null;
	static Node node = null;

	static Queue qVip = null;
	static Queue qNormal = null;
	static Queue qSlow = null;

	//static JobParameter jp = null;
//	static JobParameter jpd = null;
//	static JobParameter jpdm = null;

	static JobDef jd = new JobDef();

	static JobDef jdDemoMaven = null;

	static JobDef jdDemo = null;

	public static JobDefParameter jdp = null;
	public static JobDefParameter jdp2 = null;
	public static ArrayList<JobDefParameter> jdargs = new ArrayList<JobDefParameter>();

	/**
	 * @param args
	 * @throws Exception
	 */
    public static void main(String[] args) throws Exception {

    	EntityManagerFactory emf = Persistence.createEntityManagerFactory("jobqueue-api-pu");
    	EntityManager em = emf.createEntityManager();

		EntityTransaction transac = em.getTransaction();
		transac = em.getTransaction();
		transac.begin();
		em.createQuery("DELETE FROM DatabaseProp").executeUpdate();
		transac.commit();
		transac = em.getTransaction();
		transac.begin();
		em.createQuery("DELETE FROM DeploymentParameter").executeUpdate();
		transac.commit();
		transac = em.getTransaction();
		transac.begin();
		em.createQuery("DELETE FROM Node").executeUpdate();
		transac.commit();
		transac = em.getTransaction();
		transac.begin();
		em.createQuery("DELETE FROM JobHistoryParameter").executeUpdate();
		transac.commit();
		transac = em.getTransaction();
		transac.begin();
		em.createQuery("DELETE FROM Message").executeUpdate();
		transac.commit();
		transac = em.getTransaction();
		transac.begin();
		em.createQuery("DELETE FROM History").executeUpdate();
		transac.commit();
		transac = em.getTransaction();
		transac.begin();
		em.createQuery("DELETE FROM JobDefParameter").executeUpdate();
		transac.commit();
		transac = em.getTransaction();
		transac.begin();
		em.createQuery("DELETE FROM JobParameter").executeUpdate();
		transac.commit();
		transac = em.getTransaction();
		transac.begin();
		em.createQuery("DELETE FROM JobInstance").executeUpdate();
		transac.commit();
		transac = em.getTransaction();
		transac.begin();
		em.createQuery("DELETE FROM JobDef").executeUpdate();
		transac.commit();
		transac = em.getTransaction();
		transac.begin();
		em.createQuery("DELETE FROM Queue").executeUpdate();
		transac.commit();

		jdp = CreationTools.createJobDefParameter("p1", "POUPETTE", em);
//		jdp2 = CreationTools.createJobDefParameter("p2", "2");
		jdargs.add(jdp);
//		jdargs.add(jdp2);

		db = CreationTools.createDatabaseProp("jdbc/marsu", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "SA", "", em);

		qVip = CreationTools.initQueue("VIPQueue", "Queue for the winners", 42 , 100, em);
		qNormal = CreationTools.initQueue("NormalQueue", "Queue for the ordinary job", 7 , 100, em);
		qSlow = CreationTools.initQueue("SlowQueue", "Queue for the bad guys", 3 , 100, em);

		jd = CreationTools.createJobDef(true, "App", jdargs, "../JobBaseAPI/testprojects/Fibo/",
				"../JobBaseAPI/testprojects/Fibo/Fibo.jar",
				qVip,
				42, "Fibo", 42, "Franquin", "ModuleMachin", "other", "other", "other", false, em);
//
//		jd = CreationTools.createJobDef(true, "App", jdargs, "../JobBaseAPI/testprojects/DateTimeMaven/",
//				"../JobBaseAPI/testprojects/DateTimeMaven/DateTimeMaven.jar", qVip, 42,
//		        "MarsuApplication", 42, "Franquin", "ModuleMachin", "other", "other", "other", false, em);
//
		jdDemoMaven = CreationTools.createJobDef(true, "Main", null, "/Users/pico/Dropbox/projets/enioka/tests/DateTimeMaven/",
				"/Users/pico/Dropbox/projets/enioka/tests/DateTimeMaven/target/DateTimeMaven-0.0.1-SNAPSHOT.jar", qNormal,
				42, "MarsuApplication2", 42, "Franquin", "ModuleMachin", "other", "other", "other", true, em);


		jdDemo = CreationTools.createJobDef(true, "App", jdargs, "./testprojects/JobGenADeliverable/",
		        "./testprojects/JobGenADeliverable/JobGenADeliverable.jar", qVip, 42, "deliverable", 42, "Franquin", "ModuleMachin",
		        "other", "other", "other", false, em);
//
////		jp = CreationTools.createJobParameter("arg1", "/Users/pico/Dropbox/projets/enioka/tests/DateTimeMaven/target/DateTimeMaven-0.0.1-SNAPSHOT.jar", jd);
////		jpdm = CreationTools.createJobParameter("", "", jdDemoMaven);
////		jpd = CreationTools.createJobParameter("", "", jdDemo);
//
		node = CreationTools.createNode("localhost", 8081, "../JobBaseAPI/dlRepository/", em);
//
		dp = CreationTools.createDeploymentParameter(1, node, 3, 1000, qVip, em);
		dpNormal = CreationTools.createDeploymentParameter(1, node, 2, 5000, qNormal, em);

		JobDefinition newJob = new JobDefinition("deliverable", "MAG");
		newJob.addParameter("filepath",
		        "/Users/pico/Dropbox/projets/enioka/jqm/serious/JobBaseAPI/testprojects/JobGenADeliverable/JobGenADeliverable.txt");
//		newJob.addParameter("p1", "1");
//		newJob.addParameter("p2", "2");

        @SuppressWarnings("unused")
        JobDefinition newDemoMaven = new JobDefinition("MarsuApplication2", "MAG");

//
//		Dispatcher.enQueue(newDemoMaven);
//		Dispatcher.enQueue(newDemoMaven);
//		Dispatcher.enQueue(newDemoMaven);
//		Dispatcher.enQueue(newDemoMaven);
//		Dispatcher.enQueue(newDemoMaven);
//		Dispatcher.enQueue(newDemoMaven);
		Dispatcher.enQueue(newJob);
//		Dispatcher.enQueue(newJob);
//		Dispatcher.enQueue(newJob);

//		transac = em.getTransaction();
//		transac.begin();
//
//		JobInstance ji = em.createQuery("SELECT j FROM JobInstance j WHERE j.position = :myId AND j.jd.id = :i", JobInstance.class)
//		        .setParameter("myId", 2).setParameter("i", jd.getId()).getSingleResult();
//
//		em.createQuery("UPDATE JobInstance j SET j.state = 'ATTRIBUTED' WHERE j.id = :idJob").setParameter("idJob", ji.getId()).executeUpdate();
//
//		transac.commit();
////		Dispatcher.enQueue(jd);
////		Dispatcher.enQueue(jd);
////		Dispatcher.getDeliverables(499);
		CreationTools.close(em);
	}

}