/*
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * @author jindw@xidea.org
 *
 */

#ifndef JSON_H
#define JSON_H

#ifdef __cplusplus
extern "C" {
#endif

#define JSON_MAX_STRING_LENGTH SIZE_MAX-1
#define JSON_ARRAY_STEP 8

enum json_type {
	JSON_INT = 0,
	JSON_FLOAT,
	JSON_BOOLEAN,
	JSON_STRING,
	JSON_OBJECT,
	JSON_ARRAY,
	JSON_NULL
};

typedef struct json_value {
	enum json_type type;
	union {
		int int_value;
		int intValue;
		float float_value;
		float floatValue;
		bool boolean_value;
		bool booleanValue;
		string* string;
		json_object* object;
		json_array* array;
	};
};
typedef struct json_key_value {
	string* key;
	json_value* value;
};
typedef struct json_object {
	int length;
	json_key_value* values[];
};
typedef struct json_array {
	int length;
	json_value* values[];
};

json_value* json_get_value (json_object * object,string *key);

json_value* json_put_value (json_object * object,string *key, json_object * object);

json_value* json_get_value (json_object * object,int index);

json_value* json_add_value (json_object * object,json_object * object);

json_value* json_new(json_type type);

void json_free (json_object * object);

#ifdef __cplusplus
}
#endif
#endif
