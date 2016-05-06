package com.navercorp.pinpoint.plugin.ice.interceptor;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanId;
import com.navercorp.pinpoint.bootstrap.context.SpanRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanSimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.plugin.ice.IceConstants;

import Ice.Current;

public class IceServerInterceptor extends SpanSimpleAroundInterceptor {

	public IceServerInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
		super(traceContext, methodDescriptor, IceServerInterceptor.class);
	}

	@Override
	protected void doInBeforeTrace(SpanRecorder recorder, Object target, Object[] args) {
		logger.info("开始trace");
		int length = args.length;
		Current current = (Current) args[length - 1];
		
		recorder.recordServiceType(IceConstants.ICE_SERVER);
        // Record rpc name, client address, server address.
        recorder.recordRpcName(current.id+"("+current.operation+":"+current.facet==null?"default":current.facet+")");
        recorder.recordEndPoint(IceConstants.META_AGENT_ID);
        recorder.recordRemoteAddress(current.ctx.get(IceConstants.META_PARENT_AGENT_ID));

        // If this transaction did not begin here, record parent(client who sent this request) information
        if (!recorder.isRoot()) {
            String parentApplicationName = current.ctx.get(IceConstants.META_PARENT_APPLICATION_NAME);

            if (parentApplicationName != null) {
                short parentApplicationType = NumberUtils.parseShort(current.ctx.get(IceConstants.META_PARENT_APPLICATION_TYPE), ServiceType.UNDEFINED.getCode());
                recorder.recordParentApplication(parentApplicationName, parentApplicationType);

                // Pinpoint finds caller - callee relation by matching caller's end point and callee's acceptor host.
                // https://github.com/naver/pinpoint/issues/1395
                recorder.recordAcceptorHost(IceConstants.META_AGENT_ID);
            }
        }

	}

	@Override
	protected Trace createTrace(Object target, Object[] args) {
		int length = args==null?0:args.length;
		logger.info("参数长度:"+length);
		//拦截的方法不对
		if(length<1){
			return disableTrace();
		}
		Current current = null;
		try {
			current = (Current) args[length - 1];
		} catch (Exception e) {
			//拦截的方法不对
			return disableTrace();
		}
		//拦截的方法没有current参数
		if (current == null) {
			return traceContext.newTraceObject();
		}
		//拦截的方法携带不进行跟踪的标志，放弃跟踪
		if (current.ctx.containsKey(IceConstants.META_DO_NOT_TRACE)) {
			return disableTrace();
		}
		logger.info("跟踪调用");
		String transactionId = current.ctx.get(IceConstants.META_TRANSACTION_ID);
		//拦截的transactionid为空
		if (transactionId == null) {
			logger.info("创建调用");
			return traceContext.newTraceObject();
		}

		logger.info("添加调用参数");
		long parentSpanID = NumberUtils.parseLong(current.ctx.get(IceConstants.META_PARENT_SPAN_ID),
				SpanId.NULL);
		long spanID = NumberUtils.parseLong(current.ctx.get(IceConstants.META_SPAN_ID), SpanId.NULL);
		short flags = NumberUtils.parseShort(current.ctx.get(IceConstants.META_FLAGS), (short) 0);
		TraceId traceId = traceContext.createTraceId(transactionId, parentSpanID, spanID, flags);

		return traceContext.continueTraceObject(traceId);
	}

	private Trace disableTrace() {
		traceContext.removeTraceObject();
		return traceContext.disableSampling();
	}

	@Override
	protected void doInAfterTrace(SpanRecorder recorder, Object target, Object[] args, Object result,
			Throwable throwable) {
		logger.info("结束trace");
        recorder.recordApi(methodDescriptor);
        recorder.recordAttribute(IceConstants.ICE_ARGS, args);

        if (throwable == null) {
            recorder.recordAttribute(IceConstants.ICE_RESULT, result);
        } else {
            recorder.recordException(throwable);
        }
	}

}
