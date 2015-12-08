package com.hyd.ssdb.spring;

import com.hyd.ssdb.SsdbClient;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * (description)
 * created at 15-12-8
 *
 * @author Yiding
 */
public class SpringTest {

    private static ApplicationContext applicationContext;

    @BeforeClass
    public static void init() {
        applicationContext =
                new ClassPathXmlApplicationContext("classpath:application-context.xml");
    }

    @Test
    public void testSingleServer() throws Exception {
        SsdbClient client;

        client = applicationContext.getBean("singleServerSsdbClient", SsdbClient.class);
        client.set("name", "hydrogen-ssdb");
        System.out.println(client.get("name"));
    }

    @Test
    public void testSingleCluster() throws Exception {
        SsdbClient client;

        client = applicationContext.getBean("singleClusterSsdbClient", SsdbClient.class);
        client.set("name", "hydrogen-ssdb");
        System.out.println(client.get("name"));
    }
}
