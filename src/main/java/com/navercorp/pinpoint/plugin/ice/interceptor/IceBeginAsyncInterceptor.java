package com.navercorp.pinpoint.plugin.ice.interceptor;

import java.util.HashMap;
import java.util.Map;

import com.navercorp.pinpoint.bootstrap.async.AsyncTraceIdAccessor;
import com.navercorp.pinpoint.bootstrap.context.AsyncTraceId;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.plugin.ice.AsyncAccessor;
import com.navercorp.pinpoint.plugin.ice.IceConstants;
import com.navercorp.pinpoint.plugin.ice.TraceAccessor;

public class IceBeginAsyncInterceptor implements AroundInterceptor {

	private final PLogger logger = PLoggerFactory.getLogger(IceBeginAsyncInterceptor.class);;
	private final MethodDescriptor descriptor;
	private final TraceContext traceContext;

	public IceBeginAsyncInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
		this.descriptor = descriptor;
		this.traceContext = traceContext;
	}

	@Override
	public void before(Object target, Object[] args) {
		this.logger.beforeInterceptor(target, target.getClass().getName(), this.descriptor.getMethodName(), "", args);
		Trace trace = this.traceContext.currentTraceObject();
		if (trace == null) {
			return;
		}

		if (args[args.length - 2] == null) {
			logger.info("map is null");
		}
		if (args[args.length - 2] instanceof Map) {
			logger.info("is map...");
		}
		Map<String, String> ctx = (Map<String, String>) args[args.length - 3];
		if (ctx == null) {
			ctx = new HashMap<>();
		}

		if (trace.canSampled()) {

			logger.info("添加trace参数");
			SpanEventRecorder recorder = trace.traceBlockBegin();

			recorder.recordServiceType(IceConstants.ICE_CLIENT);
			recorder.recordApi(descriptor);
			recorder.recordAttribute(IceConstants.ICE_ARGS, args);

			AsyncTraceId asyncTraceId = trace.getAsyncTraceId();
			recorder.recordNextAsyncId(asyncTraceId.getAsyncId());

			TraceId nextId = asyncTraceId.getNextTraceId();

			// Then record it as next span id.
			recorder.recordNextSpanId(nextId.getSpanId());

			// Finally, pass some tracing data to the server.
			// How to put them in a message is protocol specific.
			// This example assumes that the target protocol message can include
			// any metadata (like HTTP headers).
			ctx.put(IceConstants.META_TRANSACTION_ID, nextId.getTransactionId());
			ctx.put(IceConstants.META_SPAN_ID, Long.toString(nextId.getSpanId()));
			ctx.put(IceConstants.META_PARENT_SPAN_ID, Long.toString(nextId.getParentSpanId()));
			ctx.put(IceConstants.META_PARENT_APPLICATION_TYPE, Short.toString(traceContext.getServerTypeCode()));
			ctx.put(IceConstants.META_PARENT_APPLICATION_NAME, traceContext.getApplicationName());
			ctx.put(IceConstants.META_FLAGS, Short.toString(nextId.getFlags()));
			ctx.put(IceConstants.META_PARENT_AGENT_ID, IceConstants.META_AGENT_ID);
		} else {
			logger.info("trace is disabled");
			ctx.put(IceConstants.META_DO_NOT_TRACE, "1");
		}
		logger.info("trace info:" + ctx);
	}

	@Override
	public void after(Object target, Object[] args, Object result, Throwable throwable) {
		this.logger.afterInterceptor(target, "", this.descriptor.getMethodName(), "", args);

		Trace trace = traceContext.currentTraceObject();
		if (trace == null) {
			return;
		}

		SpanEventRecorder recorder = trace.currentSpanEventRecorder();

		if (validate(target, result, throwable)) {
			((AsyncAccessor) target)._$PINPOINT$_setAsync(Boolean.TRUE.booleanValue());
			((TraceAccessor) target)._$PINPOINT$_setTrace(trace);
		} else {
			recorder.recordException(throwable);
		}
		trace.traceBlockEnd();
	}

	private boolean validate(Object target, Object result, Throwable throwable) {
		if ((throwable != null) || (result == null)) {
			return false;
		}
		if (!(target instanceof AsyncAccessor)) {
			this.logger.debug("Invalid target object. Need field accessor({}).", AsyncAccessor.class.getName());
			return false;
		}
		if (!(result instanceof AsyncTraceIdAccessor)) {
			this.logger.debug("Invalid target object. Need metadata accessor({}).",
					AsyncTraceIdAccessor.class.getName());
			return false;
		}
		return true;
	}

}
