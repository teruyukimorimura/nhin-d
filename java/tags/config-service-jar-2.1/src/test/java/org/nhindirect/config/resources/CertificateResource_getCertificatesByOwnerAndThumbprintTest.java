package org.nhindirect.config.resources;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.ConfigServiceRunner;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.config.model.utils.CertUtils.CertContainer;
import org.nhindirect.config.store.dao.CertificateDao;
import org.nhindirect.stagent.cert.Thumbprint;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class CertificateResource_getCertificatesByOwnerAndThumbprintTest 
{
	   protected CertificateDao certDao;
	    
	   static WebResource resource;
		
		abstract class TestPlan extends BaseTestPlan 
		{
			@Override
			protected void setupMocks()
			{
				try
				{
					certDao = (CertificateDao)ConfigServiceRunner.getSpringApplicationContext().getBean("certificateDao");
					
					resource = 	getResource(ConfigServiceRunner.getConfigServiceURL());		
				}
				catch (Throwable t)
				{
					throw new RuntimeException(t);
				}
			}
			
			@Override
			protected void tearDownMocks()
			{

			}

			protected abstract Collection<Certificate> getCertsToAdd();

			protected abstract String getOwnerToRetrieve();
			
			protected abstract String getTPToRetrieve() throws Exception;
			
			@Override
			protected void performInner() throws Exception
			{				
				
				final Collection<Certificate> certsToAdd = getCertsToAdd();
				
				if (certsToAdd != null)
				{
					for (Certificate addCert : certsToAdd)
					{
						try
						{
							resource.path("/api/certificate").entity(addCert, MediaType.APPLICATION_JSON).put(addCert);
						}
						catch (UniformInterfaceException e)
						{
							throw e;
						}
					}
				}
				
				try
				{
					final Certificate getCertificate = 
							resource.path("/api/certificate/" + TestUtils.uriEscape(getOwnerToRetrieve()) +
									"/" + TestUtils.uriEscape(getTPToRetrieve())).get(Certificate.class);

					doAssertions(getCertificate);
				}
				catch (UniformInterfaceException e)
				{
					if (e.getResponse().getStatus() == 404)
						doAssertions(null);
					else
						throw e;
				}
				
			}
				
			protected void doAssertions(Certificate cert) throws Exception
			{
				
			}
	  }

		@Test
		public void testGetCertificatesByOwnerAndThumbrint_assertCertRetrieved() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Certificate> certs;
				
				@Override
				protected Collection<Certificate> getCertsToAdd()
				{
					try
					{
						certs = new ArrayList<Certificate>();
						
						Certificate cert = new Certificate();					
						cert.setData(TestUtils.loadCert("gm2552.der").getEncoded());
						
						certs.add(cert);
			
						cert = new Certificate();					
						cert.setData(TestUtils.loadCert("umesh.der").getEncoded());
						
						certs.add(cert);
						
						return certs;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}

				@Override
				protected String getOwnerToRetrieve()
				{
					return "gm2552@securehealthemail.com";
				}
				
				protected String getTPToRetrieve() throws Exception
				{
					return Thumbprint.toThumbprint(TestUtils.loadCert("gm2552.der")).toString();
				}
				
				@Override
				protected void doAssertions(Certificate retrievedCert) throws Exception
				{
					assertNotNull(retrievedCert);
					
					Certificate addedCert = new Certificate();					
					addedCert.setData(TestUtils.loadCert("gm2552.der").getEncoded());

					final X509Certificate retrievedX509Cert = CertUtils.toX509Certificate(retrievedCert.getData());
					final X509Certificate addedX509Cert = CertUtils.toX509Certificate(addedCert.getData());
					
					assertEquals(CertUtils.getOwner(addedX509Cert), retrievedCert.getOwner());
					assertEquals(Thumbprint.toThumbprint(addedX509Cert).toString(), retrievedCert.getThumbprint());
					assertEquals(retrievedX509Cert, addedX509Cert);
					assertEquals(EntityStatus.NEW, retrievedCert.getStatus());
					assertEquals(addedX509Cert.getNotAfter(), retrievedCert.getValidEndDate().getTime());
					assertEquals(addedX509Cert.getNotBefore(), retrievedCert.getValidStartDate().getTime());

				}
			}.perform();
		}				
		
		@Test
		public void testGetCertificatesByOwnerAndThumbrint_wrappedKey_assertCertRetrieved() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Certificate> certs;
				
				@Override
				protected Collection<Certificate> getCertsToAdd()
				{
					try
					{
						certs = new ArrayList<Certificate>();
						
						Certificate cert = new Certificate();	
						byte[] keyData = FileUtils.readFileToByteArray(new File("./src/test/resources/certs/gm2552Key.der"));
						
						cert.setData(CertUtils.certAndWrappedKeyToRawByteFormat(keyData, TestUtils.loadCert("gm2552.der")));
						
						certs.add(cert);
			
						
						return certs;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}

				@Override
				protected String getOwnerToRetrieve()
				{
					return "gm2552@securehealthemail.com";
				}
				
				protected String getTPToRetrieve() throws Exception
				{
					return Thumbprint.toThumbprint(TestUtils.loadCert("gm2552.der")).toString();
				}
				
				@Override
				protected void doAssertions(Certificate retrievedCert) throws Exception
				{
					assertNotNull(retrievedCert);
					
					Certificate addedCert = new Certificate();					
					addedCert.setData(TestUtils.loadCert("gm2552.der").getEncoded());

					final X509Certificate retrievedX509Cert = CertUtils.toX509Certificate(retrievedCert.getData());
					final X509Certificate addedX509Cert = CertUtils.toX509Certificate(addedCert.getData());
					final CertContainer cont = CertUtils.toCertContainer(retrievedCert.getData());
					assertNotNull(cont.getWrappedKeyData());
					
					assertEquals(CertUtils.getOwner(addedX509Cert), retrievedCert.getOwner());
					assertEquals(Thumbprint.toThumbprint(addedX509Cert).toString(), retrievedCert.getThumbprint());
					assertEquals(retrievedX509Cert, addedX509Cert);
					assertEquals(EntityStatus.NEW, retrievedCert.getStatus());
					assertEquals(addedX509Cert.getNotAfter(), retrievedCert.getValidEndDate().getTime());
					assertEquals(addedX509Cert.getNotBefore(), retrievedCert.getValidStartDate().getTime());

				}
			}.perform();
		}	
		
		@Test
		public void testGetCertificatesByOwnerAndThumbrint_TPNotFound_assertCertNotRetrieved() throws Exception
		{
			new TestPlan()
			{
				protected Collection<Certificate> certs;
				
				@Override
				protected Collection<Certificate> getCertsToAdd()
				{
					try
					{
						certs = new ArrayList<Certificate>();
						
						Certificate cert = new Certificate();					
						cert.setData(TestUtils.loadCert("gm2552.der").getEncoded());
						
						certs.add(cert);
			
						cert = new Certificate();					
						cert.setData(TestUtils.loadCert("umesh.der").getEncoded());
						
						certs.add(cert);
						
						return certs;
					}
					catch (Exception e)
					{
						throw new RuntimeException (e);
					}
				}

				@Override
				protected String getOwnerToRetrieve()
				{
					return "gm2552@securehealthemail.com";
				}
				
				protected String getTPToRetrieve() throws Exception
				{
					return "12345";
				}
				
				@Override
				protected void doAssertions(Certificate retrievedCert) throws Exception
				{
					assertNull(retrievedCert);				

				}
			}.perform();
		}		
	
		@Test
		public void testGetCertificatesByOwnerAndThumbprint_errorInLookup_assertServerError() throws Exception
		{
			new TestPlan()
			{
				
				protected CertificateResource certService;
				
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();
						
						certService = (CertificateResource)ConfigServiceRunner.getSpringApplicationContext().getBean("certificateResource");

						CertificateDao mockDAO = mock(CertificateDao.class);
						doThrow(new RuntimeException()).when(mockDAO).load((String)any(), (String)any());
						
						certService.setCertificateDao(mockDAO);
					}
					catch (Throwable t)
					{
						throw new RuntimeException(t);
					}
				}
				
				@Override
				protected void tearDownMocks()
				{
					super.tearDownMocks();
					
					certService.setCertificateDao(certDao);
				}
				
				@Override
				protected Collection<Certificate> getCertsToAdd()
				{
					return null;
				}

				@Override
				protected String getOwnerToRetrieve()
				{
					return "gm2554345432@securehealthemail.com";
				}
				
				protected String getTPToRetrieve() throws Exception
				{
					return "12345";
				}

				@Override
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof UniformInterfaceException);
					UniformInterfaceException ex = (UniformInterfaceException)exception;
					assertEquals(500, ex.getResponse().getStatus());
				}
			}.perform();
		}	
}
