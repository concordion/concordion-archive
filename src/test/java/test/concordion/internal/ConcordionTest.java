package test.concordion.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import nu.xom.Document;
import nu.xom.Nodes;

import org.apache.commons.io.FileUtils;
import org.concordion.Concordion;
import org.concordion.api.Resource;
import org.concordion.api.ResultSummary;
import org.concordion.internal.ConcordionBuilder;
import org.concordion.internal.XMLParser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import test.concordion.StubSource;
import test.concordion.StubTarget;

public class ConcordionTest {

	private static StubTarget target;
	private static StubSource source;

	private static final String NON_EXISTING_RESOURCE = "/does/not/exist";
	private static final String DUMMY_RESOURCE = "/mytest";
	private static final String BASEPATH = "src/test/resources/test/concordion/internal/";

	@Before
	public void setUp() {
		target = new StubTarget();
		source = new StubSource();
	}

	@Test
	public void testProcessFixtureIsNull() throws IOException {
		Concordion concordion = new ConcordionBuilder().build();
		Object fixture = null;
		try {
			concordion.process(fixture);
		} catch (RuntimeException e) {
			assertEquals("Fixture is null", e.getMessage());
		}
	}

	@Test
	public void testProcessResourceNotFound() {
		Concordion concordion = new ConcordionBuilder().build();
		Object fixture = null;
		Resource resource = new Resource(NON_EXISTING_RESOURCE);
		try {
			concordion.process(resource, fixture);
		} catch (IOException e) {
			assertEquals("Resource '" + NON_EXISTING_RESOURCE + "' not found",
					e.getMessage());
		}
	}

	@Test
	public void testProcessSuccess() throws IOException {
		String fileName = "HelloBob.html";
		addDocumentToSource(fileName, DUMMY_RESOURCE);
		Concordion concordion = createConcordion(source, target);
		
		HelloBobFixture fixture = new HelloBobFixture();
		Resource resource = new Resource(DUMMY_RESOURCE);
		ResultSummary summary = concordion.process(resource, fixture);
		
		assertSummaryNumbers(summary, 1, 0, 0);
		try {
			summary.assertIsSatisfied();
		} catch (AssertionError e) {
			String xml = target.getWrittenString(resource);
			System.err.println(xml);
			fail(e.getMessage());
		}
	}

	@Test
	public void testProcessFailure() throws IOException {
		String fileName = "HelloBob.html";
		addDocumentToSource(fileName, DUMMY_RESOURCE);
		Concordion concordion = createConcordion(source, target);
		
		Object fixture = new Object() {
			public String getGreeting() {
				return "Hello Bo!";
			}
		};
		Resource resource = new Resource(DUMMY_RESOURCE);
		ResultSummary summary = concordion.process(resource, fixture);
		
		assertSummaryNumbers(summary, 0, 1, 0);
		try {
			summary.assertIsSatisfied();
		} catch (AssertionError e) {
			String xml = target.getWrittenString(resource);
			// System.out.println(e.getMessage());
			// System.err.println(xml);
			Document dom = XMLParser.parse(xml);
			Nodes failures = dom.query("/html/body//span[@class='failure']");
			assertEquals(1, failures.size());
			assertEquals("Hello Bob!", failures.get(0).query(
					"del[@class='expected']").get(0).getValue());
			assertEquals("Hello Bo!", failures.get(0).query(
					"ins[@class='actual']").get(0).getValue());
		}
	}
	
	@Ignore
	@Test
	public void testProcessException() {
		
	}

	@Test
	public void testResourceFromFixtureClassName() throws IOException {
		String fileName = "empty.html";
		String resourceName = "/test/concordion/internal/ConcordionTest$EmptyFixture.html";
		addDocumentToSource(fileName, resourceName);
		
		Concordion concordion = createConcordion(source, target);
		EmptyFixture fixture = new EmptyFixture();
		ResultSummary summary = concordion.process(fixture);
		
		assertSummaryNumbers(summary, 0, 0, 0);
		try {
			summary.assertIsSatisfied();
		} catch (AssertionError e) {
			Resource resource = new Resource(resourceName);
			String xml = target.getWrittenString(resource);
			System.err.println(xml);
			fail(e.getMessage());
		}
	}

	@Test
	public void testResourcesWithLinks() throws IOException {
		String fileName = "EmptyWithLinks.html";
		String resourceName = "/test/concordion/internal/ConcordionTest$EmptyFixture.html";
		addDocumentToSource(fileName, resourceName);
		
		String fileName_S1 = "EmptyWithLinkToNowhere.html";
		String resourceName_S1 = "/test/concordion/internal/ConcordionTest$EmptyFixture_S1.html";
		addDocumentToSource(fileName_S1, resourceName_S1);

		Concordion concordion = createConcordion(source, target);
		EmptyFixture fixture = new EmptyFixture();
		ResultSummary summary = concordion.process(fixture);
		
		assertSummaryNumbers(summary, 0, 0, 0);

		EmptyFixture_S1 fixture_S1 = new EmptyFixture_S1();
		ResultSummary summary_S1 = concordion.process(fixture_S1);
		
		assertSummaryNumbers(summary_S1, 0, 1, 0);
		 
	}

	class EmptyFixture {

	}

	class EmptyFixture_S1 {
		public String getGreeting() {
			return "Hello Bo!";
		}
	}

	class HelloBobFixture {
		public String getGreeting() {
			return "Hello Bob!";
		}
	}
	
	private void assertSummaryNumbers(ResultSummary summary, int successCount,
			int failureCount, int exceptionCount) {
		assertEquals(successCount, summary.getSuccessCount());
		assertEquals(failureCount, summary.getFailureCount());
		assertEquals(exceptionCount, summary.getExceptionCount());
	}

	private Concordion createConcordion(StubSource source, StubTarget target) {
		return new ConcordionBuilder().withTarget(target).withSource(source)
				.build();
	}

	private void addDocumentToSource(String fileName, String resourceName) throws IOException {
		String document = FileUtils.readFileToString(new File(BASEPATH
				+ fileName));
		source.addResource(resourceName, document);
	}

}
