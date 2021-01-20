package com.progbits.osgi.db.commands;

import com.progbits.osgi.db.DbActivator;
import java.util.List;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

/**
 *
 * @author scarr
 */
@Service
public class DbNameCompleter implements Completer {

    @Reference
    private DbActivator _db;
    
    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        
        for (String name : _db.listDbNames()) {
            delegate.getStrings().add(name);
        }
        
        return delegate.complete(session, commandLine, candidates);
    }
}
