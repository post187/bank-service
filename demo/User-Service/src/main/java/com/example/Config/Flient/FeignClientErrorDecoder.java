package com.example.Config.Flient;

import com.example.Exception.GlobalException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;


public class FeignClientErrorDecoder implements ErrorDecoder {
    private static final Logger log = LoggerFactory.getLogger(FeignClientErrorDecoder.class);

    @Override
    public Exception decode(String s, Response response) {
        GlobalException globalException = extractGlobalException(response);
        switch (response.status()) {
            case 400 -> {
                log.error("global exception is handled by feign client");
                return globalException;
            }
            default -> {
                log.error("common exception thrown");
                return new Exception();
            }
        }
    }

    private GlobalException extractGlobalException(Response response) {
        GlobalException globalException = null;
        Reader reader = null;

        try {
            reader = response.body().asReader(StandardCharsets.UTF_8);
            String result = IOUtils.toString(reader);

            log.error(result);

            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            globalException = mapper.readValue(result, GlobalException.class);
            log.error(globalException.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return globalException;
    }
}
