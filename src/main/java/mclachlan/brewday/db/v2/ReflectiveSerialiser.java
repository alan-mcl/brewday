package mclachlan.brewday.db.v2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import mclachlan.brewday.BrewdayException;

/**
 *
 */
public class ReflectiveSerialiser<E extends V2DataObject> implements V2SerialiserMap<E>
{
	private Class<E> clazz;
	private List<String> fields;
	private Map<Class, V2SerialiserObject> customSerialisers;

	/*-------------------------------------------------------------------------*/
	public ReflectiveSerialiser(Class<E> clazz, String... fields)
	{
		this.clazz = clazz;
		this.fields = Arrays.asList(fields);
		customSerialisers = new HashMap<>();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(E e)
	{
		try
		{
			Map<String, Object> result = new HashMap<>();

			for (String field : fields)
			{
				Method method = null;
				try
				{
					method = clazz.getMethod("get" + getMethodSuffix(field));
				}
				catch (NoSuchMethodException ex)
				{
					method = clazz.getMethod("is"+getMethodSuffix(field));
				}
				catch (SecurityException ex)
				{
					throw new RuntimeException(ex);
				}

				Object value = method.invoke(e);

				if (value == null)
				{
					result.put(field, value == null ? "" : value);
				}
				else
				{
					V2SerialiserObject customSerialiser = customSerialisers.get(value.getClass());

					if (customSerialiser != null)
					{
						result.put(field, customSerialiser.toObj(value));
					}
					else
					{
						result.put(field, value == null ? "" : value.toString());
					}
				}
			}

			return result;
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex)
		{
			throw new BrewdayException(ex);
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public E fromMap(Map<String, ?> map)
	{
		try
		{
			E result = clazz.newInstance();

			for (String field : fields)
			{
				String setMethodName = "set" + getMethodSuffix(field);
				Method[] methods = clazz.getMethods();
				Method setMethod = null;
				for (Method m : methods)
				{
					if (m.getName().equals(setMethodName) && m.getParameterTypes().length == 1)
					{
						setMethod = m;
						break;
					}
				}

				Object value = map.get(field);
				Class parameterType = setMethod.getParameterTypes()[0];

				try
				{
					if (parameterType == String.class)
					{
						setMethod.invoke(result, (String)value);
					}
					else if (parameterType == Integer.class || parameterType == int.class)
					{
						setMethod.invoke(result, Integer.valueOf((String)value));
					}
					else if (parameterType == Short.class|| parameterType == short.class)
					{
						setMethod.invoke(result, Short.valueOf((String)value));
					}
					else if (parameterType == Byte.class|| parameterType == byte.class)
					{
						setMethod.invoke(result, Byte.valueOf((String)value));
					}
					else if (parameterType == Double.class || parameterType == double.class)
					{
						setMethod.invoke(result, Double.valueOf((String)value));
					}
					else if (parameterType == Float.class|| parameterType == float.class)
					{
						setMethod.invoke(result, Float.valueOf((String)value));
					}
					else if (parameterType == Boolean.class || parameterType == boolean.class)
					{
						setMethod.invoke(result, Boolean.valueOf((String)value));
					}
					else if (parameterType == Character.class || parameterType == char.class)
					{
						setMethod.invoke(result, Character.valueOf(value.toString().charAt(0)));
					}
					else if (Enum.class.isAssignableFrom(parameterType))
					{
						setMethod.invoke(result, Enum.valueOf(parameterType, (String)value));
					}
					else
					{
						V2SerialiserObject customSerialiser = customSerialisers.get(parameterType);

						if (customSerialiser != null)
						{
							// honestly this probably won't work
							Object val = customSerialiser.fromObj(value);
							setMethod.invoke(result, parameterType.cast(val));
						}
						else
						{
							setMethod.invoke(result, value);
						}
					}
				}
				catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
				{
					throw new BrewdayException("Error setting field [" +field+
						"] paramType [" +parameterType+
						"] setMethod [" +setMethod+
						"]", e);
				}
			}

			return result;
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	private String getMethodSuffix(String field)
	{
		return field.substring(0, 1).toUpperCase() + field.substring(1);
	}

	/*-------------------------------------------------------------------------*/
	public void addCustomSerialiser(
		Class<?> clazz,
		V2SerialiserObject<?> serialiser)
	{
		this.customSerialisers.put(clazz, serialiser);
	}

}
