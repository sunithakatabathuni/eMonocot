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
package org.emonocot.model.marshall.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.batch.core.JobExecution;

/**
 *
 * @author ben
 *
 */
public class JobExecutionSerializer extends JsonSerializer<JobExecution> {

	@Override
	public final Class<JobExecution> handledType() {
		return JobExecution.class;
	}

	@Override
	public final void serialize(final JobExecution jobExecution,
			final JsonGenerator jsonGenerator,
			final SerializerProvider serializerProvider) throws IOException {
		jsonGenerator.writeStartObject();
		jsonGenerator.writeFieldName("id");
		jsonGenerator.writeNumber(jobExecution.getId());
		jsonGenerator.writeFieldName("jobInstance");
		jsonGenerator.writeNumber(jobExecution.getJobId());
		jsonGenerator.writeEndObject();
	}

}
