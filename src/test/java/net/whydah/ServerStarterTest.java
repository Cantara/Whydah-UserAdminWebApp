package net.whydah;

import net.whydah.identity.ServerRunner;
import net.whydah.sso.config.ApplicationMode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import static org.junit.Assert.assertTrue;

public class ServerStarterTest {

    private static ServerRunner serverRunner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Map<String, String> addToEnv = new HashMap<>();
        addToEnv.put(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        setEnv(addToEnv);
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        Random r = new Random(System.currentTimeMillis());
        serverRunner = new ServerRunner(10000 + r.nextInt(20000));

        serverRunner.start();
        Thread.sleep(6000);
    }

    @AfterClass
    public static void shutdown() throws Exception {
        serverRunner.stop();
    }

    @Test
    public void testGetHealth() {
        URL url;
        URLConnection conn;
        try {
            url = new URL(ServerRunner.getHEALTHURL());
            conn = url.openConnection();
            conn.connect();

            InputStream respose = conn.getInputStream();
            try (Scanner scanner = new Scanner(respose)) {
                String responseBody = scanner.useDelimiter("\\A").next();
                System.out.println(responseBody);
                assertTrue(responseBody.length() > 10);
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
            assertTrue("Unable to connect to server", true);
        }
    }

    protected static void setEnv(Map<String, String> newenv) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for (Class cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}