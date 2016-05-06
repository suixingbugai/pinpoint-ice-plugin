package com.navercorp.pinpoint.plugin.ice;

import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.trace.AnnotationKeyFactory;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.ServiceTypeFactory;
import com.navercorp.pinpoint.common.trace.ServiceTypeProperty;

public interface IceConstants {

	public static final ServiceType ICE_SERVER = ServiceTypeFactory.of(1120, "ICE_SERVER", ServiceTypeProperty.RECORD_STATISTICS);
	public static final ServiceType ICE_CLIENT = ServiceTypeFactory.of(9120, "ICE_CLIENT", ServiceTypeProperty.RECORD_STATISTICS);

	public static final AnnotationKey ICE_ARGS = AnnotationKeyFactory.of(96, "ice.args");
	public static final AnnotationKey ICE_RESULT = AnnotationKeyFactory.of(97, "ice.result");

	String META_DO_NOT_TRACE = "_ICE_DO_NOT_TRACE";
    String META_TRANSACTION_ID = "_ICE_TRASACTION_ID";
    String META_SPAN_ID = "_ICE_SPAN_ID";
    String META_PARENT_SPAN_ID = "_ICE_PARENT_SPAN_ID";
    String META_PARENT_APPLICATION_NAME = "_ICE_PARENT_APPLICATION_NAME";
    String META_PARENT_APPLICATION_TYPE = "_ICE_PARENT_APPLICATION_TYPE";
    String META_FLAGS = "_ICE_FLAGS";
    
    String META_AGENT_ID = System.getProperty("pinpoint.agentId");
    String META_PARENT_AGENT_ID = "_ICE_REQUEST_AGENT_ID";
    
}
