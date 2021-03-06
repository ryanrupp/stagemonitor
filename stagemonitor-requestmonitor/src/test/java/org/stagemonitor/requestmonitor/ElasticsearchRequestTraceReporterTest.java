package org.stagemonitor.requestmonitor;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchRequestTraceReporterTest {

	private ElasticsearchRequestTraceReporter reporter;
	private ElasticsearchClient elasticsearchClient;
	private RequestMonitorPlugin requestMonitorPlugin;

	@Before
	public void setUp() throws Exception {
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(Integer.MAX_VALUE);
		when(requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch()).thenReturn(Collections.singleton("Report Me"));
		final CorePlugin corePlugin = mock(CorePlugin.class);
		when(corePlugin.getElasticsearchUrl()).thenReturn("http://localhost:9200");
		elasticsearchClient = mock(ElasticsearchClient.class);
		reporter = new ElasticsearchRequestTraceReporter(corePlugin, requestMonitorPlugin, elasticsearchClient);
	}

	@Test
	public void testReportRequestTrace() throws Exception {
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Report Me");

		reporter.reportRequestTrace(requestTrace);

		verify(elasticsearchClient).index(anyString(), anyString(), anyObject());
		Assert.assertTrue(reporter.isActive(requestTrace));
	}

	@Test
	public void testReportRequestTraceDontReport() throws Exception {
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Regular Foo");

		reporter.reportRequestTrace(requestTrace);

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
		Assert.assertTrue(reporter.isActive(requestTrace));
	}

	@Test
	public void testElasticsearchReportingDeactive() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(0);
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Report Me");

		reporter.reportRequestTrace(requestTrace);

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
	}

	@Test
	public void testElasticsearchReportingRateLimited() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1);
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Report Me");

		reporter.reportRequestTrace(requestTrace);
		Thread.sleep(5010); // the meter only updates every 5 seconds
		reporter.reportRequestTrace(requestTrace);

		verify(elasticsearchClient, times(1)).index(anyString(), anyString(), anyObject());
	}
}