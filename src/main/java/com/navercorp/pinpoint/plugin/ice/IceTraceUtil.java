package com.navercorp.pinpoint.plugin.ice;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.MethodFilters;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;

public class IceTraceUtil {

	private static final PLogger logger = PLoggerFactory.getLogger(IceTraceUtil.class);;
	
	private static Map<String,List<InstrumentMethod>> cache = new HashMap<>();
	
	public static String[] argumentsOfMethod(Method method) {
		String[] args = new String[method.getParameterTypes().length];
		int i = 0;
		for (Class<?> clazz : method.getParameterTypes()) {
			args[i++] = clazz.getSimpleName();
		}
		return args;
	}

	public static InstrumentMethodMetaData getClientMethods(InstrumentClass target, String className) {
		List<InstrumentMethod> methods = getClientInstrumentMethods(target,className);
		Map<String,InstrumentMethod> args = new HashMap<>();
		Map<String,InstrumentMethod> syncMethods = new HashMap<>();
		Map<String,InstrumentMethod> beginMethods = new HashMap<>();
		Map<String,InstrumentMethod> endMethods = new HashMap<>();
		Set<String> actions = new HashSet<>();
		
		for(InstrumentMethod method:methods){
			args.put(method.getName(), method);
		}
		for(Entry<String, InstrumentMethod> entry:args.entrySet()){
			if(args.containsKey(entry.getKey())&&args.containsKey("begin_"+entry.getKey())){
				actions.add(entry.getKey());
			}
		}
		for(String action:actions){
			syncMethods.put(action, args.get(action));
			beginMethods.put(action, args.get("begin_"+action));
			endMethods.put(action, target.getDeclaredMethod("end_"+action, "Ice.AsyncResult"));
		}
		return new InstrumentMethodMetaData(syncMethods, beginMethods, endMethods, actions);
	}

	public static List<InstrumentMethod> getClientInstrumentMethods(InstrumentClass target, String className) {
		logger.info("className:"+className);
		if(cache.containsKey(className)){
			return cache.get(className);
		}
		List<InstrumentMethod> methods = target.getDeclaredMethods(
				MethodFilters.chain(MethodFilters.modifier(Modifier.PRIVATE),
						MethodFilters.modifierNot(Modifier.STATIC)));
		cache.put(className, methods);
		return methods;
	}
	
	public static List<InstrumentMethod> getServerInstrumentMethods(InstrumentClass target, String className) {
		logger.info("className:"+className);
		if(cache.containsKey(className)){
			return cache.get(className);
		}
		List<InstrumentMethod> methods = target.getDeclaredMethods(
				MethodFilters.chain(MethodFilters.modifier(Modifier.PUBLIC),
						MethodFilters.modifierNot(Modifier.STATIC)));
		cache.put(className, methods);
		return methods;
	}

	public static class InstrumentMethodMetaData{
		
		private Map<String,InstrumentMethod> syncMethods;
		private Map<String,InstrumentMethod> beginMethods;
		private Map<String,InstrumentMethod> endMethods;
		private Set<String> actions;
		
		public InstrumentMethodMetaData(Map<String, InstrumentMethod> syncMethods,
				Map<String, InstrumentMethod> beginMethods, Map<String, InstrumentMethod> endMethods,
				Set<String> actions) {
			super();
			this.syncMethods = syncMethods;
			this.beginMethods = beginMethods;
			this.endMethods = endMethods;
			this.actions = actions;
		}

		public Map<String, InstrumentMethod> getSyncMethods() {
			return syncMethods;
		}

		public Map<String, InstrumentMethod> getBeginMethods() {
			return beginMethods;
		}

		public Map<String, InstrumentMethod> getEndMethods() {
			return endMethods;
		}

		public Set<String> getActions() {
			return actions;
		}
		
	}
	
}
