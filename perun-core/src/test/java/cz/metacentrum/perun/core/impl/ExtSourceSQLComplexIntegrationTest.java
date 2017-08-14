package cz.metacentrum.perun.core.impl;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import cz.metacentrum.perun.core.AbstractPerunIntegrationTest;
import cz.metacentrum.perun.core.api.*;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.bl.ExtSourcesManagerBl;
import cz.metacentrum.perun.core.blImpl.PerunBlImpl;
import cz.metacentrum.perun.core.implApi.ExtSourceApi;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.eq;

import cz.metacentrum.perun.core.api.exceptions.WrongAttributeValueException;
import cz.metacentrum.perun.core.api.exceptions.WrongReferenceAttributeValueException;
import cz.metacentrum.perun.core.impl.PerunSessionImpl;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtSourceSQLComplexIntegrationTest extends AbstractPerunIntegrationTest{

	private static ExtSourceSqlComplex classInstance;
	private static PerunSessionImpl session;
	private static final String GROUP_ID_QUERY = "someQuery";
	private static Map<String, String> nullMap;

	@Before
	public void setUp() {
		classInstance = new ExtSourceSqlComplex();
		session = mock(PerunSessionImpl.class, RETURNS_DEEP_STUBS);
		nullMap = new HashMap<>();
		nullMap.put("groupIDQuery", null);
		setPerun(session.getPerunBl());
	}

	@Test(expected=InternalErrorException.class)
	public void testGetGroupByLoginNullQuery() throws Exception{
		System.out.println("test GetGroupByLogin()");

		when(perun.getExtSourcesManagerBl().getAttributes(any(ExtSource.class))).thenReturn(nullMap);

		classInstance.getGroupByID("PV028");
	}

	@Test(expected=InternalErrorException.class)
	public void testFindGroupsNullQuery() throws Exception{
		System.out.println("test FindGroups()");

		when(perun.getExtSourcesManagerBl().getAttributes(any(ExtSource.class))).thenReturn(nullMap);

		classInstance.findGroups("PV028", 5);
	}

	//PRIVATE METHODS----------------------------------
	private void setUpMap(){

	}

}