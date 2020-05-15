package org.jobrunr.utils.mapper.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.lang.reflect.Type;

public class JobParameterDeserializer implements JsonDeserializer<JobParameter> {

    @Override
    public JobParameter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String jobParameterType = jsonObject.get("className").getAsString();
        return new JobParameter(jobParameterType, deserializeToObject(context, jobParameterType, jsonObject.get("object")));
    }

    private Object deserializeToObject(JsonDeserializationContext context, String type, JsonElement jsonElement) {
        return context.deserialize(jsonElement, ReflectionUtils.toClass(type));
    }
}