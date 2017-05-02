package org.xidea.lite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.impl.ValueStackImpl;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by jinjinyun on 17/4/28.
 */
public class FutureWaitStack extends ValueStackImpl {
    private static final Log log = LogFactory.getLog(LiteEngine.class);
    public static final Class<?> futureClass ;
    public static final Method futureGetter ;
    static{
        Class<?> clazz = null;
        Method getMethod = null;
        try {
            clazz = Class.forName( "java.util.concurrent.Future");
            //java.util.concurrent.Future f = null;
            getMethod = clazz.getMethod("get");
        } catch (Exception e) {
            log.error(e);
        }
        futureClass = clazz;
        futureGetter = getMethod;
    }
    public static Map<String,Object> wrap(Map<String,Object> values){
        return wrap(values,false);
    }
    public static Map<String,Object> wrap(Map<String,Object> values,boolean newTop){
        if(newTop){
            return new FutureWaitStack(values,newTop);
        }
        if(futureGetter != null ) {
            for (Object v : values.values()) {
                if (futureClass.isInstance(v)) {
                    return new FutureWaitStack(values,newTop);
                }
            }
        }
        return values;
    }
    public FutureWaitStack(Object source,boolean newTop){
        if(newTop){
            initialize(source,new HashMap<String,Object>());
        }else{
            initialize(source);
        }
    }

    public Object get(Object key) {
        Object value = super.get(key);
        if(futureGetter != null && futureClass.isInstance(value)){
            try {
                return futureGetter.invoke(value);
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }
        return value;
    }

}
