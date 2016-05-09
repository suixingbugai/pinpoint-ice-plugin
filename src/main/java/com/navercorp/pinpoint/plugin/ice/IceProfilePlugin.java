package com.navercorp.pinpoint.plugin.ice;

import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.matcher.ClassMatcher;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.plugin.ice.IceTraceUtil.InstrumentMethodMetaData;
import com.navercorp.pinpoint.profiler.ClassFileFilter;

public class IceProfilePlugin implements ProfilerPlugin, TransformTemplateAware {

	private final PLogger logger = PLoggerFactory.getLogger(getClass());
	private TransformTemplate transformTemplate;

	@Override
	public void setup(ProfilerPluginSetupContext context) {
		context.addApplicationTypeDetector(new IceServerDetector());
		this.addTransformers();
	}

	private void addTransformers() {
		// server
		List<String> servers = new ArrayList<>();
		servers.addAll(IceTraceUtil.getServerInstrumentClasses());
		for (String serverClass : servers) {
			logger.info("add server class:" + serverClass);
			transformTemplate.transform(serverClass, new TransformCallback() {
				@Override
				public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className,
						Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
								throws InstrumentException {
					InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
					if(!target.hasDeclaredMethod("ice_id")){
						for (InstrumentMethod m : IceTraceUtil.getServerInstrumentMethods(target,className)) {
							m.addInterceptor("com.navercorp.pinpoint.plugin.ice.interceptor.IceServerInterceptor");
						}
					}
						
					return target.toBytecode();
				}
			});
		}
		// client
		List<String> clients = new ArrayList<>();
		clients.addAll(IceTraceUtil.getClientInstrumentClasses());
//		clients.add("Demo.testPrxHelper");
//		clients.add("com.founder.ice.slice.Demo2.TestPrxHelper");
		for (String clientClass : clients) {
			logger.info("add client class:" + clientClass);
			transformTemplate.transform(clientClass, new TransformCallback() {
				@Override
				public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className,
						Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
								throws InstrumentException {
					InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
					InstrumentMethodMetaData metaData = IceTraceUtil.getClientMethods(target,className);
					
					target.addField("com.navercorp.pinpoint.plugin.ice.AsyncAccessor");
					target.addField("com.navercorp.pinpoint.plugin.ice.TraceAccessor");
					
					for (InstrumentMethod m : metaData.getSyncMethods().values()) {
						m.addInterceptor("com.navercorp.pinpoint.plugin.ice.interceptor.IceClientInterceptor");
					}
					for (InstrumentMethod m : metaData.getBeginMethods().values()) {
						m.addInterceptor("com.navercorp.pinpoint.plugin.ice.interceptor.IceBeginAsyncInterceptor");
					}
					for (InstrumentMethod m : metaData.getEndMethods().values()) {
						m.addInterceptor("com.navercorp.pinpoint.plugin.ice.interceptor.IceEndAsyncInterceptor");
					}
					return target.toBytecode();
				}
			});
		}
	}

	@Override
	public void setTransformTemplate(TransformTemplate template) {
		this.transformTemplate = template;
	}

}
