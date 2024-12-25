package com.mgmresorts.booking.room.reservation.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class BaseUnitTest {

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public <T> T convert(String fileName, Class<T> target) {

		File file = new File(getClass().getResource(fileName).getPath());

		try {
			return MAPPER.readValue(file, target);
		} catch (IOException e) {
			log.debug("Exception trying to convert file to json", e);
		}
		return null;
	}

	public <T> T convertString(String json, Class<T> target) {

		try {
			return MAPPER.readValue(json, target);
		} catch (IOException e) {
			log.debug("Exception trying to convert file to json", e);
		}
		return null;
	}

	private String readFromInputStream(InputStream inputStream) throws IOException {
		StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = br.readLine()) != null) {
				resultStringBuilder.append(line).append("\n");
			}
		}
		return resultStringBuilder.toString();
	}

	public String getFileContents(String fileName) {

		String content = StringUtils.EMPTY;
		try {
			content = readFromInputStream(getClass().getResourceAsStream(fileName));
		} catch (IOException e) {
			log.debug("Reading reading files", e);
		}
		return content;
	}
}
