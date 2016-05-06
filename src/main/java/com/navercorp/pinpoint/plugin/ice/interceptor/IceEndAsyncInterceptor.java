package com.navercorp.pinpoint.plugin.ice.interceptor;

import java.util.Map;

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

import Ice.ObjectPrxHelperBase;

public class IceEndAsyncInterceptor implements AroundInterceptor {

	private final PLogger logger = PLoggerFactory.getLogger(IceEndAsyncInterceptor.class);;
	private final MethodDescriptor descriptor;

	public IceEndAsyncInterceptor(MethodDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public void before(Object target, Object[] args) {
		this.logger.beforeInterceptor(target, target.getClass().getName(), this.descriptor.getMethodName(), "", args);
	}

	@Override
	public void after(Object target, Object[] args, Object result, Throwable throwable) {
		this.logger.afterInterceptor(target, target.getClass().getName(), this.descriptor.getMethodName(), "", args);
		if ((target instanceof AsyncAccessor)) {
			((AsyncAccessor) target)._$PINPOINT$_setAsync(Boolean.FALSE.booleanValue());
		}
		if ((target instanceof TraceAccessor)) {
			Trace trace = ((TraceAccessor) target)._$PINPOINT$_getTrace();
			if ((trace != null) && (trace.canSampled())) {
				SpanEventRecorder recorder = trace.currentSpanEventRecorder();
				if (throwable == null) {
					ObjectPrxHelperBase helper = (ObjectPrxHelperBase) target;
					String endPoint = helper.ice_getAdapterId();
					recorder.recordEndPoint(endPoint);
					recorder.recordDestinationId(endPoint);
					recorder.recordAttribute(IceConstants.ICE_RESULT, result);
				} else {
					recorder.recordException(throwable);
				}
				trace.close();
			}
			((TraceAccessor) target)._$PINPOINT$_setTrace(null);
		}
	}

}
