package com.progbits.osgi.db.commands;

import com.progbits.osgi.db.DbActivator;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

/**
 *
 * @author scarr
 */
@Command(scope = "osgidb", name = "list", description="List the Databases in the System")
@Service
public class OsgDbCommandList implements Action {
    @Reference
    private DbActivator _db;
    
    @Override
    public Object execute() throws Exception {
        _db.list();
        return null;
    }
    
}
