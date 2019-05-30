#version 330

in vec3 a_position;
in vec4 a_color;

flat out vec4 v_color;

uniform mat4 u_projViewTrans;
uniform mat4 u_modelTrans;

uniform vec3 u_animalCenter;
uniform float u_time;
uniform sampler2D u_displacement_texture;

uniform float u_animalSubmerge;

void main() {
	v_color = a_color;
	vec3 pos = a_position;

	if (u_animalCenter.z < 0.0) {
		// General
		vec2 oceanSamplePos = (u_animalCenter.xy + vec2(u_time)) * 0.01;
		float displacement = texture(u_displacement_texture, oceanSamplePos).x;

		// Position
		float mixFactor = sqrt(clamp(-u_animalCenter.z, 0.0, 1.0));
		float heightDisplacement = displacement - 1.0;
		float newAnimalCenterZ = mix(0.0, heightDisplacement, mixFactor);
		pos.z += newAnimalCenterZ - u_animalCenter.z;

		pos.z += u_animalCenter.z * u_animalSubmerge;
	}

	gl_Position = u_projViewTrans * u_modelTrans * vec4(pos, 1.0);
}
