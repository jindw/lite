package net.juyantang;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

public class PersistenceFacade {

    private static final PersistenceManagerFactory pmfInstance =
        JDOHelper.getPersistenceManagerFactory("transactions-optional");

    private PersistenceFacade() {}

    public static PersistenceManagerFactory getFactory() {
        return pmfInstance;
    }
    public static PersistenceManager getManager(){
    	return pmfInstance.getPersistenceManager();
    }

}
