/*
 * This is eMonocot, a global online biodiversity information resource.
 *
 * Copyright © 2011–2015 The Board of Trustees of the Royal Botanic Gardens, Kew and The University of Oxford
 *
 * eMonocot is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * eMonocot is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * The complete text of the GNU Affero General Public License is in the source repository as the file
 * ‘COPYING’.  It is also available from <http://www.gnu.org/licenses/>.
 */
package org.emonocot.model.constants;

/**
 *
 * @author ben
 *
 */
public enum ResourceType {
	DwC_Archive("DarwinCoreArchiveHarvesting"),
	IDENTIFICATION_KEY("IdentificationKeyHarvesting"),
	IUCN("IUCNImport"),
	GBIF("GBIFImport"),
	DOWNLOAD("Download"),
	PHYLOGENETIC_TREE("PhylogeneticTreeHarvesting");

	private String jobName;

    private ResourceType(String jobName) {
		this.jobName = jobName;
	}

	public String getJobName() {
		return jobName;
	}
}
