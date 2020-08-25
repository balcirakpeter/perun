package cz.metacentrum.perun.core.implApi;


import cz.metacentrum.perun.core.api.HashedGenData;

/**
 * @author Vojtech Sassmann <vojtech.sassmann@gmail.com>
 */
public interface HashedDataGenerator {

	/**
	 * Generated hashed data structure used for provisioning.
	 *
	 * @return hashed data structure
	 */
	HashedGenData generateData();
}
