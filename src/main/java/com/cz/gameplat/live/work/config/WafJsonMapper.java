package com.cz.gameplat.live.work.config;

import org.springframework.util.*;
import java.io.*;
import java.util.*;
import java.text.*;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.*;

public class WafJsonMapper
{
    private static ObjectMapper mapper;
    
    public static ObjectMapper getMapper() {
        return WafJsonMapper.mapper;
    }
    
    public static void setMapper(final ObjectMapper mapper) {
        WafJsonMapper.mapper = mapper;
    }
    
    public static <T> T parse(final String json, final Class<T> objectType) throws IOException {
        if (json == null) {
            return null;
        }
        Assert.notNull((Object)objectType, "objectType cannot be null.");
        return (T)WafJsonMapper.mapper.readValue(json, (Class)objectType);
    }
    
    public static <T> T parse(final InputStream stream, final Class<T> objectType) throws IOException {
        Assert.notNull((Object)stream, "stream cannot be null.");
        Assert.notNull((Object)objectType, "objectType cannot be null.");
        return (T)WafJsonMapper.mapper.readValue(stream, (Class)objectType);
    }
    
    public static String toJson(final Object obj) throws IOException {
        return WafJsonMapper.mapper.writeValueAsString(obj);
    }
    
    static {
        (WafJsonMapper.mapper = new ObjectMapper()).setTimeZone(TimeZone.getDefault());
        WafJsonMapper.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        WafJsonMapper.mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        WafJsonMapper.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        WafJsonMapper.mapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, false);
        WafJsonMapper.mapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        WafJsonMapper.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        WafJsonMapper.mapper.setDateFormat((DateFormat)new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        WafJsonMapper.mapper.setAnnotationIntrospector((AnnotationIntrospector)new JacksonAnnotationIntrospector());
    }
}
