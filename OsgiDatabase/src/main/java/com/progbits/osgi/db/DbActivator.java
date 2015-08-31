/*
 Licenced under Apache 2.0 License

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */
package com.progbits.osgi.db;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scarr
 */
@Component(name = "datasources", properties = {"name=DbActivator"}, immediate = true,
        configurationPolicy = ConfigurationPolicy.require)
public class DbActivator {

    private final Logger log = LoggerFactory.getLogger(DbActivator.class);
    private BundleContext _context = null;
    private final Map<String, Map<String, String>> dbSettings = new HashMap<>();

    private final Map<String, HikariDataSource> dbMap = new HashMap<>();
    private final Map<String, ServiceRegistration> dbSrv = new HashMap<>();

    @Activate
    public void start(BundleContext context, Map<String, Object> config) throws Exception {
        _context = context;

        Dictionary<String, Object> cmdProps = new Hashtable<>();
        cmdProps.put("osgi.command.scope", "osgidb");
        cmdProps.put("osgi.command.function", new String[]{"list", "reset", "config"});
        _context.registerService(DbActivator.class, this, cmdProps);

        updated(config);
    }

    @Deactivate
    public void stop(BundleContext context) throws Exception {
        // Unregister all Services
        Set<String> dbNames = dbSrv.keySet();

        for (String name : dbNames) {
            dbSrv.get(name).unregister();

            dbMap.get(name).shutdown();
        }
    }

    @Modified
    public void updated(Map<String, Object> dctnr) {
        if (dctnr != null) {
            for (Map.Entry<String, Object> entry : dctnr.entrySet()) {
                String sKey = entry.getKey();

                int iLoc = sKey.lastIndexOf("_");

                if (iLoc > -1) {
                    String name = sKey.substring(0, iLoc);

                    if (!dbSettings.containsKey(name)) {
                        dbSettings.put(name, new HashMap<String, String>());
                    }

                    dbSettings.get(name).put(sKey, (String) dctnr.get(sKey));
                }
            }

            for (String name : dbSettings.keySet()) {
                try {
                    registerDb(name);
                } catch (Exception ex) {
                    log.error("registerDb", ex);
                }
            }
        }
    }

    private void registerDb(String name) {
        Map<String, String> dbConfig = dbSettings.get(name);

        String refreshDb = dbConfig.get(name + "_Refresh");

        int addType = 1;

        if (dbMap.containsKey(name)) {
            if ("true".equalsIgnoreCase(refreshDb)) {
                addType = 2;
            } else {
                addType = 0;
            }
        }

        if (addType == 1) {
            HikariDataSource ds = new HikariDataSource();

            ds.setDriverClassName((String) dbConfig.get(name + "_Driver"));
            ds.setJdbcUrl((String) dbConfig.get(name + "_URL"));
            ds.setUsername((String) dbConfig.get(name + "_Username"));
            ds.setPassword((String) dbConfig.get(name + "_Password"));

            ds.setPoolName(name + "_Pool");

            String strTemp = (String) dbConfig.get(name + "_MaxConn");

            if (strTemp != null) {
                ds.setMaximumPoolSize(Integer.parseInt(strTemp));
            } else {
                dbConfig.put(name + "_MaxConn", "20");
                ds.setMaximumPoolSize(20);
            }

            strTemp = (String) dbConfig.get(name + "_ConnTimeout");

            if (strTemp != null) {
                Integer iTimeout = Integer.parseInt(strTemp);

                if (iTimeout < 80) {
                    iTimeout = iTimeout * 1000;
                }

                ds.setConnectionTimeout(iTimeout);
            } else {
                // Default to 30 seconds
                ds.setConnectionTimeout(30000);
            }

            strTemp = (String) dbConfig.get(name + "_ConnTest");

            if (strTemp != null) {
                ds.setConnectionTestQuery(strTemp);
            }

            Connection conn = null;
            boolean bSuccess = false;

            try {
                conn = ds.getConnection();

                bSuccess = true;
            } catch (SQLException ex) {
                log.error("Connection Error", ex);
            } finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (Exception ex) {
                }
            }

            if (bSuccess) {
                dbMap.put(name, ds);

                Hashtable<String, Object> properties = new Hashtable<>();
                properties.put("name", name);

                // Put the service Registration in Map so we can remove them when we stop.
                dbSrv.put(name, _context.registerService(DataSource.class.getName(), ds, properties));

                log.info("Registed New Database: " + name + " using URL: "
                        + (String) dbConfig.get(name + "_URL"));
            }
        } else if (addType == 2) {
            // TODO:  Create refresh code
        }
    }

    public void list() {

        System.out.print(String.format("%1$15s", "DataSource"));
        System.out.print(String.format("%1$15s", "Max Cn"));
        System.out.print(String.format("%1$15s", "Active Cn"));
        System.out.print(String.format("%1$15s", "Free Cn"));
        System.out.print(String.format("%1$15s", "Cn Wait"));
        System.out.print("\n");

        for (Map.Entry<String, HikariDataSource> entry : dbMap.entrySet()) {
            HikariPool pool = getPool(entry.getValue());

            int totalConnections = pool.getTotalConnections();
            int activeConnections = pool.getActiveConnections();
            int freeConnections = totalConnections - activeConnections;
            int connectionWaiting = pool.getThreadsAwaitingConnection();

            System.out.print(String.format("%1$15s", entry.getKey()));
            System.out.print(String.format("%1$15s", totalConnections));
            System.out.print(String.format("%1$15s", activeConnections));
            System.out.print(String.format("%1$15s", freeConnections));
            System.out.print(String.format("%1$15s", connectionWaiting));
            System.out.print("\n");
        }
    }

    public void reset(String dsName) {
        if (dbSrv.containsKey(dsName)) {
            try {
                System.out.println("Unregistering Datasource: " + dsName);

                dbSrv.get(dsName).unregister();

                System.out.println("Datasource Unregistered: " + dsName);

                dbSrv.remove(dsName);

                dbMap.get(dsName).shutdown();

                dbMap.remove(dsName);

                System.out.println("Refreshing: " + dsName);

                registerDb(dsName);
            } catch (Exception ex) {
                System.out.println("Failed to Reset: " + dsName + " Error: " + ex.getMessage());
            }
        } else {
            System.out.println("DataSource: " + dsName + " Doesn't Exist");
        }
    }

    public void config(String dsName) {
        Map<String, String> dbConfig = dbSettings.get(dsName);

        if (dbConfig == null) {
            System.out.println("DataSource: " + dsName + " Doesn't Exist");
        } else {
            for (Map.Entry<String, String> entry : dbConfig.entrySet()) {
                System.out.println(entry.getKey() + ":  " + entry.getValue());
            }
        }
    }

    public HikariPool getPool(HikariDataSource ds) {
        try {
            Field field = ds.getClass().getDeclaredField("pool");
            field.setAccessible(true);
            return (HikariPool) field.get(ds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
