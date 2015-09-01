package com.progbits.osgi.db.commands;

import com.progbits.osgi.db.DbActivator;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

/**
 *
 * @author scarr
 */
@Command(scope = "osgidb", name = "refresh", description="Refresh/Reset a Database in the Instance")
@Service
public class OsgDbCommandRefresh implements Action {
    @Reference
    private DbActivator _db;
    
    @Argument(index = 0, name = "name", description = "The name of the Database to Refresh.", required = true, multiValued = false)
    @Completion(DbNameCompleter.class)
    private String name;
    
    @Override
    public Object execute() throws Exception {
        _db.reset(name);
        return null;
    }
    
}
