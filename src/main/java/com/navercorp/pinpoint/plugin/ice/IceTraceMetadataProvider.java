package com.navercorp.pinpoint.plugin.ice;

import com.navercorp.pinpoint.common.trace.TraceMetadataProvider;
import com.navercorp.pinpoint.common.trace.TraceMetadataSetupContext;

public class IceTraceMetadataProvider implements TraceMetadataProvider {

	@Override
	public void setup(TraceMetadataSetupContext context) {
		context.addServiceType(IceConstants.ICE_SERVER);
		context.addServiceType(IceConstants.ICE_CLIENT);
		context.addAnnotationKey(IceConstants.ICE_ARGS);
		context.addAnnotationKey(IceConstants.ICE_RESULT);
	}

}
