package cz.metacentrum.perun.rpc.serializer;

import cz.metacentrum.perun.core.api.*;
import cz.metacentrum.perun.core.api.exceptions.PerunException;
import cz.metacentrum.perun.core.api.exceptions.rt.PerunRuntimeException;
import cz.metacentrum.perun.core.api.exceptions.RpcException;
import cz.metacentrum.perun.taskslib.model.Task;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;

/**
 * JSON serializer.
 *
 * @author Jan Klos <ddd@mail.muni.cz>
 * @since 0.1
 */
public final class JsonSerializer implements Serializer {

	@JsonIgnoreProperties({"name"})
	private interface AttributeMixIn {
	}

	@JsonIgnoreProperties({"name"})
	private interface AttributeDefinitionMixIn {
	}

	@JsonIgnoreProperties({"commonName", "displayName"})
	private interface UserMixIn {
	}

	@JsonIgnoreProperties({"userExtSources"})
	private interface CandidateMixIn {
	}

	@JsonIgnoreProperties({"uimessage"})
	public interface AuditMessageMixIn {}

	@JsonIgnoreProperties({"cause", "localizedMessage", "stackTrace"})
	private interface PerunExceptionMixIn {
	}

	@SuppressWarnings("unused")
	private interface TaskMixIn {
		@JsonSerialize
		@JsonProperty(value = "startTime")
		Long getStartTimeAsLong();

		@JsonIgnore
		LocalDateTime getStartTime();

		@JsonSerialize
		@JsonProperty(value = "schedule")
		Long getScheduleAsLong();

		@JsonIgnore
		LocalDateTime getSchedule();

		@JsonSerialize
		@JsonProperty(value = "genEndTime")
		Long getGenEndTimeAsLong();

		@JsonIgnore
		LocalDateTime getGenEndTime();

		@JsonSerialize
		@JsonProperty(value = "sendEndTime")
		Long getSendEndTimeAsLong();

		@JsonIgnore
		LocalDateTime getSendEndTime();

		@JsonSerialize
		@JsonProperty(value = "sendStartTime")
		Long getSendStartTimeAsLong();

		@JsonIgnore
		LocalDateTime getSendStartTime();

		@JsonSerialize
		@JsonProperty(value = "genStartTime")
		Long getGenStartTimeAsLong();

		@JsonIgnore
		LocalDateTime getGenStartTime();

		@JsonSerialize
		@JsonProperty(value = "sentToEngine")
		Long getSentToEngineAsLong();

		@JsonIgnore
		LocalDateTime getSentToEngine();

		@JsonSerialize
		@JsonProperty(value = "endTime")
		Long getEndTimeAsLong();

		@JsonIgnore
		LocalDateTime getEndTime();
	}

	public static final String CONTENT_TYPE = "application/json; charset=utf-8";
	private static final ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.getSerializationConfig().addMixInAnnotations(Attribute.class, AttributeMixIn.class);
		mapper.getSerializationConfig().addMixInAnnotations(AttributeDefinition.class, AttributeDefinitionMixIn.class);
		mapper.getSerializationConfig().addMixInAnnotations(User.class, UserMixIn.class);
		mapper.getSerializationConfig().addMixInAnnotations(Candidate.class, CandidateMixIn.class);
		mapper.getSerializationConfig().addMixInAnnotations(PerunException.class, PerunExceptionMixIn.class);
		mapper.getSerializationConfig().addMixInAnnotations(PerunRuntimeException.class, PerunExceptionMixIn.class);
		mapper.getSerializationConfig().addMixInAnnotations(AuditMessage.class, AuditMessageMixIn.class);
		mapper.getSerializationConfig().addMixInAnnotations(Task.class, TaskMixIn.class);
	}

	private static final JsonFactory jsonFactory = new JsonFactory();

	static {
		//FIXME odstraneno disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)
		jsonFactory.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET).disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT).setCodec(mapper);
	}

	private OutputStream out;

	/**
	 * @param out {@code OutputStream} to output serialized data
	 * @throws IOException if an IO error occurs
	 */
	public JsonSerializer(OutputStream out) throws IOException {
		this.out = out;
	}

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

	@Override
	public void write(Object object) throws RpcException, IOException {
		JsonGenerator gen = jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);

		try {
			gen.writeObject(object);
			gen.flush();
			gen.close();
		} catch (JsonProcessingException ex) {
			throw new RpcException(RpcException.Type.CANNOT_SERIALIZE_VALUE, ex);
		}
	}

	@Override
	public void writePerunException(PerunException pex) throws IOException {

		JsonGenerator gen = jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);
		if (pex == null) {
			throw new IllegalArgumentException("pex is null");
		} else {
			gen.writeObject(pex);
			gen.flush();
		}
		gen.close();

	}

	@Override
	public void writePerunRuntimeException(PerunRuntimeException prex) throws IOException {

		JsonGenerator gen = jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);
		if (prex == null) {
			throw new IllegalArgumentException("prex is null");
		} else {
			gen.writeObject(prex);
			gen.flush();
		}
		gen.close();

	}
}
