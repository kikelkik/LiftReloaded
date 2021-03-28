package com.minecraftcorp.lift.common.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.minecraftcorp.lift.common.exception.ConfigurationException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ConfigReader {

	public void mapConfigurationToFields(Set<String> configKeys, Object object,
			BiFunction<String, Class<?>, Object> valueResolver, Class<?> clazz) {

		Map<String, Field> classFields = Arrays.stream(clazz.getDeclaredFields())
				.collect(Collectors.toMap(Field::getName, field -> field));

		configKeys.stream()
				.filter(classFields::containsKey)
				.forEach(name -> {
					try {
						Field field = classFields.get(name);
						field.setAccessible(true);
						field.set(object, valueResolver.apply(name, field.getType()));
					} catch (IllegalAccessException e) {
						throw new ConfigurationException("Error while mapping configuration to class fields", e);
					}
				});
	}
}
