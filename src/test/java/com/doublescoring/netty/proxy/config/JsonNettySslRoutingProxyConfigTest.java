package com.doublescoring.netty.proxy.config;

import com.doublescoring.netty.proxy.config.rules.ChainingRoutingRule;
import com.doublescoring.netty.proxy.config.rules.ExplicitRoutingRule;
import com.doublescoring.netty.proxy.config.rules.IntermediateCertificateRoutingRule;
import com.doublescoring.netty.proxy.config.rules.X509SubjectContainsStringRoutingRule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JsonNettySslRoutingProxyConfigTest {

	@Test
	public void testConfig() throws Exception {
		NettySslRoutingProxyConfig config = JsonNettySslRoutingProxyConfig.parse(
				JsonNettySslRoutingProxyConfigTest.class.getResource("config.json").getPath(),
				new File(JsonNettySslRoutingProxyConfigTest.class.getResource("config.json").getPath()).getParentFile());
		assertEquals(443, config.getBindPort());
		assertEquals("0.0.0.0", config.getBindHost());
		assertNotNull(config.getRoutingRule());
		assertEquals(ChainingRoutingRule.class, config.getRoutingRule().getClass());
		ChainingRoutingRule chain = (ChainingRoutingRule) config.getRoutingRule();
		RoutingRule[] rules = chain.getRules();
		assertEquals(3, rules.length);

		assertEquals(X509SubjectContainsStringRoutingRule.class, rules[0].getClass());
		X509SubjectContainsStringRoutingRule x509Rule = (X509SubjectContainsStringRoutingRule) rules[0];
		assertEquals("match", x509Rule.getPattern());
		assertEquals(new RoutingTarget("localhost", 123), x509Rule.getTarget());
		assertEquals(IntermediateCertificateRoutingRule.class, rules[1].getClass());
		IntermediateCertificateRoutingRule intermediateRule = (IntermediateCertificateRoutingRule) rules[1];
		assertEquals("CN=test.example.com", intermediateRule.getCaSubject());
		assertEquals(new RoutingTarget("localhost", 456), intermediateRule.getTarget());
		assertEquals(ExplicitRoutingRule.class, rules[2].getClass());
		ExplicitRoutingRule expRule = (ExplicitRoutingRule) rules[2];
		assertEquals(new RoutingTarget("localhost", 789), expRule.getTarget());

		assertNotNull(config.getTrustedCertificates());
		assertEquals(2, config.getTrustedCertificates().size());
		assertTrue(config.getTrustedCertificates().get(0).getSubjectDN().getName()
				.contains("CN=ca.example.com"));
		assertTrue(config.getTrustedCertificates().get(1).getSubjectDN().getName()
				.contains("CN=intermediate.ca.example.com"));

		assertNotNull(config.getKeyMaterialSource().getCertificateChain());
		assertEquals(2, config.getKeyMaterialSource().getCertificateChain().length);
		assertTrue(config.getKeyMaterialSource().getCertificateChain()[0].getSubjectDN().getName()
				.contains("CN=server.example.com"));
		assertTrue(config.getKeyMaterialSource().getCertificateChain()[1].getSubjectDN().getName()
				.contains("CN=ca.example.com"));
	}
}