package com.navercorp.pinpoint.plugin.ice;

import com.navercorp.pinpoint.bootstrap.context.Trace;

public abstract interface TraceAccessor {

	public abstract void _$PINPOINT$_setTrace(Trace paramTrace);

	public abstract Trace _$PINPOINT$_getTrace();

}
