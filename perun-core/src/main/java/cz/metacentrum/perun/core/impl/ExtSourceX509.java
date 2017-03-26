/**
 *
 */
package cz.metacentrum.perun.core.impl;

import java.util.List;
import java.util.Map;

import cz.metacentrum.perun.core.api.Group;
import cz.metacentrum.perun.core.api.PerunSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.metacentrum.perun.core.api.ExtSource;
import cz.metacentrum.perun.core.api.exceptions.ExtSourceUnsupportedOperationException;
import cz.metacentrum.perun.core.implApi.ExtSourceSimpleApi;

/**
 * Dummy ExtSource - X.508
 *
 * @author Michal Prochazka michalp@ics.muni.cz
 */
public class ExtSourceX509 extends ExtSource implements ExtSourceSimpleApi {

	private final static Logger log = LoggerFactory.getLogger(ExtSourceX509.class);

	@Override
	public List<Map<String,String>> findSubjectsLogins(String searchString) throws ExtSourceUnsupportedOperationException {
		return findSubjectsLogins(searchString, 0);
	}

	@Override
	public List<Map<String, String>> findSubjectsLogins(String searchString, int maxResults) throws ExtSourceUnsupportedOperationException {
		throw new ExtSourceUnsupportedOperationException();
	}

	@Override
	public Map<String, String> getSubjectByLogin(String login) throws ExtSourceUnsupportedOperationException {
		throw new ExtSourceUnsupportedOperationException();
	}

	@Override
	public void close() throws ExtSourceUnsupportedOperationException {
		throw new ExtSourceUnsupportedOperationException();
	}

	public String getGroupSubjects(PerunSession sess, Group group, String status, List<Map<String, String>> subjects) throws ExtSourceUnsupportedOperationException {
		throw new ExtSourceUnsupportedOperationException();
	}

	@Override
	public List<Map<String, String>> getSubjectGroups(Map<String, String> attributes) throws ExtSourceUnsupportedOperationException {
		throw new ExtSourceUnsupportedOperationException();
	}
}
