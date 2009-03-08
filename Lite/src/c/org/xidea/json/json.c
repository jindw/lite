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

#include "json.h"

#include <stdio.h>


struct json_value* json_get_by_key (struct json_object * thiz, const char* key){
	return NULL;
}
struct json_value* json_put_value (struct json_object * thiz, const char* key, struct json_value * value){
	return NULL;
}

struct json_value* json_get_by_index (struct json_array * thiz,int index){
	return NULL;
}

struct json_value* json_add_value (struct json_array * thiz,struct json_value * value){
	return NULL;
}



struct json_value* json_new_value (const enum json_type type){
	/* allocate memory to the new object */
	json_value *new_object = malloc (sizeof(json_value));
	if (new_object == NULL){
		return NULL;
	}
	/* initialize members */
	new_object->type = type;
	new_object->intValue = 0;
	return &new_object;
}


void json_free (struct json_value ** value){
	//	assert (value != NULL);
	//	assert ((*value) != NULL);
	//	switch ((*value)->type){
	//	case JSON_ARRAY:
	//		struct json_array* array = (*value)->array;
	//		int len = array->length;
	//		struct json_value values[] = array->values;
	//		int i = len;
	//		while (i--) {
	//			json_free(&(&(values)[i]));
	//		}
	//		int left = len/JSON_ARRAY_STEP;
	//		int size =left * JSON_ARRAY_STEP;
	//		if(size!=len){
	//			size+=JSON_ARRAY_STEP;
	//		}
	//		json_free((*values)[size]);/* 这个语法可能有错？？*/
	//		break;
	//	case JSON_OBJECT:
	//		struct json_object* array = (*value)->object;
	//		int len = array->length;
	//		struct json_key_value values[] = array->values;
	//		int i = len;
	//		while (i--) {
	//			struct json_key_value * kv = &(*values)[i];
	//			free(kv->key);
	//			json_free(kv->value);
	//			free(kv);
	//		}
	//		int left = len/JSON_ARRAY_STEP;
	//		int size =left * JSON_ARRAY_STEP;
	//		if(size!=len){
	//			size+=JSON_ARRAY_STEP;
	//		}
	//		json_free((*values)[size]);/* 这个语法可能有错？？*/
	//		break;
	//	case JSON_STRING:
	//		free ((*value)->string);
	//		break;
	//	}
	//	free (*value);		/* the json value */
	//	(*value) = NULL;
}
