#version 330

in vec3 a_position;
in vec4 a_color;
in vec4 a_normal;

#if defined(LAND_LAND)
in vec3 a_position_blend;
in vec4 a_color_blend;
in vec4 a_normal_blend;
#endif

flat out vec4 v_color;

uniform float u_blend;

uniform mat4 u_projViewTrans;
uniform vec3 u_eye_position;

#if defined(WATER_WATER)
uniform vec2 u_worldTrans[64];
#endif

#if defined(WATER_LAND)
uniform vec4 u_water_color_from;
#endif
#if defined(LAND_WATER) || defined(WATER_WATER)
uniform vec4 u_water_color_to;
#endif

uniform float u_time;

uniform sampler2D u_displacement_texture;
uniform sampler2D u_normal_texture;

const vec3 lightDirection = normalize(vec3(0.4, 0.0, 1.0));
#if defined(WATER_WATER)
const float lodDst = 200;
const float lodDst2 = lodDst * lodDst;
#endif

void main() {
	float blend = u_blend;

	vec3 b_position;
	vec4 b_color;
	vec3 b_normal;
	#if defined(LAND_LAND)
	b_position = mix(a_position, a_position_blend, blend);
	b_color = mix(a_color, a_color_blend, blend);
	b_normal = normalize(mix(a_normal.xyz, a_normal_blend.xyz, blend));
	#elif defined(LAND_WATER)
	b_position = mix(a_position, vec3(a_position.xy, -1.0), blend);
	b_color = mix(a_color, u_water_color_to, blend);
	b_normal = normalize(mix(a_normal.xyz, vec3(0.0, 0.0, 1.0), blend));
	#elif defined(WATER_LAND)
	b_position = mix(vec3(a_position.xy, -1.0), a_position, blend);
	b_color = mix(u_water_color_from, a_color, blend);
	b_normal = normalize(mix(vec3(0.0, 0.0, 1.0), a_normal.xyz, blend));
	#else // WATER_WATER
	b_position = vec3(a_position.xy + u_worldTrans[gl_InstanceID], -1.0);
	b_color = mix(a_color, u_water_color_to, blend);
	b_normal = a_normal.xyz;//vec3(0.0, 0.0, 1.0);
	#endif

	float diffuse = dot(b_normal.xyz, lightDirection);
	v_color = vec4(b_color.rgb * mix(0.6, 1.0, diffuse), 1.0);

	vec4 pos = vec4(b_position, 1.0);

	if (pos.z <= 0.0) {
		// General
		vec2 oceanSamplePos = (pos.xy + vec2(u_time)) * 0.01;
		float displacement = texture(u_displacement_texture, oceanSamplePos).x;

		// Position
		#if defined(WATER_WATER)
		{
			vec3 dst = pos.xyz - u_eye_position.xyz;
			// To prevent sky showing through in distant LODs, flatten the surface
			if (dot(dst, dst) < lodDst2) {
				float mixFactor = sqrt(clamp(-pos.z, 0.0, 1.0));
				float heightDisplacement = displacement - 1.0;
				pos.z = mix(0.0, heightDisplacement, mixFactor);
			}
		}
		#else
		{
			float mixFactor = sqrt(clamp(-pos.z, 0.0, 1.0));
			float heightDisplacement = displacement - 1.0;
			pos.z = mix(0.0, heightDisplacement, mixFactor);
		}
		#endif

		// Color
		vec3 normal = normalize(texture(u_normal_texture, oceanSamplePos).xyz);
		vec3 cameraToHere = normalize(u_eye_position - pos.xyz);
		vec3 reflection = reflect(-lightDirection, normal);
		float specularBase = dot(reflection, cameraToHere);
		float specular = 0.0;
		if (specularBase > 0.0) {
			specular = pow(specularBase, 20.0);
		}

		float colorDisplacement = pow(displacement, 2.0);

		v_color.rgb += vec3(specular + colorDisplacement * 0.5);
	}

	gl_Position = u_projViewTrans * pos;
}
