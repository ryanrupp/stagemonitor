package org.stagemonitor.requestmonitor;

import java.util.HashMap;
import java.util.Map;

public class MonitoredMethodRequest implements MonitoredRequest<RequestTrace> {

	private final String methodSignature;
	private final MethodExecution methodExecution;
	private final Object[] parameters;

	public MonitoredMethodRequest(String methodSignature, MethodExecution methodExecution, Object... parameters) {
		this.methodSignature = methodSignature;
		this.methodExecution = methodExecution;
		this.parameters = parameters;
	}

	@Override
	public String getInstanceName() {
		return null;
	}

	@Override
	public RequestTrace createRequestTrace() {
		RequestTrace requestTrace = new RequestTrace(null, new RequestTrace.GetNameCallback() {
			@Override
			public String getName() {
				return methodSignature;
			}
		});

		if (parameters != null && parameters.length > 0) {
			Map<String, String> params = new HashMap<String, String>();
			for (int i = 0; i < parameters.length; i++) {
				params.put(Integer.toString(i), String.valueOf(parameters[i]));
			}
			requestTrace.setParameters(params);
		}
		return requestTrace;
	}

	@Override
	public Object execute() throws Exception {
		return methodExecution.execute();
	}

	@Override
	public void onPostExecute(RequestMonitor.RequestInformation<RequestTrace> requestTrace) {
	}

	/**
	 * In a Method execution context, we only want to monitor the topmost monitored (forwarding) method call.
	 * <p/>
	 * Example:<br/>
	 * Suppose, we have three methods: monitored1(), monitored2() and notMonitored().
	 * <pre><code>
	 * public void monitored1() {
	 *     monitored2();
	 * }
	 * public void monitored2() {
	 *     notMonitored();
	 * }
	 * public void notMonitored() {}
	 * </code></pre>
	 * The first two Methods are monitored with a {@link RequestMonitor}.<br/>
	 * If method1() is called, we only want to collect metrics for method1() and not for method2().<br/>
	 * If method2() is called, we want to collect metrics for that method.<br/>
	 * If notMonitored() is called directly, we don't want to collect metrics.
	 *
	 * @return false
	 */
	@Override
	public boolean isMonitorForwardedExecutions() {
		return false;
	}

	public interface MethodExecution {
		Object execute() throws Exception;
	}
}
