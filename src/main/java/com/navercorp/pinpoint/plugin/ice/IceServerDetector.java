package com.navercorp.pinpoint.plugin.ice;

import com.navercorp.pinpoint.bootstrap.plugin.ApplicationTypeDetector;
import com.navercorp.pinpoint.bootstrap.resolver.ConditionProvider;
import com.navercorp.pinpoint.common.trace.ServiceType;

import Ice.Application;

public class IceServerDetector implements ApplicationTypeDetector {

	@Override
	public ServiceType getApplicationType() {
		return IceConstants.ICE_SERVER;
	}

	@Override
	public boolean detect(ConditionProvider paramConditionProvider) {
		return paramConditionProvider.checkForClass(Application.class.getName());
	}

}
