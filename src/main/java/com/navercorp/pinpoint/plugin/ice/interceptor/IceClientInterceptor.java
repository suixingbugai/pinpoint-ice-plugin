package com.navercorp.pinpoint.plugin.ice.interceptor;

import java.util.HashMap;
import java.util.Map;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.plugin.ice.IceConstants;

import Ice.ObjectPrxHelperBase;

public class IceClientInterceptor implements AroundInterceptor {

	private final PLogger logger = PLoggerFactory.getLogger(IceClientInterceptor.class);;
	private final MethodDescriptor descriptor;
	private final TraceContext traceContext;

	public IceClientInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
		this.descriptor = descriptor;
		this.traceContext = traceContext;
	}

	@Override
	public void before(Object target, Object[] args) {
		logger.info("开始trace");
		for(Object arg:args){
			logger.info(String.valueOf(arg));
		}
		
		if(args[args.length-2] == null){
			logger.info("map is null");
		}
		if(args[args.length-2] instanceof Map){
			logger.info("is map...");
		}
		Map<String,String> ctx = (Map<String, String>) args[args.length-2];
		if(ctx == null){
			ctx = new HashMap<>();
		}
		Trace trace = traceContext.currentTraceObject();
		if (trace == null) {
			logger.info("trace == null");
			return;
		}
		
		if (trace.canSampled()) {
			logger.info("添加trace参数");
			SpanEventRecorder recorder = trace.traceBlockBegin();

			// RPC call trace have to be recorded with a service code in RPC
			// client code range.
			recorder.recordServiceType(IceConstants.ICE_CLIENT);

			// You have to issue a TraceId the receiver of this request will
			// use.
			TraceId nextId = trace.getTraceId().getNextTraceId();

			// Then record it as next span id.
			recorder.recordNextSpanId(nextId.getSpanId());

			// Finally, pass some tracing data to the server.
			// How to put them in a message is protocol specific.
			// This example assumes that the target protocol message can include
			// any metadata (like HTTP headers).
			ctx.put(IceConstants.META_TRANSACTION_ID, nextId.getTransactionId());
			ctx.put(IceConstants.META_SPAN_ID, Long.toString(nextId.getSpanId()));
			ctx.put(IceConstants.META_PARENT_SPAN_ID, Long.toString(nextId.getParentSpanId()));
			ctx.put(IceConstants.META_PARENT_APPLICATION_TYPE,
					Short.toString(traceContext.getServerTypeCode()));
			ctx.put(IceConstants.META_PARENT_APPLICATION_NAME, traceContext.getApplicationName());
			ctx.put(IceConstants.META_FLAGS, Short.toString(nextId.getFlags()));
			ctx.put(IceConstants.META_PARENT_AGENT_ID, IceConstants.META_AGENT_ID);
		} else {
			logger.info("trace is disabled");
			ctx.put(IceConstants.META_DO_NOT_TRACE, "1");
		}
		logger.info("trace info:"+ctx);
	}

	@Override
	public void after(Object target, Object[] args, Object result, Throwable throwable) {
		logger.info("结束trace");
		for(Object arg:args){
			logger.info(String.valueOf(arg));
		}
		
		Trace trace =  traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        try {
            SpanEventRecorder recorder = trace.currentSpanEventRecorder();

            recorder.recordApi(descriptor);

            if (throwable == null) {
            	ObjectPrxHelperBase helper = (ObjectPrxHelperBase) target; 
            	String endPoint = helper.ice_getAdapterId();
                // RPC client have to record end point (server address)
                recorder.recordEndPoint(endPoint);

                // Optionally, record the destination id (logical name of server. e.g. DB name)
                recorder.recordDestinationId(endPoint);
                recorder.recordAttribute(IceConstants.ICE_ARGS, args);
                logger.info("trace context:"+(Map<String, String>) args[args.length-2]);
                recorder.recordAttribute(IceConstants.ICE_RESULT, result);
            } else {
                recorder.recordException(throwable);
            }
        } finally {
            trace.traceBlockEnd();
        }
	}

}
