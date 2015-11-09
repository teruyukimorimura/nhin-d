package org.nhindirect.config.resources;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.nhindirect.config.BaseTestPlan;
import org.nhindirect.config.ConfigServiceRunner;
import org.nhindirect.config.TestUtils;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.CertPolicy;
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.store.dao.CertPolicyDao;
import org.nhindirect.config.store.dao.DomainDao;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class CertPolicyResource_disassociatePolicyGroupFromDomainsTest 
{
	   protected CertPolicyDao policyDao;
	   protected DomainDao domainDao;  
	   
		static WebResource resource;
		
		abstract class TestPlan extends BaseTestPlan 
		{
			protected Collection<CertPolicyGroup> groups;
			
			protected Collection<CertPolicy> policies;
			
			@Override
			protected void setupMocks()
			{
				try
				{
					policyDao = (CertPolicyDao)ConfigServiceRunner.getSpringApplicationContext().getBean("certPolicyDao");
					domainDao =  (DomainDao)ConfigServiceRunner.getSpringApplicationContext().getBean("domainDao");
					
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

			
			protected  Collection<CertPolicyGroup> getGroupsToAdd()
			{
				try
				{
					groups = new ArrayList<CertPolicyGroup>();
					
					CertPolicyGroup group = new CertPolicyGroup();
					group.setPolicyGroupName("Group1");
					groups.add(group);
					
					group = new CertPolicyGroup();
					group.setPolicyGroupName("Group2");
					groups.add(group);
					
					return groups;
				}
				catch (Exception e)
				{
					throw new RuntimeException (e);
				}
			}
			
			
			protected  Domain getDomainToAdd()
			{
				final Address postmasterAddress = new Address();
				postmasterAddress.setEmailAddress("me@test.com");
				
				Domain domain = new Domain();
				
				domain.setDomainName("test.com");
				domain.setStatus(EntityStatus.ENABLED);
				domain.setPostmasterAddress(postmasterAddress);			
				
				return domain;
			}
			
			
			
			protected  String getGroupNameToAssociate()
			{
				return "Group1";
			}
			
			protected  String getDomainNameToAssociate()
			{
				return "test.com";
			}
			
			protected abstract String getGroupNameToDisassociate();
			
			
			@Override
			protected void performInner() throws Exception
			{				
				final Domain addDomain = getDomainToAdd();
				
				if (addDomain != null)
				{
					try
					{
						resource.path("/api/domain").entity(addDomain, MediaType.APPLICATION_JSON).put(addDomain);
					}
					catch (UniformInterfaceException e)
					{
						throw e;
					}
				}
				
				final Collection<CertPolicyGroup> groupsToAdd = getGroupsToAdd();
				
				if (groupsToAdd != null)
				{
					for (CertPolicyGroup addGroup : groupsToAdd)
					{
						try
						{
							resource.path("/api/certpolicy/groups").entity(addGroup, MediaType.APPLICATION_JSON).put(addGroup);
						}
						catch (UniformInterfaceException e)
						{
							throw e;
						}
					}
				}
				

				// associate the bundle and domain
				if (groupsToAdd != null && addDomain != null)
					resource.path("/api/certpolicy/groups/domain/" + TestUtils.uriEscape(getGroupNameToAssociate()) + 
						"/" + TestUtils.uriEscape(getDomainNameToAssociate())).post();
				
				// disassociate
				resource.path("/api/certpolicy/groups/domain/" + TestUtils.uriEscape(getGroupNameToDisassociate()) + "/deleteFromGroup").delete();

				doAssertions();
				
			}
				
			protected void doAssertions() throws Exception
			{
				
			}
		}
		
		@Test
		public void testDisassociatePolicyGroupFromDomains_assertGroupDomainDisassociated()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected String getGroupNameToDisassociate()
				{
					return "Group1";
				}
				
				
				@Override
				protected void doAssertions() throws Exception
				{
					final org.nhindirect.config.store.Domain domain = domainDao.getDomainByName(getDomainNameToAssociate());
					
					final Collection<org.nhindirect.config.store.CertPolicyGroupDomainReltn> reltns = policyDao.getPolicyGroupsByDomain(domain.getId());
					
					assertEquals(0, reltns.size());
				}
			}.perform();
		}	
		
		@Test
		public void testDisassociatePolicyGroupFromDomains_unknownGroup_assertNotFound()  throws Exception
		{
			new TestPlan()
			{

				@Override
				protected String getGroupNameToDisassociate()
				{
					return "Group4";
				}
				
				
				@Override
				protected void assertException(Exception exception) throws Exception 
				{
					assertTrue(exception instanceof UniformInterfaceException);
					UniformInterfaceException ex = (UniformInterfaceException)exception;
					assertEquals(404, ex.getResponse().getStatus());
				}
			}.perform();
		}		
		
		@Test
		public void testDisassociatePolicyGroupFromDomains_errorInGroupLookup_assertServiceError()  throws Exception
		{
			new TestPlan()
			{

				protected CertPolicyResource certService;
				
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();
						
						certService = (CertPolicyResource)ConfigServiceRunner.getSpringApplicationContext().getBean("certPolicyResource");

						CertPolicyDao mockDAO = mock(CertPolicyDao.class);
						doThrow(new RuntimeException()).when(mockDAO).getPolicyGroupByName((String)any());
						
						certService.setCertPolicyDao(mockDAO);
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
					
					certService.setCertPolicyDao(policyDao);
				}
				
				@Override
				protected  Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected  Domain getDomainToAdd()
				{
					return null;
				}
				
				@Override
				protected String getGroupNameToDisassociate()
				{
					return "Group1";
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
		
		@Test
		public void testDisassociatePolicyGroupFromDomains_errorInDisassociate_assertServiceError()  throws Exception
		{
			new TestPlan()
			{
				protected CertPolicyResource certService;
				
				@Override
				protected void setupMocks()
				{
					try
					{
						super.setupMocks();
						
						certService = (CertPolicyResource)ConfigServiceRunner.getSpringApplicationContext().getBean("certPolicyResource");

						CertPolicyDao mockPolicyDAO = mock(CertPolicyDao.class);
						DomainDao mockDomainDAO = mock(DomainDao.class);
						
						when(mockPolicyDAO.getPolicyGroupByName("Group1")).thenReturn(new org.nhindirect.config.store.CertPolicyGroup());
						doThrow(new RuntimeException()).when(mockPolicyDAO).disassociatePolicyGroupFromDomains(eq(0L));
						
						certService.setCertPolicyDao(mockPolicyDAO);
						certService.setDomainDao(mockDomainDAO);
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
					
					certService.setCertPolicyDao(policyDao);
					certService.setDomainDao(domainDao);
				}
				
				@Override
				protected  Collection<CertPolicyGroup> getGroupsToAdd()
				{
					return null;
				}
				
				@Override
				protected  Domain getDomainToAdd()
				{
					return null;
				}
				
				@Override
				protected String getGroupNameToDisassociate()
				{
					return "Group1";
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
