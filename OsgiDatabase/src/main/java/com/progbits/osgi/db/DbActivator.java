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

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scarr
 */
public class DbActivator implements BundleActivator, ManagedService {

    private Logger log = LoggerFactory.getLogger(DbActivator.class);
    private BundleContext _context = null;
    private Map<String, HikariDataSource> dbMap = new HashMap<>();
    private Map<String, ServiceRegistration> dbSrv = new HashMap<>();

    public void start(BundleContext context) throws Exception {
        _context = context;

        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, "datasources");
        _context.registerService(ManagedService.class.getName(), this, properties);

        Dictionary<String, Object> cmdProps = new Hashtable();
        cmdProps.put("osgi.command.scope", "pbdb");
        cmdProps.put("osgi.command.function", new String[]{"list"});
        _context.registerService(DbActivator.class, this, cmdProps);
    }

    public void stop(BundleContext context) throws Exception {
        // Unregister all Services
        for (Map.Entry<String, ServiceRegistration> db : dbSrv.entrySet()) {
            db.getValue().unregister();
        }
    }

    public void updated(Dictionary<String, ?> dctnr) throws ConfigurationException {
        if (dctnr != null) {
            Enumeration<String> keys = dctnr.keys();
            List<String> names = new ArrayList<>();

            while (keys.hasMoreElements()) {
                String sKey = keys.nextElement();

                if (sKey.contains("_URL")) {
                    int iLoc = sKey.indexOf("_URL");

                    String name = sKey.substring(0, iLoc);

                    names.add(name);
                }
            }

            for (String name : names) {
                try {
                    registerDb(name, dctnr);
                } catch (Exception ex) {
                    log.error("registerDb", ex);
                }
            }
        }
    }

    private void registerDb(String name, Dictionary<String, ?> dctnr) {
        String refreshDb = (String) dctnr.get(name + "_Refresh");

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

            ds.setDriverClassName((String) dctnr.get(name + "_Driver"));
            ds.setJdbcUrl((String) dctnr.get(name + "_URL"));
            ds.setUsername((String) dctnr.get(name + "_Username"));
            ds.setPassword((String) dctnr.get(name + "_Password"));

            ds.setPoolName(name + "_Pool");

            String strTemp = (String) dctnr.get(name + "_MaxConn");

            if (strTemp != null) {
                ds.setMaximumPoolSize(Integer.parseInt(strTemp));
            }

            strTemp = (String) dctnr.get(name + "_ConnTimeout");

            if (strTemp != null) {
                ds.setConnectionTimeout(Integer.parseInt(strTemp));
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
                        + (String) dctnr.get(name + "_URL"));
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
            int totalConnections = getPool(entry.getValue()).getTotalConnections();
            int activeConnections = getPool(entry.getValue()).getActiveConnections();
            int freeConnections = totalConnections - activeConnections;
            int connectionWaiting = getPool(entry.getValue()).getThreadsAwaitingConnection();

            System.out.print(String.format("%1$15s", entry.getKey()));
            System.out.print(String.format("%1$15s", totalConnections));
            System.out.print(String.format("%1$15s", activeConnections));
            System.out.print(String.format("%1$15s", freeConnections));
            System.out.print(String.format("%1$15s", connectionWaiting));
            System.out.print("\n");
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
